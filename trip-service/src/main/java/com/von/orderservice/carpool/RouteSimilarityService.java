package com.von.orderservice.carpool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.von.orderservice.entity.CarpoolPool;
import com.von.orderservice.entity.Trip;
import com.von.orderservice.map.dto.DrivingRouteResult;
import com.von.orderservice.mapper.CarpoolPoolMapper;
import com.von.orderservice.mapper.TripMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RouteSimilarityService {

    /** 加入拼车池的最低路线相似度 */
    public static final double POOL_JOIN_THRESHOLD = 0.62;

    private final CarpoolPoolMapper carpoolPoolMapper;
    private final TripMapper tripMapper;

    public RouteSimilarityService(CarpoolPoolMapper carpoolPoolMapper, TripMapper tripMapper) {
        this.carpoolPoolMapper = carpoolPoolMapper;
        this.tripMapper = tripMapper;
    }

    public Optional<CarpoolPool> findBestWaitingPool(DrivingRouteResult route) {
        return findBestPool(route, "WAITING");
    }

    /** 已派单拼车池仍可加入（同路线、未满员） */
    public Optional<CarpoolPool> findBestDispatchingPool(DrivingRouteResult route) {
        return findBestPool(route, "DISPATCHING");
    }

    private Optional<CarpoolPool> findBestPool(DrivingRouteResult route, String poolStatus) {
        List<CarpoolPool> pools = carpoolPoolMapper.selectList(new LambdaQueryWrapper<CarpoolPool>()
                .eq(CarpoolPool::getStatus, poolStatus)
                .lt(CarpoolPool::getCurrentSeats, 4));

        return pools.stream()
                .map(pool -> new ScoredPool(pool, scorePool(route, pool)))
                .filter(sp -> sp.score() >= POOL_JOIN_THRESHOLD)
                .max(Comparator.comparingDouble(ScoredPool::score))
                .map(ScoredPool::pool);
    }

    public double scoreTrips(Trip a, Trip b) {
        return RouteSimilarityUtils.score(
                a.getStartLocation(), a.getEndLocation(),
                b.getStartLocation(), b.getEndLocation()
        );
    }

    private double scorePool(DrivingRouteResult route, CarpoolPool pool) {
        List<Trip> trips = tripMapper.selectByPoolId(pool.getId());
        if (trips.isEmpty()) {
            return 0;
        }
        Trip anchor = trips.getFirst();
        return RouteSimilarityUtils.score(
                route.originLocation(), route.destinationLocation(),
                anchor.getStartLocation(), anchor.getEndLocation()
        );
    }

    private record ScoredPool(CarpoolPool pool, double score) {
    }
}
