package org.dbs.poc.unblu.application.route;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.route.processor.ConversationEventProcessor;
import org.dbs.poc.unblu.application.route.processor.PersonEventProcessor;
import org.dbs.poc.unblu.application.route.processor.UnknownEventProcessor;
import org.dbs.poc.unblu.application.route.processor.WebhookEventTypeExtractor;
import org.springframework.stereotype.Component;

/**
 * Camel route to process Unblu webhook events.
 * Routes events to appropriate handlers based on event type.
 */
@Slf4j
@Component
public class WebhookEventRoute extends RouteBuilder {

    private final WebhookEventTypeExtractor eventTypeExtractor;
    private final ConversationEventProcessor conversationEventProcessor;
    private final PersonEventProcessor personEventProcessor;
    private final UnknownEventProcessor unknownEventProcessor;

    private static final String CONVERSATION_PREFIX = "conversation.";
    private static final String CONVERSATION_CLASS_PREFIX = "Conversation";
    private static final String PERSON_PREFIX = "person.";
    private static final String PERSON_CLASS_PREFIX = "Person";

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

    public WebhookEventRoute(
            WebhookEventTypeExtractor eventTypeExtractor,
            ConversationEventProcessor conversationEventProcessor,
            PersonEventProcessor personEventProcessor,
            UnknownEventProcessor unknownEventProcessor) {
        this.eventTypeExtractor = eventTypeExtractor;
        this.conversationEventProcessor = conversationEventProcessor;
        this.personEventProcessor = personEventProcessor;
        this.unknownEventProcessor = unknownEventProcessor;
    }

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
            .process(eventTypeExtractor)
            .choice()
                .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(CONVERSATION_PREFIX))
                    .log(LOG_ROUTING_CONVERSATION_EVENT_TYPE)
                    .to(DIRECT_WEBHOOK_HANDLE_CONVERSATION)
                .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(CONVERSATION_CLASS_PREFIX))
                    .log(LOG_ROUTING_CONVERSATION_TYPE)
                    .to(DIRECT_WEBHOOK_HANDLE_CONVERSATION)
                .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(PERSON_PREFIX))
                    .log(LOG_ROUTING_PERSON_EVENT_TYPE)
                    .to(DIRECT_WEBHOOK_HANDLE_PERSON)
                .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(PERSON_CLASS_PREFIX))
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
            .process(conversationEventProcessor)
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
            .process(personEventProcessor)
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
            .process(unknownEventProcessor)
            .log(SEPARATOR)
            .log("========== END UNKNOWN EVENT HANDLER ==========")
            .log(SEPARATOR);
    }
}
