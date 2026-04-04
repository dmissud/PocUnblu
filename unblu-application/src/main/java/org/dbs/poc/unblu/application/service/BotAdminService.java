package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.port.in.CreateBotUseCase;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * Implémentation du cas d'utilisation {@link CreateBotUseCase}.
 * Délègue la création du bot au port secondaire {@link UnbluPort}.
 */
public class BotAdminService implements CreateBotUseCase {

    private final UnbluPort unbluPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public String createSummaryBot(String name, String description) {
        return unbluPort.createBot(name, description);
    }
}
