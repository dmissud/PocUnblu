package org.dbs.poc.unblu.integration.domain.model.history;

import java.util.List;
import java.util.Objects;

public record ConversationHistoryPage(
        List<ConversationHistory> items,
        long totalItems,
        int page,
        int size,
        int totalPages) {

    public ConversationHistoryPage {
        Objects.requireNonNull(items, "items cannot be null");
        items = List.copyOf(items);
    }
}
