package com.von.driver.controller;

import com.von.common.dto.DriverLocationDto;
import com.von.common.dto.DriverSummaryDto;
import com.von.common.dto.PageResult;
import com.von.driver.entity.Driver;
import com.von.driver.service.DriverService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 司机服务内部接口（供 trip-service 通过 Feign 调用）。
 * <p>
 * 路径前缀：{@code /api/internal/driver/**} → driver-service<br>
 * Gateway 不做 JWT 校验，仅供微服务间调用。
 * </p>
 */
@RestController
@RequestMapping("/api/internal/driver")
public class InternalDriverController {

    private final DriverService driverService;

    public InternalDriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * 查询司机是否在线（接单前校验）。
     * <p>
     * <b>请求</b>：{@code GET /api/internal/driver/online/check?driverId=1}<br>
     * <b>调用方</b>：trip-service 司机接单 {@code acceptTrip} 时调用
     * </p>
     *
     * @param driverId 司机 ID
     * @return true=在 Redis driver:online 集合中
     */
    @GetMapping("/online/check")
    public Boolean isOnline(@RequestParam("driverId") Long driverId) {
        return driverService.isOnline(driverId);
    }

    /**
     * 司机收入入账（完单时由 Seata 全局事务触发）。
     * <p>
     * <b>请求</b>：{@code POST /api/internal/driver/credit?driverId=1&amount=35.50}<br>
     * <b>调用方</b>：trip-service 完单 {@code completeTrip} 时 Feign 调用<br>
     * <b>说明</b>：与乘客扣款在同一 Seata 全局事务中，任一步失败则全部回滚
     * </p>
     *
     * @param driverId 司机 ID
     * @param amount   本单收入（元），当前等于乘客支付金额
     * @return true 表示入账成功
     */
    @PostMapping("/credit")
    public Boolean creditBalance(@RequestParam("driverId") Long driverId, @RequestParam("amount") BigDecimal amount) {
        driverService.creditBalance(driverId, amount);
        return true;
    }

    @GetMapping("/list")
    public PageResult<DriverSummaryDto> list(
            @RequestParam(value = "auditStatus", required = false) String auditStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResult<Driver> pageResult = driverService.listDrivers(auditStatus, page, size);
        return new PageResult<>(
                pageResult.records().stream().map(driverService::toSummaryDto).toList(),
                pageResult.total(),
                pageResult.page(),
                pageResult.size()
        );
    }

    @PutMapping("/{driverId}/audit")
    public DriverSummaryDto audit(
            @PathVariable Long driverId,
            @RequestParam("auditStatus") String auditStatus
    ) {
        return driverService.toSummaryDto(driverService.auditDriver(driverId, auditStatus));
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "onlineCount", driverService.countOnlineDrivers(),
                "pendingAuditCount", driverService.countPendingAudit()
        );
    }

    /**
     * 查询单个司机最新位置（Redis）。
     */
    @GetMapping("/{driverId}/location")
    public DriverLocationDto location(@PathVariable Long driverId) {
        return driverService.getLocation(driverId)
                .orElseThrow(() -> new IllegalArgumentException("司机暂未上报位置: " + driverId));
    }

    /**
     * 查询所有在线且已上报位置的司机（供地图展示）。
     */
    @GetMapping("/locations/online")
    public List<DriverLocationDto> onlineLocations() {
        return driverService.listOnlineLocations();
    }

    @GetMapping("/{driverId}/documents/content")
    public byte[] documentContent(@PathVariable Long driverId, @RequestParam("docType") String docType) {
        return driverService.readDocument(driverId, docType);
    }
}
