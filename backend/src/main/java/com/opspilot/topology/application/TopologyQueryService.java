package com.opspilot.topology.application;

import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.topology.application.port.out.KubernetesTopologyInventoryPort;
import com.opspilot.topology.domain.KubernetesTopologySnapshot;
import com.opspilot.topology.domain.TopologyGraph;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopologyQueryService {

    private final KubernetesTopologyInventoryPort topologyInventoryPort;
    private final KubernetesInventoryService kubernetesInventoryService;
    private final TopologyGraphBuilder topologyGraphBuilder;

    public TopologyGraph getTopology(String clusterId, String namespace) {
        return getTopology(clusterId, namespace, null, null);
    }

    public TopologyGraph getTopology(String clusterId, String namespace, String rootKind, String rootName) {
        List<DeploymentSummary> deployments = kubernetesInventoryService.getDeployments(clusterId, namespace);
        List<PodSummary> pods = kubernetesInventoryService.getPods(clusterId, namespace);
        List<NodeSummary> nodes = kubernetesInventoryService.getNodes(clusterId);
        KubernetesTopologySnapshot snapshot = topologyInventoryPort.getTopologySnapshot(namespace);

        return topologyGraphBuilder.build(
                clusterId,
                snapshot,
                deployments,
                pods,
                nodes,
                rootKind,
                rootName
        );
    }
}
