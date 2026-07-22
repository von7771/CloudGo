package com.von.orderservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.von.orderservice.entity.Trip;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TripMapper extends BaseMapper<Trip> {

    @org.apache.ibatis.annotations.Update("""
            UPDATE t_trip SET status = #{toStatus}, driver_id = #{driverId}, updated_at = NOW()
            WHERE id = #{tripId} AND status = #{fromStatus}
            """)
    int acceptTrip(@Param("tripId") Long tripId,
                   @Param("driverId") Long driverId,
                   @Param("fromStatus") String fromStatus,
                   @Param("toStatus") String toStatus);

    @org.apache.ibatis.annotations.Update("""
            UPDATE t_trip SET status = #{toStatus}, updated_at = NOW()
            WHERE id = #{tripId} AND status = #{fromStatus}
            """)
    int updateStatus(@Param("tripId") Long tripId,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus);

    @Select("SELECT * FROM t_trip WHERE pool_id = #{poolId} ORDER BY id")
    List<Trip> selectByPoolId(@Param("poolId") Long poolId);
}
