package org.dbs.poc.unblu.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluBotService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes the Unblu bot at application startup.
 * This component ensures that the bot exists in Unblu and updates the configuration if needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final UnbluBotService unbluBotService;
    private final UnbluProperties unbluProperties;

    /**
     * Initializes the bot when the application is ready.
     * This method is called after the application context is fully started.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeBot() {
        try {
            String botName = unbluProperties.getBotName();
            if (botName == null || botName.isBlank()) {
                log.warn("Bot name not configured (unblu.api.bot-name), skipping bot initialization");
                return;
            }

            log.info("Initializing Unblu bot with name: {}", botName);

            String botPersonId = unbluBotService.createOrGetBot(
                botName,
                "Bot automatique pour l'envoi de résumés de conversation"
            );

            log.info("Bot initialized successfully: name={}, botPersonId={}", botName, botPersonId);

            // Log a reminder if the bot person ID is not configured
            String configuredBotId = unbluProperties.getSummaryBotPersonId();
            if (configuredBotId == null || configuredBotId.isBlank()) {
                log.warn("⚠️  Bot created but unblu.api.summary-bot-person-id is not configured!");
                log.warn("⚠️  Please add this to your configuration: unblu.api.summary-bot-person-id={}", botPersonId);
            } else if (!configuredBotId.equals(botPersonId)) {
                log.warn("⚠️  Bot person ID mismatch!");
                log.warn("⚠️  Configured: {}", configuredBotId);
                log.warn("⚠️  Actual: {}", botPersonId);
                log.warn("⚠️  Please update unblu.api.summary-bot-person-id to: {}", botPersonId);
            }

        } catch (Exception e) {
            log.error("Failed to initialize bot at startup", e);
            // Don't throw the exception to prevent application startup failure
        }
    }
}
