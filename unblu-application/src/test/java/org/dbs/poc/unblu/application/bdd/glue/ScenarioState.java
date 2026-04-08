package org.dbs.poc.unblu.application.bdd.glue;

import org.dbs.poc.unblu.domain.model.ConversationOrchestrationState;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.springframework.stereotype.Component;

/**
 * État partagé entre les step definitions d'un même scénario.
 * Instancié une fois par scénario grâce au scope cucumber-glue.
 * Réinitialisé automatiquement entre chaque scénario.
 */
@Component
@io.cucumber.spring.ScenarioScope
public class ScenarioState {

    // Résultats des use cases
    public ConversationOrchestrationState orchestrationState;
    public UnbluConversationInfo directConversationInfo;
    public ConversationHistory enrichedHistory;

    // Exception capturée si le use case lève une erreur attendue
    public Exception capturedException;
}
