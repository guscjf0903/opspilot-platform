package com.opspilot.kafka.application.port.out;

import com.opspilot.kafka.domain.KafkaMonitoringSnapshot;

public interface KafkaMonitoringPort {

    KafkaMonitoringSnapshot fetchSnapshot(String clusterId);
}
