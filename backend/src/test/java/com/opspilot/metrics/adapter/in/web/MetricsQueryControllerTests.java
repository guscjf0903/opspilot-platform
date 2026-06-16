package com.opspilot.metrics.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.MetricSeries;
import com.opspilot.metrics.domain.ResourceMetricSummary;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MetricsQueryController.class)
@Import(ApiExceptionHandler.class)
class MetricsQueryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricsQueryService metricsQueryService;

    @Test
    void returnsWorkloadMetrics() throws Exception {
        MetricQueryWindow window = MetricQueryWindow.recent(30, Instant.parse("2026-06-02T00:30:00Z"));
        when(metricsQueryService.getWorkloadMetrics(eq("local"), eq("sample-app"), eq("pod"), eq("worker-1"), eq(30)))
                .thenReturn(ResourceMetrics.available(
                        "local",
                        "sample-app",
                        "pod",
                        "worker-1",
                        window.end(),
                        window,
                        new MetricSeries("cpu", "cores", List.of()),
                        new MetricSeries("memory", "bytes", List.of()),
                        new ResourceMetricSummary(0.12, 256L, 0.5, 512L, null, null, 24.0, 50.0)
                ));

        mockMvc.perform(get("/api/clusters/local/namespaces/sample-app/workloads/pod/worker-1/metrics")
                        .queryParam("rangeMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("available"))
                .andExpect(jsonPath("$.kind").value("pod"))
                .andExpect(jsonPath("$.summary.cpuUsagePercent").value(24.0));
    }
}
