package org.dbs.poc.unblu.exposition.rest.mapper;

import org.dbs.poc.unblu.domain.model.ConversationSyncResult;
import org.dbs.poc.unblu.exposition.rest.dto.SyncConversationsResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper pour la synchronisation des conversations.
 * Convertit un {@link ConversationSyncResult} domaine en {@link SyncConversationsResponse} REST.
 */
@Component
public class SyncConversationsMapper {

    /**
     * Transforme un {@link ConversationSyncResult} en {@link SyncConversationsResponse}.
     */
    public SyncConversationsResponse toResponse(ConversationSyncResult result) {
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
