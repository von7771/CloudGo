package com.von.orderservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.von.orderservice.entity.CarpoolPool;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CarpoolPoolMapper extends BaseMapper<CarpoolPool> {

    @Update("UPDATE t_carpool_pool SET current_seats = current_seats + 1 WHERE id = #{poolId} AND current_seats < max_seats")
    int incrementSeats(@Param("poolId") Long poolId);

    @Update("UPDATE t_carpool_pool SET current_seats = current_seats - 1 WHERE id = #{poolId} AND current_seats > 0")
    int decrementSeats(@Param("poolId") Long poolId);

    @Update("UPDATE t_carpool_pool SET status = #{status} WHERE id = #{poolId}")
    int updateStatus(@Param("poolId") Long poolId, @Param("status") String status);
}
