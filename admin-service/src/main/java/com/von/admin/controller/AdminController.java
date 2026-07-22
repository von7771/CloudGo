package com.von.admin.controller;

import com.von.admin.config.JwtProperties;
import com.von.admin.service.AdminService;
import com.von.common.api.ApiResponse;
import com.von.common.dto.DashboardDto;
import com.von.common.dto.DriverSummaryDto;
import com.von.common.dto.PageResult;
import com.von.common.dto.PassengerSummaryDto;
import com.von.common.dto.PricingRuleDto;
import com.von.common.dto.TripSummaryDto;
import com.von.common.enums.UserRole;
import com.von.common.security.JwtUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台接口（P4 完整能力）。
 * <p>
 * 经 Gateway 访问：{@code http://localhost:8080/api/admin/**} → admin-service<br>
 * 除登录外需管理员 Bearer Token（role=ADMIN）。
 * </p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    private final JwtProperties jwtProperties;
    private final AdminService adminService;

    public AdminController(JwtProperties jwtProperties, AdminService adminService) {
        this.jwtProperties = jwtProperties;
        this.adminService = adminService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    ) {
        if (!ADMIN_USER.equals(username) || !ADMIN_PASS.equals(password)) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        String token = JwtUtils.generateToken(1L, UserRole.ADMIN, jwtProperties.getSecret(), jwtProperties.getExpireMs());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("adminId", 1L);
        data.put("role", UserRole.ADMIN.name());
        return ApiResponse.ok("登录成功", data);
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardDto> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/trips")
    public ApiResponse<PageResult<TripSummaryDto>> listTrips(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminService.listTrips(status, page, size));
    }

    @GetMapping("/trips/{tripId}")
    public ApiResponse<TripSummaryDto> tripDetail(@PathVariable Long tripId) {
        return ApiResponse.ok(adminService.getTrip(tripId));
    }

    @GetMapping("/trips/{tripId}/receipt-url")
    public ApiResponse<Map<String, String>> tripReceiptUrl(@PathVariable Long tripId) {
        return ApiResponse.ok(adminService.getTripReceiptUrl(tripId));
    }

    @GetMapping("/trips/{tripId}/receipt")
    public ResponseEntity<byte[]> tripReceipt(@PathVariable Long tripId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(adminService.getTripReceipt(tripId));
    }

    @GetMapping("/drivers")
    public ApiResponse<PageResult<DriverSummaryDto>> listDrivers(
            @RequestParam(value = "auditStatus", required = false) String auditStatus,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminService.listDrivers(auditStatus, page, size));
    }

    @PutMapping("/drivers/{driverId}/audit")
    public ApiResponse<DriverSummaryDto> auditDriver(
            @PathVariable Long driverId,
            @RequestParam("auditStatus") String auditStatus
    ) {
        return ApiResponse.ok("审核完成", adminService.auditDriver(driverId, auditStatus));
    }

    @GetMapping("/drivers/{driverId}/documents/license-image")
    public ResponseEntity<byte[]> driverLicenseImage(@PathVariable Long driverId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(adminService.getDriverDocument(driverId, "license"));
    }

    @GetMapping("/drivers/{driverId}/documents/id_card-image")
    public ResponseEntity<byte[]> driverIdCardImage(@PathVariable Long driverId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(adminService.getDriverDocument(driverId, "id_card"));
    }

    @GetMapping("/passengers")
    public ApiResponse<PageResult<PassengerSummaryDto>> listPassengers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminService.listPassengers(page, size));
    }

    @PutMapping("/passengers/{passengerId}/ban")
    public ApiResponse<Map<String, Object>> banPassenger(
            @PathVariable Long passengerId,
            @RequestParam("banned") boolean banned
    ) {
        return ApiResponse.ok(banned ? "已封禁" : "已解封", adminService.banPassenger(passengerId, banned));
    }

    @GetMapping("/pricing")
    public ApiResponse<PricingRuleDto> getPricing() {
        return ApiResponse.ok(adminService.getPricing());
    }

    @PutMapping("/pricing")
    public ApiResponse<PricingRuleDto> updatePricing(
            @RequestParam("baseFare") BigDecimal baseFare,
            @RequestParam("perKmRate") BigDecimal perKmRate,
            @RequestParam("minFare") BigDecimal minFare
    ) {
        return ApiResponse.ok("计价规则已更新", adminService.updatePricing(baseFare, perKmRate, minFare));
    }
}
