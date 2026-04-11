package org.dbs.poc.unblu.integration.domain.model;

import java.util.List;
import java.util.Objects;

public record ConversationSyncResult(
        int totalScanned,
        int newlyPersisted,
        int alreadyExisting,
        int errors,
        List<String> errorConversationIds) {

    public ConversationSyncResult {
        Objects.requireNonNull(errorConversationIds, "errorConversationIds cannot be null");
        errorConversationIds = List.copyOf(errorConversationIds);
    }
}
