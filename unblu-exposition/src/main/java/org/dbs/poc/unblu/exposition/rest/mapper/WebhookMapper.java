package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
import org.dbs.poc.unblu.domain.port.in.SetupWebhookUseCase;
import org.springframework.stereotype.Component;

/**
 * Mapper for webhook-related operations.
 * Handles webhook setup, status retrieval, and teardown.
 */
@Component
public class WebhookMapper {

    private static final String HEADER_DELETE_WEBHOOK = "deleteWebhook";
    private static final String DEFAULT_DELETE_WEBHOOK = "false";

    private final SetupWebhookUseCase setupWebhookUseCase;

    public WebhookMapper(SetupWebhookUseCase setupWebhookUseCase) {
        this.setupWebhookUseCase = setupWebhookUseCase;
    }

    /**
     * Executes webhook setup and places result in Exchange body.
     */
    public void setupWebhook(Exchange exchange) {
        WebhookSetupResult result = setupWebhookUseCase.setupWebhook();
        exchange.getIn().setBody(result);
    }

    /**
     * Retrieves webhook status and places it in Exchange body.
     */
    public void getWebhookStatus(Exchange exchange) {
        WebhookStatus status = setupWebhookUseCase.getWebhookStatus();
        exchange.getIn().setBody(status);
    }

    /**
     * Executes webhook teardown based on header parameter.
     */
    public void teardownWebhook(Exchange exchange) {
        boolean deleteWebhook = extractDeleteWebhookParameter(exchange);
        setupWebhookUseCase.teardownWebhook(deleteWebhook);
        exchange.getIn().setBody(null);
    }

    private boolean extractDeleteWebhookParameter(Exchange exchange) {
        String deleteWebhookParam = exchange.getIn().getHeader(
                HEADER_DELETE_WEBHOOK,
                DEFAULT_DELETE_WEBHOOK,
                String.class
        );
        return Boolean.parseBoolean(deleteWebhookParam);
    }
}
