package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.ConversationOrchestrationState;
import org.dbs.poc.unblu.domain.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.domain.port.in.command.StartConversationCommand;
import org.dbs.poc.unblu.domain.port.out.ErpPort;
import org.dbs.poc.unblu.domain.port.out.RuleEnginePort;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Implémentation du cas d'utilisation {@link StartConversationUseCase}.
 * Orchestre le workflow synchrone en Java pur : ERP -> RuleEngine -> Unblu.
 */
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final ErpPort erpPort;
    private final RuleEnginePort ruleEnginePort;

    @Qualifier("unbluApiAdapter")
    private final UnbluPort unbluPort;

    private final ConversationWorkflowService workflowService;

    @Override
    public ConversationOrchestrationState startConversation(StartConversationCommand command) {
        log.info("Démarrage de l'orchestration synchrone pour clientId: {}", command.clientId());

        // 1. Initialisation du contexte
        ConversationContext context = new ConversationContext(command.clientId(), command.origin());

        // 2. Enrichissement ERP
        var profile = erpPort.getCustomerProfile(command.clientId());
        context.setCustomerProfile(profile);

        // 3. Décision de routage
        var decision = ruleEnginePort.evaluateRouting(context);
        context.setRoutingDecision(decision);

        // 4. Création de l'état d'orchestration
        ConversationOrchestrationState state = new ConversationOrchestrationState(context);

        // 5. Création conversation Unblu (l'adapter gère sa propre résilience)
        var unbluInfo = unbluPort.createConversation(context);
        state.updateUnbluConversation(unbluInfo.unbluConversationId(), unbluInfo.unbluJoinUrl());

        // 6. Post-traitement : ajout du résumé (si succès Unblu)
        if (state.isSuccess()) {
            workflowService.addSummary(state.unbluConversationId());
        }

        return state;
    }
}
