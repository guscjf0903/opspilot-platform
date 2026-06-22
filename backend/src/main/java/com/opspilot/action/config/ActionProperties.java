package com.opspilot.action.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspilot.actions")
public class ActionProperties {

    private boolean executionEnabled = true;

    public boolean isExecutionEnabled() {
        return executionEnabled;
    }

    public void setExecutionEnabled(boolean executionEnabled) {
        this.executionEnabled = executionEnabled;
    }
}
