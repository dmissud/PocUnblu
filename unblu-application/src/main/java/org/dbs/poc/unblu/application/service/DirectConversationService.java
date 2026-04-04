package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.port.in.command.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.port.out.ErpPort;
import org.dbs.poc.unblu.domain.port.out.RuleEnginePort;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Implémentation du cas d'utilisation {@link StartDirectConversationUseCase}.
 * Orchestre le workflow synchrone en Java pur : ERP -> RuleEngine -> Unblu.
 */
public class DirectConversationService implements StartDirectConversationUseCase {

    private final ErpPort erpPort;
    private final RuleEnginePort ruleEnginePort;

    @Qualifier("unbluApiAdapter")
    private final UnbluPort unbluPort;

    private final ConversationWorkflowService workflowService;

    @Override
    public UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command) {
        log.info("Démarrage d'une conversation directe synchrone - VIRTUAL: {}, Agent: {}",
                command.virtualParticipantSourceId(), command.agentParticipantSourceId());

        // 1. Préparation des participants (PersonInfo légers pour Unblu)
        PersonInfo virtualPerson = new PersonInfo(
                command.virtualParticipantId(),
                command.virtualParticipantSourceId(),
                command.virtualParticipantSourceId(),
                null, null, null, "VISITOR");

        PersonInfo agentPerson = new PersonInfo(
                command.agentParticipantId(),
                command.agentParticipantSourceId(),
                command.agentParticipantSourceId(),
                null, null, null, "AGENT");

        // 2. Validation métier (ERP + Rules)
        ConversationContext context = new ConversationContext(command.virtualParticipantSourceId(), "DIRECT_CHANNELS");

        var profile = erpPort.getCustomerProfile(command.virtualParticipantSourceId());
        context.setCustomerProfile(profile);

        var decision = ruleEnginePort.evaluateRouting(context);
        context.setRoutingDecision(decision);

        workflowService.validateAuthorization(decision);

        // 3. Création de la conversation
        UnbluConversationInfo info = unbluPort.createDirectConversation(virtualPerson, agentPerson, command.subject());

        // 4. Post-traitement : ajout du résumé
        if (!"OFFLINE-PENDING".equals(info.unbluConversationId())) {
            workflowService.addSummary(info.unbluConversationId());
        }

        return new UnbluConversationInfo(info.unbluConversationId(), info.unbluConversationId());
    }
}
