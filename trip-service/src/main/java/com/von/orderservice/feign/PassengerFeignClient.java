package com.von.orderservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "passenger-service", fallbackFactory = PassengerFeignFallbackFactory.class)
public interface PassengerFeignClient {

    @GetMapping("/api/internal/passenger/credit")
    Integer getPassengerCredit(@RequestParam("passengerId") Long passengerId);

    @GetMapping("/api/internal/passenger/balance/check")
    Boolean checkBalance(@RequestParam("passengerId") Long passengerId, @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/internal/passenger/deduct")
    Boolean deductBalance(@RequestParam("passengerId") Long passengerId, @RequestParam("amount") BigDecimal amount);
}
