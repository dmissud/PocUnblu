package org.dbs.poc.unblu.integration.application.port.in;

import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;

public interface EnrichConversationUseCase {
    ConversationHistory enrichOne(String conversationId);
}
