package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.port.out.ErpPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub de {@link ErpPort} configurable par scénario.
 * Les profils sont enregistrés explicitement dans les steps Given.
 * Tout clientId non enregistré retourne un profil STANDARD par défaut.
 */
@Component
public class StubErpPort implements ErpPort {

    private final Map<String, CustomerProfile> profiles = new HashMap<>();

    public void registerProfile(String clientId, String segment) {
        profiles.put(clientId, new CustomerProfile(clientId, "Prénom", "Nom", segment, true));
    }

    public void reset() {
        profiles.clear();
    }

    @Override
    public CustomerProfile getCustomerProfile(String clientId) {
        return profiles.getOrDefault(clientId,
                new CustomerProfile(clientId, "Prénom", "Nom", "STANDARD", false));
    }
}
