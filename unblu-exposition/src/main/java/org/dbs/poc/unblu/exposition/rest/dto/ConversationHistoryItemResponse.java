package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * DTO représentant une ligne du tableau de l'historique des conversations (vue liste).
 * Ne contient pas les événements ni les participants pour limiter la charge réseau.
 */
@Value
@Builder
public class ConversationHistoryItemResponse {

    String conversationId;
    String topic;
    Instant createdAt;
    Instant endedAt;
    /** {@code ACTIVE} ou {@code ENDED} selon la présence de {@code endedAt}. */
    String status;
}
