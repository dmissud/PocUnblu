package org.dbs.poc.unblu.webhook.entrypoint.rest;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST entry point for Unblu webhook callbacks.
 *
 * <p>Receives POST /api/webhooks/unblu from Unblu (via ngrok tunnel),
 * bridges the HTTP request into the Camel route {@code direct:webhook-receiver-internal}.
 */
@Slf4j
@RestController
public class WebhookReceiverController {

    private static final String DIRECT_WEBHOOK_RECEIVER = "direct:webhook-receiver-internal";

    private final ProducerTemplate producerTemplate;

    public WebhookReceiverController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @PostMapping("/api/webhooks/unblu")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "X-Unblu-Signature", required = false) String signature,
            @RequestHeader(value = "X-Unblu-Event-Type", required = false) String eventType,
            @RequestBody(required = false) String body) {

        log.debug("Webhook received — eventType={}", eventType);

        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Unblu-Signature", signature);
        headers.put("X-Unblu-Event-Type", eventType);

        Exchange result = producerTemplate.request(DIRECT_WEBHOOK_RECEIVER, exchange -> {
            exchange.getIn().setBody(body);
            exchange.getIn().getHeaders().putAll(headers);
        });

        Integer statusCode = result.getIn().getHeader("CamelHttpResponseCode", Integer.class);
        String responseBody = result.getIn().getBody(String.class);

        return ResponseEntity
                .status(statusCode != null ? statusCode : 200)
                .body(responseBody);
    }
}
