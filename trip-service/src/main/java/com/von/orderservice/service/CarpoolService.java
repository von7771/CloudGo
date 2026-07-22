package com.von.orderservice.service;

import com.von.common.enums.TripMode;
import com.von.common.enums.TripStatus;
import com.von.orderservice.carpool.RouteSimilarityService;
import com.von.orderservice.entity.CarpoolPool;
import com.von.orderservice.entity.Trip;
import com.von.orderservice.exception.TripException;
import com.von.orderservice.map.dto.DrivingRouteResult;
import com.von.orderservice.mapper.CarpoolPoolMapper;
import com.von.orderservice.mapper.TripMapper;
import com.von.orderservice.notify.TripNotificationPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarpoolService {

    private static final int MIN_CARPOOL_SEATS_TO_DISPATCH = 1;
    private static final BigDecimal CARPOOL_DISCOUNT = new BigDecimal("0.85");

    private final TripMapper tripMapper;
    private final CarpoolPoolMapper carpoolPoolMapper;
    private final TripNotificationPublisher tripNotificationPublisher;
    private final RouteSimilarityService routeSimilarityService;
    private final StringRedisTemplate redisTemplate;

    public CarpoolService(TripMapper tripMapper,
                          CarpoolPoolMapper carpoolPoolMapper,
                          TripNotificationPublisher tripNotificationPublisher,
                          RouteSimilarityService routeSimilarityService,
                          StringRedisTemplate redisTemplate) {
        this.tripMapper = tripMapper;
        this.carpoolPoolMapper = carpoolPoolMapper;
        this.tripNotificationPublisher = tripNotificationPublisher;
        this.routeSimilarityService = routeSimilarityService;
        this.redisTemplate = redisTemplate;
    }

    public Trip createCarpoolTrip(Long passengerId, DrivingRouteResult route) {
        BigDecimal discounted = route.estimatedFare().multiply(CARPOOL_DISCOUNT).setScale(2, RoundingMode.HALF_UP);
        String endKey = normalizeKey(route.destinationAddress());

        CarpoolPool pool = routeSimilarityService.findBestWaitingPool(route).orElse(null);
        if (pool == null) {
            pool = routeSimilarityService.findBestDispatchingPool(route).orElse(null);
        }

        boolean joiningDispatchingPool = pool != null && "DISPATCHING".equals(pool.getStatus());

        if (pool == null) {
            pool = new CarpoolPool();
            pool.setStatus("WAITING");
            pool.setEndPointKey(endKey);
            pool.setEndPoint(route.destinationAddress());
            pool.setMaxSeats(4);
            pool.setCurrentSeats(0);
            pool.setCreatedAt(LocalDateTime.now());
            carpoolPoolMapper.insert(pool);
        }

        Trip trip = buildTrip(passengerId, route, discounted, TripMode.CARPOOL.name(), pool.getId());
        trip.setStatus(joiningDispatchingPool
                ? TripStatus.DISPATCHING.name()
                : TripStatus.POOL_WAITING.name());
        tripMapper.insert(trip);

        int rows = carpoolPoolMapper.incrementSeats(pool.getId());
        if (rows == 0) {
            throw new TripException("拼车池已满，请稍后重试");
        }
        pool = carpoolPoolMapper.selectById(pool.getId());

        if (!joiningDispatchingPool && pool.getCurrentSeats() >= MIN_CARPOOL_SEATS_TO_DISPATCH) {
            dispatchPool(pool.getId());
            return tripMapper.selectById(trip.getId());
        }
        if (joiningDispatchingPool) {
            tripNotificationPublisher.publishNewTrip(tripMapper.selectById(trip.getId()));
        }
        return tripMapper.selectById(trip.getId());
    }

    /** 修复拼车池未派单或成员状态不一致（如仍显示「等待拼友」） */
    public void ensurePoolDispatched(Long poolId) {
        if (poolId == null) {
            return;
        }
        CarpoolPool pool = carpoolPoolMapper.selectById(poolId);
        if (pool == null) {
            return;
        }
        if ("WAITING".equals(pool.getStatus())
                && pool.getCurrentSeats() >= MIN_CARPOOL_SEATS_TO_DISPATCH) {
            dispatchPool(poolId);
            return;
        }
        if ("DISPATCHING".equals(pool.getStatus())) {
            syncPoolWaitingToDispatching(poolId);
        }
    }

    public void dispatchPool(Long poolId) {
        List<Trip> trips = tripMapper.selectByPoolId(poolId);
        if (trips.isEmpty()) {
            return;
        }
        carpoolPoolMapper.updateStatus(poolId, "DISPATCHING");
        for (Trip trip : trips) {
            promoteToDispatching(trip);
        }
    }

    private void syncPoolWaitingToDispatching(Long poolId) {
        for (Trip trip : tripMapper.selectByPoolId(poolId)) {
            promoteToDispatching(trip);
        }
    }

    private void promoteToDispatching(Trip trip) {
        if (!TripStatus.POOL_WAITING.name().equals(trip.getStatus())) {
            return;
        }
        trip.setStatus(TripStatus.DISPATCHING.name());
        trip.setUpdatedAt(LocalDateTime.now());
        tripMapper.updateById(trip);
        redisTemplate.opsForSet().add("trip:dispatching", String.valueOf(trip.getId()));
        tripNotificationPublisher.publishNewTrip(trip);
    }

    public void onPoolTripCancelled(Trip trip) {
        if (trip.getPoolId() == null) {
            return;
        }
        carpoolPoolMapper.decrementSeats(trip.getPoolId());
        CarpoolPool pool = carpoolPoolMapper.selectById(trip.getPoolId());
        if (pool != null && pool.getCurrentSeats() <= 0) {
            carpoolPoolMapper.updateStatus(trip.getPoolId(), "CANCELLED");
        }
    }

    public void acceptPoolTrips(Long poolId, Long driverId) {
        if (poolId == null) {
            return;
        }
        ensurePoolDispatched(poolId);
        List<Trip> trips = tripMapper.selectByPoolId(poolId);
        for (Trip t : trips) {
            if (!TripStatus.DISPATCHING.name().equals(t.getStatus()) || t.getDriverId() != null) {
                continue;
            }
            int rows = tripMapper.acceptTrip(t.getId(), driverId,
                    TripStatus.DISPATCHING.name(), TripStatus.ACCEPTED.name());
            if (rows > 0) {
                redisTemplate.opsForSet().remove("trip:dispatching", String.valueOf(t.getId()));
            }
        }
    }

    public CarpoolPool getPool(Long poolId) {
        return carpoolPoolMapper.selectById(poolId);
    }

    public void markPoolCompleted(Long poolId) {
        if (poolId != null) {
            carpoolPoolMapper.updateStatus(poolId, "COMPLETED");
        }
    }

    private Trip buildTrip(Long passengerId, DrivingRouteResult route, BigDecimal estimated, String mode, Long poolId) {
        Trip trip = new Trip();
        trip.setPassengerId(passengerId);
        trip.setTripMode(mode);
        trip.setPoolId(poolId);
        trip.setStartPoint(route.originAddress());
        trip.setEndPoint(route.destinationAddress());
        trip.setStartLocation(route.originLocation());
        trip.setEndLocation(route.destinationLocation());
        trip.setEstimatedAmount(estimated);
        trip.setDistanceMeters(route.distanceMeters());
        trip.setDurationSeconds(route.durationSeconds());
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        return trip;
    }

    private String normalizeKey(String endPoint) {
        if (!StringUtils.hasText(endPoint)) {
            throw new TripException("终点不能为空");
        }
        return endPoint.trim().toLowerCase();
    }
}
