package org.dbs.poc.unblu.livekit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.ConversationCreationRequest;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitConversationService implements StartLiveKitConversationUseCase {

    private static final String NAMED_AREA_CH_HB_PREMIUM = "CH_HB_PREMIUM";
    private final UnbluPort unbluPort;

    @Override
    public UnbluConversationInfo startConversation(String personId) {
        log.info("Démarrage d'une conversation LiveKit pour la personne Unblu: {}", personId);

        var request = ConversationCreationRequest.builder()
                .namedAreaId(NAMED_AREA_CH_HB_PREMIUM)
                .topic("LiveKit Conversation")
                .participants(List.of(
                        ConversationCreationRequest.ParticipantRequest.builder()
                                .personId(personId)
                                .participationType("CONTEXT_PERSON")
                                .build()
                ))
                .build();

        return unbluPort.createConversation(request);
    }
}
