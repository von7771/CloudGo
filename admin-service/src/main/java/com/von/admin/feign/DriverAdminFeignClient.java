package com.von.admin.feign;

import com.von.common.dto.DriverSummaryDto;
import com.von.common.dto.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "driver-service")
public interface DriverAdminFeignClient {

    @GetMapping("/api/internal/driver/list")
    PageResult<DriverSummaryDto> listDrivers(
            @RequestParam(value = "auditStatus", required = false) String auditStatus,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    @PutMapping("/api/internal/driver/{driverId}/audit")
    DriverSummaryDto auditDriver(
            @PathVariable("driverId") Long driverId,
            @RequestParam("auditStatus") String auditStatus
    );

    @GetMapping("/api/internal/driver/stats")
    Map<String, Long> stats();

    @GetMapping("/api/internal/driver/{driverId}/documents/content")
    byte[] getDocumentContent(@PathVariable("driverId") Long driverId, @RequestParam("docType") String docType);
}
