package org.dbs.poc.unblu.domain.port.out;

import org.dbs.poc.unblu.domain.model.history.ConversationHistory;

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
     * Find a conversation history by conversation ID.
     */
    Optional<ConversationHistory> findByConversationId(String conversationId);

    /**
     * Check if a conversation exists.
     */
    boolean existsByConversationId(String conversationId);
}
