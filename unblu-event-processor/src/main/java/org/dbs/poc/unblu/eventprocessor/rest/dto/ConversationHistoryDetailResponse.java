package org.dbs.poc.unblu.eventprocessor.rest.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ConversationHistoryDetailResponse(
        String conversationId,
        String topic,
        String createdAt,
        String endedAt,
        String status,
        List<ConversationParticipantResponse> participants,
        List<ConversationEventResponse> events) {
}
