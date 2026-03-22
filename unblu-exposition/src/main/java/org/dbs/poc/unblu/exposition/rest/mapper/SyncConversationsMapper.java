package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.ConversationSyncResult;
import org.dbs.poc.unblu.exposition.rest.dto.SyncConversationsResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper Camel pour la route de synchronisation des conversations.
 * Convertit un {@link ConversationSyncResult} domaine en {@link SyncConversationsResponse} REST.
 */
@Component
public class SyncConversationsMapper {

    /**
     * Extrait le {@link ConversationSyncResult} du corps de l'échange Camel
     * et le transforme en {@link SyncConversationsResponse}.
     *
     * @param exchange l'échange Camel portant le résultat du scan
     */
    public void mapResultToResponse(Exchange exchange) {
        ConversationSyncResult result = exchange.getIn().getBody(ConversationSyncResult.class);
        exchange.getIn().setBody(toResponse(result));
    }

    private SyncConversationsResponse toResponse(ConversationSyncResult result) {
        String message = String.format(
                "%d conversation(s) scannée(s) : %d nouvelle(s), %d déjà existante(s), %d erreur(s)",
                result.totalScanned(), result.newlyPersisted(), result.alreadyExisting(), result.errors());

        return SyncConversationsResponse.builder()
                .totalScanned(result.totalScanned())
                .newlyPersisted(result.newlyPersisted())
                .alreadyExisting(result.alreadyExisting())
                .errors(result.errors())
                .errorConversationIds(result.errorConversationIds())
                .message(message)
                .build();
    }
}
