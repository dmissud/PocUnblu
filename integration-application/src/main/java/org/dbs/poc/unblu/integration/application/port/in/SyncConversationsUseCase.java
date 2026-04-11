package org.dbs.poc.unblu.integration.application.port.in;

import org.dbs.poc.unblu.integration.domain.model.ConversationSyncResult;

public interface SyncConversationsUseCase {
    ConversationSyncResult syncAll();
}
