package org.dbs.poc.unblu.application.route.webhook;

/**
 * Typed DTO for the conversationMessage field in a ConversationNewMessageEvent webhook payload.
 */
public record ConversationMessageData(
        String conversationId,
        String text,
        SenderPersonData senderPerson
) {}
