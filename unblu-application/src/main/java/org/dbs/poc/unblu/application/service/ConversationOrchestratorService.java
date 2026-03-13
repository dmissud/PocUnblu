package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final ProducerTemplate producerTemplate;

    @Override
    public ConversationContext startConversation(StartConversationCommand command) {
        log.info("Appel de l'orchestrateur Camel (Pragmatic) pour clientId: {}", command.getClientId());
        return producerTemplate.requestBody("direct:start-conversation", command, ConversationContext.class);
    }
}
