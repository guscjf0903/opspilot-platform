package com.opspilot.kubernetes.application.port.out;

import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import java.util.List;

public interface KubernetesInventoryPort {

    boolean isReachable();

    String getApiServerUrl();

    List<NamespaceSummary> getNamespaces();

    List<DeploymentSummary> getDeployments(String namespace);

    List<PodSummary> getPods(String namespace);

    List<EventSummary> getEvents(String namespace);

    List<NodeSummary> getNodes();
}
