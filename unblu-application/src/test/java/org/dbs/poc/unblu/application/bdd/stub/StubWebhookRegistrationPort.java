package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.port.out.WebhookRegistrationPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub de {@link WebhookRegistrationPort} — opérations sans effet en test.
 */
@Component
public class StubWebhookRegistrationPort implements WebhookRegistrationPort {

    @Override
    public WebhookRegistration findByName(String name) {
        return new WebhookRegistration("stub-id", name, "http://stub-endpoint");
    }

    @Override
    public WebhookRegistration create(String name, String endpoint, List<String> events) {
        return new WebhookRegistration("stub-created-id", name, endpoint);
    }

    @Override
    public WebhookRegistration update(String id, String endpoint, List<String> events) {
        return new WebhookRegistration(id, "stub", endpoint);
    }

    @Override
    public void delete(String id) {
    }
}
