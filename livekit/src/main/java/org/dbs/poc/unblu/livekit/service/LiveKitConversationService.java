package org.dbs.poc.unblu.livekit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.ConversationCreationRequest;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitConversationService implements StartLiveKitConversationUseCase {

    private static final String NAMED_AREA_CH_HB_PREMIUM = "CH_HB_PREMIUM";
    private final UnbluPort unbluPort;

    @Override
    public UnbluConversationInfo startConversation(String clientId) {
        log.info("Démarrage d'une conversation LiveKit pour le client: {}", clientId);

        // On utilise la Named Area demandée
        // On passe le clientId dans visitorData pour l'identification
        var request = ConversationCreationRequest.builder()
                .namedAreaId(NAMED_AREA_CH_HB_PREMIUM)
                .topic("LiveKit Conversation - " + clientId)
                .visitorData(clientId)
                .build();

        return unbluPort.createConversation(request);
    }
}
