package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectConversationService implements StartDirectConversationUseCase {

    private final ProducerTemplate producerTemplate;

    @Override
    public UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command) {
        log.info("Appel de l'orchestrateur Camel (Direct) pour VIRTUAL: {}", command.getVirtualParticipantSourceId());
        return producerTemplate.requestBody("direct:start-direct-conversation", command, UnbluConversationInfo.class);
    }
}
