package org.dbs.poc.unblu.integration.application.port.in.query;

import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortDirection;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortField;

public record ListConversationHistoryQuery(
        int page, int size,
        ConversationSortField sortField,
        ConversationSortDirection sortDirection) {

    public static ListConversationHistoryQuery of(int page, int size, String sortField, String sortDir) {
        ConversationSortField field;
        try { field = ConversationSortField.valueOf(sortField.toUpperCase()); }
        catch (Exception e) { field = ConversationSortField.CREATED_AT; }
        ConversationSortDirection dir;
        try { dir = ConversationSortDirection.valueOf(sortDir.toUpperCase()); }
        catch (Exception e) { dir = ConversationSortDirection.DESC; }
        return new ListConversationHistoryQuery(page, size, field, dir);
    }
}
