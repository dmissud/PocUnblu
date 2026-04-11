package org.dbs.poc.unblu.integration.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.ConversationHistoryApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.MessageExportQuery;
import com.unblu.webapi.model.v4.MessageExportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.UnbluMessageData;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service dédié à la récupération de l'historique des messages via l'API Unblu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnbluMessageClient {

    private static final int MSG_PAGE_SIZE = 100;
    private final ApiClient integrationUnbluApiClient;

    private ConversationHistoryApi historyApi() {
        return new ConversationHistoryApi(integrationUnbluApiClient);
    }

    public List<UnbluMessageData> fetchConversationMessages(String conversationId) throws ApiException {
        List<UnbluMessageData> all = new ArrayList<>();
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            MessageExportQuery query = new MessageExportQuery();
            query.setOffset(offset);
            query.setLimit(MSG_PAGE_SIZE);

            MessageExportResult result = historyApi().conversationHistoryExportMessageLog(conversationId, query);
            if (result.getItems() != null) {
                result.getItems().stream()
                        .filter(m -> m.getText() != null && m.getDeletedForAll() == null)
                        .map(m -> new UnbluMessageData(
                                m.getId(), m.getText(), m.getSenderPersonId(),
                                m.getSendTimestamp() != null ? Instant.ofEpochMilli(m.getSendTimestamp()) : Instant.now()))
                        .forEach(all::add);
            }

            hasMore = Boolean.TRUE.equals(result.isHasMoreItems());
            offset = hasMore ? result.getNextOffset() : offset;
        }
        return all;
    }
}
