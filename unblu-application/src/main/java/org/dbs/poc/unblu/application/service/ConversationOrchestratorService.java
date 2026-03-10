package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.ErpPort;
import org.dbs.poc.unblu.domain.port.secondary.RuleEnginePort;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final ErpPort erpPort;
    private final RuleEnginePort ruleEnginePort;
    private final UnbluPort unbluPort;

    @Override
    public ConversationContext startConversation(StartConversationCommand command) {
        log.info("Démarrage de l'orchestration de conversation pour clientId: {}", command.getClientId());

        // 1. Initialiser le contexte
        ConversationContext context = ConversationContext.builder()
                .initialClientId(command.getClientId())
                .originApplication(command.getOrigin())
                .build();

        // 2. Appel ERP
        CustomerProfile profile = erpPort.getCustomerProfile(command.getClientId());
        context.setCustomerProfile(profile);

        // 3. Appel Moteur de Règles
        ChatRoutingDecision decision = ruleEnginePort.evaluateRouting(context);
        context.setRoutingDecision(decision);

        // 4. Décision Métier d'Orchestration (Autorisé ou Rejeté)
        if (!decision.isAuthorized()) {
            log.warn("Accès refusé par le moteur de règles pour clientId: {}. Motif: {}", command.getClientId(), decision.getRoutingReason());
            throw new ChatAccessDeniedException("Accès refusé", decision.getRoutingReason());
        }

        // 5. Appel à l'API Unblu
        UnbluConversationInfo unbluInfo = unbluPort.createConversation(context);
        context.setUnbluConversationId(unbluInfo.unbluConversationId());
        context.setUnbluJoinUrl(unbluInfo.unbluJoinUrl());
        
        return context;
    }
}
