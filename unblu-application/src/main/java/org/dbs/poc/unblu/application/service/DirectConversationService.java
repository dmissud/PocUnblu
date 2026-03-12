package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.ConversationSummaryPort;
import org.dbs.poc.unblu.domain.port.secondary.ErpPort;
import org.dbs.poc.unblu.domain.port.secondary.RuleEnginePort;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectConversationService implements StartDirectConversationUseCase {

    private final ErpPort erpPort;
    private final RuleEnginePort ruleEnginePort;
    private final UnbluPort unbluPort;
    private final ConversationSummaryPort conversationSummaryPort;

    @Override
    public UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command) {
        log.info("Démarrage d'une conversation directe - VIRTUAL: {}, Agent: {}",
                command.getVirtualParticipantSourceId(), command.getAgentParticipantSourceId());

        // 1. Résoudre le participant VIRTUAL dans Unblu
        List<PersonInfo> virtualPersons = unbluPort.searchPersons(command.getVirtualParticipantSourceId(), PersonSource.VIRTUAL);
        if (virtualPersons.isEmpty()) {
            throw new IllegalArgumentException("Participant VIRTUAL introuvable dans Unblu pour sourceId: " + command.getVirtualParticipantSourceId());
        }
        PersonInfo virtualPerson = virtualPersons.getFirst();

        // 2. ERP — enrichissement du profil client
        CustomerProfile profile = erpPort.getCustomerProfile(command.getVirtualParticipantSourceId());

        // 3. Rule Engine — décision de routage
        ConversationContext context = ConversationContext.builder()
                .initialClientId(command.getVirtualParticipantSourceId())
                .customerProfile(profile)
                .build();
        ChatRoutingDecision decision = ruleEnginePort.evaluateRouting(context);

        if (!decision.isAuthorized()) {
            log.warn("Accès refusé par le moteur de règles pour sourceId: {}. Motif: {}",
                    command.getVirtualParticipantSourceId(), decision.getRoutingReason());
            throw new ChatAccessDeniedException("Accès refusé", decision.getRoutingReason());
        }

        // 4. Résoudre l'agent USER_DB dans Unblu
        List<PersonInfo> agentPersons = unbluPort.searchPersons(command.getAgentParticipantSourceId(), PersonSource.USER_DB);
        if (agentPersons.isEmpty()) {
            throw new IllegalArgumentException("Agent USER_DB introuvable dans Unblu pour sourceId: " + command.getAgentParticipantSourceId());
        }
        PersonInfo agentPerson = agentPersons.getFirst();

        // 5. Créer la conversation directe dans Unblu
        UnbluConversationInfo info = unbluPort.createDirectConversation(virtualPerson, agentPerson, command.getSubject());

        // 6. Génération et ajout du résumé comme message au nom du participant VIRTUAL
        String summary = conversationSummaryPort.generateSummary(info.unbluConversationId());
        unbluPort.addSummaryToConversation(info.unbluConversationId(), summary);

        return info;
    }
}
