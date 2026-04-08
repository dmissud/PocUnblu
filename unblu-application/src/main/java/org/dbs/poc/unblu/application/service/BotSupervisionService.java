package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchBotsUseCase;
import org.dbs.poc.unblu.domain.model.BotInfo;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implémentation du cas d'utilisation {@link SearchBotsUseCase}.
 * Délègue la récupération des bots au port secondaire {@link UnbluPort}.
 */
@Service
@RequiredArgsConstructor
public class BotSupervisionService implements SearchBotsUseCase {

    private final UnbluPort unbluPort;

    @Override
    public List<BotInfo> listBots() {
        return unbluPort.listBots();
    }
}
