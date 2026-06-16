package com.opspilot.kafka.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspilot.kafka")
public class OpspilotKafkaProperties {

    private boolean enabled = true;
    private String mode = "admin";
    private String bootstrapServers = "localhost:9092";
    private Duration requestTimeout = Duration.ofSeconds(3);
    private long warningLagThreshold = 100;
    private long criticalLagThreshold = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public long getWarningLagThreshold() {
        return warningLagThreshold;
    }

    public void setWarningLagThreshold(long warningLagThreshold) {
        this.warningLagThreshold = warningLagThreshold;
    }

    public long getCriticalLagThreshold() {
        return criticalLagThreshold;
    }

    public void setCriticalLagThreshold(long criticalLagThreshold) {
        this.criticalLagThreshold = criticalLagThreshold;
    }
}
