package org.dbs.poc.unblu.infrastructure.adapter.rules;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.port.secondary.RuleEnginePort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuleEngineCamelAdapter implements RuleEnginePort {

    private final ProducerTemplate producerTemplate;

    @Override
    public ChatRoutingDecision evaluateRouting(ConversationContext context) {
        // Call the Rule Engine Camel route (which has retry/resilience config)
        return producerTemplate.requestBody("direct:rule-engine-adapter", context, ChatRoutingDecision.class);
    }
}
