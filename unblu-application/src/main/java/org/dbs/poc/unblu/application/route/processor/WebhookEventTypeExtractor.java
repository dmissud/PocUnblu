package org.dbs.poc.unblu.application.route.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Extracts and sets the event type from webhook payload.
 */
@Slf4j
@Component
public class WebhookEventTypeExtractor implements Processor {

    public static final String EVENT_TYPE_HEADER = "webhookEventType";
    private static final String EVENT_TYPE_FIELD = "$_type";
    private static final String EVENT_TYPE_FALLBACK_FIELD = "eventType";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_ACCOUNT_ID = "accountId";

    @Override
    public void process(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        logReceivedBody(body);

        Map<String, Object> payload = getPayloadOrThrow(exchange);
        String eventType = extractEventType(payload);

        logEventDetails(payload, eventType);
        exchange.getIn().setHeader(EVENT_TYPE_HEADER, eventType);
    }

    private void logReceivedBody(Object body) {
        String bodyClassName = body != null ? body.getClass().getName() : "null";
        log.info("Received body class: {}", bodyClassName);
        log.info("Received body: {}", body);
    }

    private Map<String, Object> getPayloadOrThrow(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        if (payload == null) {
            log.error("ERROR: Payload is null!");
            throw new IllegalArgumentException("Webhook payload cannot be null");
        }
        return payload;
    }

    private String extractEventType(Map<String, Object> payload) {
        return Optional.ofNullable((String) payload.get(EVENT_TYPE_FIELD))
                .orElseGet(() -> (String) payload.get(EVENT_TYPE_FALLBACK_FIELD));
    }

    private void logEventDetails(Map<String, Object> payload, String eventType) {
        log.info("Event Type from payload ($_type): {}", payload.get(EVENT_TYPE_FIELD));
        log.info("Event Type from payload (eventType): {}", payload.get(EVENT_TYPE_FALLBACK_FIELD));
        log.info("Event Type selected: {}", eventType);
        log.info("Event Timestamp: {}", payload.get(FIELD_TIMESTAMP));
        log.info("Event Account ID: {}", payload.get(FIELD_ACCOUNT_ID));
    }
}
