package org.dbs.poc.unblu.application.route.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.application.route.webhook.UnbluWebhookPayload;
import org.springframework.stereotype.Component;

/**
 * Extracts and sets the event type from a typed {@link UnbluWebhookPayload}.
 * Sets the {@value #EVENT_TYPE_HEADER} header for downstream routing.
 */
@Slf4j
@Component
public class WebhookEventTypeExtractor implements Processor {

    public static final String EVENT_TYPE_HEADER = "webhookEventType";

    /**
     * Extracts the event type from the payload and stores it in the
     * {@value #EVENT_TYPE_HEADER} header for downstream content-based routing.
     *
     * @throws IllegalArgumentException if the payload is {@code null}
     */
    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        if (payload == null) {
            log.error("ERROR: Payload is null!");
            throw new IllegalArgumentException("Webhook payload cannot be null");
        }

        String eventType = payload.type() != null ? payload.type() : payload.eventType();

        log.info("Event type ($_type): {}", payload.type());
        log.info("Event type (eventType): {}", payload.eventType());
        log.info("Event type selected: {}", eventType);
        log.info("Event timestamp: {}", payload.timestamp());
        log.info("Event accountId: {}", payload.accountId());

        exchange.getIn().setHeader(EVENT_TYPE_HEADER, eventType);
    }
}
