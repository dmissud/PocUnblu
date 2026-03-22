package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.SyncConversationsUseCase;
import org.springframework.stereotype.Component;

/**
 * Route Camel exposant le cas d'utilisation de synchronisation des conversations
 * sur l'endpoint {@code direct:sync-conversations}.
 *
 * <p>Cette route est le point d'entrée Camel unique pour le scan Unblu → base de données.
 * La logique métier est déléguée à {@link SyncConversationsUseCase}.
 */
@Component
@RequiredArgsConstructor
public class SyncConversationsRoute extends RouteBuilder {

    private final SyncConversationsUseCase syncConversationsUseCase;

    @Override
    public void configure() {
        from(OrchestratorEndpoints.DIRECT_SYNC_CONVERSATIONS)
            .routeId("sync-conversations")
            .log("Démarrage du scan de synchronisation des conversations Unblu")
            .process(exchange -> exchange.getIn().setBody(syncConversationsUseCase.syncAll()))
            .log("Synchronisation terminée: ${body}");
    }
}
