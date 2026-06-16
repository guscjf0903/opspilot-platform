package com.opspilot.topology.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.topology.application.TopologyQueryService;
import com.opspilot.topology.domain.TopologyEdge;
import com.opspilot.topology.domain.TopologyGraph;
import com.opspilot.topology.domain.TopologyNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TopologyQueryController.class)
@Import(ApiExceptionHandler.class)
class TopologyQueryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopologyQueryService topologyQueryService;

    @Test
    void returnsTopologyGraphForRootWorkload() throws Exception {
        when(topologyQueryService.getTopology("local", "sample-app", "Deployment", "payment-api"))
                .thenReturn(new TopologyGraph(
                        "local",
                        "sample-app",
                        "deployment:sample-app:payment-api",
                        Instant.parse("2026-06-02T00:30:00Z"),
                        List.of(new TopologyNode(
                                "deployment:sample-app:payment-api",
                                "Deployment",
                                "sample-app",
                                "payment-api",
                                ResourceStatus.HEALTHY,
                                "Healthy",
                                "deployment status"
                        )),
                        List.of(new TopologyEdge(
                                "deployment:sample-app:payment-api",
                                "replicaset:sample-app:payment-api-rs",
                                "owns"
                        ))
                ));

        mockMvc.perform(get("/api/clusters/local/namespaces/sample-app/topology/Deployment/payment-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootNodeId").value("deployment:sample-app:payment-api"))
                .andExpect(jsonPath("$.nodes[0].status").value("healthy"))
                .andExpect(jsonPath("$.edges[0].type").value("owns"));
    }
}
