package org.dbs.poc.unblu.domain.model;

import java.time.Instant;

/**
 * Données d'un message récupéré depuis l'historique Unblu.
 *
 * @param text           contenu texte du message (peut être {@code null} pour les messages non-texte)
 * @param senderPersonId identifiant Unblu de l'émetteur
 * @param sentAt         horodatage d'envoi
 */
public record UnbluMessageData(String text, String senderPersonId, Instant sentAt) {
}
