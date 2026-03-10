package org.dbs.poc.unblu.domain.port.secondary;

import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

import java.util.List;

public interface UnbluPort {
    /**
     * Creates a new conversation in Unblu and returns its info.
     */
    UnbluConversationInfo createConversation(ConversationContext context);

    /**
     * Searches for known persons (VIRTUAL source) in Unblu.
     * @param sourceId optional filter by source ID
     */
    List<PersonInfo> searchPersons(String sourceId);

    List<TeamInfo> searchTeams();
}
