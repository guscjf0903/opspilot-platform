package com.opspilot.cost.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspilot.opencost")
public class OpenCostProperties {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:9003";
    private String allocationWindow = "1d";
    private String allocationResolution = "1m";
    private boolean shareIdle = true;
    private double allocationWindowHours = 24.0;
    private double monthlyHours = 730.0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAllocationWindow() {
        return allocationWindow;
    }

    public void setAllocationWindow(String allocationWindow) {
        this.allocationWindow = allocationWindow;
    }

    public String getAllocationResolution() {
        return allocationResolution;
    }

    public void setAllocationResolution(String allocationResolution) {
        this.allocationResolution = allocationResolution;
    }

    public boolean isShareIdle() {
        return shareIdle;
    }

    public void setShareIdle(boolean shareIdle) {
        this.shareIdle = shareIdle;
    }

    public double getAllocationWindowHours() {
        return allocationWindowHours;
    }

    public void setAllocationWindowHours(double allocationWindowHours) {
        this.allocationWindowHours = allocationWindowHours;
    }

    public double getMonthlyHours() {
        return monthlyHours;
    }

    public void setMonthlyHours(double monthlyHours) {
        this.monthlyHours = monthlyHours;
    }
}
