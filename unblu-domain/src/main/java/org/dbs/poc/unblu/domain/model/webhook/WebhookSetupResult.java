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
    /**
     * Crée un résultat de succès après la configuration complète du webhook.
     *
     * @param ngrokUrl    URL publique du tunnel ngrok actif
     * @param webhookId   identifiant du webhook enregistré dans Unblu
     * @param webhookName nom du webhook enregistré
     * @return résultat représentant un succès
     */
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

    /**
     * Crée un résultat d'échec avec un message d'erreur descriptif.
     *
     * @param message description de l'erreur survenue
     * @return résultat représentant un échec
     */
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
