package org.dbs.poc.unblu.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Value object representing a participant in a conversation history.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantHistory {

    private String personId;
    private String displayName;
    private ParticipantType type;

    /** @return identifiant unique de la personne dans Unblu */
    public String personId() {
        return personId;
    }

    // --- Accessors ---

    /** @return nom affiché du participant */
    public String displayName() { return displayName;
    }

    /** @return type de participant (VISITOR, AGENT ou BOT) */
    public ParticipantType participantType() { return type; }

    /**
     * Type de participant dans une conversation.
     */
    public enum ParticipantType {
        /**
         * Visiteur (client) qui a initié la conversation.
         */
        VISITOR,
        /** Agent humain traitant la conversation. */
        AGENT,
        /** Bot automatique (ex. bot de résumé). */
        BOT
    }
}
