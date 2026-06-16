package com.opspilot.kubernetes.adapter.in.web;

import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}")
@RequiredArgsConstructor
public class KubernetesInventoryController {

    private final KubernetesInventoryService kubernetesInventoryService;

    @GetMapping("/namespaces")
    public List<NamespaceSummary> getNamespaces(@PathVariable String clusterId) {
        return kubernetesInventoryService.getNamespaces(clusterId);
    }

    @GetMapping("/namespaces/{namespace}/deployments")
    public List<DeploymentSummary> getDeployments(
            @PathVariable String clusterId,
            @PathVariable String namespace
    ) {
        return kubernetesInventoryService.getDeployments(clusterId, namespace);
    }

    @GetMapping("/namespaces/{namespace}/pods")
    public List<PodSummary> getPods(
            @PathVariable String clusterId,
            @PathVariable String namespace
    ) {
        return kubernetesInventoryService.getPods(clusterId, namespace);
    }

    @GetMapping("/namespaces/{namespace}/events")
    public List<EventSummary> getEvents(
            @PathVariable String clusterId,
            @PathVariable String namespace
    ) {
        return kubernetesInventoryService.getEvents(clusterId, namespace);
    }

    @GetMapping("/nodes")
    public List<NodeSummary> getNodes(@PathVariable String clusterId) {
        return kubernetesInventoryService.getNodes(clusterId);
    }
}
