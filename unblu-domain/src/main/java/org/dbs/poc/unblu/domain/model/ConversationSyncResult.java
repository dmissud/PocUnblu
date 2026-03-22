package org.dbs.poc.unblu.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Résultat d'une opération de synchronisation des conversations Unblu vers la base de données.
 *
 * <p>Ce value object agrège les compteurs issus du traitement de chaque conversation :
 * nombre total scannées, nouvellement persistées, déjà existantes, et en erreur.
 * Les identifiants des conversations en erreur sont conservés pour traçabilité.
 */
public record ConversationSyncResult(
        int totalScanned,
        int newlyPersisted,
        int alreadyExisting,
        int errors,
        List<String> errorConversationIds) {

    public ConversationSyncResult {
        Objects.requireNonNull(errorConversationIds, "errorConversationIds cannot be null");
        errorConversationIds = List.copyOf(errorConversationIds);
    }
}