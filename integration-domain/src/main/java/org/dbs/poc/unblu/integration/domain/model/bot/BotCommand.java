package org.dbs.poc.unblu.integration.domain.model.bot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Message publié sur le topic {@code unblu.bot.commands}.
 * Représente un événement bot reçu d'Unblu à traiter par l'engine.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BotCommand(
        String commandType,
        String correlationId,
        String dialogToken,
        String conversationId,
        String timestamp,
        String rawPayload
) {}
