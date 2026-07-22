package com.von.orderservice.controller;

import com.von.common.dto.DashboardChartsDto;
import com.von.common.dto.PageResult;
import com.von.common.dto.PricingRuleDto;
import com.von.common.dto.TripStatsDto;
import com.von.common.dto.SmartCarpoolBundleDto;
import com.von.common.dto.TripSummaryDto;
import com.von.orderservice.carpool.SmartCarpoolMatchService;
import com.von.orderservice.entity.Trip;
import com.von.orderservice.service.PricingService;
import com.von.orderservice.service.TripAdminService;
import com.von.orderservice.service.TripService;
import com.von.orderservice.support.TripDtoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 行程服务内部管理接口（供 admin-service Feign 调用）。
 */
@RestController
@RequestMapping("/api/internal/trip")
public class InternalTripController {

    private final TripAdminService tripAdminService;
    private final PricingService pricingService;
    private final SmartCarpoolMatchService smartCarpoolMatchService;
    private final TripService tripService;

    public InternalTripController(TripAdminService tripAdminService,
                                  PricingService pricingService,
                                  SmartCarpoolMatchService smartCarpoolMatchService,
                                  TripService tripService) {
        this.tripAdminService = tripAdminService;
        this.pricingService = pricingService;
        this.smartCarpoolMatchService = smartCarpoolMatchService;
        this.tripService = tripService;
    }

    @GetMapping("/list")
    public PageResult<TripSummaryDto> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        PageResult<Trip> pageResult = tripAdminService.listTrips(status, page, size);
        return new PageResult<>(
                pageResult.records().stream().map(TripDtoMapper::toSummary).toList(),
                pageResult.total(),
                pageResult.page(),
                pageResult.size()
        );
    }

    @GetMapping("/stats")
    public TripStatsDto stats() {
        return tripAdminService.getStats();
    }

    @GetMapping("/chart-stats")
    public DashboardChartsDto chartStats() {
        return tripAdminService.getChartStats();
    }

    /** 仅匹配数字 ID，避免与 /chart-stats 等固定路径冲突 */
    @GetMapping("/{tripId:\\d+}")
    public TripSummaryDto detail(@PathVariable Long tripId) {
        return TripDtoMapper.toSummary(tripAdminService.getTrip(tripId));
    }

    @GetMapping("/pricing")
    public PricingRuleDto getPricing() {
        return pricingService.getRule();
    }

    @PutMapping("/pricing")
    public PricingRuleDto updatePricing(
            @RequestParam("baseFare") BigDecimal baseFare,
            @RequestParam("perKmRate") BigDecimal perKmRate,
            @RequestParam("minFare") BigDecimal minFare
    ) {
        return pricingService.updateRule(baseFare, perKmRate, minFare);
    }

    @GetMapping("/passenger/{passengerId}/recent")
    public List<TripSummaryDto> passengerRecent(
            @PathVariable Long passengerId,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        return tripService.listPassengerTrips(passengerId).stream()
                .limit(limit)
                .map(TripDtoMapper::toSummary)
                .toList();
    }

    @GetMapping("/smart-bundles")
    public List<SmartCarpoolBundleDto> smartBundles(
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        return smartCarpoolMatchService.findSmartBundles(limit);
    }

    @GetMapping("/smart-bundles/for-trip/{tripId}")
    public List<SmartCarpoolBundleDto> smartBundlesForTrip(
            @PathVariable Long tripId,
            @RequestParam(value = "limit", defaultValue = "3") int limit
    ) {
        return smartCarpoolMatchService.findBundlesForTrip(tripId, limit);
    }
}
