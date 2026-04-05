package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.application.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.domain.model.history.ConversationHistoryPage;

/**
 * Cas d'utilisation : liste paginée des conversations persistées en base de données.
 */
public interface ListConversationHistoryUseCase {

    /**
     * Retourne une page de l'historique des conversations, triées par date de création décroissante.
     *
     * @param query les paramètres de pagination (page, taille)
     * @return la page de résultats correspondante
     */
    ConversationHistoryPage listConversations(ListConversationHistoryQuery query);
}
