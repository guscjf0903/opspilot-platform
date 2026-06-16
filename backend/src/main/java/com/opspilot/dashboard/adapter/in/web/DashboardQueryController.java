package com.opspilot.dashboard.adapter.in.web;

import com.opspilot.dashboard.application.DashboardQueryService;
import com.opspilot.dashboard.domain.DashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}/dashboard")
@RequiredArgsConstructor
public class DashboardQueryController {

    private final DashboardQueryService dashboardQueryService;

    @GetMapping
    public DashboardSummary getDashboard(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace
    ) {
        return dashboardQueryService.getDashboard(clusterId, namespace);
    }
}
