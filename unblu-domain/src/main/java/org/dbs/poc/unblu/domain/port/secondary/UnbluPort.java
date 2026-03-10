package org.dbs.poc.unblu.domain.port.secondary;

import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

public interface UnbluPort {
    /**
     * Creates a new conversation in Unblu and returns its info.
     * @param context The current conversation context
     * @return The Unblu conversation info (id and url)
     */
    UnbluConversationInfo createConversation(ConversationContext context);
}
