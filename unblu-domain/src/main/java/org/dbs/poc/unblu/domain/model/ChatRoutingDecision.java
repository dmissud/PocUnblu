package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoutingDecision {
    private boolean isAuthorized;
    private String unbluAssignedGroupId; // ex: "vip_advisors_group"
    private String routingReason;
}
