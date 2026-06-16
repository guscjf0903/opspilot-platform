package com.opspilot.cost.adapter.out.opencost;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.cost.application.port.out.OpenCostAllocationPort;
import com.opspilot.cost.config.OpenCostProperties;
import com.opspilot.cost.domain.OpenCostAllocationSnapshot;
import com.opspilot.cost.domain.OpenCostWorkloadAllocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class OpenCostHttpAllocationAdapter implements OpenCostAllocationPort {

    private static final String OPENCOST_DISABLED = "OPENCOST_DISABLED";
    private static final String OPENCOST_QUERY_FAILED = "OPENCOST_QUERY_FAILED";
    private static final String OPENCOST_ALLOCATIONS_EMPTY = "OPENCOST_ALLOCATIONS_EMPTY";

    private final RestClient openCostRestClient;
    private final OpenCostProperties openCostProperties;

    @Override
    public OpenCostAllocationSnapshot getWorkloadAllocations(String clusterId) {
        if (!openCostProperties.isEnabled()) {
            return OpenCostAllocationSnapshot.unavailable(OPENCOST_DISABLED);
        }

        try {
            JsonNode body = openCostRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/allocation")
                            .queryParam("window", openCostProperties.getAllocationWindow())
                            .queryParam("aggregate", "namespace,pod")
                            .queryParam("resolution", openCostProperties.getAllocationResolution())
                            .queryParam("shareIdle", openCostProperties.isShareIdle())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            List<OpenCostWorkloadAllocation> allocations = parseAllocations(body);
            if (allocations.isEmpty()) {
                return OpenCostAllocationSnapshot.unavailable(OPENCOST_ALLOCATIONS_EMPTY);
            }

            return OpenCostAllocationSnapshot.available(allocations);
        } catch (RestClientException | IllegalArgumentException exception) {
            return OpenCostAllocationSnapshot.unavailable(OPENCOST_QUERY_FAILED);
        }
    }

    private List<OpenCostWorkloadAllocation> parseAllocations(JsonNode body) {
        if (body == null || body.path("code").asInt(200) >= 400) {
            return List.of();
        }

        JsonNode data = body.path("data");
        Map<String, OpenCostWorkloadAllocation> allocations = new LinkedHashMap<>();

        if (data.isArray()) {
            for (JsonNode allocationSet : data) {
                parseAllocationSet(allocationSet).forEach(allocation -> mergeAllocation(allocations, allocation));
            }
        } else if (data.isObject()) {
            parseAllocationSet(data).forEach(allocation -> mergeAllocation(allocations, allocation));
        }

        return new ArrayList<>(allocations.values());
    }

    private List<OpenCostWorkloadAllocation> parseAllocationSet(JsonNode allocationSet) {
        JsonNode allocationsNode = allocationSet.path("allocations");
        JsonNode source = allocationsNode.isObject() ? allocationsNode : allocationSet;
        List<OpenCostWorkloadAllocation> allocations = new ArrayList<>();

        source.properties().forEach(entry ->
                parseAllocation(entry.getKey(), entry.getValue()).ifPresent(allocations::add));

        return allocations;
    }

    private Optional<OpenCostWorkloadAllocation> parseAllocation(String key, JsonNode allocationNode) {
        if (!allocationNode.isObject() || "__idle__".equals(key)) {
            return Optional.empty();
        }

        JsonNode properties = allocationNode.path("properties");
        String namespace = firstText(properties, "namespace").orElseGet(() -> namespaceFromKey(key));
        String pod = firstText(properties, "pod").orElseGet(() -> podFromKey(key));
        String controller = firstText(properties, "controller").orElseGet(() -> controllerFromPod(pod));

        if (isBlank(namespace) || isBlank(controller) || "__unallocated__".equals(controller)) {
            return Optional.empty();
        }

        double multiplier = monthlyMultiplier();
        double cpuCost = number(allocationNode, "cpuCost") * multiplier;
        double memoryCost = number(allocationNode, "ramCost", "memoryCost") * multiplier;
        double totalCost = number(allocationNode, "totalCost", "total") * multiplier;

        if (totalCost <= 0.0) {
            totalCost = cpuCost + memoryCost;
        }

        if (totalCost <= 0.0) {
            return Optional.empty();
        }

        return Optional.of(new OpenCostWorkloadAllocation(
                namespace,
                controller,
                roundMoney(totalCost),
                roundMoney(cpuCost),
                roundMoney(memoryCost)
        ));
    }

    private void mergeAllocation(
            Map<String, OpenCostWorkloadAllocation> allocations,
            OpenCostWorkloadAllocation allocation
    ) {
        String key = allocation.namespace() + "/" + allocation.controller();
        allocations.merge(key, allocation, (left, right) -> new OpenCostWorkloadAllocation(
                left.namespace(),
                left.controller(),
                roundMoney(left.monthlyCost() + right.monthlyCost()),
                roundMoney(left.cpuMonthlyCost() + right.cpuMonthlyCost()),
                roundMoney(left.memoryMonthlyCost() + right.memoryMonthlyCost())
        ));
    }

    private Optional<String> firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText(null);
            if (!isBlank(value)) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    private double number(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isNumber()) {
                return value.asDouble();
            }
        }

        return 0.0;
    }

    private String namespaceFromKey(String key) {
        String[] slashParts = key.split("/");
        if (slashParts.length >= 2) {
            return slashParts[0];
        }

        String[] commaParts = key.split(",");
        for (String part : commaParts) {
            String[] labelParts = part.split("=", 2);
            if (labelParts.length == 2 && "namespace".equals(labelParts[0].trim())) {
                return labelParts[1].trim();
            }
        }

        return null;
    }

    private String podFromKey(String key) {
        String[] slashParts = key.split("/");
        if (slashParts.length >= 2) {
            return slashParts[slashParts.length - 1];
        }

        String[] commaParts = key.split(",");
        for (String part : commaParts) {
            String[] labelParts = part.split("=", 2);
            if (labelParts.length == 2 && "pod".equals(labelParts[0].trim())) {
                return labelParts[1].trim();
            }
        }

        return null;
    }

    private String controllerFromPod(String pod) {
        if (isBlank(pod)) {
            return null;
        }

        int lastDash = pod.lastIndexOf('-');
        if (lastDash <= 0) {
            return pod;
        }

        int previousDash = pod.lastIndexOf('-', lastDash - 1);
        if (previousDash <= 0) {
            return pod;
        }

        return pod.substring(0, previousDash);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private double monthlyMultiplier() {
        if (openCostProperties.getAllocationWindowHours() <= 0.0) {
            return 1.0;
        }

        return openCostProperties.getMonthlyHours() / openCostProperties.getAllocationWindowHours();
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
