package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.SearchConversationsByStateQuery;
import org.dbs.poc.unblu.application.port.in.SearchConversationsByStateUseCase;
import org.springframework.stereotype.Component;

/**
 * Route Camel exposant le use case {@link SearchConversationsByStateUseCase}
 * sur l'endpoint {@code direct:search-conversations-by-state}.
 *
 * <p>Le header {@code state} porte l'état de conversation recherché.
 */
@Component
@RequiredArgsConstructor
public class SearchConversationsByStateRoute extends RouteBuilder {

    private final SearchConversationsByStateUseCase searchConversationsByStateUseCase;

    @Override
    public void configure() {
        from(OrchestratorEndpoints.DIRECT_SEARCH_CONVERSATIONS_BY_STATE)
                .routeId("search-conversations-by-state")
                .log("Recherche de conversations par état: ${header.state}")
                .process(exchange -> {
                    String state = exchange.getIn().getHeader("state", String.class);
                    SearchConversationsByStateQuery query = new SearchConversationsByStateQuery(state);
                    exchange.getIn().setBody(searchConversationsByStateUseCase.searchByState(query));
                })
                .log("Trouvé ${body.size()} conversation(s) avec état ${header.state}");
    }
}
