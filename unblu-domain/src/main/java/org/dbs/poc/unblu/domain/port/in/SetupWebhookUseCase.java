package org.dbs.poc.unblu.domain.port.in;

import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;

/**
 * Use case for setting up webhook configuration
 */
public interface SetupWebhookUseCase {

    /**
     * Setup complete webhook configuration:
     * - Start ngrok tunnel
     * - Register/update webhook in Unblu
     *
     * @return Result of the setup operation
     */
    WebhookSetupResult setupWebhook();

    /**
     * Get current webhook status
     *
     * @return Current status of webhook configuration
     */
    WebhookStatus getWebhookStatus();

    /**
     * Teardown webhook configuration:
     * - Stop ngrok tunnel
     * - Optionally delete webhook from Unblu
     *
     * @param deleteWebhook If true, delete the webhook registration from Unblu
     */
    void teardownWebhook(boolean deleteWebhook);
}
