package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.port.out.RuleEnginePort;
import org.springframework.stereotype.Component;

/**
 * Stub de {@link RuleEnginePort}.
 * Reproduit la logique de l'adapter de production :
 * refuse si le profil client est BANNED, autorise sinon avec l'équipe configurée.
 */
@Component
public class StubRuleEnginePort implements RuleEnginePort {

    private static final String DEFAULT_TEAM_ID = "team-default-stub";

    public void reset() {
        // logique stateless — rien à réinitialiser
    }

    @Override
    public ChatRoutingDecision evaluateRouting(ConversationContext context) {
        if (context.customerProfile() != null && context.customerProfile().isBanned()) {
            return new ChatRoutingDecision(false, null, "Client blacklisté — accès refusé.");
        }
        return new ChatRoutingDecision(true, DEFAULT_TEAM_ID, "Client éligible.");
    }
}
