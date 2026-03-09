package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile {
    private String customerId;
    private String firstName;
    private String lastName;
    private String customerSegment; // ex: "VIP", "STANDARD", "PRO"
    private boolean isKnown;
}
