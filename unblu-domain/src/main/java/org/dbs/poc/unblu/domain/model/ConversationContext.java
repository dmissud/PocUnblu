package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Domain object representing the context of a conversation being initiated.
 * Holds client identity, origin, ERP profile and routing decision.
 *
 * <p>This is a domain concept — it does NOT carry infrastructure results
 * (e.g. the Unblu conversation ID). Those belong in the orchestration layer.
 */
public class ConversationContext {

    private final String initialClientId;
    private final String originApplication;

    private CustomerProfile customerProfile;
    private ChatRoutingDecision routingDecision;

    public ConversationContext(String initialClientId, String originApplication) {
        this.initialClientId = Objects.requireNonNull(initialClientId, "initialClientId is required");
        this.originApplication = Objects.requireNonNull(originApplication, "originApplication is required");
    }

    public boolean isChatAuthorized() {
        return routingDecision != null && routingDecision.isAuthorized();
    }

    public String initialClientId() { return initialClientId; }
    public String originApplication() { return originApplication; }
    public CustomerProfile customerProfile() { return customerProfile; }
    public ChatRoutingDecision routingDecision() { return routingDecision; }

    public void setCustomerProfile(CustomerProfile customerProfile) { this.customerProfile = customerProfile; }
    public void setRoutingDecision(ChatRoutingDecision routingDecision) { this.routingDecision = routingDecision; }
}
