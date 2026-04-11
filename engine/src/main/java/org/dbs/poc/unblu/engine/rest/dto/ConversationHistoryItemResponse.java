package org.dbs.poc.unblu.engine.rest.dto;

import lombok.Builder;

@Builder
public record ConversationHistoryItemResponse(
        String conversationId,
        String topic,
        String createdAt,
        String endedAt,
        String status) {
}
