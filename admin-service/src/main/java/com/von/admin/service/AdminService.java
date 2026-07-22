package com.von.admin.service;

import com.von.admin.feign.DriverAdminFeignClient;
import com.von.admin.feign.PassengerAdminFeignClient;
import com.von.admin.feign.TripAdminFeignClient;
import com.von.admin.kafka.TripReceiptHandler;
import com.von.common.dto.DashboardChartsDto;
import com.von.common.dto.DashboardDto;
import com.von.common.dto.DriverSummaryDto;
import com.von.common.dto.PageResult;
import com.von.common.dto.PassengerSummaryDto;
import com.von.common.dto.PricingRuleDto;
import com.von.common.dto.TripStatsDto;
import com.von.common.dto.TripSummaryDto;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final TripAdminFeignClient tripAdminFeignClient;
    private final DriverAdminFeignClient driverAdminFeignClient;
    private final PassengerAdminFeignClient passengerAdminFeignClient;
    private final Optional<TripReceiptHandler> tripReceiptHandler;

    public AdminService(TripAdminFeignClient tripAdminFeignClient,
                        DriverAdminFeignClient driverAdminFeignClient,
                        PassengerAdminFeignClient passengerAdminFeignClient,
                        Optional<TripReceiptHandler> tripReceiptHandler) {
        this.tripAdminFeignClient = tripAdminFeignClient;
        this.driverAdminFeignClient = driverAdminFeignClient;
        this.passengerAdminFeignClient = passengerAdminFeignClient;
        this.tripReceiptHandler = tripReceiptHandler;
    }

    public DashboardDto dashboard() {
        TripStatsDto tripStats = tripAdminFeignClient.stats();
        Map<String, Long> driverStats = driverAdminFeignClient.stats();
        Map<String, Long> passengerStats = passengerAdminFeignClient.stats();
        PricingRuleDto pricing = tripAdminFeignClient.getPricing();
        DashboardChartsDto charts = loadChartStats();
        return new DashboardDto(
                tripStats,
                driverStats.getOrDefault("onlineCount", 0L),
                driverStats.getOrDefault("pendingAuditCount", 0L),
                passengerStats.getOrDefault("totalCount", 0L),
                pricing,
                charts
        );
    }

    public PageResult<TripSummaryDto> listTrips(String status, int page, int size) {
        return tripAdminFeignClient.listTrips(status, page, size);
    }

    public TripSummaryDto getTrip(Long tripId) {
        return tripAdminFeignClient.getTrip(tripId);
    }

    public Map<String, String> getTripReceiptUrl(Long tripId) {
        return tripReceiptHandler
                .flatMap(handler -> handler.getReceiptUrl(tripId).map(url -> Map.of("receiptUrl", url)))
                .orElse(Map.of());
    }

    public byte[] getTripReceipt(Long tripId) {
        return tripReceiptHandler
                .map(handler -> handler.downloadReceipt(tripId))
                .orElseThrow(() -> new IllegalArgumentException("暂无电子凭证"));
    }

    public PageResult<DriverSummaryDto> listDrivers(String auditStatus, int page, int size) {
        return driverAdminFeignClient.listDrivers(auditStatus, page, size);
    }

    public DriverSummaryDto auditDriver(Long driverId, String auditStatus) {
        return driverAdminFeignClient.auditDriver(driverId, auditStatus);
    }

    public byte[] getDriverDocument(Long driverId, String docType) {
        return driverAdminFeignClient.getDocumentContent(driverId, docType);
    }

    public PageResult<PassengerSummaryDto> listPassengers(int page, int size) {
        return passengerAdminFeignClient.listPassengers(page, size);
    }

    public Map<String, Object> banPassenger(Long passengerId, boolean banned) {
        return passengerAdminFeignClient.banPassenger(passengerId, banned);
    }

    public PricingRuleDto getPricing() {
        return tripAdminFeignClient.getPricing();
    }

    public PricingRuleDto updatePricing(BigDecimal baseFare, BigDecimal perKmRate, BigDecimal minFare) {
        return tripAdminFeignClient.updatePricing(baseFare, perKmRate, minFare);
    }

    private DashboardChartsDto loadChartStats() {
        try {
            return tripAdminFeignClient.chartStats();
        } catch (Exception ex) {
            log.warn("加载图表数据失败，返回空图表: {}", ex.getMessage());
            return emptyCharts();
        }
    }

    private static DashboardChartsDto emptyCharts() {
        List<String> days = List.of("—", "—", "—", "—", "—", "—", "—");
        List<Long> zeros = List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L);
        List<BigDecimal> zeroGmv = List.of(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        List<String> hours = List.of("00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00");
        List<Long> hourZeros = List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        return new DashboardChartsDto(days, zeros, zeroGmv, 0L, 0L, 0L, 0L, hours, hourZeros);
    }
}
