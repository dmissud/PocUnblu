package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Contexte de la conversation, enrichi au fur et à mesure de l'orchestration.
 * Cette classe n'est pas immuable car elle est enrichie par les étapes Camel (pattern Objet Pivot).
 */
public class ConversationContext {
    private final String initialClientId;
    private final String originApplication;
    
    // Données enrichies durant l'orchestration
    private CustomerProfile customerProfile;
    private ChatRoutingDecision routingDecision;
    
    // Résultat final Unblu
    private String unbluConversationId;
    private String unbluJoinUrl;

    public ConversationContext(String initialClientId, String originApplication) {
        this.initialClientId = Objects.requireNonNull(initialClientId, "initialClientId is required");
        this.originApplication = Objects.requireNonNull(originApplication, "originApplication is required");
    }

    // Business Logic
    public boolean isChatAuthorized() {
        return routingDecision != null && routingDecision.isAuthorized();
    }

    public void updateUnbluConversation(String conversationId, String joinUrl) {
        this.unbluConversationId = conversationId;
        this.unbluJoinUrl = joinUrl;
    }

    // Getters
    public String getInitialClientId() { return initialClientId; }
    public String getOriginApplication() { return originApplication; }
    public CustomerProfile getCustomerProfile() { return customerProfile; }
    public ChatRoutingDecision getRoutingDecision() { return routingDecision; }
    public String getUnbluConversationId() { return unbluConversationId; }
    public String getUnbluJoinUrl() { return unbluJoinUrl; }

    // Setters (Package-private or public for Camel)
    public void setCustomerProfile(CustomerProfile customerProfile) { this.customerProfile = customerProfile; }
    public void setRoutingDecision(ChatRoutingDecision routingDecision) { this.routingDecision = routingDecision; }
    public void setUnbluConversationId(String id) { this.unbluConversationId = id; }
    public void setUnbluJoinUrl(String url) { this.unbluJoinUrl = url; }
}
