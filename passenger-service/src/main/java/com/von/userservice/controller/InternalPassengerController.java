package com.von.userservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.von.common.dto.PageResult;
import com.von.common.dto.PassengerSummaryDto;
import com.von.userservice.entity.Passenger;
import com.von.userservice.support.PassengerDtoMapper;
import com.von.userservice.sentinel.UserSentinelBlockHandler;
import com.von.userservice.service.PassengerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 乘客服务内部接口（供 trip-service 通过 Feign 调用）。
 * <p>
 * 路径前缀：{@code /api/internal/passenger/**} → passenger-service<br>
 * Gateway 对内部路径不做 JWT 校验，仅限微服务间调用，不要暴露给公网。
 * </p>
 */
@RestController
@RequestMapping("/api/internal/passenger")
public class InternalPassengerController {

    private final PassengerService passengerService;

    public InternalPassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    /**
     * 查询乘客信用分（发单前校验，需 ≥ 60）。
     * <p>
     * <b>请求</b>：{@code GET /api/internal/passenger/credit?passengerId=1}<br>
     * <b>调用方</b>：trip-service Feign<br>
     * <b>限流</b>：Sentinel 资源名 getUserCredit
     * </p>
     *
     * @param passengerId 乘客 ID
     * @return 信用分整数；用户不存在返回 0
     */
    @GetMapping("/credit")
    @SentinelResource(value = "getUserCredit", blockHandler = "creditBlockHandler", blockHandlerClass = UserSentinelBlockHandler.class)
    public Integer getCreditLevel(@RequestParam("passengerId") Long passengerId) {
        return passengerService.getCreditLevel(passengerId);
    }

    /**
     * 校验乘客余额是否 ≥ 指定金额（发单时校验，不扣款）。
     * <p>
     * <b>请求</b>：{@code GET /api/internal/passenger/balance/check?passengerId=1&amount=35.50}<br>
     * <b>调用方</b>：trip-service 发单前调用
     * </p>
     *
     * @param passengerId 乘客 ID
     * @param amount      预估车费（元）
     * @return true=余额足够，false=不足
     */
    @GetMapping("/balance/check")
    public Boolean checkBalance(
            @RequestParam("passengerId") Long passengerId,
            @RequestParam("amount") BigDecimal amount
    ) {
        return passengerService.hasSufficientBalance(passengerId, amount);
    }

    /**
     * 扣减乘客余额（完单时由 Seata 全局事务触发）。
     * <p>
     * <b>请求</b>：{@code POST /api/internal/passenger/deduct?passengerId=1&amount=35.50}<br>
     * <b>调用方</b>：trip-service 完单 {@code completeTrip} 时 Feign 调用<br>
     * <b>失败</b>：余额不足抛 InsufficientBalanceException，errorCode=INSUFFICIENT_BALANCE
     * </p>
     *
     * @param passengerId 乘客 ID
     * @param amount      实际扣款金额（元）
     * @return true 表示扣款成功
     */
    @PostMapping("/deduct")
    @SentinelResource(value = "deductBalance", blockHandler = "deductBlockHandler", blockHandlerClass = UserSentinelBlockHandler.class)
    public Boolean deductBalance(
            @RequestParam("passengerId") Long passengerId,
            @RequestParam("amount") BigDecimal amount
    ) {
        passengerService.deductBalance(passengerId, amount);
        return true;
    }

    @GetMapping("/list")
    public PageResult<PassengerSummaryDto> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResult<Passenger> pageResult = passengerService.listPassengers(page, size);
        return new PageResult<>(
                pageResult.records().stream().map(PassengerDtoMapper::toSummary).toList(),
                pageResult.total(),
                pageResult.page(),
                pageResult.size()
        );
    }

    @PutMapping("/{passengerId}/ban")
    public Map<String, Object> ban(
            @PathVariable Long passengerId,
            @RequestParam("banned") boolean banned
    ) {
        passengerService.banPassenger(passengerId, banned);
        return Map.of("passengerId", passengerId, "banned", banned);
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("totalCount", passengerService.countPassengers());
    }
}
