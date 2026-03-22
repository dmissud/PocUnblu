package org.dbs.poc.unblu.application.route.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Typed envelope for all Unblu webhook event payloads.
 *
 * <p>Unblu sends events in two formats:
 * <ul>
 *   <li>{@code $_type} — Java class name (e.g. {@code ConversationCreatedEvent})</li>
 *   <li>{@code eventType} — dot-notation name (e.g. {@code conversation.created})</li>
 * </ul>
 * Both are mapped here; downstream routing uses whichever is present.
 */
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
