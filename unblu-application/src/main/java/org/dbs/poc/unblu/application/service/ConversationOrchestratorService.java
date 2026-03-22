package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.springframework.stereotype.Service;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.DIRECT_START_CONVERSATION;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Implémentation du cas d'utilisation {@link StartConversationUseCase}.
 * Délègue l'exécution à la route Camel {@code direct:start-conversation} via un {@link ProducerTemplate}.
 */
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final ProducerTemplate producerTemplate;

    /**
     * {@inheritDoc}
     * Envoie la commande à la route Camel principale et retourne l'état d'orchestration résultant.
     */
    @Override
    public ConversationOrchestrationState startConversation(StartConversationCommand command) {
        log.info("Appel de l'orchestrateur Camel (Pragmatic) pour clientId: {}", command.clientId());
        return producerTemplate.requestBody(DIRECT_START_CONVERSATION, command, ConversationOrchestrationState.class);
    }
}
