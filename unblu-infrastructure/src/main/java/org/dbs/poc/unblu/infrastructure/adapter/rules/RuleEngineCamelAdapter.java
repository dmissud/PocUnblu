package org.dbs.poc.unblu.infrastructure.adapter.rules;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.port.out.RuleEnginePort;
import org.springframework.stereotype.Component;

/**
 * Adaptateur secondaire implémentant {@link RuleEnginePort} en déléguant l'évaluation
 * des règles de routage à la route Camel {@code direct:rule-engine-adapter} via un {@link ProducerTemplate}.
 */
@Component
@RequiredArgsConstructor
public class RuleEngineCamelAdapter implements RuleEnginePort {

    private final ProducerTemplate producerTemplate;

    /**
     * Évalue les règles de routage pour le contexte de conversation donné
     * via la route Camel du moteur de règles.
     *
     * @param context le contexte de la conversation à évaluer
     * @return la décision de routage (autorisation, équipe cible, raison)
     */
    @Override
    public ChatRoutingDecision evaluateRouting(ConversationContext context) {
        // Call the Rule Engine Camel route (which has retry/resilience config)
        return producerTemplate.requestBody("direct:rule-engine-adapter", context, ChatRoutingDecision.class);
    }
}
