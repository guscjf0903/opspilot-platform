package com.opspilot.trends.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.trends.application.TrendSnapshotService;
import com.opspilot.trends.domain.CostDailySnapshot;
import com.opspilot.trends.domain.KubernetesWorkloadUsageSnapshot;
import com.opspilot.trends.domain.TrendSnapshotCollectionResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TrendSnapshotController.class)
@Import(ApiExceptionHandler.class)
class TrendSnapshotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrendSnapshotService trendSnapshotService;

    @Test
    void collectsTrendSnapshots() throws Exception {
        when(trendSnapshotService.collect("local", "sample-app"))
                .thenReturn(new TrendSnapshotCollectionResult(
                        "local",
                        5,
                        5,
                        Instant.parse("2026-06-09T02:00:00Z")
                ));

        mockMvc.perform(post("/api/clusters/local/trends/snapshots").queryParam("namespace", "sample-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kubernetesSnapshotCount").value(5))
                .andExpect(jsonPath("$.costSnapshotCount").value(5));
    }

    @Test
    void returnsKubernetesAndCostSnapshots() throws Exception {
        when(trendSnapshotService.getKubernetesWorkloadUsageSnapshots(
                "local",
                "sample-app",
                "payment-api",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-09T00:00:00Z")
        )).thenReturn(List.of(new KubernetesWorkloadUsageSnapshot(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "local",
                "sample-app",
                "Deployment",
                "payment-api",
                ResourceStatus.HEALTHY,
                2,
                2,
                2,
                0.1,
                128L,
                0.02,
                0.03,
                64L,
                80L,
                20.0,
                50.0,
                true,
                "PROMETHEUS_CONNECTED",
                60,
                Instant.parse("2026-06-09T02:00:00Z")
        )));
        when(trendSnapshotService.getCostDailySnapshots(
                "local",
                "sample-app",
                "payment-api",
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-09")
        )).thenReturn(List.of(new CostDailySnapshot(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "local",
                "sample-app",
                "Deployment",
                "payment-api",
                ResourceStatus.HEALTHY,
                "USD",
                0.1,
                3.0,
                2.0,
                1.0,
                1.5,
                "OPENCOST_ALLOCATION",
                LocalDate.parse("2026-06-09"),
                Instant.parse("2026-06-09T02:00:00Z")
        )));

        mockMvc.perform(get("/api/clusters/local/trends/kubernetes/workloads")
                        .queryParam("namespace", "sample-app")
                        .queryParam("workloadName", "payment-api")
                        .queryParam("from", "2026-06-01T00:00:00Z")
                        .queryParam("to", "2026-06-09T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workloadName").value("payment-api"))
                .andExpect(jsonPath("$[0].cpuUsagePercent").value(20.0));
        mockMvc.perform(get("/api/clusters/local/trends/cost/workloads")
                        .queryParam("namespace", "sample-app")
                        .queryParam("workloadName", "payment-api")
                        .queryParam("from", "2026-06-01")
                        .queryParam("to", "2026-06-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estimatedMonthlyCost").value(3.0))
                .andExpect(jsonPath("$[0].estimatedMonthlySaving").value(1.5));
    }
}
