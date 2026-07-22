package com.von.orderservice.carpool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.von.common.dto.SmartCarpoolBundleDto;
import com.von.common.dto.SmartCarpoolTripItemDto;
import com.von.common.enums.TripStatus;
import com.von.orderservice.entity.Trip;
import com.von.orderservice.mapper.TripMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 智能拼车打包：拼车池整包 + 独享单路线聚类，每包最多 3 单。
 */
@Service
public class SmartCarpoolMatchService {

    public static final double BUNDLE_PAIR_THRESHOLD = 0.55;
    private static final int MAX_BUNDLE_SIZE = 3;
    private static final int MAX_BUNDLES = 5;

    private final TripMapper tripMapper;
    private final RouteSimilarityService routeSimilarityService;

    public SmartCarpoolMatchService(TripMapper tripMapper, RouteSimilarityService routeSimilarityService) {
        this.tripMapper = tripMapper;
        this.routeSimilarityService = routeSimilarityService;
    }

    public List<SmartCarpoolBundleDto> findSmartBundles(int limit) {
        List<SmartCarpoolBundleDto> bundles = new ArrayList<>();
        Set<Long> usedTripIds = new HashSet<>();

        // 1. 拼车池：同池多乘客各起终点，整包推荐
        List<Trip> poolTrips = tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getStatus, TripStatus.DISPATCHING.name())
                .isNull(Trip::getDriverId)
                .isNotNull(Trip::getPoolId)
                .orderByAsc(Trip::getId));

        Map<Long, List<Trip>> byPool = poolTrips.stream()
                .collect(Collectors.groupingBy(Trip::getPoolId));

        for (List<Trip> poolGroup : byPool.values()) {
            if (poolGroup.size() < 2 || bundles.size() >= limit) {
                continue;
            }
            List<Trip> cluster = poolGroup.size() > MAX_BUNDLE_SIZE
                    ? poolGroup.subList(0, MAX_BUNDLE_SIZE)
                    : poolGroup;
            bundles.add(toBundle(cluster, "拼车池 · " + cluster.size() + " 位乘客 · 各起终点独立接送"));
            cluster.forEach(t -> usedTripIds.add(t.getId()));
        }

        // 1b. 不同拼车池各 1 单：路线相似时打包（司机一键接多单拼车）
        List<Trip> singlePoolTrips = byPool.values().stream()
                .filter(list -> list.size() == 1)
                .map(list -> list.getFirst())
                .filter(t -> !usedTripIds.contains(t.getId()))
                .toList();

        for (Trip seed : singlePoolTrips) {
            if (usedTripIds.contains(seed.getId()) || bundles.size() >= limit) {
                continue;
            }
            List<Trip> cluster = new ArrayList<>();
            cluster.add(seed);

            for (Trip candidate : singlePoolTrips) {
                if (cluster.size() >= MAX_BUNDLE_SIZE) {
                    break;
                }
                if (candidate.getId().equals(seed.getId()) || usedTripIds.contains(candidate.getId())) {
                    continue;
                }
                if (cluster.stream().allMatch(t ->
                        routeSimilarityService.scoreTrips(t, candidate) >= BUNDLE_PAIR_THRESHOLD)) {
                    cluster.add(candidate);
                }
            }

            if (cluster.size() >= 2) {
                cluster.forEach(t -> usedTripIds.add(t.getId()));
                bundles.add(toBundle(cluster, cluster.size() + " 单拼车同向 · 各独立拼车池 · 一键接单"));
            }
        }

        // 2. 独享单：路线相似度聚类
        List<Trip> soloPending = listSoloCandidates().stream()
                .filter(t -> !usedTripIds.contains(t.getId()))
                .toList();

        for (Trip seed : soloPending) {
            if (usedTripIds.contains(seed.getId()) || bundles.size() >= limit) {
                continue;
            }
            List<Trip> cluster = new ArrayList<>();
            cluster.add(seed);
            usedTripIds.add(seed.getId());

            for (Trip candidate : soloPending) {
                if (cluster.size() >= MAX_BUNDLE_SIZE) {
                    break;
                }
                if (usedTripIds.contains(candidate.getId())) {
                    continue;
                }
                if (cluster.stream().allMatch(t ->
                        routeSimilarityService.scoreTrips(t, candidate) >= BUNDLE_PAIR_THRESHOLD)) {
                    cluster.add(candidate);
                    usedTripIds.add(candidate.getId());
                }
            }

            if (cluster.size() >= 2) {
                bundles.add(toBundle(cluster, null));
            }
        }

        return bundles.stream()
                .sorted(Comparator.comparingDouble(SmartCarpoolBundleDto::similarityScore).reversed())
                .limit(Math.min(limit, MAX_BUNDLES))
                .toList();
    }

    public List<SmartCarpoolBundleDto> findBundlesForTrip(Long tripId, int limit) {
        Trip target = tripMapper.selectById(tripId);
        if (target == null || !TripStatus.DISPATCHING.name().equals(target.getStatus())) {
            return List.of();
        }

        if (target.getPoolId() != null) {
            List<Trip> poolGroup = tripMapper.selectByPoolId(target.getPoolId()).stream()
                    .filter(t -> TripStatus.DISPATCHING.name().equals(t.getStatus()))
                    .filter(t -> t.getDriverId() == null)
                    .toList();
            if (poolGroup.size() >= 2) {
                return List.of(toBundle(poolGroup, "拼车池同包订单"));
            }
            return List.of();
        }

        List<Trip> pending = listSoloCandidates().stream()
                .filter(t -> !t.getId().equals(tripId))
                .toList();

        List<Trip> cluster = new ArrayList<>();
        cluster.add(target);
        for (Trip candidate : pending) {
            if (cluster.size() >= MAX_BUNDLE_SIZE) {
                break;
            }
            if (cluster.stream().allMatch(t ->
                    routeSimilarityService.scoreTrips(t, candidate) >= BUNDLE_PAIR_THRESHOLD)) {
                cluster.add(candidate);
            }
        }

        if (cluster.size() < 2) {
            return List.of();
        }
        return List.of(toBundle(cluster, null));
    }

    private List<Trip> listSoloCandidates() {
        return tripMapper.selectList(new LambdaQueryWrapper<Trip>()
                .eq(Trip::getStatus, TripStatus.DISPATCHING.name())
                .isNull(Trip::getDriverId)
                .isNull(Trip::getPoolId)
                .orderByAsc(Trip::getId));
    }

    private SmartCarpoolBundleDto toBundle(List<Trip> cluster, String customSummary) {
        double avgScore = 0;
        int pairs = 0;
        for (int i = 0; i < cluster.size(); i++) {
            for (int j = i + 1; j < cluster.size(); j++) {
                avgScore += routeSimilarityService.scoreTrips(cluster.get(i), cluster.get(j));
                pairs++;
            }
        }
        double score = pairs > 0 ? avgScore / pairs : 1.0;

        BigDecimal total = cluster.stream()
                .map(Trip::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SmartCarpoolTripItemDto> items = cluster.stream()
                .map(t -> new SmartCarpoolTripItemDto(
                        t.getId(), t.getStartPoint(), t.getEndPoint(), t.getEstimatedAmount(), t.getTripMode()))
                .toList();

        String summary = customSummary != null
                ? customSummary
                : items.size() + " 单同向 · 终点相近 · 相似度 " + String.format("%.0f%%", score * 100);

        return new SmartCarpoolBundleDto(
                UUID.randomUUID().toString().substring(0, 8),
                score,
                summary,
                total,
                items
        );
    }
}
