package com.opspilot.kubernetes.config;

import com.opspilot.kubernetes.domain.KubernetesResourceStatusEvaluator;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpspilotKubernetesProperties.class)
public class KubernetesClientConfiguration {

    @Bean(destroyMethod = "close")
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    public KubernetesResourceStatusEvaluator kubernetesResourceStatusEvaluator() {
        return new KubernetesResourceStatusEvaluator();
    }
}
