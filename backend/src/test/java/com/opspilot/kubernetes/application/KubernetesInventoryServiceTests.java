package com.opspilot.kubernetes.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.opspilot.kubernetes.application.port.out.KubernetesInventoryPort;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import org.junit.jupiter.api.Test;

class KubernetesInventoryServiceTests {

    @Test
    void rejectsUnknownClusterIdBeforeCallingAdapter() {
        OpspilotKubernetesProperties properties = new OpspilotKubernetesProperties();
        properties.setClusterId("local");
        KubernetesInventoryService service = new KubernetesInventoryService(
                mock(KubernetesInventoryPort.class),
                properties
        );

        assertThatThrownBy(() -> service.getNamespaces("missing"))
                .isInstanceOf(UnknownClusterException.class)
                .hasMessage("Cluster not found: missing");
    }
}
