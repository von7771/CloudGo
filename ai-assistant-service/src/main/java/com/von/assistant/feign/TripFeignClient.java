package com.von.assistant.feign;

import com.von.common.dto.SmartCarpoolBundleDto;
import com.von.common.dto.TripSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "trip-service")
public interface TripFeignClient {

    @GetMapping("/api/internal/trip/passenger/{passengerId}/recent")
    List<TripSummaryDto> passengerRecent(@PathVariable("passengerId") Long passengerId,
                                         @RequestParam(value = "limit", defaultValue = "5") int limit);

    @GetMapping("/api/internal/trip/smart-bundles")
    List<SmartCarpoolBundleDto> smartBundles(@RequestParam(value = "limit", defaultValue = "5") int limit);

    @GetMapping("/api/internal/trip/smart-bundles/for-trip/{tripId}")
    List<SmartCarpoolBundleDto> smartBundlesForTrip(@PathVariable("tripId") Long tripId,
                                                    @RequestParam(value = "limit", defaultValue = "3") int limit);
}
