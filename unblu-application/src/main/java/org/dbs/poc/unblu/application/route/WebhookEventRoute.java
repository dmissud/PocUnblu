package org.dbs.poc.unblu.application.route;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Camel route to process Unblu webhook events.
 * Handles conversation, person, and unknown event types.
 */
@Slf4j
@Component
public class WebhookEventRoute extends RouteBuilder {

    private static final String EVENT_TYPE_HEADER = "webhookEventType";
    private static final String EVENT_TYPE_FIELD = "$_type";
    private static final String EVENT_TYPE_FALLBACK_FIELD = "eventType";

    private static final String CONVERSATION_PREFIX = "conversation.";
    private static final String CONVERSATION_CLASS_PREFIX = "Conversation";
    private static final String PERSON_PREFIX = "person.";
    private static final String PERSON_CLASS_PREFIX = "Person";

    private static final String CONVERSATION_CREATED_EVENT = "ConversationCreatedEvent";
    private static final String CONVERSATION_CREATED_EVENT_TYPE = "conversation.created";

    private static final String SEPARATOR = "=".repeat(100);

    // Route IDs
    private static final String ROUTE_WEBHOOK_EVENT_PROCESSOR = "webhook-event-processor";
    private static final String ROUTE_WEBHOOK_HANDLE_CONVERSATION = "webhook-handle-conversation";
    private static final String ROUTE_WEBHOOK_HANDLE_PERSON = "webhook-handle-person";
    private static final String ROUTE_WEBHOOK_HANDLE_UNKNOWN = "webhook-handle-unknown";

    // Direct endpoints
    private static final String DIRECT_WEBHOOK_EVENT_PROCESSOR = "direct:webhook-event-processor";
    private static final String DIRECT_WEBHOOK_HANDLE_CONVERSATION = "direct:webhook-handle-conversation";
    private static final String DIRECT_WEBHOOK_HANDLE_PERSON = "direct:webhook-handle-person";
    private static final String DIRECT_WEBHOOK_HANDLE_UNKNOWN = "direct:webhook-handle-unknown";

    // Log messages
    private static final String LOG_ROUTING_CONVERSATION_EVENT_TYPE = "→ Routing to conversation handler (eventType format)";
    private static final String LOG_ROUTING_CONVERSATION_TYPE = "→ Routing to conversation handler ($_type format)";
    private static final String LOG_ROUTING_PERSON_EVENT_TYPE = "→ Routing to person handler (eventType format)";
    private static final String LOG_ROUTING_PERSON_TYPE = "→ Routing to person handler ($_type format)";
    private static final String LOG_ROUTING_UNKNOWN = "→ Unknown event type, logging only";

    // Payload field keys
    private static final String FIELD_CONVERSATION_ID = "conversationId";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_ACCOUNT_ID = "accountId";

    @Override
    public void configure() {
        configureMainWebhookRoute();
        configureConversationHandler();
        configurePersonHandler();
        configureUnknownHandler();
    }

    private void configureMainWebhookRoute() {
        from(DIRECT_WEBHOOK_EVENT_PROCESSOR)
            .routeId(ROUTE_WEBHOOK_EVENT_PROCESSOR)
            .log(SEPARATOR)
            .log("========== CAMEL ROUTE: Processing Webhook Event ==========")
            .log(SEPARATOR)
            .process(this::processWebhookEvent)
            .choice()
                .when(header(EVENT_TYPE_HEADER).startsWith(CONVERSATION_PREFIX))
                    .log(LOG_ROUTING_CONVERSATION_EVENT_TYPE)
                    .to(DIRECT_WEBHOOK_HANDLE_CONVERSATION)
                .when(header(EVENT_TYPE_HEADER).startsWith(CONVERSATION_CLASS_PREFIX))
                    .log(LOG_ROUTING_CONVERSATION_TYPE)
                    .to(DIRECT_WEBHOOK_HANDLE_CONVERSATION)
                .when(header(EVENT_TYPE_HEADER).startsWith(PERSON_PREFIX))
                    .log(LOG_ROUTING_PERSON_EVENT_TYPE)
                    .to(DIRECT_WEBHOOK_HANDLE_PERSON)
                .when(header(EVENT_TYPE_HEADER).startsWith(PERSON_CLASS_PREFIX))
                    .log(LOG_ROUTING_PERSON_TYPE)
                    .to(DIRECT_WEBHOOK_HANDLE_PERSON)
                .otherwise()
                    .log(LOG_ROUTING_UNKNOWN)
                    .to(DIRECT_WEBHOOK_HANDLE_UNKNOWN)
            .end()
            .log(SEPARATOR)
            .log("========== CAMEL ROUTE: Webhook Processing Complete ==========")
            .log(SEPARATOR);
    }

    private void configureConversationHandler() {
        from(DIRECT_WEBHOOK_HANDLE_CONVERSATION)
            .routeId(ROUTE_WEBHOOK_HANDLE_CONVERSATION)
            .log(SEPARATOR)
            .log("========== CONVERSATION EVENT HANDLER ==========")
            .log(SEPARATOR)
            .process(this::handleConversationEvent)
            .log(SEPARATOR)
            .log("========== END CONVERSATION EVENT HANDLER ==========")
            .log(SEPARATOR);
    }

    private void configurePersonHandler() {
        from(DIRECT_WEBHOOK_HANDLE_PERSON)
            .routeId(ROUTE_WEBHOOK_HANDLE_PERSON)
            .log(SEPARATOR)
            .log("========== PERSON EVENT HANDLER ==========")
            .log(SEPARATOR)
            .process(this::handlePersonEvent)
            .log(SEPARATOR)
            .log("========== END PERSON EVENT HANDLER ==========")
            .log(SEPARATOR);
    }

    private void configureUnknownHandler() {
        from(DIRECT_WEBHOOK_HANDLE_UNKNOWN)
            .routeId(ROUTE_WEBHOOK_HANDLE_UNKNOWN)
            .log(SEPARATOR)
            .log("========== UNKNOWN EVENT HANDLER ==========")
            .log(SEPARATOR)
            .process(this::handleUnknownEvent)
            .log(SEPARATOR)
            .log("========== END UNKNOWN EVENT HANDLER ==========")
            .log(SEPARATOR);
    }

    private void processWebhookEvent(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        logReceivedBody(body);

        Map<String, Object> payload = getPayloadOrThrow(exchange);
        String eventType = extractEventType(payload);

        logEventDetails(payload, eventType);
        exchange.getIn().setHeader(EVENT_TYPE_HEADER, eventType);
    }

    private void handleConversationEvent(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        String eventType = extractEventType(payload);

        log.info("📞 Conversation Event Received!");
        log.info("   Type: {}", eventType);
        log.info("   Conversation ID: {}", payload.get(FIELD_CONVERSATION_ID));
        log.info("   Full Details:");

        logPayloadFields(payload);

        if (isConversationCreatedEvent(eventType)) {
            logConversationCreated();
        }
    }

    private void handlePersonEvent(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        String eventType = extractEventType(payload);

        log.info("👤 Person Event Received!");
        log.info("   Type: {}", eventType);
        log.info("   Full Details:");

        logPayloadFields(payload);
    }

    private void handleUnknownEvent(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        String eventType = extractEventType(payload);

        log.warn("⚠️  Unknown Event Type Received!");
        log.warn("   Type: {}", eventType);
        log.warn("   Full Payload: {}", payload);
    }

    private void logReceivedBody(Object body) {
        String bodyClassName = body != null ? body.getClass().getName() : "null";
        log.info("Received body class: {}", bodyClassName);
        log.info("Received body: {}", body);
    }

    private Map<String, Object> getPayloadOrThrow(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        if (payload == null) {
            log.error("ERROR: Payload is null in {}!", ROUTE_WEBHOOK_EVENT_PROCESSOR);
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

    private void logPayloadFields(Map<String, Object> payload) {
        payload.forEach((key, value) -> {
            if (value != null) {
                log.info("      • {}: {}", key, value);
            }
        });
    }

    private boolean isConversationCreatedEvent(String eventType) {
        return CONVERSATION_CREATED_EVENT.equals(eventType)
            || CONVERSATION_CREATED_EVENT_TYPE.equals(eventType);
    }

    private void logConversationCreated() {
        log.info("");
        log.info("🎉 NEW CONVERSATION CREATED!");
        log.info("   This is the event you configured in the webhook!");
        log.info("");
    }
}
