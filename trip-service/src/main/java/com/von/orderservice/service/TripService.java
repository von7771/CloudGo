package com.von.orderservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.von.common.dto.DriverLocationDto;
import com.von.common.enums.TripMode;
import com.von.common.enums.TripStatus;
import com.von.orderservice.entity.CarpoolPool;
import com.von.orderservice.entity.Trip;
import com.von.orderservice.entity.TripEvent;
import com.von.orderservice.exception.TripException;
import com.von.orderservice.feign.DriverFeignClient;
import com.von.orderservice.feign.PassengerFeignClient;
import com.von.orderservice.map.AmapService;
import com.von.orderservice.map.dto.DrivingRouteResult;
import com.von.orderservice.mapper.TripEventMapper;
import com.von.orderservice.mapper.TripMapper;
import com.von.orderservice.event.TripEventPublisher;
import com.von.orderservice.notify.TripNotificationPublisher;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripService {

    private static final int MIN_CREDIT = 60;
    private static final BigDecimal CARPOOL_DISCOUNT = new BigDecimal("0.85");
    private static final Set<String> TRACKABLE_STATUSES = Set.of(
            TripStatus.ACCEPTED.name(),
            TripStatus.ARRIVED.name(),
            TripStatus.IN_PROGRESS.name()
    );

    private final TripMapper tripMapper;
    private final TripEventMapper tripEventMapper;
    private final PassengerFeignClient passengerFeignClient;
    private final DriverFeignClient driverFeignClient;
    private final AmapService amapService;
    private final StringRedisTemplate redisTemplate;
    private final TripNotificationPublisher tripNotificationPublisher;
    private final TripEventPublisher tripEventPublisher;
    private final CarpoolService carpoolService;

    public TripService(TripMapper tripMapper,
                       TripEventMapper tripEventMapper,
                       PassengerFeignClient passengerFeignClient,
                       DriverFeignClient driverFeignClient,
                       AmapService amapService,
                       StringRedisTemplate redisTemplate,
                       TripNotificationPublisher tripNotificationPublisher,
                       TripEventPublisher tripEventPublisher,
                       CarpoolService carpoolService) {
        this.tripMapper = tripMapper;
        this.tripEventMapper = tripEventMapper;
        this.passengerFeignClient = passengerFeignClient;
        this.driverFeignClient = driverFeignClient;
        this.amapService = amapService;
        this.redisTemplate = redisTemplate;
        this.tripNotificationPublisher = tripNotificationPublisher;
        this.tripEventPublisher = tripEventPublisher;
        this.carpoolService = carpoolService;
    }

    public Trip createTrip(Long passengerId, String startPoint, String endPoint) {
        return createTrip(passengerId, startPoint, endPoint, TripMode.SOLO.name());
    }

    /** P1: 乘客发单 — 算价、校验余额，不扣款；独享或拼车 */
    public Trip createTrip(Long passengerId, String startPoint, String endPoint, String tripMode) {
        if (!StringUtils.hasText(startPoint) || !StringUtils.hasText(endPoint)) {
            throw new TripException("请填写起点和终点");
        }

        DrivingRouteResult route = amapService.planDrivingRoute(startPoint.trim(), endPoint.trim());
        if (TripMode.CARPOOL.name().equalsIgnoreCase(tripMode)) {
            return createCarpoolTrip(passengerId, route);
        }
        return createSoloTrip(passengerId, route);
    }

    private Trip createSoloTrip(Long passengerId, DrivingRouteResult route) {
        BigDecimal estimated = route.estimatedFare();
        validatePassengerCanOrder(passengerId, estimated);

        Trip trip = new Trip();
        trip.setPassengerId(passengerId);
        trip.setTripMode(TripMode.SOLO.name());
        trip.setStartPoint(route.originAddress());
        trip.setEndPoint(route.destinationAddress());
        trip.setStartLocation(route.originLocation());
        trip.setEndLocation(route.destinationLocation());
        trip.setEstimatedAmount(estimated);
        trip.setDistanceMeters(route.distanceMeters());
        trip.setDurationSeconds(route.durationSeconds());
        trip.setStatus(TripStatus.CREATED.name());
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        tripMapper.insert(trip);

        recordEvent(trip, TripStatus.CREATED.name(), TripStatus.CREATED.name(), "PASSENGER", "乘客发单（独享）");
        dispatchTrip(trip);
        return tripMapper.selectById(trip.getId());
    }

    private Trip createCarpoolTrip(Long passengerId, DrivingRouteResult route) {
        BigDecimal estimated = route.estimatedFare()
                .multiply(CARPOOL_DISCOUNT)
                .setScale(2, RoundingMode.HALF_UP);
        validatePassengerCanOrder(passengerId, estimated);

        Trip trip = carpoolService.createCarpoolTrip(passengerId, route);
        recordEvent(trip, TripStatus.CREATED.name(), trip.getStatus(), "PASSENGER", "拼车发单，预估 " + estimated + " 元");

        if (TripStatus.DISPATCHING.name().equals(trip.getStatus())) {
            redisTemplate.opsForSet().add("trip:dispatching", String.valueOf(trip.getId()));
            recordEvent(trip, TripStatus.CREATED.name(), TripStatus.DISPATCHING.name(), "SYSTEM", "拼车单进入派单");
        }
        return tripMapper.selectById(trip.getId());
    }

    private void validatePassengerCanOrder(Long passengerId, BigDecimal estimated) {
        Integer credit = passengerFeignClient.getPassengerCredit(passengerId);
        if (credit == null || credit < MIN_CREDIT) {
            throw new TripException("信用分不足，当前分: " + (credit == null ? "无" : credit));
        }
        Boolean sufficient = passengerFeignClient.checkBalance(passengerId, estimated);
        if (!Boolean.TRUE.equals(sufficient)) {
            throw new TripException("余额不足以支付预估车费 " + estimated + " 元，请先充值");
        }
    }

    private void dispatchTrip(Trip trip) {
        Set<String> onlineDrivers = redisTemplate.opsForSet().members("driver:online");
        if (onlineDrivers == null || onlineDrivers.isEmpty()) {
            recordEvent(trip, TripStatus.CREATED.name(), TripStatus.DISPATCHING.name(), "SYSTEM", "暂无在线司机，仍进入派单池");
        } else {
            recordEvent(trip, TripStatus.CREATED.name(), TripStatus.DISPATCHING.name(), "SYSTEM",
                    "已推送给 " + onlineDrivers.size() + " 位在线司机");
        }
        trip.setStatus(TripStatus.DISPATCHING.name());
        trip.setUpdatedAt(LocalDateTime.now());
        tripMapper.updateById(trip);
        redisTemplate.opsForSet().add("trip:dispatching", String.valueOf(trip.getId()));
        tripNotificationPublisher.publishNewTrip(trip);
    }

    public Trip getTrip(Long tripId) {
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw new TripException("行程不存在: " + tripId);
        }
        return trip;
    }

    public Map<String, Object> getPoolStatus(Long passengerId, Long tripId) {
        Trip trip = getTrip(tripId);
        if (!passengerId.equals(trip.getPassengerId())) {
            throw new TripException("无权查看此行程");
        }
        if (trip.getPoolId() == null) {
            throw new TripException("此行程不是拼车单");
        }
        if (TripStatus.POOL_WAITING.name().equals(trip.getStatus())) {
            carpoolService.ensurePoolDispatched(trip.getPoolId());
            trip = tripMapper.selectById(tripId);
        }
        CarpoolPool pool = carpoolService.getPool(trip.getPoolId());
        if (pool == null) {
            throw new TripException("拼车队列不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("poolId", pool.getId());
        result.put("status", pool.getStatus());
        result.put("currentSeats", pool.getCurrentSeats());
        result.put("maxSeats", pool.getMaxSeats());
        result.put("endPoint", pool.getEndPoint());
        result.put("tripStatus", trip.getStatus());
        result.put("driverId", trip.getDriverId());
        List<Map<String, Object>> members = tripMapper.selectByPoolId(pool.getId()).stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("tripId", t.getId());
                    m.put("startPoint", t.getStartPoint());
                    m.put("endPoint", t.getEndPoint());
                    m.put("status", t.getStatus());
                    m.put("isSelf", t.getId().equals(tripId));
                    return m;
                })
                .toList();
        result.put("members", members);
        return result;
    }

    public List<Trip> listDriverActiveTrips(Long driverId) {
        return tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getDriverId, driverId)
                .in(Trip::getStatus,
                        TripStatus.ACCEPTED.name(),
                        TripStatus.ARRIVED.name(),
                        TripStatus.IN_PROGRESS.name())
                .orderByAsc(Trip::getId));
    }

    public List<Trip> listPassengerTrips(Long passengerId) {
        return tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getPassengerId, passengerId)
                .orderByDesc(Trip::getId));
    }

    public DriverLocationDto getTripDriverLocation(Long passengerId, Long tripId) {
        Trip trip = getTrip(tripId);
        if (!passengerId.equals(trip.getPassengerId())) {
            throw new TripException("无权查看此行程");
        }
        if (trip.getDriverId() == null) {
            throw new TripException("行程尚未分配司机");
        }
        if (!TRACKABLE_STATUSES.contains(trip.getStatus())) {
            throw new TripException("当前行程状态不支持查看司机位置: " + trip.getStatus());
        }
        DriverLocationDto location = driverFeignClient.getDriverLocation(trip.getDriverId());
        if (location == null) {
            throw new TripException("司机暂未上报位置");
        }
        return location;
    }

    public void cancelTrip(Long passengerId, Long tripId) {
        Trip trip = getTrip(tripId);
        if (!passengerId.equals(trip.getPassengerId())) {
            throw new TripException("无权取消此行程");
        }
        String status = trip.getStatus();
        if (!TripStatus.CREATED.name().equals(status)
                && !TripStatus.DISPATCHING.name().equals(status)
                && !TripStatus.POOL_WAITING.name().equals(status)) {
            throw new TripException("当前状态不可取消: " + status);
        }
        int rows = tripMapper.updateStatus(tripId, status, TripStatus.CANCELLED.name());
        if (rows == 0) {
            throw new TripException("取消失败，行程状态已变更");
        }
        redisTemplate.opsForSet().remove("trip:dispatching", String.valueOf(tripId));
        if (trip.getPoolId() != null) {
            carpoolService.onPoolTripCancelled(trip);
        }
        recordEvent(trip, status, TripStatus.CANCELLED.name(), "PASSENGER", "乘客取消");
    }

    public List<Trip> listPendingTrips() {
        return tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getStatus, TripStatus.DISPATCHING.name())
                .isNull(Trip::getDriverId)
                .orderByAsc(Trip::getId));
    }

    /** 智能拼车包批量接单（最多 3 单，同拼车池只接一次） */
    public List<Trip> acceptSmartBundle(Long driverId, List<Long> tripIds) {
        if (tripIds == null || tripIds.isEmpty()) {
            throw new TripException("请选择要接的订单");
        }
        if (tripIds.size() > 3) {
            throw new TripException("智能拼车包最多 3 单");
        }
        List<Trip> accepted = new java.util.ArrayList<>();
        Set<Long> processedPools = new java.util.HashSet<>();
        for (Long tripId : tripIds) {
            Trip preview = getTrip(tripId);
            if (preview.getPoolId() != null) {
                if (!processedPools.add(preview.getPoolId())) {
                    continue;
                }
            }
            for (Trip t : acceptTripExpanded(driverId, tripId)) {
                if (accepted.stream().noneMatch(a -> a.getId().equals(t.getId()))) {
                    accepted.add(t);
                }
            }
        }
        if (accepted.isEmpty()) {
            throw new TripException("接单失败，订单可能已被接走");
        }
        return accepted;
    }

    /** 接单并返回本单及同池已接订单（拼车池一次接全部） */
    public List<Trip> acceptTripExpanded(Long driverId, Long tripId) {
        Trip accepted = acceptTrip(driverId, tripId);
        if (accepted.getPoolId() != null) {
            return tripMapper.selectByPoolId(accepted.getPoolId()).stream()
                    .filter(t -> driverId.equals(t.getDriverId()))
                    .filter(t -> !TripStatus.DISPATCHING.name().equals(t.getStatus()))
                    .toList();
        }
        return List.of(accepted);
    }

    public Trip acceptTrip(Long driverId, Long tripId) {
        if (!Boolean.TRUE.equals(driverFeignClient.isOnline(driverId))) {
            throw new TripException("司机未上线，无法接单");
        }
        Trip preview = getTrip(tripId);
        if (preview.getPoolId() != null) {
            carpoolService.ensurePoolDispatched(preview.getPoolId());
        }
        int rows = tripMapper.acceptTrip(tripId, driverId, TripStatus.DISPATCHING.name(), TripStatus.ACCEPTED.name());
        if (rows == 0) {
            throw new TripException("接单失败，行程已被其他司机接走或已取消");
        }
        redisTemplate.opsForSet().remove("trip:dispatching", String.valueOf(tripId));
        Trip accepted = getTrip(tripId);
        recordEvent(accepted, TripStatus.DISPATCHING.name(), TripStatus.ACCEPTED.name(), "DRIVER", "司机接单 driverId=" + driverId);

        if (accepted.getPoolId() != null) {
            carpoolService.acceptPoolTrips(accepted.getPoolId(), driverId);
            for (Trip poolTrip : tripMapper.selectByPoolId(accepted.getPoolId())) {
                redisTemplate.opsForSet().remove("trip:dispatching", String.valueOf(poolTrip.getId()));
                if (!poolTrip.getId().equals(tripId) && TripStatus.ACCEPTED.name().equals(poolTrip.getStatus())) {
                    recordEvent(poolTrip, TripStatus.DISPATCHING.name(), TripStatus.ACCEPTED.name(), "DRIVER",
                            "拼车同单接单 driverId=" + driverId);
                }
            }
        }
        return getTrip(tripId);
    }

    public Trip arriveTrip(Long driverId, Long tripId) {
        Trip trip = getTrip(tripId);
        assertDriver(trip, driverId, TripStatus.ACCEPTED.name());
        transition(trip, TripStatus.ACCEPTED.name(), TripStatus.ARRIVED.name(), "DRIVER", "司机到达上车点");
        return getTrip(tripId);
    }

    public Trip startTrip(Long driverId, Long tripId) {
        Trip trip = getTrip(tripId);
        assertDriver(trip, driverId, TripStatus.ARRIVED.name());
        transition(trip, TripStatus.ARRIVED.name(), TripStatus.IN_PROGRESS.name(), "DRIVER", "行程开始");
        return getTrip(tripId);
    }

    @GlobalTransactional(name = "complete-trip-tx", rollbackFor = Exception.class)
    public Trip completeTrip(Long driverId, Long tripId) {
        Trip trip = getTrip(tripId);
        assertDriver(trip, driverId, TripStatus.IN_PROGRESS.name());
        Trip completed = completeSingleTrip(driverId, trip);
        if (trip.getPoolId() != null) {
            boolean allDone = tripMapper.selectByPoolId(trip.getPoolId()).stream()
                    .allMatch(t -> TripStatus.COMPLETED.name().equals(t.getStatus())
                            || TripStatus.CANCELLED.name().equals(t.getStatus()));
            if (allDone) {
                carpoolService.markPoolCompleted(trip.getPoolId());
            }
        }
        return completed;
    }

    private Trip completeSingleTrip(Long driverId, Trip trip) {
        BigDecimal finalAmount = trip.getEstimatedAmount();
        passengerFeignClient.deductBalance(trip.getPassengerId(), finalAmount);
        driverFeignClient.creditBalance(driverId, finalAmount);

        Trip update = new Trip();
        update.setId(trip.getId());
        update.setFinalAmount(finalAmount);
        update.setStatus(TripStatus.COMPLETED.name());
        update.setUpdatedAt(LocalDateTime.now());
        tripMapper.updateById(update);

        Trip completed = getTrip(trip.getId());
        recordEvent(completed, TripStatus.IN_PROGRESS.name(), TripStatus.COMPLETED.name(), "DRIVER",
                "完单扣款 " + finalAmount + " 元");
        return completed;
    }

    public Trip rateTrip(Long passengerId, Long tripId, Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new TripException("评分需在 1-5 之间");
        }
        Trip trip = getTrip(tripId);
        if (!passengerId.equals(trip.getPassengerId())) {
            throw new TripException("无权评价此行程");
        }
        if (!TripStatus.COMPLETED.name().equals(trip.getStatus())) {
            throw new TripException("仅已完成行程可评价");
        }
        Trip update = new Trip();
        update.setId(tripId);
        update.setPassengerRating(rating);
        update.setUpdatedAt(LocalDateTime.now());
        tripMapper.updateById(update);
        Trip rated = getTrip(tripId);
        recordEvent(rated, TripStatus.COMPLETED.name(), TripStatus.COMPLETED.name(), "PASSENGER", "评分 " + rating);
        return rated;
    }

    private void transition(Trip trip, String from, String to, String operator, String remark) {
        int rows = tripMapper.updateStatus(trip.getId(), from, to);
        if (rows == 0) {
            throw new TripException("状态变更失败，当前可能不是 " + from);
        }
        trip.setStatus(to);
        recordEvent(trip, from, to, operator, remark);
    }

    private void assertDriver(Trip trip, Long driverId, String expectedStatus) {
        if (!driverId.equals(trip.getDriverId())) {
            throw new TripException("无权操作此行程");
        }
        if (!expectedStatus.equals(trip.getStatus())) {
            throw new TripException("行程状态不正确，期望 " + expectedStatus + " 实际 " + trip.getStatus());
        }
    }

    private void recordEvent(Trip trip, String from, String to, String operator, String remark) {
        TripEvent event = new TripEvent();
        event.setTripId(trip.getId());
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setOperator(operator);
        event.setRemark(remark);
        event.setCreatedAt(LocalDateTime.now());
        tripEventMapper.insert(event);
        tripEventPublisher.publish(trip, from, to, operator, remark);
    }
}
