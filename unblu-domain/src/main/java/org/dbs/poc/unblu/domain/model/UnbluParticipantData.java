package org.dbs.poc.unblu.domain.model;

/**
 * Données d'un participant récupéré depuis l'historique Unblu.
 *
 * @param personId          identifiant Unblu de la personne
 * @param displayName       nom affiché (peut être {@code null})
 * @param participationType type de participation (ex : CONTEXT_PERSON, ASSIGNED_AGENT)
 */
public record UnbluParticipantData(String personId, String displayName, String participationType) {
}
