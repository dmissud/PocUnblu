package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.port.out.BotRegistrationPort;
import org.springframework.stereotype.Component;

/**
 * Stub de {@link BotRegistrationPort} — opérations sans effet en test.
 */
@Component
public class StubBotRegistrationPort implements BotRegistrationPort {

    @Override
    public BotRegistration setupPocBot(String ngrokUrl) {
        return new BotRegistration("stub-bot-id", "PocBot", ngrokUrl + "/api/bot/outbound");
    }

    @Override
    public void deactivatePocBot() {
    }
}
