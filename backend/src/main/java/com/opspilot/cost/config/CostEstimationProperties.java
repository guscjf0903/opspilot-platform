package com.opspilot.cost.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspilot.cost.estimation")
public class CostEstimationProperties {

    private String currency = "USD";
    private double cpuCoreMonthlyPrice = 24.0;
    private double memoryGibMonthlyPrice = 3.0;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getCpuCoreMonthlyPrice() {
        return cpuCoreMonthlyPrice;
    }

    public void setCpuCoreMonthlyPrice(double cpuCoreMonthlyPrice) {
        this.cpuCoreMonthlyPrice = cpuCoreMonthlyPrice;
    }

    public double getMemoryGibMonthlyPrice() {
        return memoryGibMonthlyPrice;
    }

    public void setMemoryGibMonthlyPrice(double memoryGibMonthlyPrice) {
        this.memoryGibMonthlyPrice = memoryGibMonthlyPrice;
    }
}
