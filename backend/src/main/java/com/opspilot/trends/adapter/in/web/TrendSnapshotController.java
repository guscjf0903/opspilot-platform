package com.opspilot.trends.adapter.in.web;

import com.opspilot.trends.application.TrendSnapshotService;
import com.opspilot.trends.domain.CostDailySnapshot;
import com.opspilot.trends.domain.KubernetesWorkloadUsageSnapshot;
import com.opspilot.trends.domain.TrendSnapshotCollectionResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}/trends")
@RequiredArgsConstructor
public class TrendSnapshotController {

    private final TrendSnapshotService trendSnapshotService;

    @PostMapping("/snapshots")
    public TrendSnapshotCollectionResult collectSnapshots(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace
    ) {
        return trendSnapshotService.collect(clusterId, namespace);
    }

    @GetMapping("/kubernetes/workloads")
    public List<KubernetesWorkloadUsageSnapshot> getKubernetesWorkloadUsageSnapshots(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String workloadName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return trendSnapshotService.getKubernetesWorkloadUsageSnapshots(
                clusterId,
                namespace,
                workloadName,
                from,
                to
        );
    }

    @GetMapping("/cost/workloads")
    public List<CostDailySnapshot> getCostDailySnapshots(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String workloadName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return trendSnapshotService.getCostDailySnapshots(
                clusterId,
                namespace,
                workloadName,
                from,
                to
        );
    }
}
