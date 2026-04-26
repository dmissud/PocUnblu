package org.dbs.poc.unblu.integration.domain.model;

/**
 * Contexte client résolu depuis le CRM/ERP pour une conversation donnée.
 *
 * @param conversationId identifiant de la conversation Unblu
 * @param clientId       identifiant du client dans le CRM
 * @param segment        segment client (VIP, PREMIUM, STANDARD, UNKNOWN)
 * @param language       langue préférée du client (fr, en, de, ...)
 */
public record ClientContext(
        String conversationId,
        String clientId,
        String segment,
        String language
) {}
