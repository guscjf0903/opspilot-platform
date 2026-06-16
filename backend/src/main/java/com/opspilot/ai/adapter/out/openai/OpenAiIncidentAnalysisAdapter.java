package com.opspilot.ai.adapter.out.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opspilot.ai.application.AiProviderException;
import com.opspilot.ai.application.port.out.AiIncidentAnalysisPort;
import com.opspilot.ai.config.AiProperties;
import com.opspilot.ai.domain.AiTokenUsage;
import com.opspilot.ai.domain.IncidentAnalysisContext;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import com.opspilot.ai.domain.IncidentEvidence;
import com.opspilot.ai.domain.RecommendedAction;
import com.opspilot.ai.domain.RootCauseCandidate;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.metrics.domain.MetricsAvailabilityStatus;
import com.opspilot.metrics.domain.ResourceMetrics;
import com.opspilot.topology.domain.TopologyGraph;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "opspilot.ai", name = "provider", havingValue = "openai")
public class OpenAiIncidentAnalysisAdapter implements AiIncidentAnalysisPort {

    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 1200;
    private static final int TOPOLOGY_NODE_LIMIT = 12;
    private static final int TOPOLOGY_EDGE_LIMIT = 30;

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient openAiRestClient;

    public OpenAiIncidentAnalysisAdapter(
            RestClient.Builder restClientBuilder,
            AiProperties aiProperties,
            ObjectMapper objectMapper
    ) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.openAiRestClient = restClientBuilder
                .clone()
                .baseUrl(aiProperties.getOpenai().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getOpenai().getApiKey())
                .build();
    }

    @Override
    public IncidentAnalysisReport analyze(IncidentAnalysisContext context) {
        validateConfiguration();

        List<IncidentEvidence> inputEvidence = inputEvidence(context);
        ObjectNode request = requestBody(context, inputEvidence);
        long startedAt = System.nanoTime();

        try {
            JsonNode response = openAiRestClient.post()
                    .uri("/responses")
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            long latencyMs = elapsedMs(startedAt);

            return toReport(context, inputEvidence, response, latencyMs);
        } catch (RestClientException exception) {
            throw new AiProviderException("OpenAI Responses API request failed.", exception);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(aiProperties.getOpenai().getApiKey())) {
            throw new AiProviderException("OPENAI_API_KEY is required when AI_PROVIDER=openai.");
        }

        if (!StringUtils.hasText(aiProperties.getOpenai().getModel())) {
            throw new AiProviderException("OPENAI_MODEL is required when AI_PROVIDER=openai.");
        }
    }

    private ObjectNode requestBody(IncidentAnalysisContext context, List<IncidentEvidence> inputEvidence) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", aiProperties.getOpenai().getModel());
        request.put("store", false);
        request.put("max_output_tokens", maxOutputTokens());

        ObjectNode reasoning = request.putObject("reasoning");
        reasoning.put("effort", aiProperties.getOpenai().getReasoningEffort());

        ObjectNode text = request.putObject("text");
        text.put("verbosity", "low");
        text.set("format", analysisReportResponseFormat());

        ArrayNode input = request.putArray("input");
        input.add(inputMessage("developer", developerPrompt()));
        input.add(inputMessage("user", analysisContextJson(context, inputEvidence)));

        return request;
    }

    private ObjectNode analysisReportResponseFormat() {
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", "opspilot_incident_analysis_report");
        format.put("strict", true);

        ObjectNode schema = format.putObject("schema");
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        required(schema, "summary", "severity", "impactScope", "rootCauseCandidates", "evidence",
                "recommendations", "nextChecks");

        ObjectNode properties = schema.putObject("properties");
        properties.set("summary", stringSchema("Korean incident summary. Must describe candidates, not confirmed facts."));
        properties.set("severity", severitySchema());
        properties.set("impactScope", stringArraySchema("Affected namespace, workload, pod, node, or dependency names."));
        properties.set("rootCauseCandidates", rootCauseCandidatesSchema());
        properties.set("evidence", evidenceSchema());
        properties.set("recommendations", recommendationsSchema());
        properties.set("nextChecks", stringArraySchema("Concrete Korean follow-up checks for the operator."));

        return format;
    }

    private ObjectNode rootCauseCandidatesSchema() {
        ObjectNode schema = arraySchema("Candidate root causes grounded by input evidence ids.");
        ObjectNode item = objectItem(schema);
        required(item, "title", "confidence", "evidenceIds");

        ObjectNode properties = item.putObject("properties");
        properties.set("title", stringSchema("Korean candidate title."));
        properties.set("confidence", numberSchema("Confidence from 0.0 to 1.0."));
        properties.set("evidenceIds", stringArraySchema("Only ids from inputEvidence."));

        return schema;
    }

    private ObjectNode evidenceSchema() {
        ObjectNode schema = arraySchema("Evidence selected from inputEvidence.");
        ObjectNode item = objectItem(schema);
        required(item, "id", "type", "title", "message", "status", "timestamp");

        ObjectNode properties = item.putObject("properties");
        properties.set("id", stringSchema("Original input evidence id."));
        properties.set("type", stringSchema("Evidence type, such as target, event, metric, or topology."));
        properties.set("title", stringSchema("Korean evidence title."));
        properties.set("message", stringSchema("Korean evidence detail. Do not invent new facts."));
        properties.set("status", severitySchema());
        properties.set("timestamp", stringSchema("ISO-8601 timestamp or empty string when not available."));

        return schema;
    }

    private ObjectNode recommendationsSchema() {
        ObjectNode schema = arraySchema("Safe action templates. AI must not execute operations.");
        ObjectNode item = objectItem(schema);
        required(item, "action", "risk", "reason");

        ObjectNode properties = item.putObject("properties");
        properties.set("action", stringSchema("UPPER_SNAKE_CASE action template."));
        properties.set("risk", riskSchema());
        properties.set("reason", stringSchema("Korean explanation for why this action is recommended."));

        return schema;
    }

    private ObjectNode arraySchema(String description) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "array");
        schema.put("description", description);
        return schema;
    }

    private ObjectNode objectItem(ObjectNode arraySchema) {
        ObjectNode item = arraySchema.putObject("items");
        item.put("type", "object");
        item.put("additionalProperties", false);
        return item;
    }

    private ObjectNode stringArraySchema(String description) {
        ObjectNode schema = arraySchema(description);
        schema.putObject("items").put("type", "string");
        return schema;
    }

    private ObjectNode stringSchema(String description) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "string");
        schema.put("description", description);
        return schema;
    }

    private ObjectNode numberSchema(String description) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "number");
        schema.put("description", description);
        return schema;
    }

    private ObjectNode severitySchema() {
        ObjectNode schema = stringSchema("One of the normalized OpsPilot resource statuses.");
        ArrayNode enumValues = schema.putArray("enum");
        enumValues.add("healthy");
        enumValues.add("warning");
        enumValues.add("critical");
        enumValues.add("unknown");
        return schema;
    }

    private ObjectNode riskSchema() {
        ObjectNode schema = stringSchema("Operational risk of the recommended action.");
        ArrayNode enumValues = schema.putArray("enum");
        enumValues.add("low");
        enumValues.add("medium");
        enumValues.add("high");
        return schema;
    }

    private void required(ObjectNode schema, String... fields) {
        ArrayNode required = schema.putArray("required");
        for (String field : fields) {
            required.add(field);
        }
    }

    private int maxOutputTokens() {
        return Math.max(256, aiProperties.getOpenai().getMaxOutputTokens() <= 0
                ? DEFAULT_MAX_OUTPUT_TOKENS
                : aiProperties.getOpenai().getMaxOutputTokens());
    }

    private ObjectNode inputMessage(String role, String text) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", "input_text");
        content.put("text", text);
        message.putArray("content").add(content);

        return message;
    }

    private String developerPrompt() {
        return """
                You are OpsPilot's Kubernetes incident analysis assistant.
                Return exactly one valid JSON object. Do not use markdown.

                Output JSON shape:
                {
                  "summary": "string",
                  "severity": "healthy | warning | critical | unknown",
                  "impactScope": ["string"],
                  "rootCauseCandidates": [
                    {"title": "string", "confidence": 0.0, "evidenceIds": ["input evidence id"]}
                  ],
                  "evidence": [
                    {"id": "input evidence id", "type": "string", "title": "string", "message": "string", "status": "healthy | warning | critical | unknown", "timestamp": "ISO-8601 or null"}
                  ],
                  "recommendations": [
                    {"action": "UPPER_SNAKE_CASE", "risk": "low | medium | high", "reason": "string"}
                  ],
                  "nextChecks": ["string"]
                }

                Rules:
                - Treat root causes as candidates, not confirmed facts.
                - Use only ids from inputEvidence in rootCauseCandidates.evidenceIds.
                - Evidence must be grounded in inputEvidence. Do not invent events, logs, metrics, or Kubernetes objects.
                - Do not expose or infer secret values, tokens, passwords, or credentials.
                - AI must not execute operations. Recommend action templates only.
                - Write every user-facing text field in Korean: summary, impactScope descriptions when present,
                  rootCauseCandidates.title, evidence.title, evidence.message, recommendations.reason, nextChecks.
                - Keep Kubernetes identifiers, metric names, and action codes in English.
                """;
    }

    private String analysisContextJson(IncidentAnalysisContext context, List<IncidentEvidence> inputEvidence) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clusterId", context.clusterId());
        payload.put("timeRangeMinutes", context.timeRangeMinutes());
        payload.put("collectedAt", context.collectedAt());
        payload.put("target", context.target());
        payload.put("inputEvidence", inputEvidence);
        payload.put("events", context.events());
        payload.put("metrics", metricsContext(context.metrics()));
        payload.put("topology", topologyContext(context.topology()));

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new AiProviderException("Failed to serialize AI analysis context.", exception);
        }
    }

    private Map<String, Object> metricsContext(ResourceMetrics metrics) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (metrics == null) {
            payload.put("status", "unavailable");
            payload.put("reason", "METRICS_NOT_APPLICABLE");
            return payload;
        }

        payload.put("status", metrics.status().name().toLowerCase());
        payload.put("reason", metrics.reason());
        payload.put("collectedAt", metrics.collectedAt());
        payload.put("rangeMinutes", metrics.rangeMinutes());
        payload.put("summary", metrics.summary());

        return payload;
    }

    private Map<String, Object> topologyContext(TopologyGraph topology) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (topology == null) {
            payload.put("available", false);
            return payload;
        }

        payload.put("available", true);
        payload.put("rootNodeId", topology.rootNodeId());
        payload.put("collectedAt", topology.collectedAt());
        payload.put("nodeCount", topology.nodes().size());
        payload.put("edgeCount", topology.edges().size());
        payload.put("unhealthyNodes", topology.nodes().stream()
                .filter(node -> node.status() != ResourceStatus.HEALTHY)
                .limit(TOPOLOGY_NODE_LIMIT)
                .toList());
        payload.put("sampleEdges", topology.edges().stream()
                .limit(TOPOLOGY_EDGE_LIMIT)
                .toList());

        return payload;
    }

    private List<IncidentEvidence> inputEvidence(IncidentAnalysisContext context) {
        List<IncidentEvidence> evidence = new ArrayList<>();
        evidence.add(new IncidentEvidence(
                "target-1",
                "target",
                context.target().kind() + " 상태",
                "대상 상태 메시지: " + context.target().message(),
                context.target().status(),
                context.target().lastUpdatedAt()
        ));

        int eventIndex = 1;
        for (EventSummary event : context.events()) {
            evidence.add(new IncidentEvidence(
                    "event-" + eventIndex,
                    "event",
                    "Kubernetes Event: " + event.reason(),
                    "Kubernetes Event 원문: " + event.message(),
                    event.status(),
                    event.lastUpdatedAt()
            ));
            eventIndex += 1;
        }

        addMetricEvidence(context.metrics(), evidence);
        addTopologyEvidence(context.topology(), evidence);

        return evidence;
    }

    private void addMetricEvidence(ResourceMetrics metrics, List<IncidentEvidence> evidence) {
        if (metrics == null) {
            return;
        }

        if (metrics.status() == MetricsAvailabilityStatus.UNAVAILABLE) {
            evidence.add(new IncidentEvidence(
                    "metric-1",
                    "metric",
                    "Prometheus metric 사용 불가",
                    metrics.reason(),
                    ResourceStatus.UNKNOWN,
                    metrics.collectedAt()
            ));
            return;
        }

        evidence.add(new IncidentEvidence(
                "metric-1",
                "metric",
                "CPU 사용률",
                "request 대비 " + formatPercent(metrics.summary().cpuUsagePercent()),
                metricStatus(metrics.summary().cpuUsagePercent()),
                metrics.collectedAt()
        ));
        evidence.add(new IncidentEvidence(
                "metric-2",
                "metric",
                "Memory 사용률",
                "request 대비 " + formatPercent(metrics.summary().memoryUsagePercent()),
                metricStatus(metrics.summary().memoryUsagePercent()),
                metrics.collectedAt()
        ));
    }

    private void addTopologyEvidence(TopologyGraph topology, List<IncidentEvidence> evidence) {
        if (topology == null) {
            return;
        }

        evidence.add(new IncidentEvidence(
                "topology-1",
                "topology",
                "관련 객체 그래프",
                "%d개 관련 노드와 %d개 관계를 수집했습니다.".formatted(topology.nodes().size(), topology.edges().size()),
                ResourceStatus.HEALTHY,
                topology.collectedAt()
        ));
    }

    private ResourceStatus metricStatus(Double usagePercent) {
        if (usagePercent == null) {
            return ResourceStatus.UNKNOWN;
        }

        return usagePercent >= 85.0 ? ResourceStatus.WARNING : ResourceStatus.HEALTHY;
    }

    private String formatPercent(Double usagePercent) {
        return usagePercent == null ? "알 수 없음" : "%.1f%%".formatted(usagePercent);
    }

    private IncidentAnalysisReport toReport(
            IncidentAnalysisContext context,
            List<IncidentEvidence> inputEvidence,
            JsonNode response,
            long latencyMs
    ) {
        if (response == null) {
            throw new AiProviderException("OpenAI Responses API returned an empty response.");
        }

        String status = response.path("status").asText();
        if (!status.isBlank() && !"completed".equals(status)) {
            throw new AiProviderException("OpenAI Responses API returned non-completed status: " + status);
        }

        String outputText = extractOutputText(response);
        JsonNode reportNode = parseOutputJson(outputText);
        AiTokenUsage tokenUsage = tokenUsage(response.path("usage"));

        return new IncidentAnalysisReport(
                UUID.randomUUID(),
                context.clusterId(),
                context.target().namespace(),
                context.target().kind(),
                context.target().name(),
                status(reportNode.path("severity").asText(context.target().status().value())),
                text(reportNode, "summary", fallbackSummary(context)),
                stringList(reportNode.path("impactScope"), fallbackImpactScope(context)),
                rootCauseCandidates(reportNode.path("rootCauseCandidates"), inputEvidence),
                evidence(reportNode.path("evidence"), inputEvidence),
                recommendations(reportNode.path("recommendations")),
                stringList(reportNode.path("nextChecks"), List.of("관련 Event, metric, rollout history를 추가 확인하세요.")),
                "openai",
                response.path("model").asText(aiProperties.getOpenai().getModel()),
                response.path("id").asText(null),
                tokenUsage,
                latencyMs,
                Instant.now()
        );
    }

    private String extractOutputText(JsonNode response) {
        StringBuilder builder = new StringBuilder();
        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode content : outputItem.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    builder.append(content.path("text").asText());
                }
            }
        }

        if (builder.isEmpty()) {
            throw new AiProviderException("OpenAI response did not contain output_text.");
        }

        return builder.toString().trim();
    }

    private JsonNode parseOutputJson(String outputText) {
        String normalized = stripCodeFence(outputText);
        try {
            return objectMapper.readTree(normalized);
        } catch (JsonProcessingException exception) {
            throw new AiProviderException("OpenAI response was not valid JSON.", exception);
        }
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstNewline = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewline < 0 || lastFence <= firstNewline) {
            return trimmed;
        }

        return trimmed.substring(firstNewline + 1, lastFence).trim();
    }

    private AiTokenUsage tokenUsage(JsonNode usageNode) {
        if (usageNode == null || usageNode.isMissingNode() || usageNode.isNull()) {
            return AiTokenUsage.zero();
        }

        return new AiTokenUsage(
                usageNode.path("input_tokens").asInt(0),
                usageNode.path("input_tokens_details").path("cached_tokens").asInt(0),
                usageNode.path("output_tokens").asInt(0),
                usageNode.path("output_tokens_details").path("reasoning_tokens").asInt(0),
                usageNode.path("total_tokens").asInt(0)
        );
    }

    private List<RootCauseCandidate> rootCauseCandidates(JsonNode node, List<IncidentEvidence> inputEvidence) {
        if (!node.isArray()) {
            return fallbackRootCauseCandidates(inputEvidence);
        }

        List<String> allowedEvidenceIds = inputEvidence.stream().map(IncidentEvidence::id).toList();
        List<RootCauseCandidate> candidates = new ArrayList<>();
        for (JsonNode item : node) {
            List<String> evidenceIds = stringList(item.path("evidenceIds"), List.of()).stream()
                    .filter(allowedEvidenceIds::contains)
                    .toList();
            candidates.add(new RootCauseCandidate(
                    text(item, "title", "추가 확인이 필요한 원인 후보"),
                    clamp(item.path("confidence").asDouble(0.5)),
                    evidenceIds.isEmpty() ? allowedEvidenceIds : evidenceIds
            ));
        }

        return candidates.isEmpty()
                ? fallbackRootCauseCandidates(inputEvidence)
                : candidates;
    }

    private List<RootCauseCandidate> fallbackRootCauseCandidates(List<IncidentEvidence> inputEvidence) {
        return List.of(new RootCauseCandidate(
                "수집된 근거 안에서 명확한 원인 후보를 추가 확인해야 합니다.",
                0.5,
                inputEvidence.stream().map(IncidentEvidence::id).toList()
        ));
    }

    private List<IncidentEvidence> evidence(JsonNode node, List<IncidentEvidence> fallbackEvidence) {
        if (!node.isArray()) {
            return fallbackEvidence;
        }

        List<IncidentEvidence> evidence = new ArrayList<>();
        for (JsonNode item : node) {
            evidence.add(new IncidentEvidence(
                    text(item, "id", "ai-evidence-" + (evidence.size() + 1)),
                    text(item, "type", "ai"),
                    text(item, "title", "AI 근거"),
                    text(item, "message", ""),
                    status(item.path("status").asText("unknown")),
                    instantOrNull(item.path("timestamp").asText(null))
            ));
        }

        return evidence.isEmpty() ? fallbackEvidence : evidence;
    }

    private List<RecommendedAction> recommendations(JsonNode node) {
        if (!node.isArray()) {
            return List.of(new RecommendedAction(
                    "CHECK_EVENTS_AND_METRICS",
                    "low",
                    "수집된 Event와 metric을 기준으로 증상 발생 시점을 확인하세요."
            ));
        }

        List<RecommendedAction> recommendations = new ArrayList<>();
        for (JsonNode item : node) {
            recommendations.add(new RecommendedAction(
                    text(item, "action", "CHECK_EVENTS_AND_METRICS"),
                    text(item, "risk", "low"),
                    text(item, "reason", "수집된 근거를 기준으로 추가 확인하세요.")
            ));
        }

        return recommendations;
    }

    private List<String> stringList(JsonNode node, List<String> fallback) {
        if (!node.isArray()) {
            return fallback;
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.asText("").isBlank()) {
                values.add(item.asText());
            }
        }

        return values.isEmpty() ? fallback : values;
    }

    private ResourceStatus status(String value) {
        if (value == null) {
            return ResourceStatus.UNKNOWN;
        }

        return switch (value.trim().toLowerCase()) {
            case "healthy" -> ResourceStatus.HEALTHY;
            case "warning" -> ResourceStatus.WARNING;
            case "critical" -> ResourceStatus.CRITICAL;
            default -> ResourceStatus.UNKNOWN;
        };
    }

    private String text(JsonNode node, String fieldName, String fallback) {
        String value = node.path(fieldName).asText(null);
        return value == null || value.isBlank() ? fallback : value;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private Instant instantOrNull(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private List<String> fallbackImpactScope(IncidentAnalysisContext context) {
        if (context.target().namespace() == null) {
            return List.of(context.target().kind() + "/" + context.target().name());
        }

        return List.of(context.target().namespace() + "/" + context.target().kind() + "/" + context.target().name());
    }

    private String fallbackSummary(IncidentAnalysisContext context) {
        return "%s %s/%s 분석 결과를 생성했습니다."
                .formatted(context.target().kind(), context.target().namespace(), context.target().name());
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
