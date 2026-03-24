package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.EnrichConversationUseCase;
import org.springframework.stereotype.Component;

/**
 * Route Camel exposant le cas d'utilisation d'enrichissement d'une conversation
 * sur l'endpoint {@code direct:enrich-conversation}.
 *
 * <p>Attend le {@code conversationId} dans le header Camel {@code conversationId}.
 */
@Component
@RequiredArgsConstructor
public class EnrichConversationRoute extends RouteBuilder {

    private final EnrichConversationUseCase enrichConversationUseCase;

    @Override
    public void configure() {
        from(OrchestratorEndpoints.DIRECT_ENRICH_CONVERSATION)
            .routeId("enrich-conversation")
            .log("Enrichissement depuis Unblu de la conversation: ${header.conversationId}")
            .process(exchange -> {
                String conversationId = exchange.getIn().getHeader("conversationId", String.class);
                exchange.getIn().setBody(enrichConversationUseCase.enrichOne(conversationId));
            })
            .log("Enrichissement terminé pour la conversation: ${header.conversationId}");
    }
}
