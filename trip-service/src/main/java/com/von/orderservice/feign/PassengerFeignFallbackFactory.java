package com.von.orderservice.feign;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PassengerFeignFallbackFactory implements FallbackFactory<PassengerFeignClient> {

    private final UserFeignExceptionTranslator exceptionTranslator;

    public PassengerFeignFallbackFactory(UserFeignExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator;
    }

    @Override
    public PassengerFeignClient create(Throwable cause) {
        return new PassengerFeignClient() {
            @Override
            public Integer getPassengerCredit(Long passengerId) {
                throw exceptionTranslator.translate("查询乘客信用分", cause);
            }

            @Override
            public Boolean checkBalance(Long passengerId, BigDecimal amount) {
                throw exceptionTranslator.translate("校验乘客余额", cause);
            }

            @Override
            public Boolean deductBalance(Long passengerId, BigDecimal amount) {
                throw exceptionTranslator.translate("乘客扣款", cause);
            }
        };
    }
}
