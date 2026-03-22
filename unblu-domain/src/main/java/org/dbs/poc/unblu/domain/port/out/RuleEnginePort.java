package org.dbs.poc.unblu.domain.port.out;

import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;

/**
 * Port secondaire vers le moteur de règles métier.
 * Évalue si un client est autorisé à accéder au chat et détermine l'équipe cible.
 */
public interface RuleEnginePort {
    /**
     * Evaluates the routing decision based on the conversation context.
     * @param context The current conversation context including customer profile
     * @return The routing decision
     */
    ChatRoutingDecision evaluateRouting(ConversationContext context);
}
