package com.opspilot.ai.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.ai.domain.AiTokenUsage;
import com.opspilot.ai.application.IncidentAnalysisService;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import com.opspilot.ai.domain.IncidentAnalysisRequest;
import com.opspilot.ai.domain.IncidentEvidence;
import com.opspilot.ai.domain.RecommendedAction;
import com.opspilot.ai.domain.RootCauseCandidate;
import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = IncidentAnalysisController.class)
@Import(ApiExceptionHandler.class)
class IncidentAnalysisControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentAnalysisService incidentAnalysisService;

    @Test
    void createsIncidentAnalysisReport() throws Exception {
        when(incidentAnalysisService.analyzeIncident(eq("local"), any(IncidentAnalysisRequest.class)))
                .thenReturn(new IncidentAnalysisReport(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "local",
                        "sample-app",
                        "Deployment",
                        "payment-api",
                        ResourceStatus.WARNING,
                        "Deployment sample-app/payment-api는 현재 경고 상태입니다.",
                        List.of("sample-app/Deployment/payment-api"),
                        List.of(new RootCauseCandidate(
                                "워크로드 health check 또는 rollout 가용성 문제 가능성",
                                0.72,
                                List.of("target-1")
                        )),
                        List.of(new IncidentEvidence(
                                "target-1",
                                "target",
                                "Deployment 상태",
                                "대상 상태 메시지: Available replicas 1/2",
                                ResourceStatus.WARNING,
                                Instant.parse("2026-06-04T00:30:00Z")
                        )),
                        List.of(new RecommendedAction(
                                "REVIEW_ROLLOUT_OR_RESTART",
                                "medium",
                                "최근 rollout history를 비교하세요."
                        )),
                        List.of("Event 발생 시각 전후의 application log를 확인하세요."),
                        "stub",
                        "stub-rule-engine",
                        null,
                        AiTokenUsage.zero(),
                        0L,
                        Instant.parse("2026-06-04T00:31:00Z")
                ));

        mockMvc.perform(post("/api/clusters/local/analysis/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "namespace": "sample-app",
                                  "targetKind": "Deployment",
                                  "targetName": "payment-api",
                                  "timeRangeMinutes": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.severity").value("warning"))
                .andExpect(jsonPath("$.rootCauseCandidates[0].evidenceIds[0]").value("target-1"))
                .andExpect(jsonPath("$.provider").value("stub"))
                .andExpect(jsonPath("$.model").value("stub-rule-engine"))
                .andExpect(jsonPath("$.tokenUsage.totalTokens").value(0));
    }
}
