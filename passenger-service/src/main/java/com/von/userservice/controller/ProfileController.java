package com.von.userservice.controller;

import com.von.common.api.ApiResponse;
import com.von.common.dto.UserProfileDto;
import com.von.common.security.JwtConstants;
import com.von.userservice.service.PassengerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/passenger/profile")
public class ProfileController {

    private final PassengerService passengerService;

    public ProfileController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @GetMapping
    public ApiResponse<UserProfileDto> getProfile(@RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId) {
        return ApiResponse.ok(passengerService.getProfile(passengerId));
    }

    @PutMapping
    public ApiResponse<UserProfileDto> updateProfile(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @RequestParam(value = "nickname", required = false) String nickname
    ) {
        return ApiResponse.ok("更新成功", passengerService.updateProfile(passengerId, nickname));
    }

    @PostMapping("/avatar")
    public ApiResponse<UserProfileDto> uploadAvatar(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok("头像上传成功", passengerService.uploadAvatar(passengerId, file));
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar(@RequestHeader(JwtConstants.HEADER_USER_ID) Long passengerId) {
        byte[] bytes = passengerService.readAvatar(passengerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(bytes);
    }
}
