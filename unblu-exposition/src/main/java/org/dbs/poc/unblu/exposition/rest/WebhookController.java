package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SetupWebhookUseCase;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Gestion de l'infrastructure Webhook Unblu")
public class WebhookController {

    private final SetupWebhookUseCase setupWebhookUseCase;

    @PostMapping("/setup")
    @Operation(summary = "Configure l'infrastructure webhook (tunnel + Unblu)")
    public ResponseEntity<WebhookSetupResult> setup() {
        return ResponseEntity.ok(setupWebhookUseCase.setupWebhook());
    }

    @GetMapping("/status")
    @Operation(summary = "Récupère le statut actuel du webhook")
    public ResponseEntity<WebhookStatus> status() {
        return ResponseEntity.ok(setupWebhookUseCase.getWebhookStatus());
    }

    @DeleteMapping("/teardown")
    @Operation(summary = "Désinstalle l'infrastructure webhook")
    public ResponseEntity<Void> teardown(@RequestParam(defaultValue = "false") boolean deleteWebhook) {
        setupWebhookUseCase.teardownWebhook(deleteWebhook);
        return ResponseEntity.noContent().build();
    }
}
