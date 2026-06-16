package com.opspilot.cluster.application;

import com.opspilot.cluster.domain.ClusterConnectionStatus;
import com.opspilot.cluster.domain.ClusterSummary;
import com.opspilot.kubernetes.application.port.out.KubernetesInventoryPort;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterQueryService {

    private final KubernetesInventoryPort kubernetesInventoryPort;
    private final OpspilotKubernetesProperties kubernetesProperties;

    public List<ClusterSummary> getClusters() {
        ClusterConnectionStatus status = kubernetesInventoryPort.isReachable()
                ? ClusterConnectionStatus.CONNECTED
                : ClusterConnectionStatus.DISCONNECTED;

        return List.of(new ClusterSummary(
                kubernetesProperties.getClusterId(),
                kubernetesProperties.getDisplayName(),
                kubernetesProperties.getProvider(),
                status,
                kubernetesInventoryPort.getApiServerUrl()
        ));
    }
}
