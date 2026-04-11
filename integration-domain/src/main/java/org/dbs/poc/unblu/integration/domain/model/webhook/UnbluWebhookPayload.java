package org.dbs.poc.unblu.integration.domain.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UnbluWebhookPayload(
        @JsonProperty("$_type") String type,
        String eventType,
        Long timestamp,
        String accountId,
        String conversationId,
        ConversationData conversation,
        ConversationMessageData conversationMessage,
        String endReason
) {
    public String extractConversationId() {
        if (conversation != null && conversation.id() != null) return conversation.id();
        return conversationId;
    }

    public Instant extractTimestamp() {
        return timestamp != null ? Instant.ofEpochMilli(timestamp) : Instant.now();
    }
}
