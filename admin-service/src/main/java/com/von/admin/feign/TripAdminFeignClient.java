package com.von.admin.feign;

import com.von.common.dto.DashboardChartsDto;
import com.von.common.dto.PageResult;
import com.von.common.dto.PricingRuleDto;
import com.von.common.dto.TripStatsDto;
import com.von.common.dto.TripSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "trip-service")
public interface TripAdminFeignClient {

    @GetMapping("/api/internal/trip/list")
    PageResult<TripSummaryDto> listTrips(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    @GetMapping("/api/internal/trip/{tripId}")
    TripSummaryDto getTrip(@PathVariable("tripId") Long tripId);

    @GetMapping("/api/internal/trip/stats")
    TripStatsDto stats();

    @GetMapping("/api/internal/trip/chart-stats")
    DashboardChartsDto chartStats();

    @GetMapping("/api/internal/trip/pricing")
    PricingRuleDto getPricing();

    @PutMapping("/api/internal/trip/pricing")
    PricingRuleDto updatePricing(
            @RequestParam("baseFare") BigDecimal baseFare,
            @RequestParam("perKmRate") BigDecimal perKmRate,
            @RequestParam("minFare") BigDecimal minFare
    );
}
