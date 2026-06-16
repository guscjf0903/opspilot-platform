package com.opspilot.cost.domain;

public record OpenCostWorkloadAllocation(
        String namespace,
        String controller,
        double monthlyCost,
        double cpuMonthlyCost,
        double memoryMonthlyCost
) {
}
