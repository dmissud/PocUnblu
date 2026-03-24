package org.dbs.poc.unblu.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Value object representing a single event that occurred during a conversation.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEventHistory {

    private EventType eventType;
    private Instant eventTime;

    // For MESSAGE events
    private String messageId;
    private String messageText;
    private String senderPersonId;
    private String senderDisplayName;

    /** @return type de l'événement */
    public EventType eventType() { return eventType;
    }

    // --- Accessors ---

    /** @return horodatage de l'événement */
    public Instant occurredAt() { return eventTime;
    }

    /** @return identifiant Unblu du message (uniquement pour {@code MESSAGE}), utilisé pour la déduplication */
    public String messageId() { return messageId; }

    /** @return texte du message (uniquement pour les événements de type {@code MESSAGE}) */
    public String messageText() { return messageText;
    }

    /** @return identifiant de la personne expéditrice (uniquement pour {@code MESSAGE}) */
    public String senderPersonId() { return senderPersonId;
    }

    /** @return nom affiché de l'expéditeur (uniquement pour {@code MESSAGE}) */
    public String senderDisplayName() { return senderDisplayName; }

    /**
     * Types d'événements pouvant survenir dans une conversation.
     */
    public enum EventType {
        /**
         * La conversation a été créée.
         */
        CREATED,
        /** Un message a été envoyé dans la conversation. */
        MESSAGE,
        /** La conversation s'est terminée. */
        ENDED
    }
}
