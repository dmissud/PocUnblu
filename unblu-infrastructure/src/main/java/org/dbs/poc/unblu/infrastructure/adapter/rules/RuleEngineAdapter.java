package org.dbs.poc.unblu.infrastructure.adapter.rules;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.port.out.RuleEnginePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptateur secondaire implémentant {@link RuleEnginePort} en Java pur.
 * Évalue les règles de routage en fonction du contexte de conversation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngineAdapter implements RuleEnginePort {

    @Value("${mock.rule-engine.default-team-id:cAaYUeKyTZ25_OaA6jUeVA}")
    private String defaultTeamId;

    /**
     * Évalue les règles de routage pour le contexte de conversation donné.
     * Logique simulée : Refuse l'accès si le client est banni.
     *
     * @param context le contexte de la conversation
     * @return la décision de routage
     */
    @Override
    @CircuitBreaker(name = "ruleEngine")
    public ChatRoutingDecision evaluateRouting(ConversationContext context) {
        log.info("Évaluation du routage pour clientId: {}", context.initialClientId());

        if (context.customerProfile() != null && context.customerProfile().isBanned()) {
            return new ChatRoutingDecision(false, null, "Client blacklisté - Accès au chat refusé.");
        }

        return new ChatRoutingDecision(true, defaultTeamId, "Client éligible.");
    }
}
