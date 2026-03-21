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

    public enum ParticipantType {
        VISITOR,
        AGENT,
        BOT
    }

    // --- Accessors ---

    public String personId() { return personId; }

    public String displayName() { return displayName; }

    public ParticipantType participantType() { return type; }
}
