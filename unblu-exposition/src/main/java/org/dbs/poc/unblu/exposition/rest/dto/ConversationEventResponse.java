package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

/**
 * DTO représentant un événement de la timeline d'une conversation.
 *
 * <p>L'horodatage est sérialisé en ISO 8601 (String) pour garantir
 * la compatibilité avec l'ObjectMapper de Camel REST.
 */
@Value
@Builder
public class ConversationEventResponse {

    /** {@code CREATED}, {@code MESSAGE} ou {@code ENDED}. */
    String eventType;
    /** Horodatage ISO 8601. */
    String occurredAt;
    String messageText;
    String senderPersonId;
    String senderDisplayName;
}
