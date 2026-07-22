package com.von.orderservice.map;

import com.von.orderservice.config.AmapProperties;
import com.von.orderservice.map.dto.AmapDrivingResponse;
import com.von.orderservice.map.dto.AmapGeocodeResponse;
import com.von.orderservice.map.dto.DrivingRouteResult;
import com.von.orderservice.map.dto.GeocodeResult;
import com.von.orderservice.map.dto.RoutePoint;
import com.von.orderservice.weather.OpenWeatherService;
import com.von.orderservice.weather.dto.WeatherSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德地图核心服务类。
 * <p>
 * 封装对高德 Web 服务 REST API 的调用，对外提供：
 * <ul>
 *   <li>{@link #geocode(String)} — 文字地址 → 经纬度</li>
 *   <li>{@link #planDrivingRoute(String, String)} — 起终点 → 路线 + 里程 + 车费</li>
 * </ul>
 * 第三方 API 细节都隔离在此类，业务层（OrderService）只依赖本类，不直接写 HTTP 代码。
 * </p>
 */
@Service // 注册为 Spring 单例 Bean，可被 Controller / TripService 注入
public class AmapService {

    /** 从 Nacos / amap-local.yml 读取 api-key、city 等配置 */
    private final AmapProperties properties;

    /** Spring 6+ HTTP 客户端，baseUrl 已在 AmapConfig 中设为 https://restapi.amap.com */
    private final RestClient restClient;

    private final com.von.orderservice.service.PricingService pricingService;
    private final OpenWeatherService openWeatherService;

    public AmapService(AmapProperties properties,
                       @Qualifier("amapRestClient") RestClient amapRestClient,
                       com.von.orderservice.service.PricingService pricingService,
                       OpenWeatherService openWeatherService) {
        this.properties = properties;
        this.restClient = amapRestClient;
        this.pricingService = pricingService;
        this.openWeatherService = openWeatherService;
    }

    /**
     * 地理编码：把文字地址转换成经纬度坐标。
     * <p>
     * 调用高德接口：GET /v3/geocode/geo?address=xxx&key=xxx&city=xxx
     * </p>
     *
     * @param address 用户输入的地址，如「天安门」
     * @return 包含经纬度和标准化地址的结果
     * @throws AmapException Key 未配置、地址为空、高德返回失败或找不到地址时抛出
     */
    public GeocodeResult geocode(String address) {
        // 先检查 API Key 是否已配置，避免发出无效请求
        requireApiKey();

        // 地址不能为空或纯空格
        if (!StringUtils.hasText(address)) {
            throw new AmapException("地址不能为空");
        }

        String trimmedAddress = address.trim();
        // 完整地址（含省）不传 city；短地址（如「广州南站」）才用配置的默认城市
        String city = resolveCityForGeocode(trimmedAddress);

        // 先按推断的 city 请求；若与城市冲突导致 ENGINE_RESPONSE_DATA_ERROR，则去掉 city 重试
        AmapGeocodeResponse response = requestGeocode(trimmedAddress, city);
        if (!isSuccess(response) && StringUtils.hasText(city)) {
            response = requestGeocode(trimmedAddress, null);
        }

        // 检查高德业务状态码 status 是否为 "1"（成功）
        assertSuccess(response != null ? response.status() : null,
                response != null ? response.info() : null, "地理编码");

        // 取出 geocodes 数组
        List<AmapGeocodeResponse.GeocodeItem> geocodes = response.geocodes();
        // 没有匹配结果时抛业务异常
        if (geocodes == null || geocodes.isEmpty()) {
            throw new AmapException("未找到地址: " + address);
        }

        // 取第一条匹配结果（高德按相关度排序，第一条通常最准）
        AmapGeocodeResponse.GeocodeItem first = geocodes.get(0);
        // 读取经纬度字段，格式 "116.xxx,39.xxx"
        String location = first.location();
        // location 为空说明解析异常
        if (!StringUtils.hasText(location)) {
            throw new AmapException("地址解析失败: " + address);
        }

        // 优先用高德返回的标准化地址；若为空则退回用户原始输入
        String formattedAddress = StringUtils.hasText(first.formatted_address())
                ? first.formatted_address()
                : address.trim();

        // 封装为对外 DTO 并返回
        return new GeocodeResult(address.trim(), location, formattedAddress);
    }

    /**
     * 调用高德地理编码 API。
     *
     * @param address 已 trim 的地址
     * @param city    可选城市限定；为 null 时不传 city，高德全国范围检索
     */
    private AmapGeocodeResponse requestGeocode(String address, String city) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v3/geocode/geo")
                            .queryParam("address", address)
                            .queryParam("key", properties.getApiKey());
                    if (StringUtils.hasText(city)) {
                        uriBuilder.queryParam("city", city);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(AmapGeocodeResponse.class);
    }

    /**
     * 决定是否向高德传递 city 参数。
     * <p>
     * 完整结构化地址（含「省」或「XX市XX区」）自带城市信息，再传默认 city（如北京）
     * 会与广州等地址冲突，触发 ENGINE_RESPONSE_DATA_ERROR。
     * 短地址（如「广州南站」「体育馆」）才使用配置的默认城市提高命中率。
     * </p>
     */
    private String resolveCityForGeocode(String address) {
        if (address.contains("省") || address.contains("自治区") || address.contains("特别行政区")) {
            return null;
        }
        if (address.contains("市") && address.indexOf('市') > 0) {
            return null;
        }
        return StringUtils.hasText(properties.getCity()) ? properties.getCity() : null;
    }

    private boolean isSuccess(AmapGeocodeResponse response) {
        return response != null && "1".equals(response.status());
    }

    /**
     * 驾车路线规划：根据起终点文字地址，计算路线、里程、时长和预估车费。
     * <p>
     * 步骤：① 分别 geocode 起终点 → ② 调 /v3/direction/driving → ③ 解析距离时长 → ④ 算价
     * </p>
     *
     * @param originAddress      起点文字地址
     * @param destinationAddress 终点文字地址
     * @return 完整路线信息（含 estimatedFare）
     */
    public DrivingRouteResult planDrivingRoute(String originAddress, String destinationAddress) {
        // 路线规划 API 要求传经纬度，所以先把两个文字地址分别转成坐标
        GeocodeResult origin = geocode(originAddress);
        GeocodeResult destination = geocode(destinationAddress);

        // 调用高德驾车路径规划接口
        AmapDrivingResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/direction/driving")                    // 驾车路线规划接口
                        .queryParam("origin", origin.location())          // 起点经纬度
                        .queryParam("destination", destination.location()) // 终点经纬度
                        .queryParam("key", properties.getApiKey())        // API Key
                        .build())
                .retrieve()
                .body(AmapDrivingResponse.class);

        // 校验高德返回的业务状态码
        assertSuccess(response != null ? response.status() : null,
                response != null ? response.info() : null, "驾车路线规划");

        // 取出 route.paths 数组
        AmapDrivingResponse.Route route = response.route();
        // 没有可用路线时抛异常
        if (route == null || route.paths() == null || route.paths().isEmpty()) {
            throw new AmapException("无法规划路线: " + originAddress + " -> " + destinationAddress);
        }

        // 取第一条推荐路线（高德可能返回多条：最快/最短等）
        AmapDrivingResponse.Path path = route.paths().get(0);
        // 高德返回 distance/duration 是字符串，转成 int 便于计算
        int distanceMeters = parseInt(path.distance());
        int durationSeconds = parseInt(path.duration());

        BigDecimal baseFare = pricingService.estimateBaseFare(distanceMeters);
        WeatherSnapshot weather = openWeatherService.getCurrentWeatherByLocation(origin.location()).orElse(null);
        com.von.orderservice.service.PricingService.WeatherAdjustedFare adjusted =
                pricingService.applyWeather(baseFare, weather);

        List<RoutePoint> routePoints = extractRoutePoints(path);
        if (routePoints.size() < 2) {
            routePoints = List.of(
                    toRoutePoint(origin.location()),
                    toRoutePoint(destination.location())
            );
        }

        return new DrivingRouteResult(
                origin.formattedAddress(),
                destination.formattedAddress(),
                origin.location(),
                destination.location(),
                distanceMeters,
                durationSeconds,
                adjusted.baseFare(),
                adjusted.finalFare(),
                weather != null ? weather.main() : null,
                weather != null ? weather.description() : null,
                weather != null ? weather.temperatureCelsius() : null,
                adjusted.multiplier(),
                adjusted.surcharge(),
                routePoints
        );
    }

    /**
     * 解析高德驾车路线 steps 中的 polyline，合并为地图折线点列表。
     * 高德格式：经度,纬度;经度,纬度
     */
    private List<RoutePoint> extractRoutePoints(AmapDrivingResponse.Path path) {
        List<RoutePoint> points = new ArrayList<>();
        if (path.steps() == null) {
            return points;
        }
        for (AmapDrivingResponse.Step step : path.steps()) {
            if (!StringUtils.hasText(step.polyline())) {
                continue;
            }
            appendPolyline(points, step.polyline());
        }
        return points;
    }

    private void appendPolyline(List<RoutePoint> points, String polyline) {
        for (String segment : polyline.split(";")) {
            if (!StringUtils.hasText(segment) || !segment.contains(",")) {
                continue;
            }
            String[] parts = segment.split(",");
            if (parts.length < 2) {
                continue;
            }
            try {
                double lng = Double.parseDouble(parts[0].trim());
                double lat = Double.parseDouble(parts[1].trim());
                RoutePoint next = new RoutePoint(lat, lng);
                if (!points.isEmpty()) {
                    RoutePoint last = points.get(points.size() - 1);
                    if (last.latitude() == next.latitude() && last.longitude() == next.longitude()) {
                        continue;
                    }
                }
                points.add(next);
            } catch (NumberFormatException ignored) {
                // 跳过非法坐标段
            }
        }
    }

    private RoutePoint toRoutePoint(String location) {
        String[] parts = location.split(",");
        double lng = Double.parseDouble(parts[0].trim());
        double lat = Double.parseDouble(parts[1].trim());
        return new RoutePoint(lat, lng);
    }

    public BigDecimal estimateFare(int distanceMeters) {
        return pricingService.estimateBaseFare(distanceMeters);
    }

    /**
     * 校验 API Key 是否已配置。
     * 未配置时抛 AmapException，提示用户去 Nacos 或环境变量中设置。
     */
    private void requireApiKey() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AmapException("未配置高德 API Key，请在 Nacos order-service.yml 或环境变量 AMAP_API_KEY 中设置 amap.api-key");
        }
    }

    /**
     * 校验高德 API 的业务响应状态。
     * <p>
     * 注意：HTTP 200 不代表业务成功，高德用 JSON 里的 status 字段表示，
     * "1" = 成功，其他值 = 失败（如 INVALID_USER_KEY）。
     * </p>
     *
     * @param status 高德返回的 status 字段
     * @param info   高德返回的 info 字段（失败原因）
     * @param action 当前操作名称，用于拼接错误提示
     */
    private void assertSuccess(String status, String info, String action) {
        if (!"1".equals(status)) { // 高德约定 "1" 表示成功
            throw new AmapException(action + "失败: " + (info != null ? info : "未知错误"));
        }
    }

    /**
     * 安全地将字符串转为 int。
     * 高德 API 返回的 distance、duration 是字符串类型，需要手动转换。
     *
     * @param value 待转换的字符串
     * @return 转换后的整数；空串或格式错误时返回 0
     */
    private int parseInt(String value) {
        if (!StringUtils.hasText(value)) {
            return 0; // 空值按 0 处理
        }
        try {
            return Integer.parseInt(value); // 正常解析
        } catch (NumberFormatException e) {
            return 0; // 解析失败不抛异常，降级为 0
        }
    }
}
