package org.dbs.poc.unblu.domain.model;

import java.time.Instant;

/**
 * Données d'un message récupéré depuis l'historique Unblu.
 *
 * @param messageId      identifiant unique Unblu du message (utilisé pour la déduplication)
 * @param text           contenu texte du message
 * @param senderPersonId identifiant Unblu de l'émetteur
 * @param sentAt         horodatage d'envoi
 */
public record UnbluMessageData(String messageId, String text, String senderPersonId, Instant sentAt) {
}
