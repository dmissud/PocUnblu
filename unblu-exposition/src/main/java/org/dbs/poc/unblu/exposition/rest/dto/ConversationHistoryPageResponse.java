package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * DTO représentant une page paginée de l'historique des conversations.
 */
@Value
@Builder
public class ConversationHistoryPageResponse {

    List<ConversationHistoryItemResponse> items;
    long totalItems;
    int page;
    int size;
    int totalPages;
}
