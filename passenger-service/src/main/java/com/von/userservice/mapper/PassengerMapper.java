package com.von.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.von.userservice.entity.Passenger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface PassengerMapper extends BaseMapper<Passenger> {

    @Update("UPDATE t_passenger SET balance = balance - #{amount} WHERE id = #{passengerId} AND balance >= #{amount}")
    int deductBalance(@Param("passengerId") Long passengerId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_passenger SET balance = balance + #{amount} WHERE id = #{passengerId}")
    int creditBalance(@Param("passengerId") Long passengerId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_passenger SET nickname = #{nickname} WHERE id = #{passengerId}")
    int updateNickname(@Param("passengerId") Long passengerId, @Param("nickname") String nickname);

    @Update("UPDATE t_passenger SET avatar_object = #{objectKey} WHERE id = #{passengerId}")
    int updateAvatarObject(@Param("passengerId") Long passengerId, @Param("objectKey") String objectKey);
}
