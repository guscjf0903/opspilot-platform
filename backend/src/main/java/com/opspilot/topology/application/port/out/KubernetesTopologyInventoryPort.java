package com.opspilot.topology.application.port.out;

import com.opspilot.topology.domain.KubernetesTopologySnapshot;

public interface KubernetesTopologyInventoryPort {

    KubernetesTopologySnapshot getTopologySnapshot(String namespace);
}
