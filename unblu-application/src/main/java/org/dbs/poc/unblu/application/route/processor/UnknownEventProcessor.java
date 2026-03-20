package org.dbs.poc.unblu.application.route.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Processes unknown webhook events.
 */
@Slf4j
@Component
public class UnknownEventProcessor implements Processor {

    private static final String EVENT_TYPE_FIELD = "$_type";
    private static final String EVENT_TYPE_FALLBACK_FIELD = "eventType";

    @Override
    public void process(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        String eventType = extractEventType(payload);

        log.warn("⚠️  Unknown Event Type Received!");
        log.warn("   Type: {}", eventType);
        log.warn("   Full Payload: {}", payload);
    }

    private String extractEventType(Map<String, Object> payload) {
        return Optional.ofNullable((String) payload.get(EVENT_TYPE_FIELD))
                .orElseGet(() -> (String) payload.get(EVENT_TYPE_FALLBACK_FIELD));
    }
}
