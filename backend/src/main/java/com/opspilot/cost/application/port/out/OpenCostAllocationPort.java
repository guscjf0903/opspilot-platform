package com.opspilot.cost.application.port.out;

import com.opspilot.cost.domain.OpenCostAllocationSnapshot;

public interface OpenCostAllocationPort {

    OpenCostAllocationSnapshot getWorkloadAllocations(String clusterId);
}
