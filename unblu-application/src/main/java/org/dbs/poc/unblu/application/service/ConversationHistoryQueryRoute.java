package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.GetConversationHistoryUseCase;
import org.dbs.poc.unblu.application.port.in.ListConversationHistoryQuery;
import org.dbs.poc.unblu.application.port.in.ListConversationHistoryUseCase;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Routes Camel exposant les cas d'utilisation de consultation de l'historique
 * des conversations sur les endpoints {@code direct:} dédiés.
 */
@Component
@RequiredArgsConstructor
public class ConversationHistoryQueryRoute extends RouteBuilder {

    private final ListConversationHistoryUseCase listConversationHistoryUseCase;
    private final GetConversationHistoryUseCase getConversationHistoryUseCase;

    @Override
    public void configure() {

        from(OrchestratorEndpoints.DIRECT_LIST_CONVERSATION_HISTORY)
            .routeId("list-conversation-history")
            .log("Listing conversations — page: ${header.page}, size: ${header.size}, sort: ${header.sortField} ${header.sortDir}")
            .process(exchange -> {
                int page = exchange.getIn().getHeader("page", 0, Integer.class);
                int size = exchange.getIn().getHeader("size", 10, Integer.class);
                String sortField = exchange.getIn().getHeader("sortField", String.class);
                String sortDir = exchange.getIn().getHeader("sortDir", String.class);
                exchange.getIn().setBody(
                        listConversationHistoryUseCase.listConversations(
                                ListConversationHistoryQuery.of(page, size, sortField, sortDir)));
            });

        from(OrchestratorEndpoints.DIRECT_GET_CONVERSATION_HISTORY)
            .routeId("get-conversation-history")
            .log("Loading conversation detail: ${header.conversationId}")
            .process(exchange -> {
                String conversationId = exchange.getIn().getHeader("conversationId", String.class);
                Optional<ConversationHistory> result =
                        getConversationHistoryUseCase.getByConversationId(conversationId);
                if (result.isPresent()) {
                    exchange.getIn().setBody(result.get());
                } else {
                    exchange.getIn().setBody(null);
                    exchange.getIn().setHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE, 404);
                }
            });
    }
}
