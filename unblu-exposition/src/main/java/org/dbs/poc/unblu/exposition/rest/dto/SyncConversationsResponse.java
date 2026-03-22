package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Réponse REST du scan de synchronisation des conversations Unblu.
 * Expose les compteurs du traitement et les identifiants des conversations en erreur.
 */
@Value
@Builder
public class SyncConversationsResponse {

    int totalScanned;
    int newlyPersisted;
    int alreadyExisting;
    int errors;
    List<String> errorConversationIds;
    String message;
}
