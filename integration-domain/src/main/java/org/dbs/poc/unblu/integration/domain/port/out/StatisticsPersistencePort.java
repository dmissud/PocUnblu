package org.dbs.poc.unblu.integration.domain.port.out;

import org.dbs.poc.unblu.integration.domain.model.statistics.ConversationStatistics;

/**
 * Port de sortie pour la persistance des statistiques (Bloc 1 — Integration).
 *
 * <p>Interface hexagonale permettant de sauvegarder les statistiques
 * calculées dans un système de stockage (fichier, base de données, etc.).</p>
 */
public interface StatisticsPersistencePort {

    /**
     * Sauvegarde les statistiques calculées.
     *
     * @param statistics Les statistiques à persister
     */
    void save(ConversationStatistics statistics);
}

// Made with Bob
