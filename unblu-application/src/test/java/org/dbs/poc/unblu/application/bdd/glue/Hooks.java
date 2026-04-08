package org.dbs.poc.unblu.application.bdd.glue;

import io.cucumber.java.Before;
import org.dbs.poc.unblu.application.bdd.stub.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Hooks Cucumber : réinitialise tous les stubs avant chaque scénario
 * pour garantir l'isolation entre scénarios.
 */
public class Hooks {

    @Autowired private StubErpPort stubErpPort;
    @Autowired private StubRuleEnginePort stubRuleEnginePort;
    @Autowired private StubUnbluPort stubUnbluPort;
    @Autowired private StubConversationSummaryPort stubConversationSummaryPort;
    @Autowired private InMemoryConversationHistoryRepository historyRepository;
    @Autowired private ScenarioState state;

    @Before
    public void resetAllStubs() {
        stubErpPort.reset();
        stubRuleEnginePort.reset();
        stubUnbluPort.reset();
        stubConversationSummaryPort.reset();
        historyRepository.reset();
        state.orchestrationState = null;
        state.directConversationInfo = null;
        state.enrichedHistory = null;
        state.capturedException = null;
    }
}
