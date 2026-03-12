package org.dbs.poc.unblu.domain.port.secondary;

import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

import java.util.List;

public interface UnbluPort {
    /**
     * Creates a new conversation in Unblu and returns its info.
     */
    UnbluConversationInfo createConversation(ConversationContext context);

    /**
     * Searches for persons in Unblu.
     * @param sourceId optional filter by source ID
     * @param personSource optional filter by person source (USER_DB or VIRTUAL)
     */
    List<PersonInfo> searchPersons(String sourceId, PersonSource personSource);

    List<TeamInfo> searchTeams();

    /**
     * Creates a direct conversation between a VIRTUAL person and a USER_DB agent.
     * @param virtualPerson the VIRTUAL participant
     * @param agentPerson   the USER_DB agent participant
     * @param subject       the conversation subject
     */
    UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject);
}
