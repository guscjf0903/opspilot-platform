package com.opspilot.cost.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({CostEstimationProperties.class, OpenCostProperties.class})
public class CostConfiguration {

    @Bean
    public RestClient openCostRestClient(
            RestClient.Builder restClientBuilder,
            OpenCostProperties openCostProperties
    ) {
        return restClientBuilder
                .clone()
                .baseUrl(openCostProperties.getBaseUrl())
                .build();
    }
}
