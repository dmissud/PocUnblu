package org.dbs.poc.unblu.application.port.out;

import java.util.List;

/**
 * Port for managing webhook registrations in the Unblu platform.
 */
public interface WebhookRegistrationPort {

    WebhookRegistration findByName(String name);

    WebhookRegistration create(String name, String endpoint, List<String> events);

    WebhookRegistration update(String id, String endpoint, List<String> events);

    void delete(String id);

    record WebhookRegistration(String id, String name, String endpoint) {}
}
