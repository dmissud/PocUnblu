package org.dbs.poc.unblu.integration.domain.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UnbluWebhookPayload(
        @JsonProperty("$_type") String type,
        String eventType,
        Long timestamp,
        String accountId,
        String conversationId,
        ConversationData conversation,
        ConversationMessageData conversationMessage,
        String endReason
) {}
