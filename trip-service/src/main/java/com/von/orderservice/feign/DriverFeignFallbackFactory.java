package com.von.orderservice.feign;

import com.von.common.dto.DriverLocationDto;
import com.von.orderservice.exception.UserServiceCallException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DriverFeignFallbackFactory implements FallbackFactory<DriverFeignClient> {

    @Override
    public DriverFeignClient create(Throwable cause) {
        return new DriverFeignClient() {
            @Override
            public Boolean isOnline(Long driverId) {
                throw new UserServiceCallException("查询司机在线状态失败: " + cause.getMessage());
            }

            @Override
            public Boolean creditBalance(Long driverId, BigDecimal amount) {
                throw new UserServiceCallException("司机入账失败: " + cause.getMessage());
            }

            @Override
            public DriverLocationDto getDriverLocation(Long driverId) {
                throw new UserServiceCallException("查询司机位置失败: " + cause.getMessage());
            }

            @Override
            public List<DriverLocationDto> listOnlineLocations() {
                throw new UserServiceCallException("查询在线司机位置失败: " + cause.getMessage());
            }
        };
    }
}
