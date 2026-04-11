package org.dbs.poc.unblu.integration.application.port.in;

import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import java.util.Optional;

public interface GetConversationHistoryUseCase {
    Optional<ConversationHistory> getByConversationId(String conversationId);
}
