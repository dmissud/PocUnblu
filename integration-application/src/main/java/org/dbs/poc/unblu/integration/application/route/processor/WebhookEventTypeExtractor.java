package org.dbs.poc.unblu.integration.application.route.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.integration.domain.model.webhook.UnbluWebhookPayload;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventTypeExtractor implements Processor {
    public static final String EVENT_TYPE_HEADER = "X-Unblu-Event-Type";

    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        String eventType = payload.eventType() != null ? payload.eventType() : payload.type();
        exchange.getIn().setHeader(EVENT_TYPE_HEADER, eventType != null ? eventType : "unknown");
    }
}
