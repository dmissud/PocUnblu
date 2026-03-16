package org.dbs.poc.unblu.application.route;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Camel route to process Unblu webhook events
 */
@Slf4j
@Component
public class WebhookEventRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Main webhook processor route
        from("direct:webhook-event-processor")
            .routeId("webhook-event-processor")
            .log("=".repeat(100))
            .log("========== CAMEL ROUTE: Processing Webhook Event ==========")
            .log("=".repeat(100))
            .process(exchange -> {
                Map<String, Object> payload = exchange.getIn().getBody(Map.class);
                String eventType = (String) payload.get("$_type");

                log.info("Event Type from payload: {}", eventType);
                log.info("Event Timestamp: {}", payload.get("timestamp"));
                log.info("Event Account ID: {}", payload.get("accountId"));

                // Store event type in header for routing
                exchange.getIn().setHeader("webhookEventType", eventType);
            })
            .choice()
                .when(header("webhookEventType").startsWith("conversation."))
                    .log("→ Routing to conversation handler")
                    .to("direct:webhook-handle-conversation")
                .when(header("webhookEventType").startsWith("person."))
                    .log("→ Routing to person handler")
                    .to("direct:webhook-handle-person")
                .otherwise()
                    .log("→ Unknown event type, logging only")
                    .to("direct:webhook-handle-unknown")
            .end()
            .log("=".repeat(100))
            .log("========== CAMEL ROUTE: Webhook Processing Complete ==========")
            .log("=".repeat(100));

        // Conversation events handler
        from("direct:webhook-handle-conversation")
            .routeId("webhook-handle-conversation")
            .log("=".repeat(100))
            .log("========== CONVERSATION EVENT HANDLER ==========")
            .log("=".repeat(100))
            .process(exchange -> {
                Map<String, Object> payload = exchange.getIn().getBody(Map.class);
                String eventType = (String) payload.get("$_type");

                log.info("📞 Conversation Event Received!");
                log.info("   Type: {}", eventType);
                log.info("   Conversation ID: {}", payload.get("conversationId"));
                log.info("   Full Details:");

                // Log all fields in the payload
                payload.forEach((key, value) -> {
                    if (value != null) {
                        log.info("      • {}: {}", key, value);
                    }
                });

                // Special handling for conversation.created
                if ("conversation.created".equals(eventType)) {
                    log.info("");
                    log.info("🎉 NEW CONVERSATION CREATED!");
                    log.info("   This is the event you configured in the webhook!");
                    log.info("");
                }
            })
            .log("=".repeat(100))
            .log("========== END CONVERSATION EVENT HANDLER ==========")
            .log("=".repeat(100));

        // Person events handler
        from("direct:webhook-handle-person")
            .routeId("webhook-handle-person")
            .log("=".repeat(100))
            .log("========== PERSON EVENT HANDLER ==========")
            .log("=".repeat(100))
            .process(exchange -> {
                Map<String, Object> payload = exchange.getIn().getBody(Map.class);
                String eventType = (String) payload.get("$_type");

                log.info("👤 Person Event Received!");
                log.info("   Type: {}", eventType);
                log.info("   Full Details:");

                payload.forEach((key, value) -> {
                    if (value != null) {
                        log.info("      • {}: {}", key, value);
                    }
                });
            })
            .log("=".repeat(100))
            .log("========== END PERSON EVENT HANDLER ==========")
            .log("=".repeat(100));

        // Unknown events handler
        from("direct:webhook-handle-unknown")
            .routeId("webhook-handle-unknown")
            .log("=".repeat(100))
            .log("========== UNKNOWN EVENT HANDLER ==========")
            .log("=".repeat(100))
            .process(exchange -> {
                Map<String, Object> payload = exchange.getIn().getBody(Map.class);
                String eventType = (String) payload.get("$_type");

                log.warn("⚠️  Unknown Event Type Received!");
                log.warn("   Type: {}", eventType);
                log.warn("   Full Payload: {}", payload);
            })
            .log("=".repeat(100))
            .log("========== END UNKNOWN EVENT HANDLER ==========")
            .log("=".repeat(100));
    }
}
