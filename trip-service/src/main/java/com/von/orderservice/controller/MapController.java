package com.von.orderservice.controller;

import com.von.common.dto.DriverLocationDto;
import com.von.orderservice.feign.DriverFeignClient;
import com.von.orderservice.map.AmapService;
import com.von.orderservice.map.dto.DrivingRouteResult;
import com.von.orderservice.map.dto.GeocodeResult;
import com.von.orderservice.weather.OpenWeatherService;
import com.von.orderservice.weather.WeatherPricingCalculator;
import com.von.orderservice.weather.dto.WeatherInfoDto;
import com.von.orderservice.weather.dto.WeatherSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 地图相关 HTTP 接口（Controller 层）。
 * <p>
 * 职责：接收外部 HTTP 请求 → 调用 AmapService → 把结果以 JSON 返回。
 * 不包含业务逻辑，只做「转发」。
 * Gateway 需配置 {@code Path=/api/map/**} 路由到此服务。
 * </p>
 */
@RestController // 声明为 REST 控制器，方法返回值自动序列化为 JSON
@RequestMapping("/api/map") // 该类所有接口的统一路径前缀
public class MapController {

    private final AmapService amapService;
    private final DriverFeignClient driverFeignClient;
    private final OpenWeatherService openWeatherService;
    private final WeatherPricingCalculator weatherPricingCalculator;

    public MapController(AmapService amapService,
                         DriverFeignClient driverFeignClient,
                         OpenWeatherService openWeatherService,
                         WeatherPricingCalculator weatherPricingCalculator) {
        this.amapService = amapService;
        this.driverFeignClient = driverFeignClient;
        this.openWeatherService = openWeatherService;
        this.weatherPricingCalculator = weatherPricingCalculator;
    }

    /**
     * 地理编码接口：文字地址 → 经纬度。
     * <p>
     * 示例：GET /api/map/geocode?address=天安门
     * 经 Gateway：GET http://localhost:8080/api/map/geocode?address=天安门
     * </p>
     *
     * @param address 要查询的地址（必填，URL 参数名 address）
     * @return GeocodeResult JSON，含 address、location、formattedAddress
     */
    @GetMapping("/geocode") // 映射 GET /api/map/geocode
    public GeocodeResult geocode(@RequestParam("address") String address) {
        return amapService.geocode(address); // 委托给 Service 层处理，Controller 不写业务逻辑
    }

    /**
     * 驾车路线规划接口：起终点 → 里程、时长、预估车费。
     * <p>
     * 示例：GET /api/map/route?origin=天安门&destination=北京西站
     * </p>
     *
     * @param origin      起点地址（URL 参数名 origin）
     * @param destination 终点地址（URL 参数名 destination）
     * @return DrivingRouteResult JSON，含起终点、坐标、里程、时长、费用
     */
    @GetMapping("/route") // 映射 GET /api/map/route
    public DrivingRouteResult route(
            @RequestParam("origin") String origin,           // 从 URL 读取起点
            @RequestParam("destination") String destination  // 从 URL 读取终点
    ) {
        return amapService.planDrivingRoute(origin, destination); // 委托 Service 完成路线规划
    }

    /**
     * 查询所有在线司机的最新位置（公开接口，供演示地图每 60 秒刷新）。
     */
    @GetMapping("/drivers/locations")
    public List<DriverLocationDto> onlineDriverLocations() {
        return driverFeignClient.listOnlineLocations();
    }

    /**
     * 查询某地当前天气及建议价格倍率（公开接口）。
     * 示例：GET /api/map/weather?lat=23.13&lon=113.27
     */
    @GetMapping("/weather")
    public WeatherInfoDto weather(@RequestParam("lat") double lat, @RequestParam("lon") double lon) {
        WeatherSnapshot snapshot = openWeatherService.getCurrentWeather(lat, lon).orElse(null);
        if (snapshot == null) {
            return new WeatherInfoDto(null, "天气服务未配置或暂不可用", 0, BigDecimal.ONE);
        }
        BigDecimal multiplier = weatherPricingCalculator.multiplier(snapshot);
        return new WeatherInfoDto(snapshot.main(), snapshot.description(), snapshot.temperatureCelsius(), multiplier);
    }
}
