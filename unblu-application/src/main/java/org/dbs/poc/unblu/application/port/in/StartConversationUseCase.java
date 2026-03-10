package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.ConversationContext;

public interface StartConversationUseCase {
    /**
     * Orchestrates the startup of a new conversation (ERP -> Rules -> Unblu).
     * @param command The initial command
     * @return The resulting conversation context
     */
    ConversationContext startConversation(StartConversationCommand command);
}
