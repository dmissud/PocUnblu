package org.dbs.poc.unblu.engine.rest.dto;

import lombok.Builder;

@Builder
public record ConversationParticipantResponse(
        String personId,
        String displayName,
        String participantType) {
}
