package org.dbs.poc.unblu.webhook.entrypoint.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.route.webhook.UnbluWebhookPayload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Internal Camel route to process webhook events from Unblu.
 * Called by {@link org.dbs.poc.unblu.webhook.entrypoint.rest.WebhookReceiverController}
 * after receiving POST /api/webhooks/unblu.
 *
 * <p>Internal Route: direct:webhook-receiver-internal
 *
 * <p>Headers processed:
 * <ul>
 *   <li>X-Unblu-Signature: Webhook signature for verification</li>
 *   <li>X-Unblu-Event-Type: Type of the event being sent</li>
 * </ul>
 *
 * <p>Body contract: sets an {@link UnbluWebhookPayload} on the exchange after deserialization.
 */
@Slf4j
@Component
public class WebhookReceiverRoute extends RouteBuilder {

    private static final String ROUTE_WEBHOOK_RECEIVER = "webhook-receiver";
    private static final String HEADER_UNBLU_SIGNATURE = "X-Unblu-Signature";
    private static final String HEADER_UNBLU_EVENT_TYPE = "X-Unblu-Event-Type";

    private final ObjectMapper objectMapper;

    public WebhookReceiverRoute(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure() {
        from("direct:webhook-receiver-internal")
                .routeId(ROUTE_WEBHOOK_RECEIVER)
                .log("=".repeat(100))
                .log("========== WEBHOOK RECEIVED FROM UNBLU ==========")
                .log("=".repeat(100))
                .process(exchange -> {
                    String signature = exchange.getIn().getHeader(HEADER_UNBLU_SIGNATURE, String.class);
                    String eventType = exchange.getIn().getHeader(HEADER_UNBLU_EVENT_TYPE, String.class);

                    UnbluWebhookPayload payload = deserializePayload(exchange.getIn().getBody());

                    log.info("Event Type Header: {}", eventType);
                    log.info("Signature Header: {}", signature);
                    log.info("Payload type: {}", payload != null ? payload.type() : "null");

                    exchange.getIn().setHeader("unbluSignature", signature);
                    exchange.getIn().setHeader("unbluEventType", eventType);
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

    /**
     * Deserializes the raw exchange body to a typed {@link UnbluWebhookPayload}.
     * Supports three input formats: already-typed payload, {@code Map}, or JSON {@code String}.
     */
    private UnbluWebhookPayload deserializePayload(Object body) throws Exception {
        switch (body) {
            case null -> {
                return null;
            }
            case UnbluWebhookPayload typed -> {
                return typed;
            }
            case Map<?, ?> map -> {
                return objectMapper.convertValue(map, UnbluWebhookPayload.class);
            }
            case String json when !json.isBlank() -> {
                return objectMapper.readValue(json, UnbluWebhookPayload.class);
            }
            default -> {
            }
        }
        log.warn("Unexpected payload type: {}", body.getClass().getName());
        return null;
    }
}
