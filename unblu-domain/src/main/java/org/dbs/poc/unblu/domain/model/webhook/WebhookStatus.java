package org.dbs.poc.unblu.domain.model.webhook;

/**
 * Current status of webhook configuration
 *
 * @param ngrokRunning True if ngrok tunnel is running
 * @param ngrokUrl Public URL of the ngrok tunnel (if running)
 * @param webhookRegistered True if webhook is registered in Unblu
 * @param webhookId ID of the webhook (if registered)
 * @param webhookName Name of the webhook (if registered)
 * @param webhookEndpoint Endpoint URL configured in Unblu (if registered)
 */
public record WebhookStatus(
    boolean ngrokRunning,
    String ngrokUrl,
    boolean webhookRegistered,
    String webhookId,
    String webhookName,
    String webhookEndpoint
) {
    /**
     * Crée un statut indiquant qu'aucun webhook n'est configuré.
     *
     * @return statut vide (ngrok arrêté, webhook non enregistré)
     */
    public static WebhookStatus notConfigured() {
        return new WebhookStatus(false, null, false, null, null, null);
    }

    /**
     * Indique si la configuration est complète (tunnel actif ET webhook enregistré).
     *
     * @return {@code true} si ngrok tourne et le webhook est enregistré dans Unblu
     */
    public boolean isFullyConfigured() {
        return ngrokRunning && webhookRegistered;
    }
}
