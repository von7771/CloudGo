package com.von.orderservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.von.common.api.ApiResponse;
import com.von.common.dto.DriverLocationDto;
import com.von.common.security.JwtConstants;
import com.von.orderservice.entity.Trip;
import com.von.orderservice.sentinel.TripSentinelBlockHandler;
import com.von.orderservice.service.TripService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 乘客端行程接口（发单、查询、取消、评价）。
 * <p>
 * 经 Gateway 访问：{@code http://localhost:8080/api/passenger/trips/**} → trip-service<br>
 * 除地图接口外，均需乘客 Bearer Token。Gateway 注入 {@code X-User-Id} 作为 passengerId。
 * </p>
 */
@RestController
@RequestMapping("/api/passenger/trips")
public class PassengerTripController {

    private final TripService tripService;

    public PassengerTripController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * 乘客发单（调用高德算价，校验信用分与余额，不扣款）。
     * <p>
     * <b>请求</b>：{@code POST /api/passenger/trips?startPoint=起点&endPoint=终点}<br>
     * <b>鉴权</b>：需要乘客 Bearer Token<br>
     * <b>示例</b>：{@code POST .../trips?startPoint=学苑路1号&endPoint=广州南站}<br>
     * <b>状态流转</b>：CREATED → DISPATCHING（进入派单池，等待司机接单）<br>
     * <b>注意</b>：返回 data.id 为 tripId，司机接单时 URL 里填此数字，不要写字面量 {@code {tripId}}
     * </p>
     *
     * @param passengerId 乘客 ID（JWT 自动注入）
     * @param startPoint  起点文字地址
     * @param endPoint    终点文字地址
     * @return 行程详情，含 id、status、estimatedAmount、distanceMeters 等
     */
    @PostMapping
    @SentinelResource(value = "createTrip", blockHandler = "createTripBlockHandler", blockHandlerClass = TripSentinelBlockHandler.class)
    public ApiResponse<Trip> create(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @RequestParam("startPoint") String startPoint,
            @RequestParam("endPoint") String endPoint,
            @RequestParam(value = "tripMode", defaultValue = "SOLO") String tripMode
    ) {
        return ApiResponse.ok("发单成功", tripService.createTrip(passengerId, startPoint, endPoint, tripMode));
    }

    @GetMapping("/{tripId}/pool")
    public ApiResponse<java.util.Map<String, Object>> poolStatus(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @PathVariable Long tripId
    ) {
        return ApiResponse.ok(tripService.getPoolStatus(passengerId, tripId));
    }

    /**
     * 查询当前乘客的历史行程列表。
     * <p>
     * <b>请求</b>：{@code GET /api/passenger/trips}<br>
     * <b>鉴权</b>：需要乘客 Bearer Token
     * </p>
     *
     * @param passengerId 乘客 ID（JWT 自动注入）
     * @return 行程列表，按 id 倒序
     */
    @GetMapping
    public ApiResponse<List<Trip>> history(@RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId) {
        return ApiResponse.ok(tripService.listPassengerTrips(passengerId));
    }

    /**
     * 查询单笔行程详情。
     * <p>
     * <b>请求</b>：{@code GET /api/passenger/trips/{tripId}}<br>
     * <b>鉴权</b>：需要乘客 Bearer Token，且只能查自己的行程
     * </p>
     *
     * @param passengerId 乘客 ID（JWT 自动注入）
     * @param tripId      行程 ID（路径变量，填真实数字如 1）
     */
    @GetMapping("/{tripId}")
    public ApiResponse<Trip> detail(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @PathVariable Long tripId
    ) {
        Trip trip = tripService.getTrip(tripId);
        if (!passengerId.equals(trip.getPassengerId())) {
            throw new com.von.orderservice.exception.TripException("无权查看此行程");
        }
        return ApiResponse.ok(trip);
    }

    /**
     * 查询本单司机实时位置（建议地图页每 60 秒轮询一次）。
     * <p>
     * <b>前置</b>：行程已接单（ACCEPTED / ARRIVED / IN_PROGRESS），且司机已上线并上报位置。
     * </p>
     */
    @GetMapping("/{tripId}/driver-location")
    public ApiResponse<DriverLocationDto> driverLocation(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @PathVariable Long tripId
    ) {
        return ApiResponse.ok(tripService.getTripDriverLocation(passengerId, tripId));
    }

    /**
     * 乘客取消行程（仅 CREATED / DISPATCHING 状态可取消）。
     * <p>
     * <b>请求</b>：{@code POST /api/passenger/trips/{tripId}/cancel}<br>
     * <b>鉴权</b>：需要乘客 Bearer Token<br>
     * <b>状态流转</b>：→ CANCELLED
     * </p>
     *
     * @param passengerId 乘客 ID（JWT 自动注入）
     * @param tripId      行程 ID
     */
    @PostMapping("/{tripId}/cancel")
    public ApiResponse<Void> cancel(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @PathVariable Long tripId
    ) {
        tripService.cancelTrip(passengerId, tripId);
        return ApiResponse.ok("取消成功", null);
    }

    /**
     * 乘客对已完成行程评分（完单后调用）。
     * <p>
     * <b>请求</b>：{@code POST /api/passenger/trips/{tripId}/rate?rating=5}<br>
     * <b>鉴权</b>：需要乘客 Bearer Token<br>
     * <b>前置</b>：行程 status 必须为 COMPLETED<br>
     * <b>参数</b>：rating 取值 1～5
     * </p>
     *
     * @param passengerId 乘客 ID（JWT 自动注入）
     * @param tripId      行程 ID
     * @param rating      评分 1-5 星
     */
    @PostMapping("/{tripId}/rate")
    public ApiResponse<Trip> rate(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @PathVariable Long tripId,
            @RequestParam("rating") Integer rating
    ) {
        return ApiResponse.ok("评价成功", tripService.rateTrip(passengerId, tripId, rating));
    }
}
