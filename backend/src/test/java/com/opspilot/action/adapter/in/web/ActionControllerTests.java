package com.opspilot.action.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.action.application.ActionService;
import com.opspilot.action.domain.ActionActor;
import com.opspilot.action.domain.ActionDiff;
import com.opspilot.action.domain.ActionPreview;
import com.opspilot.action.domain.ActionRequest;
import com.opspilot.action.domain.ActionRiskLevel;
import com.opspilot.action.domain.ActionStatus;
import com.opspilot.action.domain.ActionType;
import com.opspilot.common.exception.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ActionController.class)
@Import(ApiExceptionHandler.class)
class ActionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActionService actionService;

    @Test
    void dryRunReturnsPreview() throws Exception {
        when(actionService.dryRun(eq("local"), any(ActionActor.class), any(ActionRequest.class)))
                .thenReturn(new ActionPreview(
                        "local",
                        ActionType.SCALE_DEPLOYMENT,
                        "sample-app",
                        "Deployment",
                        "payment-api",
                        ActionRiskLevel.LOW,
                        false,
                        true,
                        Map.of("desiredReplicas", 2),
                        Map.of("desiredReplicas", 3),
                        List.of(new ActionDiff("spec.replicas", "2", "3")),
                        List.of(),
                        "Deployment replica 수를 변경합니다.",
                        Instant.parse("2026-06-12T00:00:00Z")
                ));

        mockMvc.perform(post("/api/clusters/local/actions/dry-run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SCALE_DEPLOYMENT",
                                  "namespace": "sample-app",
                                  "targetKind": "Deployment",
                                  "targetName": "payment-api",
                                  "parameters": {
                                    "replicas": "3"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SCALE_DEPLOYMENT"))
                .andExpect(jsonPath("$.diff[0].field").value("spec.replicas"));
    }

    @Test
    void returnsAuditLogs() throws Exception {
        when(actionService.getAuditLogs("local", null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/clusters/local/actions/audit-logs"))
                .andExpect(status().isOk());
    }
}
