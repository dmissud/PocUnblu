package org.dbs.poc.unblu.integration.application.port.in;

import org.dbs.poc.unblu.integration.application.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage;

public interface ListConversationHistoryUseCase {
    ConversationHistoryPage listConversations(ListConversationHistoryQuery query);
}
