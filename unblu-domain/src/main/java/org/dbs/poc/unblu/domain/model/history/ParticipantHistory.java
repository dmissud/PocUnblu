package org.dbs.poc.unblu.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain entity representing a conversation participant.
 */
@Data
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
}
