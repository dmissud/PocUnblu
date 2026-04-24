package org.dbs.poc.unblu.integration.domain.port.out;

import java.util.List;
import java.util.Optional;

import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortDirection;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortField;

/**
 * Port de sortie pour accéder aux données d'historique des conversations (Bloc 1 — Integration).
 */
public interface ConversationHistoryRepository {

    ConversationHistory save(ConversationHistory domain);

    Optional<ConversationHistory> findByConversationId(String conversationId);

    ConversationHistoryPage findPage(int page, int size, ConversationSortField sortField, ConversationSortDirection sortDir);

    boolean existsByConversationId(String conversationId);

    /**
     * Récupère toutes les conversations de l'historique.
     *
     * @return Liste de toutes les conversations
     */
    List<ConversationHistory> findAll();

    /**
     * Compte le nombre total de conversations.
     *
     * @return Nombre total de conversations
     */
    long count();
}

// Made with Bob
