package org.dbs.poc.unblu.application.model;

import org.dbs.poc.unblu.domain.model.ConversationContext;

import java.util.Objects;

/**
 * Camel orchestration pivot for the {@code start-conversation} workflow.
 *
 * <p>Wraps the pure domain {@link ConversationContext} and carries the
 * infrastructure results produced during the route (Unblu conversation ID,
 * join URL). This separation keeps domain objects free of any
 * infrastructure concerns.
 */
public class ConversationOrchestrationState {

    private final ConversationContext context;
    private String unbluConversationId;
    private String unbluJoinUrl;

    public ConversationOrchestrationState(ConversationContext context) {
        this.context = Objects.requireNonNull(context, "context is required");
    }

    public void updateUnbluConversation(String conversationId, String joinUrl) {
        this.unbluConversationId = conversationId;
        this.unbluJoinUrl = joinUrl;
    }

    public ConversationContext context() { return context; }
    public String unbluConversationId() { return unbluConversationId; }
    public String unbluJoinUrl() { return unbluJoinUrl; }
}
