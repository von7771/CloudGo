package com.von.orderservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.von.common.api.ApiResponse;
import com.von.common.dto.SmartCarpoolBundleDto;
import com.von.common.security.JwtConstants;
import com.von.orderservice.carpool.SmartCarpoolMatchService;
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
 * 司机端行程接口（轮询待接单、接单、行程状态推进）。
 * <p>
 * 经 Gateway 访问：{@code http://localhost:8080/api/driver/trips/**} → trip-service<br>
 * 均需司机 Bearer Token（{@code /pending} 也建议携带，便于后续扩展权限）。
 * </p>
 */
@RestController
@RequestMapping("/api/driver/trips")
public class DriverTripController {

    private final TripService tripService;
    private final SmartCarpoolMatchService smartCarpoolMatchService;

    public DriverTripController(TripService tripService, SmartCarpoolMatchService smartCarpoolMatchService) {
        this.tripService = tripService;
        this.smartCarpoolMatchService = smartCarpoolMatchService;
    }

    @GetMapping("/smart-bundles")
    public ApiResponse<List<SmartCarpoolBundleDto>> smartBundles(
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        return ApiResponse.ok(smartCarpoolMatchService.findSmartBundles(limit));
    }

    /**
     * 一键接智能拼车包（最多 3 单，先接先得）。
     */
    @PostMapping("/smart-bundles/accept")
    public ApiResponse<List<Trip>> acceptSmartBundle(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @RequestParam("tripIds") List<Long> tripIds
    ) {
        return ApiResponse.ok("接单成功", tripService.acceptSmartBundle(driverId, tripIds));
    }

    /**
     * 轮询待接单列表（status = DISPATCHING 的行程）。
     * <p>
     * <b>请求</b>：{@code GET /api/driver/trips/pending}<br>
     * <b>鉴权</b>：建议携带司机 Bearer Token<br>
     * <b>前置</b>：司机需先 {@code POST /api/driver/online} 上线<br>
     * <b>说明</b>：MVP 返回全部派单中行程，司机抢单模式（先接先得）
     * </p>
     *
     * @return 待接单行程列表
     */
    @GetMapping("/pending")
    public ApiResponse<List<Trip>> pending() {
        return ApiResponse.ok(tripService.listPendingTrips());
    }

    @GetMapping("/active")
    public ApiResponse<List<Trip>> active(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId
    ) {
        return ApiResponse.ok(tripService.listDriverActiveTrips(driverId));
    }

    /**
     * 司机接单（乐观锁，仅一位司机能成功）。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/trips/{tripId}/accept}<br>
     * <b>鉴权</b>：需要司机 Bearer Token<br>
     * <b>示例</b>：{@code POST .../trips/1/accept}（1 为发单返回的 data.id）<br>
     * <b>状态流转</b>：DISPATCHING → ACCEPTED<br>
     * <b>前置</b>：司机已上线；tripId 必须是真实数字，不能写 {@code {tripId}}
     * </p>
     *
     * @param driverId 司机 ID（JWT 自动注入）
     * @param tripId   行程 ID（路径变量）
     */
    @PostMapping("/{tripId}/accept")
    @SentinelResource(value = "acceptTrip", blockHandler = "acceptTripBlockHandler", blockHandlerClass = TripSentinelBlockHandler.class)
    public ApiResponse<List<Trip>> accept(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @PathVariable Long tripId
    ) {
        return ApiResponse.ok("接单成功", tripService.acceptTripExpanded(driverId, tripId));
    }

    /**
     * 司机到达上车点。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/trips/{tripId}/arrive}<br>
     * <b>状态流转</b>：ACCEPTED → ARRIVED
     * </p>
     *
     * @param driverId 司机 ID（JWT 自动注入）
     * @param tripId   行程 ID
     */
    @PostMapping("/{tripId}/arrive")
    public ApiResponse<Trip> arrive(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @PathVariable Long tripId
    ) {
        return ApiResponse.ok("已到达上车点", tripService.arriveTrip(driverId, tripId));
    }

    /**
     * 开始行程（乘客上车）。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/trips/{tripId}/start}<br>
     * <b>状态流转</b>：ARRIVED → IN_PROGRESS
     * </p>
     *
     * @param driverId 司机 ID（JWT 自动注入）
     * @param tripId   行程 ID
     */
    @PostMapping("/{tripId}/start")
    public ApiResponse<Trip> start(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @PathVariable Long tripId
    ) {
        return ApiResponse.ok("行程已开始", tripService.startTrip(driverId, tripId));
    }

    /**
     * 完单（触发 Seata 全局事务：扣乘客余额 + 司机入账）。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/trips/{tripId}/complete}<br>
     * <b>状态流转</b>：IN_PROGRESS → COMPLETED<br>
     * <b>扣款</b>：此时才从乘客钱包扣款，发单时不扣<br>
     * <b>失败</b>：乘客余额不足时整单回滚，行程不会变为 COMPLETED
     * </p>
     *
     * @param driverId 司机 ID（JWT 自动注入）
     * @param tripId   行程 ID
     */
    @PostMapping("/{tripId}/complete")
    public ApiResponse<Trip> complete(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @PathVariable Long tripId
    ) {
        return ApiResponse.ok("完单成功", tripService.completeTrip(driverId, tripId));
    }
}
