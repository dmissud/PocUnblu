package org.dbs.poc.unblu.integration.infrastructure.adapter.namedarea;

import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ClientContext;
import org.dbs.poc.unblu.integration.domain.port.out.NamedAreaResolverPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Mock moteur de règles : résout le named area Unblu cible en fonction du segment client.
 * Latence simulée : 50–120ms (appel règles métier).
 */
@Slf4j
@Component
public class NamedAreaResolverMockAdapter implements NamedAreaResolverPort {

    // named area IDs fictifs par segment (à remplacer par les vrais IDs Unblu en intégration)
    private static final Map<String, String> SEGMENT_TO_NAMED_AREA = Map.of(
            "VIP",      "named-area-vip-desk",
            "PREMIUM",  "named-area-premium-desk",
            "STANDARD", "named-area-standard-desk"
    );
    private static final String DEFAULT_NAMED_AREA = "named-area-standard-desk";

    private static final int MIN_LATENCY_MS = 50;
    private static final int LATENCY_RANGE_MS = 70;

    private final Random random = new Random();

    @Override
    public String resolveNamedAreaId(ClientContext clientContext) {
        simulateLatency();
        String namedAreaId = SEGMENT_TO_NAMED_AREA.getOrDefault(clientContext.segment(), DEFAULT_NAMED_AREA);

        log.info("[EXT_TRACE] step=NAMED_AREA_RESOLVED conversationId={} segment={} namedAreaId={}",
                clientContext.conversationId(), clientContext.segment(), namedAreaId);

        return namedAreaId;
    }

    private void simulateLatency() {
        try {
            Thread.sleep(MIN_LATENCY_MS + random.nextInt(LATENCY_RANGE_MS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
