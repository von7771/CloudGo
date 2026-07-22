package com.von.admin.feign;

import com.von.common.dto.PageResult;
import com.von.common.dto.PassengerSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "passenger-service")
public interface PassengerAdminFeignClient {

    @GetMapping("/api/internal/passenger/list")
    PageResult<PassengerSummaryDto> listPassengers(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    @PutMapping("/api/internal/passenger/{passengerId}/ban")
    Map<String, Object> banPassenger(
            @PathVariable("passengerId") Long passengerId,
            @RequestParam("banned") boolean banned
    );

    @GetMapping("/api/internal/passenger/stats")
    Map<String, Long> stats();
}
