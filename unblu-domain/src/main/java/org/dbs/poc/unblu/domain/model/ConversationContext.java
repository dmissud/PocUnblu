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

    /**
     * Construit un contexte de conversation avec les informations initiales du client.
     *
     * @param initialClientId   identifiant du client à l'origine de la demande
     * @param originApplication application source de la demande (ex. portail web, mobile)
     */
    public ConversationContext(String initialClientId, String originApplication) {
        this.initialClientId = Objects.requireNonNull(initialClientId, "initialClientId is required");
        this.originApplication = Objects.requireNonNull(originApplication, "originApplication is required");
    }

    /**
     * Indique si le chat est autorisé selon la décision de routage en cours.
     *
     * @return {@code true} si la décision de routage existe et autorise l'accès
     */
    public boolean isChatAuthorized() {
        return routingDecision != null && routingDecision.isAuthorized();
    }

    /**
     * @return identifiant du client ayant initié la demande
     */
    public String initialClientId() { return initialClientId;
    }

    /** @return application source de la demande */
    public String originApplication() { return originApplication;
    }

    /** @return profil client récupéré depuis l'ERP, ou {@code null} si non encore enrichi */
    public CustomerProfile customerProfile() { return customerProfile;
    }

    /** @return décision de routage issue du moteur de règles, ou {@code null} si non encore évaluée */
    public ChatRoutingDecision routingDecision() { return routingDecision;
    }

    /**
     * Enrichit le contexte avec le profil client récupéré depuis l'ERP.
     *
     * @param customerProfile profil client à associer
     */
    public void setCustomerProfile(CustomerProfile customerProfile) { this.customerProfile = customerProfile;
    }

    /**
     * Enrichit le contexte avec la décision de routage issue du moteur de règles.
     *
     * @param routingDecision décision de routage à associer
     */
    public void setRoutingDecision(ChatRoutingDecision routingDecision) { this.routingDecision = routingDecision; }
}
