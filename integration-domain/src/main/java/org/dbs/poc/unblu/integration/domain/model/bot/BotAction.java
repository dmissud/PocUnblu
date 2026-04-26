package org.dbs.poc.unblu.integration.domain.model.bot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Message publié sur le topic {@code unblu.bot.actions}.
 * Représente une action à exécuter vers l'API Unblu.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BotAction(
        String actionType,
        String correlationId,
        String dialogToken,
        String conversationId,
        Map<String, String> payload
) {}
