package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Bloc 2 — Supervisor: proxifies conversation history API calls to the event-processor (Bloc 1).
 *
 * <p>The Angular frontend continues to call the Bloc 2 API uniformly. This controller
 * transparently delegates history/sync/enrich operations to the integration layer.
 *
 * <p>Target: {@code http://event-processor:8082/api/history/*}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversation History (proxy)", description = "Proxifies to event-processor Bloc 1")
public class ConversationHistoryProxyController {

    private final RestTemplate restTemplate;
    private final String eventProcessorBaseUrl;

    public ConversationHistoryProxyController(
            RestTemplate restTemplate,
            @Value("${integration.event-processor.base-url:http://localhost:8082}") String eventProcessorBaseUrl) {
        this.restTemplate = restTemplate;
        this.eventProcessorBaseUrl = eventProcessorBaseUrl;
    }

    @GetMapping("/history")
    @Operation(summary = "Liste l'historique (proxy → event-processor Bloc 1)")
    public ResponseEntity<?> listHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "CREATED_AT") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        String url = eventProcessorBaseUrl + "/api/history/conversations?page=" + page
                + "&size=" + size + "&sortField=" + sortField + "&sortDir=" + sortDir;
        log.debug("Proxying GET /history → {}", url);
        try {
            return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }
    }

    @GetMapping("/history/{conversationId}")
    @Operation(summary = "Détail d'une conversation historisée (proxy → event-processor Bloc 1)")
    public ResponseEntity<?> getHistory(@PathVariable String conversationId) {
        String url = eventProcessorBaseUrl + "/api/history/conversations/" + conversationId;
        log.debug("Proxying GET /history/{} → {}", conversationId, url);
        try {
            Object result = restTemplate.getForObject(url, Object.class);
            if (result == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(result);
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }
    }

    @PostMapping("/sync")
    @Operation(summary = "Déclenche la synchronisation (proxy → event-processor Bloc 1)")
    public ResponseEntity<?> sync() {
        String url = eventProcessorBaseUrl + "/api/history/sync";
        log.debug("Proxying POST /sync → {}", url);
        try {
            return ResponseEntity.ok(restTemplate.postForObject(url, null, Object.class));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }
    }

    @PostMapping("/history/{conversationId}/enrich")
    @Operation(summary = "Enrichit une conversation (proxy → event-processor Bloc 1)")
    public ResponseEntity<?> enrich(@PathVariable String conversationId) {
        String url = eventProcessorBaseUrl + "/api/history/conversations/" + conversationId + "/enrich";
        log.debug("Proxying POST /history/{}/enrich → {}", conversationId, url);
        try {
            return ResponseEntity.ok(restTemplate.postForObject(url, null, Object.class));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }
    }
}
