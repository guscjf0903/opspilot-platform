package com.opspilot.cost.adapter.out.opencost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.opspilot.cost.config.OpenCostProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenCostHttpAllocationAdapterTests {

    @Test
    void parsesControllerAllocationAndNormalizesItToMonthlyCost() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://opencost");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenCostProperties properties = new OpenCostProperties();
        properties.setEnabled(true);
        properties.setAllocationWindow("1d");
        properties.setAllocationWindowHours(24.0);
        properties.setMonthlyHours(730.0);
        OpenCostHttpAllocationAdapter adapter = new OpenCostHttpAllocationAdapter(
                restClientBuilder.build(),
                properties
        );

        server.expect(requestTo(startsWith("http://opencost/allocation")))
                .andRespond(withSuccess("""
                        {
                          "code": 200,
                          "data": [
                            {
                              "sample-app/payment-api-84f44c56fb-5n6xf": {
                                "properties": {
                                  "namespace": "sample-app",
                                  "pod": "payment-api-84f44c56fb-5n6xf"
                                },
                                "totalCost": 2.4,
                                "cpuCost": 1.2,
                                "ramCost": 0.6
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var snapshot = adapter.getWorkloadAllocations("local");

        assertThat(snapshot.available()).isTrue();
        assertThat(snapshot.workloads()).singleElement()
                .satisfies(allocation -> {
                    assertThat(allocation.namespace()).isEqualTo("sample-app");
                    assertThat(allocation.controller()).isEqualTo("payment-api");
                    assertThat(allocation.monthlyCost()).isEqualTo(73.0);
                    assertThat(allocation.cpuMonthlyCost()).isEqualTo(36.5);
                    assertThat(allocation.memoryMonthlyCost()).isEqualTo(18.25);
                });
        server.verify();
    }

    @Test
    void returnsUnavailableWhenOpenCostIsDisabled() {
        OpenCostProperties properties = new OpenCostProperties();
        properties.setEnabled(false);
        OpenCostHttpAllocationAdapter adapter = new OpenCostHttpAllocationAdapter(
                RestClient.builder().baseUrl("http://opencost").build(),
                properties
        );

        var snapshot = adapter.getWorkloadAllocations("local");

        assertThat(snapshot.available()).isFalse();
        assertThat(snapshot.reason()).isEqualTo("OPENCOST_DISABLED");
    }
}
