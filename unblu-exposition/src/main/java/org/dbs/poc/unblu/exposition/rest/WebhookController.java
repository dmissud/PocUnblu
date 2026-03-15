package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for receiving Unblu webhooks
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "API pour recevoir les webhooks Unblu")
public class WebhookController {

    private final ProducerTemplate producerTemplate;

    /**
     * Receive webhook events from Unblu
     */
    @PostMapping("/unblu")
    @Operation(summary = "Receive Unblu webhook", description = "Endpoint appelé par Unblu pour envoyer les événements webhook")
    public ResponseEntity<Void> receiveWebhook(
        @RequestHeader(value = "X-Unblu-Signature", required = false) String signature,
        @RequestHeader(value = "X-Unblu-Event-Type", required = false) String eventType,
        @RequestBody Map<String, Object> payload
    ) {
        log.info("=".repeat(100));
        log.info("========== WEBHOOK RECEIVED FROM UNBLU ==========");
        log.info("=".repeat(100));
        log.info("Event Type Header: {}", eventType);
        log.info("Signature Header: {}", signature);
        log.info("Payload Type: {}", payload.get("$_type"));
        log.info("Full Payload: {}", payload);
        log.info("=".repeat(100));

        try {
            // Send to Camel route for processing
            producerTemplate.sendBodyAndHeader(
                "direct:webhook-event-processor",
                payload,
                "eventType",
                payload.get("$_type")
            );

            log.info("Webhook successfully processed and sent to Camel route");
            log.info("=".repeat(100));

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("=".repeat(100));
            log.error("========== ERROR PROCESSING WEBHOOK ==========");
            log.error("=".repeat(100));
            log.error("Error details:", e);
            log.error("=".repeat(100));

            return ResponseEntity.internalServerError().build();
        }
    }
}
