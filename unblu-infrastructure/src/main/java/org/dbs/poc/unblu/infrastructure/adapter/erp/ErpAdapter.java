package org.dbs.poc.unblu.infrastructure.adapter.erp;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.port.out.ErpPort;
import org.springframework.stereotype.Component;

/**
 * Adaptateur secondaire implémentant {@link ErpPort} en Java pur.
 * Simule un appel à l'ERP avec une logique mock intégrée et de la résilience.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpAdapter implements ErpPort {

    /**
     * Récupère le profil client depuis l'ERP.
     * Logique simulée : Segment VIP si l'ID commence par "VIP".
     *
     * @param clientId l'identifiant du client
     * @return le profil client simulé
     */
    @Override
    @CircuitBreaker(name = "erp")
    @Retry(name = "erp")
    public CustomerProfile getCustomerProfile(String clientId) {
        log.info("Appel ERP simulé pour clientId: {}", clientId);

        return new CustomerProfile(
                clientId,
                "Jean",
                "Dupont",
                clientId.startsWith("VIP") ? "VIP" : "STANDARD",
                false);
    }
}
