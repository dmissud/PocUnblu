package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;
import org.springframework.stereotype.Service;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.DIRECT_START_CONVERSATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final ProducerTemplate producerTemplate;

    @Override
    public ConversationOrchestrationState startConversation(StartConversationCommand command) {
        log.info("Appel de l'orchestrateur Camel (Pragmatic) pour clientId: {}", command.clientId());
        return producerTemplate.requestBody(DIRECT_START_CONVERSATION, command, ConversationOrchestrationState.class);
    }
}
