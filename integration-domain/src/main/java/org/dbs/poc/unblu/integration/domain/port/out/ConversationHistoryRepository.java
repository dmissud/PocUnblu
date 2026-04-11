package org.dbs.poc.unblu.integration.domain.port.out;

import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortDirection;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortField;
import java.util.Optional;

public interface ConversationHistoryRepository {
    ConversationHistory save(ConversationHistory history);
    Optional<ConversationHistory> findByConversationId(String conversationId);
    ConversationHistoryPage findPage(int page, int size, ConversationSortField sortField, ConversationSortDirection sortDir);
    boolean existsByConversationId(String conversationId);
}
