package org.dbs.poc.unblu.infrastructure.adapter.bot;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.exception.UnbluApiException;
import org.dbs.poc.unblu.domain.port.out.BotRegistrationPort;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluBotService;
import org.springframework.stereotype.Component;

/**
 * Adaptateur secondaire implémentant {@link BotRegistrationPort} en déléguant
 * à {@link UnbluBotService} et en mappant les exceptions infrastructure vers les
 * exceptions domaine.
 */
@Component
@RequiredArgsConstructor
public class BotRegistrationAdapter implements BotRegistrationPort {

    private final UnbluBotService unbluBotService;

    @Override
    public BotRegistration setupPocBot(String ngrokUrl) {
        try {
            var botData = unbluBotService.setupPocBot(ngrokUrl);
            return new BotRegistration(botData.getId(), botData.getName(), botData.getWebhookEndpoint());
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }

    @Override
    public void deactivatePocBot() {
        try {
            unbluBotService.deactivatePocBot();
        } catch (org.dbs.poc.unblu.infrastructure.exception.UnbluApiException e) {
            throw new UnbluApiException(e.getStatusCode(), e.getStatusDescription(), e.getMessage());
        }
    }
}
