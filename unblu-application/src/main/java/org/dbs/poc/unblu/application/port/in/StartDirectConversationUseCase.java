package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.application.port.in.command.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

/**
 * Cas d'utilisation : démarrage d'une conversation directe entre un participant virtuel et un agent.
 * Le workflow inclut la recherche des participants, la validation ERP/règles et la création Unblu.
 */
public interface StartDirectConversationUseCase {
    /**
     * Creates a direct conversation between a VIRTUAL participant (via ERP + Rule Engine)
     * and a USER_DB agent identified by sourceId.
     */
    UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command);
}
