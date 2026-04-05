package org.dbs.poc.unblu.infrastructure.adapter.webhook;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.exception.UnbluApiException;
import org.dbs.poc.unblu.domain.port.out.WebhookRegistrationPort;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluWebhookService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptateur secondaire implémentant {@link WebhookRegistrationPort} en déléguant
 * à {@link UnbluWebhookService} et en mappant les exceptions infrastructure vers les
 * exceptions domaine {@link org.dbs.poc.unblu.domain.exception.UnbluApiException}.
 */
@Component
@RequiredArgsConstructor
public class UnbluWebhookRegistrationAdapter implements WebhookRegistrationPort {

    private final UnbluWebhookService unbluWebhookService;

    /**
     * {@inheritDoc}
     */
    @Override
    public WebhookRegistration findByName(String name) {
        try {
            com.unblu.webapi.model.v4.WebhookRegistration webhook = unbluWebhookService.getWebhookByName(name);
            return new WebhookRegistration(webhook.getId(), webhook.getName(), webhook.getEndpoint());
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public WebhookRegistration create(String name, String endpoint, List<String> events) {
        try {
            com.unblu.webapi.model.v4.WebhookRegistration webhook = unbluWebhookService.createWebhook(name, endpoint, events);
            return new WebhookRegistration(webhook.getId(), webhook.getName(), webhook.getEndpoint());
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public WebhookRegistration update(String id, String endpoint, List<String> events) {
        try {
            com.unblu.webapi.model.v4.WebhookRegistration webhook = unbluWebhookService.updateWebhook(id, endpoint, events);
            return new WebhookRegistration(webhook.getId(), webhook.getName(), webhook.getEndpoint());
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String id) {
        try {
            unbluWebhookService.deleteWebhook(id);
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }
}
