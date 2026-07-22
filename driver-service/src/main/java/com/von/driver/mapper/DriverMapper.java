package com.von.driver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.von.driver.entity.Driver;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface DriverMapper extends BaseMapper<Driver> {

    @Update("UPDATE t_driver SET balance = balance + #{amount} WHERE id = #{driverId}")
    int creditBalance(@Param("driverId") Long driverId, @Param("amount") BigDecimal amount);

    @Update("UPDATE t_driver SET audit_status = #{auditStatus} WHERE id = #{driverId}")
    int updateAuditStatus(@Param("driverId") Long driverId, @Param("auditStatus") String auditStatus);

    @Update("UPDATE t_driver SET license_image_object = #{objectKey} WHERE id = #{driverId}")
    int updateLicenseObject(@Param("driverId") Long driverId, @Param("objectKey") String objectKey);

    @Update("UPDATE t_driver SET id_card_image_object = #{objectKey} WHERE id = #{driverId}")
    int updateIdCardObject(@Param("driverId") Long driverId, @Param("objectKey") String objectKey);

    @Update("UPDATE t_driver SET nickname = #{nickname} WHERE id = #{driverId}")
    int updateNickname(@Param("driverId") Long driverId, @Param("nickname") String nickname);

    @Update("UPDATE t_driver SET avatar_object = #{objectKey} WHERE id = #{driverId}")
    int updateAvatarObject(@Param("driverId") Long driverId, @Param("objectKey") String objectKey);
}
