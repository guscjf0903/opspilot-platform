package com.opspilot.kafka.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.common.exception.ApiExceptionHandler;
import com.opspilot.kafka.application.KafkaQueryService;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kafka.domain.KafkaConsumerGroupSummary;
import com.opspilot.kafka.domain.KafkaOverview;
import com.opspilot.kafka.domain.KafkaPartitionLag;
import com.opspilot.kafka.domain.KafkaTopicSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = KafkaQueryController.class)
@Import(ApiExceptionHandler.class)
class KafkaQueryControllerTests {

    private static final Instant NOW = Instant.parse("2026-06-08T00:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaQueryService kafkaQueryService;

    @Test
    void returnsKafkaOverview() throws Exception {
        when(kafkaQueryService.getOverview("local"))
                .thenReturn(new KafkaOverview(
                        "local",
                        true,
                        ResourceStatus.WARNING,
                        "KAFKA_WARNING_LAG_OR_PARTITION",
                        1,
                        1,
                        1,
                        120,
                        1,
                        NOW
                ));

        mockMvc.perform(get("/api/clusters/local/kafka/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.status").value("warning"))
                .andExpect(jsonPath("$.totalLag").value(120));
    }

    @Test
    void returnsKafkaTopicsConsumerGroupsAndLag() throws Exception {
        when(kafkaQueryService.getTopics("local"))
                .thenReturn(List.of(new KafkaTopicSummary(
                        "orders.created",
                        1,
                        1,
                        0,
                        0,
                        ResourceStatus.HEALTHY,
                        List.of()
                )));
        when(kafkaQueryService.getConsumerGroups("local"))
                .thenReturn(List.of(new KafkaConsumerGroupSummary(
                        "order-consumer",
                        "STABLE",
                        1,
                        1,
                        1,
                        12,
                        ResourceStatus.HEALTHY,
                        "KAFKA_CONSUMER_GROUP_HEALTHY"
                )));
        when(kafkaQueryService.getTopConsumerGroupLags("local", 5))
                .thenReturn(List.of(new KafkaConsumerGroupLag(
                        "order-consumer",
                        ResourceStatus.HEALTHY,
                        "KAFKA_CONSUMER_GROUP_HEALTHY",
                        12,
                        NOW,
                        List.of(new KafkaPartitionLag("orders.created", 0, 8L, 20L, 12))
                )));

        mockMvc.perform(get("/api/clusters/local/kafka/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("orders.created"));
        mockMvc.perform(get("/api/clusters/local/kafka/consumer-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("order-consumer"));
        mockMvc.perform(get("/api/clusters/local/kafka/lag").queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalLag").value(12));
    }
}
