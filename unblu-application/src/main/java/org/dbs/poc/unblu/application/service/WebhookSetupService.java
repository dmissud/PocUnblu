package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SetupWebhookUseCase;
import org.dbs.poc.unblu.application.port.out.TunnelPort;
import org.dbs.poc.unblu.application.port.out.WebhookRegistrationPort;
import org.dbs.poc.unblu.domain.exception.UnbluApiException;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
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

    private final TunnelPort tunnelPort;
    private final WebhookRegistrationPort webhookRegistrationPort;

    @Value("${unblu.webhook.name:unblu-poc-webhook}")
    private String webhookName;

    @Value("${unblu.webhook.endpoint-path:/api/webhooks/unblu}")
    private String webhookEndpointPath;

    @Override
    public WebhookSetupResult setupWebhook() {
        log.info("Starting webhook setup...");

        try {
            log.info("Step 1: Starting ngrok tunnel...");
            boolean started = tunnelPort.start();
            if (!started) {
                return WebhookSetupResult.failure("Échec du démarrage de ngrok. Vérifiez que ngrok est installé.");
            }

            String ngrokUrl = tunnelPort.getPublicUrl();
            if (ngrokUrl == null) {
                tunnelPort.stop();
                return WebhookSetupResult.failure("Impossible de récupérer l'URL publique de ngrok");
            }
            log.info("Ngrok tunnel active: {}", ngrokUrl);

            String webhookEndpoint = ngrokUrl + webhookEndpointPath;
            log.info("Webhook endpoint will be: {}", webhookEndpoint);
            log.info("Webhook will listen to events: {}", WEBHOOK_EVENTS);

            WebhookRegistrationPort.WebhookRegistration webhook;
            try {
                log.info("Checking if webhook '{}' already exists...", webhookName);
                WebhookRegistrationPort.WebhookRegistration existing = webhookRegistrationPort.findByName(webhookName);
                log.info("Webhook '{}' already exists with ID: {}, updating endpoint...", webhookName, existing.id());
                webhook = webhookRegistrationPort.update(existing.id(), webhookEndpoint, WEBHOOK_EVENTS);
                log.info("Webhook updated successfully");
            } catch (UnbluApiException e) {
                log.info("Caught UnbluApiException with status code: {}", e.getStatusCode());
                if (e.getStatusCode() == 404) {
                    log.info("Webhook '{}' doesn't exist (404), creating new webhook...", webhookName);
                    try {
                        webhook = webhookRegistrationPort.create(webhookName, webhookEndpoint, WEBHOOK_EVENTS);
                        log.info("Webhook created successfully with ID: {}", webhook.id());
                    } catch (Exception createEx) {
                        log.error("Failed to create webhook", createEx);
                        tunnelPort.stop();
                        throw createEx;
                    }
                } else {
                    log.error("Unexpected error code from Unblu: {}", e.getStatusCode());
                    tunnelPort.stop();
                    throw e;
                }
            } catch (Exception e) {
                log.error("Unexpected exception while checking webhook: {}", e.getClass().getName(), e);
                tunnelPort.stop();
                throw e;
            }

            log.info("Webhook setup completed successfully!");
            return WebhookSetupResult.success(ngrokUrl, webhook.id(), webhook.name());

        } catch (Exception e) {
            log.error("Error during webhook setup", e);
            tunnelPort.stop();
            return WebhookSetupResult.failure("Erreur lors de la configuration du webhook: " + e.getMessage());
        }
    }

    @Override
    public WebhookStatus getWebhookStatus() {
        log.debug("Checking webhook status...");

        TunnelPort.TunnelStatus tunnelStatus = tunnelPort.getStatus();

        try {
            WebhookRegistrationPort.WebhookRegistration webhook = webhookRegistrationPort.findByName(webhookName);
            return new WebhookStatus(
                tunnelStatus.running(),
                tunnelStatus.publicUrl(),
                true,
                webhook.id(),
                webhook.name(),
                webhook.endpoint()
            );
        } catch (UnbluApiException e) {
            if (e.getStatusCode() == 404) {
                return new WebhookStatus(
                    tunnelStatus.running(),
                    tunnelStatus.publicUrl(),
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

        tunnelPort.stop();
        log.info("Ngrok tunnel stopped");

        if (deleteWebhook) {
            try {
                WebhookRegistrationPort.WebhookRegistration webhook = webhookRegistrationPort.findByName(webhookName);
                webhookRegistrationPort.delete(webhook.id());
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
