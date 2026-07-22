package com.von.orderservice.feign;

import com.von.common.dto.DriverLocationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "driver-service", fallbackFactory = DriverFeignFallbackFactory.class)
public interface DriverFeignClient {

    @GetMapping("/api/internal/driver/online/check")
    Boolean isOnline(@RequestParam("driverId") Long driverId);

    @PostMapping("/api/internal/driver/credit")
    Boolean creditBalance(@RequestParam("driverId") Long driverId, @RequestParam("amount") BigDecimal amount);

    @GetMapping("/api/internal/driver/{driverId}/location")
    DriverLocationDto getDriverLocation(@PathVariable("driverId") Long driverId);

    @GetMapping("/api/internal/driver/locations/online")
    List<DriverLocationDto> listOnlineLocations();
}
