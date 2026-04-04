package org.dbs.poc.unblu.domain.port.in;

import org.dbs.poc.unblu.domain.model.ConversationSyncResult;

/**
 * Cas d'utilisation : scan à la demande de toutes les conversations Unblu
 * et synchronisation vers la base de données.
 *
 * <p>L'opération est idempotente : une conversation déjà persistée n'est pas
 * recréée, seule une mise à jour de fin de conversation est appliquée si nécessaire.
 */
public interface SyncConversationsUseCase {

    /**
     * Scanne toutes les conversations présentes dans Unblu et persiste
     * les nouvelles en base de données. Met à jour l'horodatage de fin
     * pour les conversations terminées déjà connues.
     *
     * @return le résultat du scan avec les compteurs et les identifiants en erreur
     */
    ConversationSyncResult syncAll();
}
