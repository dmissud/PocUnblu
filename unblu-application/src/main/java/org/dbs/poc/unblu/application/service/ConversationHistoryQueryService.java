package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.GetConversationHistoryUseCase;
import org.dbs.poc.unblu.application.port.in.ListConversationHistoryUseCase;
import org.dbs.poc.unblu.application.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service de consultation de l'historique des conversations persistées en base de données.
 *
 * <p>Implémente les deux cas d'utilisation en lecture :
 * <ul>
 *   <li>{@link ListConversationHistoryUseCase} — listing paginé sans le détail des événements</li>
 *   <li>{@link GetConversationHistoryUseCase} — détail complet avec événements ordonnés chronologiquement</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryQueryService
        implements ListConversationHistoryUseCase, GetConversationHistoryUseCase {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public ConversationHistoryPage listConversations(ListConversationHistoryQuery query) {
        log.debug("Listing conversations — page: {}, size: {}, sort: {} {}", query.page(), query.size(), query.sortField(), query.sortDirection());
        ConversationHistoryPage page = conversationHistoryRepository.findPage(query.page(), query.size(), query.sortField(), query.sortDirection());
        log.debug("Retour de {} conversation(s) sur {} au total", page.items().size(), page.totalItems());
        return page;
    }

    @Override
    public Optional<ConversationHistory> getByConversationId(String conversationId) {
        log.debug("Chargement du détail de la conversation {}", conversationId);
        return conversationHistoryRepository.findByConversationId(conversationId);
    }
}
