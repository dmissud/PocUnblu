package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

/**
 * DTO représentant une ligne du tableau de l'historique des conversations (vue liste).
 * Ne contient pas les événements ni les participants pour limiter la charge réseau.
 *
 * <p>Les horodatages sont sérialisés en ISO 8601 (String) pour garantir
 * la compatibilité avec l'ObjectMapper de Camel REST sans configuration supplémentaire.
 */
@Value
@Builder
public class ConversationHistoryItemResponse {

    String conversationId;
    String topic;
    /** Horodatage ISO 8601, ex : {@code 2024-03-22T14:00:00Z}. */
    String createdAt;
    /** Horodatage ISO 8601, {@code null} si la conversation est encore active. */
    String endedAt;
    /** {@code ACTIVE} ou {@code ENDED} selon la présence de {@code endedAt}. */
    String status;
}
