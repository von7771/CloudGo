package com.von.driver.controller;

import com.von.common.api.ApiResponse;
import com.von.common.security.JwtConstants;
import com.von.driver.service.DriverService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 司机端对外接口（登录、上线、位置上报）。
 * <p>
 * 经 Gateway 访问：{@code http://localhost:8080/api/driver/**} → driver-service<br>
 * 除登录外，均需在 Header 携带 {@code Authorization: Bearer {driver_token}}。
 * Gateway 解析 JWT 后注入 {@code X-User-Id} 作为司机 ID。
 * </p>
 */
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * 司机登录，签发 JWT Token。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/auth/login?username=driver1&password=123456}<br>
     * <b>鉴权</b>：不需要 Token（公开路径）<br>
     * <b>测试账号</b>：driver1/123456、driver2/123456（需 audit_status=APPROVED）
     * </p>
     *
     * @param username 司机用户名
     * @param password 密码
     * @return data 含 token、driverId、realName、role=DRIVER
     */
    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    ) {
        return ApiResponse.ok("登录成功", driverService.login(username, password));
    }

    @PostMapping("/auth/register")
    public ApiResponse<Map<String, Object>> register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("realName") String realName,
            @RequestParam(value = "nickname", required = false) String nickname
    ) {
        return ApiResponse.ok("注册成功", driverService.register(username, password, realName, nickname));
    }

    @GetMapping("/profile")
    public ApiResponse<com.von.common.dto.UserProfileDto> getProfile(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId
    ) {
        return ApiResponse.ok(driverService.getProfile(driverId));
    }

    @org.springframework.web.bind.annotation.PutMapping("/profile")
    public ApiResponse<com.von.common.dto.UserProfileDto> updateProfile(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "realName", required = false) String realName
    ) {
        return ApiResponse.ok("更新成功", driverService.updateProfile(driverId, nickname, realName));
    }

    @PostMapping("/profile/avatar")
    public ApiResponse<com.von.common.dto.UserProfileDto> uploadAvatar(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok("头像上传成功", driverService.uploadAvatar(driverId, file));
    }

    @GetMapping("/profile/avatar")
    public org.springframework.http.ResponseEntity<byte[]> avatar(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId
    ) {
        byte[] bytes = driverService.readAvatar(driverId);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        org.springframework.http.MediaType.IMAGE_JPEG_VALUE)
                .body(bytes);
    }

    /**
     * 司机上线，加入 Redis 集合 {@code driver:online}，方可接单。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/online}<br>
     * <b>鉴权</b>：需要司机 Bearer Token<br>
     * <b>前置</b>：需先登录；Redis 需已启动
     * </p>
     *
     * @param driverId 由 Gateway 从 JWT 注入的 X-User-Id，无需手动传参
     */
    @PostMapping("/online")
    public ApiResponse<Void> online(@RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId) {
        driverService.goOnline(driverId);
        return ApiResponse.ok("已上线", null);
    }

    /**
     * 司机下线，从 Redis 移除在线状态。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/offline}<br>
     * <b>鉴权</b>：需要司机 Bearer Token
     * </p>
     *
     * @param driverId 司机 ID（JWT 自动注入）
     */
    @PostMapping("/offline")
    public ApiResponse<Void> offline(@RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId) {
        driverService.goOffline(driverId);
        return ApiResponse.ok("已下线", null);
    }

    /**
     * 上报司机当前经纬度（格式：{@code 经度,纬度}，如 {@code 113.27,23.13}）。
     * <p>
     * <b>请求</b>：{@code POST /api/driver/location?location=113.27,23.13}<br>
     * <b>鉴权</b>：需要司机 Bearer Token<br>
     * <b>前置</b>：需先调用 {@code /online} 上线
     * </p>
     *
     * @param driverId 司机 ID（JWT 自动注入）
     * @param location 经纬度字符串，存入 Redis {@code driver:location:{driverId}}
     */
    @PostMapping("/location")
    public ApiResponse<Void> location(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @RequestParam("location") String location
    ) {
        driverService.reportLocation(driverId, location);
        return ApiResponse.ok("位置已更新", null);
    }

    @PostMapping("/documents")
    public ApiResponse<Map<String, Object>> uploadDocument(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId,
            @RequestParam("docType") String docType,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok("上传成功", driverService.uploadDocument(driverId, docType, file));
    }

    @GetMapping("/documents/status")
    public ApiResponse<Map<String, Object>> documentStatus(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long driverId
    ) {
        return ApiResponse.ok(driverService.getDocumentStatus(driverId));
    }
}
