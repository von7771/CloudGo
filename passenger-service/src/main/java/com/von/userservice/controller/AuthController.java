package com.von.userservice.controller;

import com.von.common.api.ApiResponse;
import com.von.userservice.service.PassengerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 乘客认证接口。
 * <p>
 * 经 Gateway 访问：{@code http://localhost:8080/api/passenger/auth/**} → passenger-service<br>
 * 登录接口为公开路径，无需 Bearer Token。
 * </p>
 */
@RestController
@RequestMapping("/api/passenger/auth")
public class AuthController {

    private final PassengerService passengerService;

    public AuthController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    /**
     * 乘客登录，签发 JWT Token。
     * <p>
     * <b>请求</b>：{@code POST /api/passenger/auth/login?username=passenger1&password=123456}<br>
     * <b>鉴权</b>：不需要 Token<br>
     * <b>测试账号</b>：passenger1/123456（余额500）、passenger2/123456（余额50）、passenger3/123456（信用不足）
     * </p>
     *
     * @param username 乘客用户名
     * @param password 密码（演示环境明文）
     * @return data 含 token、passengerId、username、role=PASSENGER；后续请求在 Header 带 {@code Authorization: Bearer {token}}
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    ) {
        return ApiResponse.ok("登录成功", passengerService.login(username, password));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "nickname", required = false) String nickname
    ) {
        return ApiResponse.ok("注册成功", passengerService.register(username, password, nickname));
    }
}
