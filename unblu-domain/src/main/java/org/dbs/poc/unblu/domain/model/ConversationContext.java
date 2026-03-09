package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private String initialClientId;
    private String originApplication;
    
    // Données enrichies durant l'orchestration
    private CustomerProfile customerProfile;
    private ChatRoutingDecision routingDecision;
    
    // Résultat final Unblu
    private String unbluConversationId;
    private String unbluJoinUrl;
}
