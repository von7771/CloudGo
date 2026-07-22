package com.von.orderservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.von.common.dto.DashboardChartsDto;
import com.von.common.dto.PageResult;
import com.von.common.dto.TripStatsDto;
import com.von.common.enums.TripStatus;
import com.von.orderservice.entity.Trip;
import com.von.orderservice.exception.TripException;
import com.von.orderservice.mapper.TripMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TripAdminService {

    private final TripMapper tripMapper;

    public TripAdminService(TripMapper tripMapper) {
        this.tripMapper = tripMapper;
    }

    public PageResult<Trip> listTrips(String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<Trip> wrapper = new LambdaQueryWrapper<Trip>()
                .eq(StringUtils.hasText(status), Trip::getStatus, status)
                .orderByDesc(Trip::getId);
        long total = tripMapper.selectCount(wrapper);
        int offset = (safePage - 1) * safeSize;
        List<Trip> records = tripMapper.selectList(wrapper.last("LIMIT " + offset + "," + safeSize));
        return new PageResult<>(records, total, safePage, safeSize);
    }

    public Trip getTrip(Long tripId) {
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw new TripException("行程不存在: " + tripId);
        }
        return trip;
    }

    public TripStatsDto getStats() {
        long total = tripMapper.selectCount(null);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayTrips = tripMapper.selectCount(new LambdaQueryWrapper<Trip>()
                .ge(Trip::getCreatedAt, todayStart));
        long dispatching = countByStatus(TripStatus.DISPATCHING.name());
        long completed = countByStatus(TripStatus.COMPLETED.name());
        long cancelled = countByStatus(TripStatus.CANCELLED.name());

        List<Trip> completedTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getStatus, TripStatus.COMPLETED.name()));
        BigDecimal totalGmv = completedTrips.stream()
                .map(t -> t.getFinalAmount() != null ? t.getFinalAmount() : t.getEstimatedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todayGmv = completedTrips.stream()
                .filter(t -> t.getUpdatedAt() != null && !t.getUpdatedAt().isBefore(todayStart))
                .map(t -> t.getFinalAmount() != null ? t.getFinalAmount() : t.getEstimatedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TripStatsDto(total, todayTrips, totalGmv, todayGmv, dispatching, completed, cancelled);
    }

    public DashboardChartsDto getChartStats() {
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> days = new ArrayList<>();
        List<Long> tripTrend = new ArrayList<>();
        List<BigDecimal> gmvTrend = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            days.add(dayFmt.format(day));

            long count = tripMapper.selectCount(new LambdaQueryWrapper<Trip>()
                    .ge(Trip::getCreatedAt, start)
                    .lt(Trip::getCreatedAt, end));
            tripTrend.add(count);

            List<Trip> dayCompleted = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                    .eq(Trip::getStatus, TripStatus.COMPLETED.name())
                    .ge(Trip::getUpdatedAt, start)
                    .lt(Trip::getUpdatedAt, end));
            BigDecimal gmv = dayCompleted.stream()
                    .map(t -> t.getFinalAmount() != null ? t.getFinalAmount() : t.getEstimatedAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            gmvTrend.add(gmv);
        }

        long solo = countByTripMode("SOLO");
        long carpool = countByTripMode("CARPOOL");
        long accepted = countByStatus(TripStatus.ACCEPTED.name()) + countByStatus(TripStatus.ARRIVED.name());
        long inProgress = countByStatus(TripStatus.IN_PROGRESS.name());

        List<String> hourlyLabels = new ArrayList<>();
        List<Long> hourlyTrips = new ArrayList<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        for (int h = 0; h < 24; h += 3) {
            LocalDateTime bucketStart = todayStart.plusHours(h);
            LocalDateTime bucketEnd = bucketStart.plusHours(3);
            hourlyLabels.add(String.format("%02d:00", h));
            hourlyTrips.add(tripMapper.selectCount(new LambdaQueryWrapper<Trip>()
                    .ge(Trip::getCreatedAt, bucketStart)
                    .lt(Trip::getCreatedAt, bucketEnd)));
        }

        return new DashboardChartsDto(days, tripTrend, gmvTrend, solo, carpool, accepted, inProgress,
                hourlyLabels, hourlyTrips);
    }

    private long countByStatus(String status) {
        return tripMapper.selectCount(new LambdaQueryWrapper<Trip>().eq(Trip::getStatus, status));
    }

    private long countByTripMode(String mode) {
        try {
            return tripMapper.selectCount(new LambdaQueryWrapper<Trip>().eq(Trip::getTripMode, mode));
        } catch (Exception ex) {
            // 旧库可能缺少 trip_mode 列，降级为 0 避免 chart-stats 500
            return 0L;
        }
    }
}
