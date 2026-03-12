package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

public interface StartDirectConversationUseCase {
    /**
     * Creates a direct conversation between a VIRTUAL participant (via ERP + Rule Engine)
     * and a USER_DB agent identified by sourceId.
     */
    UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command);
}
