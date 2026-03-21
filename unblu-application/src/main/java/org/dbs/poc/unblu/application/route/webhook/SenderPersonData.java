package org.dbs.poc.unblu.application.route.webhook;

/**
 * Typed DTO for the senderPerson field in a ConversationNewMessageEvent webhook payload.
 */
public record SenderPersonData(
        String id,
        String displayName
) {}
