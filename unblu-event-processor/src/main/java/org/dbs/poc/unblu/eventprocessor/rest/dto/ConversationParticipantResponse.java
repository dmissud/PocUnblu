package org.dbs.poc.unblu.eventprocessor.rest.dto;

import lombok.Builder;

@Builder
public record ConversationParticipantResponse(
        String personId,
        String displayName,
        String participantType) {
}
