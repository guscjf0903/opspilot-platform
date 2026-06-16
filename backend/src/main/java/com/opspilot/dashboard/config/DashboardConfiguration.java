package com.opspilot.dashboard.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashboardConfiguration {

    @Bean
    public Clock dashboardClock() {
        return Clock.systemUTC();
    }
}
