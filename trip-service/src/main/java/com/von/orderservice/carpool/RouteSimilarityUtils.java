package com.von.orderservice.carpool;

import org.springframework.util.StringUtils;

/**
 * 轻量路线相似度（纯几何计算，零 Token 消耗）。
 */
public final class RouteSimilarityUtils {

    private static final double EARTH_RADIUS_M = 6371000.0;

    private RouteSimilarityUtils() {
    }

    public record LngLat(double longitude, double latitude) {
    }

    public static LngLat parseLocation(String location) {
        if (!StringUtils.hasText(location)) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new LngLat(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static double haversineMeters(LngLat a, LngLat b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(h));
    }

    /** 方位角（度，0=北，顺时针） */
    public static double bearingDegrees(LngLat from, LngLat to) {
        if (from == null || to == null) {
            return 0;
        }
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double dLng = Math.toRadians(to.longitude() - from.longitude());
        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public static double bearingSimilarity(double b1, double b2) {
        double diff = Math.abs(b1 - b2);
        diff = Math.min(diff, 360 - diff);
        return Math.max(0, 1 - diff / 90.0);
    }

    /**
     * 综合相似度 0~1。终点权重最高，不要求起点终点完全一致。
     */
    public static double score(
            String startA, String endA,
            String startB, String endB
    ) {
        LngLat sA = parseLocation(startA);
        LngLat eA = parseLocation(endA);
        LngLat sB = parseLocation(startB);
        LngLat eB = parseLocation(endB);
        if (eA == null || eB == null) {
            return 0;
        }

        double endDist = haversineMeters(eA, eB);
        double endScore = clamp01(1 - endDist / 5000.0);

        double startScore = 0.5;
        if (sA != null && sB != null) {
            double startDist = haversineMeters(sA, sB);
            startScore = clamp01(1 - startDist / 8000.0);
        }

        double bearingScore = 0.5;
        if (sA != null && sB != null) {
            bearingScore = bearingSimilarity(
                    bearingDegrees(sA, eA),
                    bearingDegrees(sB, eB)
            );
        }

        return 0.45 * endScore + 0.25 * startScore + 0.30 * bearingScore;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
