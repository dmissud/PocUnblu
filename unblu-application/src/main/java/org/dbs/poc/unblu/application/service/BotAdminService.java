package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.CreateBotUseCase;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotAdminService implements CreateBotUseCase {

    private final UnbluPort unbluPort;

    @Override
    public String createSummaryBot(String name, String description) {
        return unbluPort.createBot(name, description);
    }
}
