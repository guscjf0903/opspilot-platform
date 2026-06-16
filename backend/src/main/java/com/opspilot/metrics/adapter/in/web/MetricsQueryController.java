package com.opspilot.metrics.adapter.in.web;

import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.ResourceMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}")
@RequiredArgsConstructor
public class MetricsQueryController {

    private final MetricsQueryService metricsQueryService;

    @GetMapping("/namespaces/{namespace}/workloads/{kind}/{name}/metrics")
    public ResourceMetrics getWorkloadMetrics(
            @PathVariable String clusterId,
            @PathVariable String namespace,
            @PathVariable String kind,
            @PathVariable String name,
            @RequestParam(required = false) Integer rangeMinutes
    ) {
        return metricsQueryService.getWorkloadMetrics(clusterId, namespace, kind, name, rangeMinutes);
    }

    @GetMapping("/nodes/{nodeName}/metrics")
    public ResourceMetrics getNodeMetrics(
            @PathVariable String clusterId,
            @PathVariable String nodeName,
            @RequestParam(required = false) Integer rangeMinutes
    ) {
        return metricsQueryService.getNodeMetrics(clusterId, nodeName, rangeMinutes);
    }
}
