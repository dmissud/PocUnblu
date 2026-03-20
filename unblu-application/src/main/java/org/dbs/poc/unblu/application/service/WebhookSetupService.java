package org.dbs.poc.unblu.application.service;

import com.unblu.webapi.model.v4.WebhookRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SetupWebhookUseCase;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
import org.dbs.poc.unblu.infrastructure.adapter.ngrok.NgrokManager;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluService;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to orchestrate webhook setup
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookSetupService implements SetupWebhookUseCase {

    private static final List<String> WEBHOOK_EVENTS = List.of(
        "conversation.created",
        "conversation.new_message",
        "conversation.ended"
    );

    private final NgrokManager ngrokManager;
    private final UnbluService unbluService;

    @Value("${unblu.webhook.name:unblu-poc-webhook}")
    private String webhookName;

    @Value("${unblu.webhook.endpoint-path:/api/webhooks/unblu}")
    private String webhookEndpointPath;

    @Override
    public WebhookSetupResult setupWebhook() {
        log.info("Starting webhook setup...");

        try {
            // Step 1: Start ngrok tunnel
            log.info("Step 1: Starting ngrok tunnel...");
            boolean ngrokStarted = ngrokManager.startNgrok();
            if (!ngrokStarted) {
                return WebhookSetupResult.failure("Échec du démarrage de ngrok. Vérifiez que ngrok est installé.");
            }

            // Step 2: Get public URL
            String ngrokUrl = ngrokManager.getPublicUrl();
            if (ngrokUrl == null) {
                ngrokManager.stopNgrok();
                return WebhookSetupResult.failure("Impossible de récupérer l'URL publique de ngrok");
            }
            log.info("Ngrok tunnel active: {}", ngrokUrl);

            // Step 3: Build full webhook endpoint
            String webhookEndpoint = ngrokUrl + webhookEndpointPath;
            log.info("Webhook endpoint will be: {}", webhookEndpoint);

            // Step 4: Log event types
            log.info("Webhook will listen to events: {}", WEBHOOK_EVENTS);

            // Step 5: Check if webhook already exists
            WebhookRegistration webhook = null;
            try {
                log.info("Checking if webhook '{}' already exists...", webhookName);
                webhook = unbluService.getWebhookByName(webhookName);
                log.info("Webhook '{}' already exists with ID: {}, updating endpoint...", webhookName, webhook.getId());

                // Update existing webhook
                webhook = unbluService.updateWebhook(webhook.getId(), webhookEndpoint, WEBHOOK_EVENTS);
                log.info("Webhook updated successfully");

            } catch (UnbluApiException e) {
                log.info("Caught UnbluApiException with status code: {}", e.getStatusCode());
                if (e.getStatusCode() == 404) {
                    // Webhook doesn't exist, create it
                    log.info("Webhook '{}' doesn't exist (404), creating new webhook...", webhookName);
                    try {
                        webhook = unbluService.createWebhook(webhookName, webhookEndpoint, WEBHOOK_EVENTS);
                        log.info("Webhook created successfully with ID: {}", webhook.getId());
                    } catch (Exception createEx) {
                        log.error("Failed to create webhook", createEx);
                        ngrokManager.stopNgrok();
                        throw createEx;
                    }
                } else {
                    // Other error
                    log.error("Unexpected error code from Unblu: {}", e.getStatusCode());
                    ngrokManager.stopNgrok();
                    throw e;
                }
            } catch (Exception e) {
                log.error("Unexpected exception type while checking webhook: {}", e.getClass().getName(), e);
                ngrokManager.stopNgrok();
                throw e;
            }

            if (webhook == null) {
                log.error("Webhook is null after creation/update attempt!");
                ngrokManager.stopNgrok();
                return WebhookSetupResult.failure("Le webhook n'a pas pu être créé ou mis à jour");
            }

            log.info("Webhook setup completed successfully!");
            return WebhookSetupResult.success(ngrokUrl, webhook.getId(), webhook.getName());

        } catch (Exception e) {
            log.error("Error during webhook setup", e);
            ngrokManager.stopNgrok(); // Cleanup on error
            return WebhookSetupResult.failure("Erreur lors de la configuration du webhook: " + e.getMessage());
        }
    }

    @Override
    public WebhookStatus getWebhookStatus() {
        log.debug("Checking webhook status...");

        // Check ngrok status
        NgrokManager.NgrokStatus ngrokStatus = ngrokManager.getStatus();

        // Check webhook registration
        try {
            WebhookRegistration webhook = unbluService.getWebhookByName(webhookName);
            return new WebhookStatus(
                ngrokStatus.running(),
                ngrokStatus.publicUrl(),
                true,
                webhook.getId(),
                webhook.getName(),
                webhook.getEndpoint()
            );
        } catch (UnbluApiException e) {
            if (e.getStatusCode() == 404) {
                // Webhook not registered
                return new WebhookStatus(
                    ngrokStatus.running(),
                    ngrokStatus.publicUrl(),
                    false,
                    null,
                    null,
                    null
                );
            }
            throw e;
        }
    }

    @Override
    public void teardownWebhook(boolean deleteWebhook) {
        log.info("Tearing down webhook configuration (deleteWebhook: {})...", deleteWebhook);

        // Stop ngrok
        ngrokManager.stopNgrok();
        log.info("Ngrok tunnel stopped");

        // Optionally delete webhook from Unblu
        if (deleteWebhook) {
            try {
                WebhookRegistration webhook = unbluService.getWebhookByName(webhookName);
                unbluService.deleteWebhook(webhook.getId());
                log.info("Webhook deleted from Unblu");
            } catch (UnbluApiException e) {
                if (e.getStatusCode() == 404) {
                    log.info("Webhook not found in Unblu, nothing to delete");
                } else {
                    log.error("Error deleting webhook from Unblu", e);
                    throw e;
                }
            }
        }

        log.info("Webhook teardown completed");
    }
}
