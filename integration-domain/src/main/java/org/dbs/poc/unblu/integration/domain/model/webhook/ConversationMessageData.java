package org.dbs.poc.unblu.integration.domain.model.webhook;
public record ConversationMessageData(String conversationId, String text, SenderPersonData senderPerson) {}
