package com.opspilot.metrics.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpspilotPrometheusProperties.class)
public class MetricsConfiguration {

    @Bean
    public RestClient prometheusRestClient(
            RestClient.Builder restClientBuilder,
            OpspilotPrometheusProperties prometheusProperties
    ) {
        return restClientBuilder
                .clone()
                .baseUrl(prometheusProperties.getBaseUrl())
                .build();
    }
}
