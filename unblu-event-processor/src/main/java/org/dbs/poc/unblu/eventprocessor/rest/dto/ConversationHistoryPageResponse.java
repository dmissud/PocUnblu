package org.dbs.poc.unblu.eventprocessor.rest.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ConversationHistoryPageResponse(
        List<ConversationHistoryItemResponse> items,
        long totalItems,
        int page,
        int size,
        int totalPages) {
}
