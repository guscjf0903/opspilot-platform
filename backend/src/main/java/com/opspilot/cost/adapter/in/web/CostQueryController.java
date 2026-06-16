package com.opspilot.cost.adapter.in.web;

import com.opspilot.cost.application.CostQueryService;
import com.opspilot.cost.domain.CostRecommendation;
import com.opspilot.cost.domain.CostSummary;
import com.opspilot.cost.domain.NamespaceCost;
import com.opspilot.cost.domain.WorkloadCost;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}/cost")
@RequiredArgsConstructor
public class CostQueryController {

    private final CostQueryService costQueryService;

    @GetMapping("/summary")
    public CostSummary getSummary(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace
    ) {
        return costQueryService.getSummary(clusterId, namespace);
    }

    @GetMapping("/namespaces")
    public List<NamespaceCost> getNamespaceCosts(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace
    ) {
        return costQueryService.getNamespaceCosts(clusterId, namespace);
    }

    @GetMapping("/workloads")
    public List<WorkloadCost> getWorkloadCosts(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace
    ) {
        return costQueryService.getWorkloadCosts(clusterId, namespace);
    }

    @GetMapping("/recommendations")
    public List<CostRecommendation> getRecommendations(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace
    ) {
        return costQueryService.getRecommendations(clusterId, namespace);
    }
}
