package org.dbs.poc.unblu.application.route.webhook;

/**
 * Typed DTO for the conversation field in ConversationCreatedEvent / ConversationEndedEvent payloads.
 */
public record ConversationData(
        String id,
        String topic
) {}
