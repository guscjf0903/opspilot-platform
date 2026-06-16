package com.opspilot.cost.application;

import com.opspilot.cost.domain.CostRecommendation;
import com.opspilot.cost.domain.NamespaceCost;
import com.opspilot.cost.domain.WorkloadCost;
import java.util.List;

public record CostAnalysisSnapshot(
        List<NamespaceCost> namespaces,
        List<WorkloadCost> workloads,
        List<CostRecommendation> recommendations,
        String estimationMode
) {
}
