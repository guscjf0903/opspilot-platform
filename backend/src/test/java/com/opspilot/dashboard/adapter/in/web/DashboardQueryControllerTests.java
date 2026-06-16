package com.opspilot.dashboard.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.dashboard.application.DashboardQueryService;
import com.opspilot.dashboard.domain.DashboardCounts;
import com.opspilot.dashboard.domain.DashboardExternalSignal;
import com.opspilot.dashboard.domain.DashboardSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardQueryController.class)
@Import(ApiExceptionHandler.class)
class DashboardQueryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardQueryService dashboardQueryService;

    @Test
    void returnsDashboardSummaryForNamespaceFilter() throws Exception {
        when(dashboardQueryService.getDashboard("local", "sample-app"))
                .thenReturn(new DashboardSummary(
                        ResourceStatus.WARNING,
                        "sample-app",
                        Instant.parse("2026-06-02T00:30:00Z"),
                        new DashboardCounts(6, 1, 5, 6, 0, 1, 2),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        DashboardExternalSignal.unavailable("PROMETHEUS_NOT_CONNECTED"),
                        DashboardExternalSignal.unavailable("KAFKA_MONITORING_NOT_CONNECTED"),
                        DashboardExternalSignal.unavailable("OPENCOST_NOT_CONNECTED")
                ));

        mockMvc.perform(get("/api/clusters/local/dashboard").queryParam("namespace", "sample-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clusterStatus").value("warning"))
                .andExpect(jsonPath("$.selectedNamespace").value("sample-app"))
                .andExpect(jsonPath("$.counts.namespaceCount").value(6))
                .andExpect(jsonPath("$.nodeUsage.status").value("unavailable"));
    }
}
