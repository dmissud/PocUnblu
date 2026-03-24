package org.dbs.poc.unblu.domain.model;

/**
 * Données d'un participant récupéré depuis l'historique Unblu.
 *
 * @param personId    identifiant Unblu de la personne
 * @param displayName nom affiché (peut être {@code null})
 * @param personSource source de la personne : {@code USER_DB} (agent), {@code VIRTUAL} (visiteur)
 */
public record UnbluParticipantData(String personId, String displayName, String personSource) {
}
