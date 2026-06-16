package com.opspilot.cost.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.cost.application.CostQueryService;
import com.opspilot.cost.domain.CostRecommendation;
import com.opspilot.cost.domain.CostRecommendationType;
import com.opspilot.cost.domain.CostRiskLevel;
import com.opspilot.cost.domain.CostSummary;
import com.opspilot.cost.domain.NamespaceCost;
import com.opspilot.cost.domain.WorkloadCost;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CostQueryController.class)
@Import(ApiExceptionHandler.class)
class CostQueryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CostQueryService costQueryService;

    @Test
    void returnsCostSummary() throws Exception {
        when(costQueryService.getSummary("local", "sample-app"))
                .thenReturn(new CostSummary(
                        "local",
                        "USD",
                        27.0,
                        14.5,
                        1,
                        2,
                        3,
                        "REQUEST_BASED_LOCAL_ESTIMATE",
                        Instant.parse("2026-06-04T00:30:00Z")
                ));

        mockMvc.perform(get("/api/clusters/local/cost/summary").queryParam("namespace", "sample-app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedMonthlyCost").value(27.0))
                .andExpect(jsonPath("$.recommendationCount").value(3));
    }

    @Test
    void returnsNamespaceCostsWorkloadsAndRecommendations() throws Exception {
        when(costQueryService.getNamespaceCosts("local", null))
                .thenReturn(List.of(new NamespaceCost("sample-app", "USD", 27.0, 14.5, 2, 3)));
        when(costQueryService.getWorkloadCosts("local", null))
                .thenReturn(List.of(new WorkloadCost(
                        "sample-app",
                        "Deployment",
                        "payment-api",
                        ResourceStatus.HEALTHY,
                        "USD",
                        27.0,
                        24.0,
                        3.0,
                        1.0,
                        1024L,
                        0.1,
                        256L,
                        10.0,
                        25.0,
                        true,
                        "PROMETHEUS_CONNECTED"
                )));
        when(costQueryService.getRecommendations("local", null))
                .thenReturn(List.of(new CostRecommendation(
                        "CPU_RIGHTSIZING:sample-app:Deployment:payment-api",
                        CostRecommendationType.CPU_RIGHTSIZING,
                        "sample-app",
                        "Deployment",
                        "payment-api",
                        "payment-api CPU request 조정 후보",
                        Map.of("cpuRequest", "1000m"),
                        Map.of("cpuRequest", "200m"),
                        "USD",
                        19.2,
                        CostRiskLevel.LOW,
                        0.78,
                        "CPU 사용량이 request 대비 낮아 CPU request를 줄일 여지가 있습니다."
                )));

        mockMvc.perform(get("/api/clusters/local/cost/namespaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].namespace").value("sample-app"));
        mockMvc.perform(get("/api/clusters/local/cost/workloads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("payment-api"));
        mockMvc.perform(get("/api/clusters/local/cost/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CPU_RIGHTSIZING"));
    }
}
