package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.springframework.stereotype.Service;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.DIRECT_START_DIRECT_CONVERSATION;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Implémentation du cas d'utilisation {@link StartDirectConversationUseCase}.
 * Délègue l'exécution à la route Camel {@code direct:start-direct-conversation} via un {@link ProducerTemplate}.
 */
public class DirectConversationService implements StartDirectConversationUseCase {

    private final ProducerTemplate producerTemplate;

    /**
     * {@inheritDoc}
     * Envoie la commande à la route Camel de conversation directe et retourne les informations de la conversation créée.
     */
    @Override
    public UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command) {
        log.info("Appel de l'orchestrateur Camel (Direct) pour VIRTUAL: {}", command.virtualParticipantSourceId());
        return producerTemplate.requestBody(DIRECT_START_DIRECT_CONVERSATION, command, UnbluConversationInfo.class);
    }
}
