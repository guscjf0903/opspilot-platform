package com.opspilot.cluster.adapter.in.web;

import com.opspilot.cluster.application.ClusterQueryService;
import com.opspilot.cluster.domain.ClusterSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
public class ClusterQueryController {

    private final ClusterQueryService clusterQueryService;

    @GetMapping
    public List<ClusterSummary> getClusters() {
        return clusterQueryService.getClusters();
    }
}
