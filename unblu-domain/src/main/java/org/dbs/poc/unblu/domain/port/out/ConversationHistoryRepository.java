package org.dbs.poc.unblu.domain.port.out;

import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistoryPage;

import java.util.Optional;

/**
 * Repository port for conversation history persistence.
 */
public interface ConversationHistoryRepository {

    /**
     * Save or update a conversation history.
     */
    ConversationHistory save(ConversationHistory conversationHistory);

    /**
     * Find a conversation history by conversation ID, with all events and participants loaded.
     */
    Optional<ConversationHistory> findByConversationId(String conversationId);

    /**
     * Returns a paginated list of conversation histories, sorted by creation date descending.
     * Items contain only header data (no events/participants) for efficient listing.
     *
     * @param page zero-indexed page number
     * @param size number of items per page
     */
    ConversationHistoryPage findPage(int page, int size);

    /**
     * Check if a conversation exists.
     */
    boolean existsByConversationId(String conversationId);
}
