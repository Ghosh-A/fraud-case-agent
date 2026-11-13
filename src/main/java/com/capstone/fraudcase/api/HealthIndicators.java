package com.capstone.fraudcase.api;

import com.capstone.fraudcase.CapstoneProperties;
import com.capstone.fraudcase.knowledge.PolicyVectorStore;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Health for DB (actuator), vector store and model mode — no credentials exposed. */
@Configuration
public class HealthIndicators {

    @Bean
    public HealthIndicator policyVectorStore(PolicyVectorStore store) {
        return () -> Health.up().withDetail("policyChunks", store.size()).build();
    }

    @Bean
    public HealthIndicator processorMode(CapstoneProperties props) {
        return () -> Health.up()
                .withDetail("processorMode", props.getProcessorMode())
                .withDetail("humanReviewRequired", props.isHumanReviewRequired())
                .withDetail("financialActionsAllowed", props.isAllowFinancialActions())
                .build();
    }
}
