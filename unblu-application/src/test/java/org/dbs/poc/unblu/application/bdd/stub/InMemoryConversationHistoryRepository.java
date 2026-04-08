package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.model.history.*;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implémentation en mémoire de {@link ConversationHistoryRepository}.
 * Utilisée uniquement en test — aucune dépendance JPA ou base de données.
 * Supporte la pagination et le tri basiques pour les scénarios de test.
 */
@Component
public class InMemoryConversationHistoryRepository implements ConversationHistoryRepository {

    private final Map<String, ConversationHistory> store = new ConcurrentHashMap<>();

    public void reset() {
        store.clear();
    }

    public void seed(ConversationHistory history) {
        store.put(history.conversationId(), history);
    }

    @Override
    public ConversationHistory save(ConversationHistory conversationHistory) {
        store.put(conversationHistory.conversationId(), conversationHistory);
        return conversationHistory;
    }

    @Override
    public Optional<ConversationHistory> findByConversationId(String conversationId) {
        return Optional.ofNullable(store.get(conversationId));
    }

    @Override
    public ConversationHistoryPage findPage(int page, int size, ConversationSortField sortField, ConversationSortDirection sortDir) {
        List<ConversationHistory> all = new ArrayList<>(store.values());

        Comparator<ConversationHistory> comparator = switch (sortField) {
            case CREATED_AT -> Comparator.comparing(
                    ConversationHistory::startedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case ENDED_AT -> Comparator.comparing(
                    ConversationHistory::endedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case TOPIC -> Comparator.comparing(
                    ConversationHistory::topic, Comparator.nullsLast(String::compareTo));
        };

        if (sortDir == ConversationSortDirection.DESC) {
            comparator = comparator.reversed();
        }

        all.sort(comparator);

        int totalItems = all.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<ConversationHistory> pageItems = fromIndex >= totalItems
                ? List.of()
                : all.subList(fromIndex, toIndex);

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
        return new ConversationHistoryPage(pageItems, totalItems, page, size, totalPages);
    }

    @Override
    public boolean existsByConversationId(String conversationId) {
        return store.containsKey(conversationId);
    }
}
