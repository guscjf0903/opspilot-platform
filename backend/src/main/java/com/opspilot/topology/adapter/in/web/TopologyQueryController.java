package com.opspilot.topology.adapter.in.web;

import com.opspilot.topology.application.TopologyQueryService;
import com.opspilot.topology.domain.TopologyGraph;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}/namespaces/{namespace}/topology")
@RequiredArgsConstructor
public class TopologyQueryController {

    private final TopologyQueryService topologyQueryService;

    @GetMapping
    public TopologyGraph getTopology(
            @PathVariable String clusterId,
            @PathVariable String namespace
    ) {
        return topologyQueryService.getTopology(clusterId, namespace);
    }

    @GetMapping("/{kind}/{name}")
    public TopologyGraph getTopology(
            @PathVariable String clusterId,
            @PathVariable String namespace,
            @PathVariable String kind,
            @PathVariable String name
    ) {
        return topologyQueryService.getTopology(clusterId, namespace, kind, name);
    }
}
