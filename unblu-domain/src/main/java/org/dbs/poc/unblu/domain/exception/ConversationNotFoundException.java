package org.dbs.poc.unblu.domain.exception;

public class ConversationNotFoundException extends DomainException {
    public ConversationNotFoundException(String conversationId) {
        super("Conversation not found: " + conversationId);
    }
}
