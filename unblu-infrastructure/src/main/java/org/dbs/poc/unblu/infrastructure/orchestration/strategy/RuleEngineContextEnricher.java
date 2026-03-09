package org.dbs.poc.unblu.infrastructure.orchestration.strategy;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;

public class RuleEngineContextEnricher implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        ConversationContext context = oldExchange.getIn().getBody(ConversationContext.class);
        ChatRoutingDecision decision = newExchange.getIn().getBody(ChatRoutingDecision.class);

        context.setRoutingDecision(decision);
        oldExchange.getIn().setBody(context);

        return oldExchange;
    }
}
