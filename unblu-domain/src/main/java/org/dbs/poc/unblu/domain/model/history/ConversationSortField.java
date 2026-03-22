package org.dbs.poc.unblu.domain.model.history;

/**
 * Colonnes sur lesquelles le listing des conversations peut être trié.
 * Expose uniquement les attributs métier pertinents pour l'IHM.
 */
public enum ConversationSortField {

    /** Date de début de la conversation ({@code created_at}). */
    CREATED_AT,

    /** Date de fin de la conversation ({@code ended_at}). Peut être {@code null} pour les actives. */
    ENDED_AT,

    /** Sujet de la conversation ({@code topic}). */
    TOPIC
}
