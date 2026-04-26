package org.dbs.poc.unblu.integration.infrastructure.adapter.namedarea;

import com.unblu.webapi.jersey.v4.api.NamedAreasApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ClientContext;
import org.dbs.poc.unblu.integration.domain.port.out.NamedAreaResolverPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock moteur de règles : tire aléatoirement entre C_HB_PREMIUM et C_HB_COLLABORATEUR.
 * Résout les noms en IDs Unblu au démarrage via l'API.
 * Latence simulée : 50–120ms (appel règles métier).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NamedAreaResolverMockAdapter implements NamedAreaResolverPort {

    private static final List<String> CANDIDATE_NAMES = List.of("C_HB_PREMIUM", "C_HB_COLLABORATEUR");

    private static final int MIN_LATENCY_MS = 50;
    private static final int LATENCY_RANGE_MS = 70;

    private final ApiClient integrationUnbluApiClient;
    private final Random random = new Random();

    /** name → id, résolu au démarrage */
    private final Map<String, String> nameToId = new ConcurrentHashMap<>();

    @PostConstruct
    void resolveNamedAreaIds() {
        NamedAreasApi api = new NamedAreasApi(integrationUnbluApiClient);
        try {
            var result = api.namedAreasSearch(null, null);
            if (result == null || result.getItems() == null) return;

            result.getItems().forEach(area -> {
                if (CANDIDATE_NAMES.contains(area.getName())) {
                    nameToId.put(area.getName(), area.getId());
                    log.info("[NAMED_AREA_INIT] name={} id={}", area.getName(), area.getId());
                }
            });
            CANDIDATE_NAMES.stream()
                    .filter(n -> !nameToId.containsKey(n))
                    .forEach(n -> log.warn("[NAMED_AREA_INIT] name={} not found in Unblu", n));
        } catch (ApiException e) {
            log.warn("[NAMED_AREA_INIT] lookup failed ({}): {}", e.getCode(), e.getMessage());
        }
    }

    @Override
    public String resolveNamedAreaId(ClientContext clientContext) {
        simulateLatency();

        if (nameToId.isEmpty()) {
            log.warn("[EXT_TRACE] step=NAMED_AREA_RESOLVED conversationId={} segment={} namedAreaId=null (no named areas resolved at startup)",
                    clientContext.conversationId(), clientContext.segment());
            return null;
        }

        List<String> available = nameToId.keySet().stream().sorted().toList();
        String chosen = available.get(random.nextInt(available.size()));
        String namedAreaId = nameToId.get(chosen);

        log.info("[EXT_TRACE] step=NAMED_AREA_RESOLVED conversationId={} segment={} chosenName={} namedAreaId={}",
                clientContext.conversationId(), clientContext.segment(), chosen, namedAreaId);

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
