package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse REST pour la création d'une conversation Unblu.
 * Retourne l'identifiant Unblu, l'URL de rejoindre la conversation, le statut et un message optionnel.
 */
@Data
@Builder
public class StartConversationResponse {
    private String unbluConversationId;
    private String unbluJoinUrl;
    private String status;
    private String message;
}
