package org.dbs.poc.unblu.integration.infrastructure.adapter.client;

import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ClientContext;
import org.dbs.poc.unblu.integration.domain.port.out.ClientContextPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Mock CRM/ERP : simule la résolution du contexte client depuis une conversation Unblu.
 * Latence simulée : 80–180ms (appel réseau CRM).
 */
@Slf4j
@Component
public class ClientContextMockAdapter implements ClientContextPort {

    private static final List<String> SEGMENTS  = List.of("VIP", "PREMIUM", "STANDARD", "STANDARD", "STANDARD");
    private static final List<String> LANGUAGES = List.of("fr", "fr", "fr", "en", "de");
    private static final int MIN_LATENCY_MS = 80;
    private static final int LATENCY_RANGE_MS = 100;

    private final Random random = new Random();

    @Override
    public ClientContext resolveClientContext(String conversationId) {
        simulateLatency();
        String clientId = "CLIENT-" + conversationId.substring(0, 8).toUpperCase();
        String segment  = SEGMENTS.get(random.nextInt(SEGMENTS.size()));
        String language = LANGUAGES.get(random.nextInt(LANGUAGES.size()));

        log.info("[EXT_TRACE] step=CRM_RESOLVED conversationId={} clientId={} segment={} language={}",
                conversationId, clientId, segment, language);

        return new ClientContext(conversationId, clientId, segment, language);
    }

    private void simulateLatency() {
        try {
            Thread.sleep(MIN_LATENCY_MS + random.nextInt(LATENCY_RANGE_MS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
