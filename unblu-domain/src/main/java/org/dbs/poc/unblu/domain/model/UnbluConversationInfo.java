package org.dbs.poc.unblu.domain.model;

/**
 * Informations retournées par Unblu après la création d'une conversation.
 *
 * @param unbluConversationId identifiant unique de la conversation dans Unblu
 * @param unbluJoinUrl        URL permettant à un participant de rejoindre la conversation
 */
public record UnbluConversationInfo(String unbluConversationId, String unbluJoinUrl) {
}
