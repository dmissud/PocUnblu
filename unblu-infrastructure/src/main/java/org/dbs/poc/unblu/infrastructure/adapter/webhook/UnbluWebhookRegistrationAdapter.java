package org.dbs.poc.unblu.infrastructure.adapter.webhook;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.out.WebhookRegistrationPort;
import org.dbs.poc.unblu.domain.exception.UnbluApiException;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter implementing WebhookRegistrationPort using UnbluService.
 */
@Component
@RequiredArgsConstructor
public class UnbluWebhookRegistrationAdapter implements WebhookRegistrationPort {

    private final UnbluService unbluService;

    @Override
    public WebhookRegistration findByName(String name) {
        try {
            com.unblu.webapi.model.v4.WebhookRegistration webhook = unbluService.getWebhookByName(name);
            return new WebhookRegistration(webhook.getId(), webhook.getName(), webhook.getEndpoint());
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }

    @Override
    public WebhookRegistration create(String name, String endpoint, List<String> events) {
        try {
            com.unblu.webapi.model.v4.WebhookRegistration webhook = unbluService.createWebhook(name, endpoint, events);
            return new WebhookRegistration(webhook.getId(), webhook.getName(), webhook.getEndpoint());
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }

    @Override
    public WebhookRegistration update(String id, String endpoint, List<String> events) {
        try {
            com.unblu.webapi.model.v4.WebhookRegistration webhook = unbluService.updateWebhook(id, endpoint, events);
            return new WebhookRegistration(webhook.getId(), webhook.getName(), webhook.getEndpoint());
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }

    @Override
    public void delete(String id) {
        try {
            unbluService.deleteWebhook(id);
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }
}
