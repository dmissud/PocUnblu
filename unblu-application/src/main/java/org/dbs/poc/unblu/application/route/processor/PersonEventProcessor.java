package org.dbs.poc.unblu.application.route.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.application.route.webhook.UnbluWebhookPayload;
import org.springframework.stereotype.Component;

/**
 * Processes person-related webhook events.
 * Receives a typed {@link UnbluWebhookPayload} — logs the event for observability.
 */
@Slf4j
@Component
public class PersonEventProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        String eventType = payload.type() != null ? payload.type() : payload.eventType();

        log.info("Person Event Received - Type: {}", eventType);
        log.info("  accountId: {}, timestamp: {}", payload.accountId(), payload.timestamp());
    }
}
