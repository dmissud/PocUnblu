package org.dbs.poc.unblu.infrastructure.orchestration.strategy;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;

public class ErpContextEnricher implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        ConversationContext context = oldExchange.getIn().getBody(ConversationContext.class);
        CustomerProfile profile = newExchange.getIn().getBody(CustomerProfile.class);

        context.setCustomerProfile(profile);
        oldExchange.getIn().setBody(context);

        return oldExchange;
    }
}
