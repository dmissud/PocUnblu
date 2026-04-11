package org.dbs.poc.unblu.eventprocessor.rest.dto;

import lombok.Builder;

@Builder
public record ConversationHistoryItemResponse(
        String conversationId,
        String topic,
        String createdAt,
        String endedAt,
        String status) {
}
