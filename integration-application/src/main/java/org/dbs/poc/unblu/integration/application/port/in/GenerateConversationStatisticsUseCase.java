package org.dbs.poc.unblu.integration.application.port.in;

import org.dbs.poc.unblu.integration.domain.model.statistics.ConversationStatistics;

/**
 * Port d'entrée pour générer les statistiques de conversations (Bloc 1 — Integration).
 *
 * <p>Use case permettant de calculer et persister les statistiques
 * sur les conversations Unblu.</p>
 */
public interface GenerateConversationStatisticsUseCase {

    /**
     * Génère les statistiques de conversations et les persiste.
     *
     * @return Les statistiques calculées
     */
    ConversationStatistics generate();
}

// Made with Bob
