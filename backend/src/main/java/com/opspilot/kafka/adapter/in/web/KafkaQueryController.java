package com.opspilot.kafka.adapter.in.web;

import com.opspilot.kafka.application.KafkaQueryService;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kafka.domain.KafkaConsumerGroupSummary;
import com.opspilot.kafka.domain.KafkaOverview;
import com.opspilot.kafka.domain.KafkaTopicSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}/kafka")
@RequiredArgsConstructor
public class KafkaQueryController {

    private final KafkaQueryService kafkaQueryService;

    @GetMapping("/overview")
    public KafkaOverview getOverview(@PathVariable String clusterId) {
        return kafkaQueryService.getOverview(clusterId);
    }

    @GetMapping("/topics")
    public List<KafkaTopicSummary> getTopics(@PathVariable String clusterId) {
        return kafkaQueryService.getTopics(clusterId);
    }

    @GetMapping("/consumer-groups")
    public List<KafkaConsumerGroupSummary> getConsumerGroups(@PathVariable String clusterId) {
        return kafkaQueryService.getConsumerGroups(clusterId);
    }

    @GetMapping("/consumer-groups/{groupId}/lag")
    public KafkaConsumerGroupLag getConsumerGroupLag(
            @PathVariable String clusterId,
            @PathVariable String groupId
    ) {
        return kafkaQueryService.getConsumerGroupLag(clusterId, groupId);
    }

    @GetMapping("/lag")
    public List<KafkaConsumerGroupLag> getTopConsumerGroupLags(
            @PathVariable String clusterId,
            @RequestParam(required = false) Integer limit
    ) {
        return kafkaQueryService.getTopConsumerGroupLags(clusterId, limit);
    }
}
