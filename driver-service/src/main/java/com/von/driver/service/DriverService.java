package com.von.driver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.von.common.dto.DriverLocationDto;
import com.von.common.dto.PageResult;
import com.von.common.dto.UserProfileDto;
import com.von.common.enums.UserRole;
import com.von.common.security.JwtUtils;
import com.von.common.storage.MinioStorageService;
import com.von.common.dto.DriverSummaryDto;
import com.von.driver.config.JwtProperties;
import com.von.driver.entity.Driver;
import com.von.driver.mapper.DriverMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DriverService {

    private static final String ONLINE_KEY = "driver:online";
    private static final String LOCATION_PREFIX = "driver:location:";
    private static final Duration LOCATION_TTL = Duration.ofMinutes(3);

    private final DriverMapper driverMapper;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    private final MinioStorageService minioStorageService;

    public DriverService(DriverMapper driverMapper,
                         JwtProperties jwtProperties,
                         StringRedisTemplate redisTemplate,
                         java.util.Optional<MinioStorageService> minioStorageService) {
        this.driverMapper = driverMapper;
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
        this.minioStorageService = minioStorageService.orElse(null);
    }

    public Map<String, Object> login(String username, String password) {
        Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>().eq(Driver::getUsername, username));
        if (driver == null || !password.equals(driver.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if ("REJECTED".equals(driver.getAuditStatus())) {
            throw new IllegalArgumentException("司机账号审核未通过，请重新上传证件");
        }
        String token = JwtUtils.generateToken(driver.getId(), UserRole.DRIVER, jwtProperties.getSecret(), jwtProperties.getExpireMs());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("driverId", driver.getId());
        result.put("username", driver.getUsername());
        result.put("realName", driver.getRealName());
        result.put("role", UserRole.DRIVER.name());
        return result;
    }

    public Map<String, Object> register(String username, String password, String realName, String nickname) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(realName)) {
            throw new IllegalArgumentException("用户名、密码和真实姓名不能为空");
        }
        if (username.length() < 3 || password.length() < 6) {
            throw new IllegalArgumentException("用户名至少3位，密码至少6位");
        }
        Long exists = driverMapper.selectCount(new LambdaQueryWrapper<Driver>().eq(Driver::getUsername, username.trim()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Driver driver = new Driver();
        driver.setUsername(username.trim());
        driver.setPassword(password);
        driver.setRealName(realName.trim());
        driver.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : realName.trim());
        driver.setAuditStatus("PENDING");
        driver.setBalance(BigDecimal.ZERO);
        driver.setCreatedAt(LocalDateTime.now());
        driverMapper.insert(driver);
        return login(username.trim(), password);
    }

    public UserProfileDto getProfile(Long driverId) {
        return toProfileDto(requireDriver(driverId));
    }

    public UserProfileDto updateProfile(Long driverId, String nickname, String realName) {
        Driver driver = requireDriver(driverId);
        if (StringUtils.hasText(nickname)) {
            driverMapper.updateNickname(driverId, nickname.trim());
            driver.setNickname(nickname.trim());
        }
        if (StringUtils.hasText(realName)) {
            driver.setRealName(realName.trim());
            Driver update = new Driver();
            update.setId(driverId);
            update.setRealName(realName.trim());
            driverMapper.updateById(update);
        }
        return toProfileDto(driverMapper.selectById(driverId));
    }

    public UserProfileDto uploadAvatar(Long driverId, MultipartFile file) {
        if (minioStorageService == null) {
            throw new IllegalStateException("对象存储未启用");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }
        requireDriver(driverId);
        String ext = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String objectKey = "drivers/" + driverId + "/avatar" + ext;
        try {
            minioStorageService.upload(objectKey, file.getInputStream(), file.getSize(),
                    file.getContentType() != null ? file.getContentType() : "image/jpeg");
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }
        driverMapper.updateAvatarObject(driverId, objectKey);
        return getProfile(driverId);
    }

    public byte[] readAvatar(Long driverId) {
        if (minioStorageService == null) {
            throw new IllegalStateException("对象存储未启用");
        }
        Driver driver = requireDriver(driverId);
        if (!StringUtils.hasText(driver.getAvatarObject())) {
            throw new IllegalArgumentException("尚未上传头像");
        }
        return minioStorageService.download(driver.getAvatarObject());
    }

    public void goOnline(Long driverId) {
        ensureApproved(driverId);
        redisTemplate.opsForSet().add(ONLINE_KEY, String.valueOf(driverId));
    }

    public void goOffline(Long driverId) {
        redisTemplate.opsForSet().remove(ONLINE_KEY, String.valueOf(driverId));
        redisTemplate.delete(LOCATION_PREFIX + driverId);
    }

    public void reportLocation(Long driverId, String location) {
        if (!isOnline(driverId)) {
            throw new IllegalArgumentException("请先上线后再上报位置");
        }
        double[] coords = parseLngLat(location);
        String payload = String.format(
                "{\"lng\":%.6f,\"lat\":%.6f,\"updatedAt\":\"%s\"}",
                coords[0], coords[1], LocalDateTime.now()
        );
        redisTemplate.opsForValue().set(LOCATION_PREFIX + driverId, payload, LOCATION_TTL);
    }

    public Optional<DriverLocationDto> getLocation(Long driverId) {
        String raw = redisTemplate.opsForValue().get(LOCATION_PREFIX + driverId);
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        return Optional.of(parseLocationPayload(driverId, raw));
    }

    public List<DriverLocationDto> listOnlineLocations() {
        Set<String> onlineIds = redisTemplate.opsForSet().members(ONLINE_KEY);
        if (onlineIds == null || onlineIds.isEmpty()) {
            return List.of();
        }
        List<DriverLocationDto> locations = new ArrayList<>();
        for (String idStr : onlineIds) {
            Long driverId = Long.parseLong(idStr);
            getLocation(driverId).ifPresent(locations::add);
        }
        return locations;
    }

    public boolean isOnline(Long driverId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_KEY, String.valueOf(driverId)));
    }

    public void creditBalance(Long driverId, BigDecimal amount) {
        driverMapper.creditBalance(driverId, amount);
    }

    public PageResult<Driver> listDrivers(String auditStatus, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<Driver> wrapper = new LambdaQueryWrapper<Driver>()
                .eq(StringUtils.hasText(auditStatus), Driver::getAuditStatus, auditStatus)
                .orderByDesc(Driver::getId);
        long total = driverMapper.selectCount(wrapper);
        int offset = (safePage - 1) * safeSize;
        List<Driver> records = driverMapper.selectList(wrapper.last("LIMIT " + offset + "," + safeSize));
        return new PageResult<>(records, total, safePage, safeSize);
    }

    public Driver auditDriver(Long driverId, String auditStatus) {
        if (!List.of("APPROVED", "REJECTED", "PENDING").contains(auditStatus)) {
            throw new IllegalArgumentException("审核状态无效: " + auditStatus);
        }
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("司机不存在: " + driverId);
        }
        driverMapper.updateAuditStatus(driverId, auditStatus);
        driver.setAuditStatus(auditStatus);
        if ("REJECTED".equals(auditStatus)) {
            goOffline(driverId);
        }
        return driver;
    }

    public long countOnlineDrivers() {
        Long size = redisTemplate.opsForSet().size(ONLINE_KEY);
        return size != null ? size : 0;
    }

    public long countPendingAudit() {
        return driverMapper.selectCount(new LambdaQueryWrapper<Driver>().eq(Driver::getAuditStatus, "PENDING"));
    }

    public Map<String, Object> uploadDocument(Long driverId, String docType, MultipartFile file) {
        if (minioStorageService == null) {
            throw new IllegalStateException("对象存储未启用");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String normalizedType = normalizeDocType(docType);
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("司机不存在");
        }
        String ext = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String objectKey = "drivers/" + driverId + "/" + normalizedType + ext;
        try {
            minioStorageService.upload(objectKey, file.getInputStream(), file.getSize(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }
        if ("license".equals(normalizedType)) {
            driverMapper.updateLicenseObject(driverId, objectKey);
        } else {
            driverMapper.updateIdCardObject(driverId, objectKey);
        }
        if ("REJECTED".equals(driver.getAuditStatus()) || "APPROVED".equals(driver.getAuditStatus())) {
            driverMapper.updateAuditStatus(driverId, "PENDING");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("docType", normalizedType);
        result.put("objectKey", objectKey);
        result.put("previewUrl", minioStorageService.presignedGetUrl(objectKey));
        result.put("auditStatus", "PENDING");
        return result;
    }

    public Map<String, Object> getDocumentStatus(Long driverId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("司机不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("auditStatus", driver.getAuditStatus());
        result.put("licenseUploaded", StringUtils.hasText(driver.getLicenseImageObject()));
        result.put("idCardUploaded", StringUtils.hasText(driver.getIdCardImageObject()));
        result.put("licenseImageUrl", presignedUrl(driver.getLicenseImageObject()));
        result.put("idCardImageUrl", presignedUrl(driver.getIdCardImageObject()));
        return result;
    }

    public byte[] readDocument(Long driverId, String docType) {
        if (minioStorageService == null) {
            throw new IllegalStateException("对象存储未启用");
        }
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("司机不存在");
        }
        String objectKey = switch (normalizeDocType(docType)) {
            case "license" -> driver.getLicenseImageObject();
            case "id_card" -> driver.getIdCardImageObject();
            default -> throw new IllegalArgumentException("docType 无效");
        };
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("证件尚未上传");
        }
        return minioStorageService.download(objectKey);
    }

    public DriverSummaryDto toSummaryDto(Driver driver) {
        return new DriverSummaryDto(
                driver.getId(),
                driver.getUsername(),
                driver.getRealName(),
                driver.getAuditStatus(),
                driver.getBalance(),
                driver.getCreatedAt(),
                documentProxyPath(driver.getId(), "license", driver.getLicenseImageObject()),
                documentProxyPath(driver.getId(), "id_card", driver.getIdCardImageObject())
        );
    }

    private String documentProxyPath(Long driverId, String docType, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return "/api/admin/drivers/" + driverId + "/documents/" + docType + "-image";
    }

    private String presignedUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey) || minioStorageService == null) {
            return null;
        }
        return minioStorageService.presignedGetUrl(objectKey);
    }

    private String normalizeDocType(String docType) {
        if (!StringUtils.hasText(docType)) {
            throw new IllegalArgumentException("docType 不能为空");
        }
        return switch (docType.trim().toLowerCase()) {
            case "license", "driver_license" -> "license";
            case "id_card", "idcard", "id" -> "id_card";
            default -> throw new IllegalArgumentException("docType 仅支持 license / id_card");
        };
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

    private Driver ensureApproved(Long driverId) {
        Driver driver = requireDriver(driverId);
        if (!"APPROVED".equals(driver.getAuditStatus())) {
            throw new IllegalArgumentException("司机未通过审核");
        }
        return driver;
    }

    private Driver requireDriver(Long driverId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("司机不存在");
        }
        return driver;
    }

    private UserProfileDto toProfileDto(Driver driver) {
        String avatarUrl = StringUtils.hasText(driver.getAvatarObject())
                ? "/api/driver/profile/avatar"
                : null;
        return new UserProfileDto(
                driver.getId(),
                driver.getUsername(),
                driver.getNickname(),
                avatarUrl,
                UserRole.DRIVER.name(),
                null,
                driver.getBalance(),
                driver.getAuditStatus(),
                driver.getRealName(),
                driver.getCreatedAt()
        );
    }

    private double[] parseLngLat(String location) {
        if (!StringUtils.hasText(location) || !location.contains(",")) {
            throw new IllegalArgumentException("位置格式应为 经度,纬度");
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("位置格式应为 经度,纬度");
        }
        return new double[]{
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim())
        };
    }

    private DriverLocationDto parseLocationPayload(Long driverId, String raw) {
        if (raw.startsWith("{")) {
            double lng = extractJsonNumber(raw, "lng");
            double lat = extractJsonNumber(raw, "lat");
            String updatedAtText = extractJsonString(raw, "updatedAt");
            LocalDateTime updatedAt = StringUtils.hasText(updatedAtText)
                    ? LocalDateTime.parse(updatedAtText)
                    : LocalDateTime.now();
            return new DriverLocationDto(driverId, lng, lat, updatedAt);
        }
        double[] coords = parseLngLat(raw);
        return new DriverLocationDto(driverId, coords[0], coords[1], LocalDateTime.now());
    }

    private double extractJsonNumber(String json, String field) {
        String marker = "\"" + field + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("位置数据无效");
        }
        start += marker.length();
        int end = json.indexOf(',', start);
        if (end < 0) {
            end = json.indexOf('}', start);
        }
        return Double.parseDouble(json.substring(start, end).trim());
    }

    private String extractJsonString(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
