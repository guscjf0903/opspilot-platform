package com.opspilot.kubernetes.application;

import com.opspilot.kubernetes.application.port.out.KubernetesInventoryPort;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KubernetesInventoryService {

    private final KubernetesInventoryPort kubernetesInventoryPort;
    private final OpspilotKubernetesProperties kubernetesProperties;

    public List<NamespaceSummary> getNamespaces(String clusterId) {
        validateCluster(clusterId);
        return kubernetesInventoryPort.getNamespaces();
    }

    public List<DeploymentSummary> getDeployments(String clusterId, String namespace) {
        validateCluster(clusterId);
        return kubernetesInventoryPort.getDeployments(namespace);
    }

    public List<PodSummary> getPods(String clusterId, String namespace) {
        validateCluster(clusterId);
        return kubernetesInventoryPort.getPods(namespace);
    }

    public List<EventSummary> getEvents(String clusterId, String namespace) {
        validateCluster(clusterId);
        return kubernetesInventoryPort.getEvents(namespace);
    }

    public List<NodeSummary> getNodes(String clusterId) {
        validateCluster(clusterId);
        return kubernetesInventoryPort.getNodes();
    }

    private void validateCluster(String clusterId) {
        if (!kubernetesProperties.getClusterId().equals(clusterId)) {
            throw new UnknownClusterException(clusterId);
        }
    }
}
