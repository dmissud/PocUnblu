package org.dbs.poc.unblu.webhook.entrypoint.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for Unblu webhook callbacks.
 *
 * <p>Validates the incoming request (non-blank body, known event type header) and publishes
 * the raw JSON payload to the Kafka topic {@code unblu.webhook.events}.
 *
 * <p>Returns 202 Accepted immediately — processing is asynchronous.
 */
@Slf4j
@RestController
public class WebhookReceiverController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.webhook-events:unblu.webhook.events}")
    private String topic;

    public WebhookReceiverController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/api/webhooks/unblu")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "X-Unblu-Signature", required = false) String signature,
            @RequestHeader(value = "X-Unblu-Event-Type", required = false) String eventType,
            @RequestBody(required = false) String body) {

        if (body == null || body.isBlank()) {
            log.warn("Webhook rejected — empty body (eventType={})", eventType);
            return ResponseEntity.badRequest().body("Bad Request: empty payload");
        }

        if (eventType == null || eventType.isBlank()) {
            log.warn("Webhook rejected — missing X-Unblu-Event-Type header");
            return ResponseEntity.badRequest().body("Bad Request: missing X-Unblu-Event-Type header");
        }

        log.info("Webhook received — eventType={}, publishing to Kafka topic={}", eventType, topic);
        kafkaTemplate.send(topic, eventType, body);

        return ResponseEntity.accepted().build();
    }
}
