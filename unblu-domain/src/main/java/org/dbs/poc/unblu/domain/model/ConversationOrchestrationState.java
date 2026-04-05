package org.dbs.poc.unblu.domain.model;

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

    /**
     * Construit un état d'orchestration à partir d'un contexte de conversation domaine.
     *
     * @param context contexte domaine de la conversation (non null)
     */
    public ConversationOrchestrationState(ConversationContext context) {
        this.context = Objects.requireNonNull(context, "context is required");
    }

    /**
     * Met à jour les informations de la conversation Unblu créée.
     * Appelé par l'adaptateur Unblu après la création effective.
     *
     * @param conversationId identifiant de la conversation dans Unblu
     * @param joinUrl        URL permettant de rejoindre la conversation
     */
    public void updateUnbluConversation(String conversationId, String joinUrl) {
        this.unbluConversationId = conversationId;
        this.unbluJoinUrl = joinUrl;
    }

    /**
     * @return contexte domaine de la conversation
     */
    public ConversationContext context() { return context;
    }

    /** @return identifiant de la conversation Unblu créée, ou {@code null} si non encore créée */
    public String unbluConversationId() { return unbluConversationId;
    }

    /** @return URL de rejoindre la conversation Unblu, ou {@code null} si non encore créée */
    public String unbluJoinUrl() { return unbluJoinUrl; }

    /**
     * @return {@code true} si la conversation Unblu a été créée avec succès.
     */
    public boolean isSuccess() {
        return unbluConversationId != null && !"OFFLINE-PENDING".equals(unbluConversationId);
    }
}
