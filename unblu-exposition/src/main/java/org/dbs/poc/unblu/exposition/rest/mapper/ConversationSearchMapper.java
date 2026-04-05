package org.dbs.poc.unblu.exposition.rest.mapper;

import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.exposition.rest.dto.ConversationSearchResponse;
import org.dbs.poc.unblu.exposition.rest.dto.ConversationSearchResponse.ConversationSearchItemResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper transformant une liste de {@link UnbluConversationSummary}
 * en {@link ConversationSearchResponse} prête pour la sérialisation REST.
 */
@Slf4j
@Component
public class ConversationSearchMapper {

    /**
     * Mappe une liste de summaries en {@link ConversationSearchResponse}.
     */
    public ConversationSearchResponse toResponse(List<UnbluConversationSummary> summaries, String state) {
        List<ConversationSearchItemResponse> items = summaries.stream()
                .map(this::toItemResponse)
                .toList();

        return new ConversationSearchResponse(items, items.size(), state);
    }

    private ConversationSearchItemResponse toItemResponse(UnbluConversationSummary summary) {
        return new ConversationSearchItemResponse(
                summary.id(),
                summary.topic(),
                summary.state(),
                summary.createdAt().toString(),
                summary.endedAt() != null ? summary.endedAt().toString() : null);
    }
}
