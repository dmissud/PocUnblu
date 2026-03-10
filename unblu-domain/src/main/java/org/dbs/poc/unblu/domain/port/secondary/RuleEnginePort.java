package org.dbs.poc.unblu.domain.port.secondary;

import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;

public interface RuleEnginePort {
    /**
     * Evaluates the routing decision based on the conversation context.
     * @param context The current conversation context including customer profile
     * @return The routing decision
     */
    ChatRoutingDecision evaluateRouting(ConversationContext context);
}
