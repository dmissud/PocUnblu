package org.dbs.poc.unblu.domain.port.out;

import org.dbs.poc.unblu.domain.model.CustomerProfile;

/**
 * Port secondaire vers le système ERP.
 * Permet de récupérer le profil client à partir de son identifiant.
 */
public interface ErpPort {
    /**
     * Retrieves the customer profile from the ERP system.
     * @param clientId The ID of the client to retrieve
     * @return The customer profile
     */
    CustomerProfile getCustomerProfile(String clientId);
}
