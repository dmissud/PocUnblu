package org.dbs.poc.unblu.integration.infrastructure.adapter.url;

import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ClientContext;
import org.dbs.poc.unblu.integration.domain.port.out.ResourceUrlPort;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Mock générateur d'URL : simule le calcul d'une URL contextuelle (espace client, document, co-browsing).
 * Latence simulée : 60–160ms (appel service de gestion documentaire).
 */
@Slf4j
@Component
public class ResourceUrlMockAdapter implements ResourceUrlPort {

    private static final String BASE_URL = "https://client-portal.example.com";
    private static final int MIN_LATENCY_MS = 60;
    private static final int LATENCY_RANGE_MS = 100;

    private final Random random = new Random();

    @Override
    public String computeResourceUrl(ClientContext clientContext) {
        simulateLatency();
        String url = BASE_URL
                + "/clients/" + clientContext.clientId()
                + "/conversations/" + clientContext.conversationId()
                + "?segment=" + clientContext.segment().toLowerCase()
                + "&lang=" + clientContext.language();

        log.info("[EXT_TRACE] step=URL_COMPUTED conversationId={} clientId={} url={}",
                clientContext.conversationId(), clientContext.clientId(), url);

        return url;
    }

    private void simulateLatency() {
        try {
            Thread.sleep(MIN_LATENCY_MS + random.nextInt(LATENCY_RANGE_MS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
