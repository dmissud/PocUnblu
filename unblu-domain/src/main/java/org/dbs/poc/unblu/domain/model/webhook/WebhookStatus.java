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
    public static WebhookStatus notConfigured() {
        return new WebhookStatus(false, null, false, null, null, null);
    }

    public boolean isFullyConfigured() {
        return ngrokRunning && webhookRegistered;
    }
}
