package com.opspilot.kubernetes.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.cluster.adapter.in.web.ClusterQueryController;
import com.opspilot.cluster.application.ClusterQueryService;
import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.application.UnknownClusterException;
import com.opspilot.kubernetes.domain.PodSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        ClusterQueryController.class,
        KubernetesInventoryController.class
})
@Import(ApiExceptionHandler.class)
class KubernetesInventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClusterQueryService clusterQueryService;

    @MockitoBean
    private KubernetesInventoryService kubernetesInventoryService;

    @Test
    void returnsPodInventory() throws Exception {
        when(kubernetesInventoryService.getPods("local", "sample-app"))
                .thenReturn(List.of(new PodSummary(
                        "Pod",
                        "sample-app",
                        "worker-1",
                        ResourceStatus.CRITICAL,
                        "CrashLoopBackOff",
                        "Container is waiting: CrashLoopBackOff",
                        Instant.parse("2026-06-01T00:00:00Z"),
                        "Running",
                        "demo-control-plane",
                        3,
                        List.of("busybox:1.37.0")
                )));

        mockMvc.perform(get("/api/clusters/local/namespaces/sample-app/pods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("worker-1"))
                .andExpect(jsonPath("$[0].status").value("critical"))
                .andExpect(jsonPath("$[0].reason").value("CrashLoopBackOff"));
    }

    @Test
    void returnsNotFoundForUnknownCluster() throws Exception {
        when(kubernetesInventoryService.getNodes("missing"))
                .thenThrow(new UnknownClusterException("missing"));

        mockMvc.perform(get("/api/clusters/missing/nodes"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLUSTER_NOT_FOUND"));
    }
}
