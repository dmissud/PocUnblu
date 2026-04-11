package org.dbs.poc.unblu.integration.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantHistory {
    private String personId;
    private String displayName;
    private ParticipantType type;

    public String personId() { return personId; }
    public String displayName() { return displayName; }
    public ParticipantType participantType() { return type; }

    public enum ParticipantType { VISITOR, AGENT, BOT }
}
