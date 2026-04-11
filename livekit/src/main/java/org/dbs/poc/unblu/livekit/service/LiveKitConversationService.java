package org.dbs.poc.unblu.livekit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ConversationCreationRequest;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitConversationService implements StartLiveKitConversationUseCase {

    private static final String NAMED_AREA_C_HB_PREMIUM_ID = "ZvcLavqFTKC65YtiRKtJxg";
    private final IntegrationUnbluPort unbluPort;

    @Override
    public UnbluConversationInfo startConversation(String personId) {
        log.info("Démarrage d'une conversation LiveKit pour la personne Unblu: {}", personId);

        var request = ConversationCreationRequest.builder()
                .namedAreaId(NAMED_AREA_C_HB_PREMIUM_ID)
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
