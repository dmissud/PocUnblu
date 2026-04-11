package org.dbs.poc.unblu.integration.domain.model;

import java.time.Instant;
import java.util.Objects;

public record UnbluConversationSummary(
        String id,
        String topic,
        String state,
        Instant createdAt,
        Instant endedAt) {

    public UnbluConversationSummary {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public boolean isEnded() { return endedAt != null; }
}
