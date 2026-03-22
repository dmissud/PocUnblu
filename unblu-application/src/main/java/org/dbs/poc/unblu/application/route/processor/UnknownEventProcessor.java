package org.dbs.poc.unblu.application.route.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.application.route.webhook.UnbluWebhookPayload;
import org.springframework.stereotype.Component;

/**
 * Handles unrecognized webhook event types — logs a warning for observability.
 */
@Slf4j
@Component
public class UnknownEventProcessor implements Processor {

    /**
     * Logs a warning for any unrecognized event type. Does not throw.
     */
    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        String eventType = payload.type() != null ? payload.type() : payload.eventType();

        log.warn("Unknown webhook event type received: {}", eventType);
        log.warn("  accountId: {}, timestamp: {}", payload.accountId(), payload.timestamp());
    }
}
