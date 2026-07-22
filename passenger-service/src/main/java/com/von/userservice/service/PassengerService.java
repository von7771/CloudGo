package com.von.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.von.common.dto.PageResult;
import com.von.common.dto.UserProfileDto;
import com.von.common.enums.UserRole;
import com.von.common.security.JwtUtils;
import com.von.common.storage.MinioStorageService;
import com.von.userservice.config.JwtProperties;
import com.von.userservice.entity.Passenger;
import com.von.userservice.entity.PassengerStatus;
import com.von.userservice.exception.InsufficientBalanceException;
import com.von.userservice.mapper.PassengerMapper;
import com.von.userservice.mapper.PassengerStatusMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PassengerService {

    private final PassengerMapper passengerMapper;
    private final PassengerStatusMapper passengerStatusMapper;
    private final JwtProperties jwtProperties;
    private final MinioStorageService minioStorageService;

    public PassengerService(PassengerMapper passengerMapper,
                            PassengerStatusMapper passengerStatusMapper,
                            JwtProperties jwtProperties,
                            java.util.Optional<MinioStorageService> minioStorageService) {
        this.passengerMapper = passengerMapper;
        this.passengerStatusMapper = passengerStatusMapper;
        this.jwtProperties = jwtProperties;
        this.minioStorageService = minioStorageService.orElse(null);
    }

    public Map<String, Object> register(String username, String password, String nickname) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        if (username.length() < 3 || username.length() > 32) {
            throw new IllegalArgumentException("用户名长度需在 3-32 之间");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        Long exists = passengerMapper.selectCount(new LambdaQueryWrapper<Passenger>()
                .eq(Passenger::getUsername, username.trim()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Passenger passenger = new Passenger();
        passenger.setUsername(username.trim());
        passenger.setPassword(password);
        passenger.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : username.trim());
        passenger.setCreditScore(80);
        passenger.setBalance(new BigDecimal("200.00"));
        passenger.setCreatedAt(LocalDateTime.now());
        passengerMapper.insert(passenger);

        PassengerStatus status = new PassengerStatus();
        status.setPassengerId(passenger.getId());
        status.setStatus("ACTIVE");
        passengerStatusMapper.insert(status);
        return login(username.trim(), password);
    }

    public UserProfileDto getProfile(Long passengerId) {
        Passenger passenger = requirePassenger(passengerId);
        return toProfileDto(passenger);
    }

    public UserProfileDto updateProfile(Long passengerId, String nickname) {
        Passenger passenger = requirePassenger(passengerId);
        if (StringUtils.hasText(nickname)) {
            passengerMapper.updateNickname(passengerId, nickname.trim());
            passenger.setNickname(nickname.trim());
        }
        return toProfileDto(passenger);
    }

    public UserProfileDto uploadAvatar(Long passengerId, MultipartFile file) {
        if (minioStorageService == null) {
            throw new IllegalStateException("对象存储未启用");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }
        requirePassenger(passengerId);
        String ext = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String objectKey = "passengers/" + passengerId + "/avatar" + ext;
        try {
            minioStorageService.upload(objectKey, file.getInputStream(), file.getSize(),
                    file.getContentType() != null ? file.getContentType() : "image/jpeg");
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }
        passengerMapper.updateAvatarObject(passengerId, objectKey);
        return getProfile(passengerId);
    }

    public byte[] readAvatar(Long passengerId) {
        if (minioStorageService == null) {
            throw new IllegalStateException("对象存储未启用");
        }
        Passenger passenger = requirePassenger(passengerId);
        if (!StringUtils.hasText(passenger.getAvatarObject())) {
            throw new IllegalArgumentException("尚未上传头像");
        }
        return minioStorageService.download(passenger.getAvatarObject());
    }

    public Map<String, Object> login(String username, String password) {
        Passenger passenger = passengerMapper.selectOne(new LambdaQueryWrapper<Passenger>()
                .eq(Passenger::getUsername, username));
        if (passenger == null || !password.equals(passenger.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (isBanned(passenger.getId())) {
            throw new IllegalArgumentException("账号已被封禁，请联系管理员");
        }
        String token = JwtUtils.generateToken(passenger.getId(), UserRole.PASSENGER,
                jwtProperties.getSecret(), jwtProperties.getExpireMs());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("passengerId", passenger.getId());
        result.put("username", passenger.getUsername());
        result.put("role", UserRole.PASSENGER.name());
        return result;
    }

    public Integer getCreditLevel(Long passengerId) {
        Passenger passenger = passengerMapper.selectById(passengerId);
        return passenger != null ? passenger.getCreditScore() : 0;
    }

    public boolean hasSufficientBalance(Long passengerId, BigDecimal amount) {
        Passenger passenger = passengerMapper.selectById(passengerId);
        if (passenger == null) {
            return false;
        }
        BigDecimal balance = passenger.getBalance() != null ? passenger.getBalance() : BigDecimal.ZERO;
        return balance.compareTo(amount) >= 0;
    }

    public void deductBalance(Long passengerId, BigDecimal amount) {
        Passenger passenger = passengerMapper.selectById(passengerId);
        if (passenger == null) {
            throw new IllegalArgumentException("乘客不存在，ID: " + passengerId);
        }
        int rows = passengerMapper.deductBalance(passengerId, amount);
        if (rows > 0) {
            return;
        }
        BigDecimal balance = passenger.getBalance() != null ? passenger.getBalance() : BigDecimal.ZERO;
        throw new InsufficientBalanceException(passengerId, balance, amount);
    }

    public PageResult<Passenger> listPassengers(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = passengerMapper.selectCount(null);
        int offset = (safePage - 1) * safeSize;
        List<Passenger> records = passengerMapper.selectList(new LambdaQueryWrapper<Passenger>()
                .orderByDesc(Passenger::getId)
                .last("LIMIT " + offset + "," + safeSize));
        return new PageResult<>(records, total, safePage, safeSize);
    }

    public long countPassengers() {
        return passengerMapper.selectCount(null);
    }

    public void banPassenger(Long passengerId, boolean banned) {
        Passenger passenger = passengerMapper.selectById(passengerId);
        if (passenger == null) {
            throw new IllegalArgumentException("乘客不存在: " + passengerId);
        }
        PassengerStatus status = passengerStatusMapper.selectById(passengerId);
        if (status == null) {
            status = new PassengerStatus();
            status.setPassengerId(passengerId);
            status.setStatus(banned ? "BANNED" : "ACTIVE");
            passengerStatusMapper.insert(status);
        } else {
            status.setStatus(banned ? "BANNED" : "ACTIVE");
            passengerStatusMapper.updateById(status);
        }
    }

    private boolean isBanned(Long passengerId) {
        PassengerStatus status = passengerStatusMapper.selectById(passengerId);
        return status != null && "BANNED".equals(status.getStatus());
    }

    private Passenger requirePassenger(Long passengerId) {
        Passenger passenger = passengerMapper.selectById(passengerId);
        if (passenger == null) {
            throw new IllegalArgumentException("乘客不存在: " + passengerId);
        }
        return passenger;
    }

    private UserProfileDto toProfileDto(Passenger passenger) {
        String avatarUrl = StringUtils.hasText(passenger.getAvatarObject())
                ? "/api/passenger/profile/avatar"
                : null;
        return new UserProfileDto(
                passenger.getId(),
                passenger.getUsername(),
                passenger.getNickname(),
                avatarUrl,
                UserRole.PASSENGER.name(),
                passenger.getCreditScore(),
                passenger.getBalance(),
                null,
                null,
                passenger.getCreatedAt()
        );
    }

    private String resolveExtension(String filename, String contentType) {
        if (StringUtils.hasText(filename) && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        if (contentType != null && contentType.contains("png")) {
            return ".png";
        }
        return ".jpg";
    }
}
