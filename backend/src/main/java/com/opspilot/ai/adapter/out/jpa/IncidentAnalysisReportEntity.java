package com.opspilot.ai.adapter.out.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.ai.domain.AiTokenUsage;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import com.opspilot.ai.domain.IncidentEvidence;
import com.opspilot.ai.domain.RecommendedAction;
import com.opspilot.ai.domain.RootCauseCandidate;
import com.opspilot.kubernetes.domain.ResourceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incident_analysis_reports")
class IncidentAnalysisReportEntity {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<RootCauseCandidate>> ROOT_CAUSE_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<IncidentEvidence>> EVIDENCE_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<RecommendedAction>> ACTION_LIST = new TypeReference<>() {
    };

    @Id
    private UUID id;

    @Column(name = "cluster_id", nullable = false)
    private String clusterId;

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "target_kind", nullable = false)
    private String targetKind;

    @Column(name = "target_name", nullable = false)
    private String targetName;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus severity;

    @Column(name = "summary", nullable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "impact_scope", nullable = false, columnDefinition = "jsonb")
    private JsonNode impactScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "root_cause_candidates", nullable = false, columnDefinition = "jsonb")
    private JsonNode rootCauseCandidates;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence", nullable = false, columnDefinition = "jsonb")
    private JsonNode evidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", nullable = false, columnDefinition = "jsonb")
    private JsonNode recommendations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_checks", nullable = false, columnDefinition = "jsonb")
    private JsonNode nextChecks;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "model")
    private String model;

    @Column(name = "provider_response_id")
    private String providerResponseId;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "cached_input_tokens", nullable = false)
    private int cachedInputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "reasoning_output_tokens", nullable = false)
    private int reasoningOutputTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IncidentAnalysisReportEntity() {
    }

    private IncidentAnalysisReportEntity(IncidentAnalysisReport report, ObjectMapper objectMapper) {
        this.id = report.analysisId();
        this.clusterId = report.clusterId();
        this.namespace = report.namespace();
        this.targetKind = report.targetKind();
        this.targetName = report.targetName();
        this.severity = report.severity();
        this.summary = report.summary();
        this.impactScope = objectMapper.valueToTree(report.impactScope());
        this.rootCauseCandidates = objectMapper.valueToTree(report.rootCauseCandidates());
        this.evidence = objectMapper.valueToTree(report.evidence());
        this.recommendations = objectMapper.valueToTree(report.recommendations());
        this.nextChecks = objectMapper.valueToTree(report.nextChecks());
        this.provider = report.provider();
        this.model = report.model();
        this.providerResponseId = report.providerResponseId();
        AiTokenUsage usage = report.tokenUsage() == null ? AiTokenUsage.zero() : report.tokenUsage();
        this.inputTokens = usage.inputTokens();
        this.cachedInputTokens = usage.cachedInputTokens();
        this.outputTokens = usage.outputTokens();
        this.reasoningOutputTokens = usage.reasoningOutputTokens();
        this.totalTokens = usage.totalTokens();
        this.latencyMs = report.latencyMs();
        this.createdAt = report.createdAt();
    }

    static IncidentAnalysisReportEntity fromDomain(IncidentAnalysisReport report, ObjectMapper objectMapper) {
        return new IncidentAnalysisReportEntity(report, objectMapper);
    }

    IncidentAnalysisReport toDomain(ObjectMapper objectMapper) {
        return new IncidentAnalysisReport(
                id,
                clusterId,
                namespace,
                targetKind,
                targetName,
                severity,
                summary,
                objectMapper.convertValue(impactScope, STRING_LIST),
                objectMapper.convertValue(rootCauseCandidates, ROOT_CAUSE_LIST),
                objectMapper.convertValue(evidence, EVIDENCE_LIST),
                objectMapper.convertValue(recommendations, ACTION_LIST),
                objectMapper.convertValue(nextChecks, STRING_LIST),
                provider,
                model,
                providerResponseId,
                new AiTokenUsage(
                        inputTokens,
                        cachedInputTokens,
                        outputTokens,
                        reasoningOutputTokens,
                        totalTokens
                ),
                latencyMs,
                createdAt
        );
    }
}
