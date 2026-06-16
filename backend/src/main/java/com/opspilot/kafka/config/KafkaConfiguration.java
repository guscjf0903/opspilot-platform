package com.opspilot.kafka.config;

import java.time.Clock;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpspilotKafkaProperties.class)
public class KafkaConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "opspilot.kafka", name = "mode", havingValue = "admin", matchIfMissing = true)
    public AdminClient kafkaAdminClient(OpspilotKafkaProperties kafkaProperties) {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers(),
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                String.valueOf(kafkaProperties.getRequestTimeout().toMillis()),
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,
                String.valueOf(kafkaProperties.getRequestTimeout().toMillis()),
                CommonClientConfigs.CLIENT_ID_CONFIG,
                "opspilot-kafka-admin"
        ));
    }

    @Bean
    public Clock kafkaClock() {
        return Clock.systemUTC();
    }
}
