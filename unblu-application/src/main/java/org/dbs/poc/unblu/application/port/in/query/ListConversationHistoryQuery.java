package org.dbs.poc.unblu.application.port.in.query;

import org.dbs.poc.unblu.domain.model.history.ConversationSortDirection;
import org.dbs.poc.unblu.domain.model.history.ConversationSortField;

/**
 * Requête de listing paginé et trié des conversations historisées.
 *
 * <p>Le numéro de page est 0-indexé. La taille est bornée entre 1 et 100.
 * Par défaut : tri par date de début décroissant.
 */
public record ListConversationHistoryQuery(
        int page,
        int size,
        ConversationSortField sortField,
        ConversationSortDirection sortDirection) {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final ConversationSortField DEFAULT_SORT_FIELD = ConversationSortField.CREATED_AT;
    private static final ConversationSortDirection DEFAULT_SORT_DIR = ConversationSortDirection.DESC;

    public ListConversationHistoryQuery {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0, got: " + page);
        if (size < 1 || size > MAX_SIZE)
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE + ", got: " + size);
        if (sortField == null) sortField = DEFAULT_SORT_FIELD;
        if (sortDirection == null) sortDirection = DEFAULT_SORT_DIR;
    }

    public static ListConversationHistoryQuery of(int page, int size, String sortField, String sortDir) {
        ConversationSortField field = parseSortField(sortField);
        ConversationSortDirection direction = parseSortDirection(sortDir);
        return new ListConversationHistoryQuery(page, size > 0 ? size : DEFAULT_SIZE, field, direction);
    }

    private static ConversationSortField parseSortField(String value) {
        if (value == null || value.isBlank()) return DEFAULT_SORT_FIELD;
        try {
            return ConversationSortField.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT_SORT_FIELD;
        }
    }

    private static ConversationSortDirection parseSortDirection(String value) {
        if (value == null || value.isBlank()) return DEFAULT_SORT_DIR;
        try {
            return ConversationSortDirection.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT_SORT_DIR;
        }
    }
}
