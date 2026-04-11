package org.dbs.poc.unblu.integration.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.ConversationHistoryApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.ConversationHistoryData;
import com.unblu.webapi.model.v4.ParticipantHistoryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.UnbluParticipantData;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Service dédié à la gestion des participants via l'API Unblu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnbluParticipantClient {

    private final ApiClient integrationUnbluApiClient;

    private ConversationHistoryApi historyApi() {
        return new ConversationHistoryApi(integrationUnbluApiClient);
    }

    public List<UnbluParticipantData> fetchConversationParticipants(String conversationId) throws ApiException {
        ConversationHistoryData data = historyApi().conversationHistoryRead(conversationId, null);
        if (data.getParticipants() == null) {
            return Collections.emptyList();
        }

        return data.getParticipants().stream()
                .filter(p -> p.getPerson() != null)
                .map(this::mapToUnbluParticipantData)
                .toList();
    }

    private UnbluParticipantData mapToUnbluParticipantData(ParticipantHistoryData p) {
        String displayName = p.getPerson().getDisplayName() != null
                ? p.getPerson().getDisplayName() : p.getPerson().getId();
        String source = p.getPerson().getPersonSource() != null
                ? p.getPerson().getPersonSource().name() : "VIRTUAL";

        return new UnbluParticipantData(p.getPerson().getId(), displayName, source);
    }
}
