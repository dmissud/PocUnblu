package org.dbs.poc.unblu.exposition.rest.mapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.exposition.rest.dto.ConversationSearchResponse;
import org.dbs.poc.unblu.exposition.rest.dto.ConversationSearchResponse.ConversationSearchItemResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper Camel transformant une liste de {@link UnbluConversationSummary}
 * en {@link ConversationSearchResponse} prête pour la sérialisation REST.
 */
@Slf4j
@Component
public class ConversationSearchMapper {

    /**
     * Lit le corps de l'échange (liste de summaries) et le header {@code state},
     * puis remplace le corps par le DTO de réponse.
     *
     * @param exchange l'échange Camel portant les résultats du use case
     */
    @SuppressWarnings("unchecked")
    public void mapSummariesToResponse(Exchange exchange) {
        List<UnbluConversationSummary> summaries = exchange.getIn().getBody(List.class);
        String state = exchange.getIn().getHeader("state", String.class);

        List<ConversationSearchItemResponse> items = summaries.stream()
                .map(this::toItemResponse)
                .toList();

        exchange.getIn().setBody(new ConversationSearchResponse(items, items.size(), state));
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
