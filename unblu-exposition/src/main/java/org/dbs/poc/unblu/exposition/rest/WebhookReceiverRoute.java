package org.dbs.poc.unblu.exposition.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Internal Camel route to process webhook events from Unblu.
 * This route is called by the REST DSL endpoint defined in RestExpositionRoute.
 *
 * <p>REST Endpoint: POST /api/webhooks/unblu (defined in RestExpositionRoute)
 * <p>Internal Route: direct:webhook-receiver-internal
 *
 * <p>Headers processed:
 * <ul>
 *   <li>X-Unblu-Signature: Webhook signature for verification</li>
 *   <li>X-Unblu-Event-Type: Type of the event being sent</li>
 * </ul>
 */
@Slf4j
@Component
public class WebhookReceiverRoute extends RouteBuilder {

    private static final String ROUTE_WEBHOOK_RECEIVER = "webhook-receiver";
    private static final String HEADER_UNBLU_SIGNATURE = "X-Unblu-Signature";
    private static final String HEADER_UNBLU_EVENT_TYPE = "X-Unblu-Event-Type";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure() {
        // Internal route to process webhook events from Unblu
        // Called by REST DSL endpoint: POST /api/webhooks/unblu
        from("direct:webhook-receiver-internal")
            .routeId(ROUTE_WEBHOOK_RECEIVER)
            .log("=".repeat(100))
            .log("========== WEBHOOK RECEIVED FROM UNBLU ==========")
            .log("=".repeat(100))
            .process(exchange -> {
                // Extract headers
                String signature = exchange.getIn().getHeader(HEADER_UNBLU_SIGNATURE, String.class);
                String eventType = exchange.getIn().getHeader(HEADER_UNBLU_EVENT_TYPE, String.class);

                // Convert InputStream body to String, then to Map
                String jsonString = exchange.getIn().getBody(String.class);
                log.info("Received JSON string: {}", jsonString);

                // Parse JSON string to Map
                Map<String, Object> payload = null;
                if (jsonString != null && !jsonString.isEmpty()) {
                    try {
                        payload = objectMapper.readValue(jsonString, Map.class);
                        log.info("Parsed payload successfully");
                    } catch (Exception e) {
                        log.error("Failed to parse JSON payload", e);
                        throw new RuntimeException("Invalid JSON payload", e);
                    }
                }

                log.info("Event Type Header: {}", eventType);
                log.info("Signature Header: {}", signature);
                log.info("Payload Type: {}", payload != null ? payload.get("$_type") : "null");
                log.info("Full Payload: {}", payload);

                // Store headers for downstream processing
                exchange.getIn().setHeader("unbluSignature", signature);
                exchange.getIn().setHeader("unbluEventType", eventType);

                // IMPORTANT: Set the parsed Map as the body for downstream routes
                exchange.getIn().setBody(payload);
            })
            .log("=".repeat(100))
            .choice()
                .when(body().isNull())
                    .log("ERROR: Received null payload from Unblu")
                    .setHeader("CamelHttpResponseCode", constant(400))
                    .setBody(constant("Bad Request: Empty payload"))
                .otherwise()
                    .log("Sending webhook event to processor...")
                    .to("direct:webhook-event-processor")
                    .log("Webhook successfully processed")
                    .setHeader("CamelHttpResponseCode", constant(200))
                    .setBody(constant(""))
            .end()
            .log("=".repeat(100))
            .onException(Exception.class)
                .log("=".repeat(100))
                .log("========== ERROR PROCESSING WEBHOOK ==========")
                .log("=".repeat(100))
                .log("Error: ${exception.message}")
                .log("Stack trace: ${exception.stacktrace}")
                .log("=".repeat(100))
                .setHeader("CamelHttpResponseCode", constant(500))
                .setBody(constant("Internal Server Error"))
                .handled(true)
            .end();
    }
}
