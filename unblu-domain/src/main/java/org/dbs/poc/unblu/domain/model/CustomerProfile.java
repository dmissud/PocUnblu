package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Profil du client issu de l'ERP.
 */
public record CustomerProfile(
        String customerId,
        String firstName,
        String lastName,
        String customerSegment,
        boolean isKnown
) {
    public CustomerProfile {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        Objects.requireNonNull(customerSegment, "customerSegment cannot be null");
    }

    /**
     * Vérifie si le client est "BANNED" d'après son segment.
     */
    public boolean isBanned() {
        return "BANNED".equalsIgnoreCase(customerSegment);
    }
}
