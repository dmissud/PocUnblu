package org.dbs.poc.unblu.domain.port.out;

import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.domain.model.history.ConversationSortDirection;
import org.dbs.poc.unblu.domain.model.history.ConversationSortField;

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
     * Returns a paginated and sorted list of conversation histories.
     * Items contain only header data (no events/participants) for efficient listing.
     * Null values for nullable fields ({@code endedAt}, {@code topic}) are placed last
     * regardless of the sort direction.
     *
     * @param page      zero-indexed page number
     * @param size      number of items per page
     * @param sortField the field to sort by
     * @param sortDir   the sort direction
     */
    ConversationHistoryPage findPage(int page, int size, ConversationSortField sortField, ConversationSortDirection sortDir);

    /**
     * Check if a conversation exists.
     */
    boolean existsByConversationId(String conversationId);
}
