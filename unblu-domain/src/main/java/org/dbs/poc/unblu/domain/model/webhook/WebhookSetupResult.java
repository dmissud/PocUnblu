package org.dbs.poc.unblu.domain.model.webhook;

/**
 * Result of webhook setup operation
 *
 * @param success True if setup was successful
 * @param ngrokUrl Public URL of the ngrok tunnel
 * @param webhookId ID of the webhook registration in Unblu
 * @param webhookName Name of the webhook registration
 * @param status Status message
 * @param message Detailed message
 */
public record WebhookSetupResult(
    boolean success,
    String ngrokUrl,
    String webhookId,
    String webhookName,
    String status,
    String message
) {
    public static WebhookSetupResult success(String ngrokUrl, String webhookId, String webhookName) {
        return new WebhookSetupResult(
            true,
            ngrokUrl,
            webhookId,
            webhookName,
            "ACTIVE",
            "Webhook configuré avec succès"
        );
    }

    public static WebhookSetupResult failure(String message) {
        return new WebhookSetupResult(
            false,
            null,
            null,
            null,
            "ERROR",
            message
        );
    }
}
