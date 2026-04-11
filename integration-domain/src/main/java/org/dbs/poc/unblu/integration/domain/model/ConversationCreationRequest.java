package org.dbs.poc.unblu.integration.domain.model;

import lombok.Builder;
import java.util.List;

@Builder
public record ConversationCreationRequest(
        String topic,
        String visitorData,
        String namedAreaId,
        String conversationTemplate,
        List<ParticipantRequest> participants
) {
    @Builder
    public record ParticipantRequest(String personId, String participationType) {}
}
