package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SetupWebhookUseCase;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for webhook setup operations
 */
@Slf4j
@RestController
@RequestMapping("/rest/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook Setup", description = "API pour configurer les webhooks Unblu avec ngrok")
public class WebhookSetupController {

    private final SetupWebhookUseCase setupWebhookUseCase;

    /**
     * Setup complete webhook configuration (ngrok + Unblu registration)
     */
    @PostMapping("/setup")
    @Operation(summary = "Setup webhook", description = "Lance ngrok et enregistre/met à jour le webhook dans Unblu")
    public ResponseEntity<WebhookSetupResult> setupWebhook() {
        log.info("REST API: Setup webhook requested");

        WebhookSetupResult result = setupWebhookUseCase.setupWebhook();

        if (result.success()) {
            log.info("REST API: Webhook setup successful - URL: {}, Webhook ID: {}",
                result.ngrokUrl(), result.webhookId());
            return ResponseEntity.ok(result);
        } else {
            log.error("REST API: Webhook setup failed - {}", result.message());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get current webhook status
     */
    @GetMapping("/status")
    @Operation(summary = "Get webhook status", description = "Récupère le statut actuel de la configuration webhook")
    public ResponseEntity<WebhookStatus> getWebhookStatus() {
        log.debug("REST API: Webhook status requested");

        WebhookStatus status = setupWebhookUseCase.getWebhookStatus();
        log.debug("REST API: Webhook status - Ngrok: {}, Webhook: {}",
            status.ngrokRunning(), status.webhookRegistered());

        return ResponseEntity.ok(status);
    }

    /**
     * Teardown webhook configuration
     */
    @DeleteMapping("/teardown")
    @Operation(summary = "Teardown webhook", description = "Arrête ngrok et optionnellement supprime le webhook d'Unblu")
    public ResponseEntity<Void> teardownWebhook(
        @RequestParam(defaultValue = "false") boolean deleteWebhook
    ) {
        log.info("REST API: Teardown webhook requested (deleteWebhook: {})", deleteWebhook);

        setupWebhookUseCase.teardownWebhook(deleteWebhook);

        log.info("REST API: Webhook teardown successful");
        return ResponseEntity.ok().build();
    }
}
