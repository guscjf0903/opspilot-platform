package com.opspilot.cost.domain;

import java.util.List;
import java.util.Optional;

public record OpenCostAllocationSnapshot(
        boolean available,
        String reason,
        List<OpenCostWorkloadAllocation> workloads
) {

    public static OpenCostAllocationSnapshot available(List<OpenCostWorkloadAllocation> workloads) {
        return new OpenCostAllocationSnapshot(true, "OPENCOST_CONNECTED", workloads);
    }

    public static OpenCostAllocationSnapshot unavailable(String reason) {
        return new OpenCostAllocationSnapshot(false, reason, List.of());
    }

    public Optional<OpenCostWorkloadAllocation> findWorkload(String namespace, String controller) {
        return workloads.stream()
                .filter(allocation -> allocation.namespace().equals(namespace))
                .filter(allocation -> allocation.controller().equals(controller))
                .findFirst();
    }
}
