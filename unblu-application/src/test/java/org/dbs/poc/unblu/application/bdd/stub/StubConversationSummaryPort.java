package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.port.out.ConversationSummaryPort;
import org.springframework.stereotype.Component;

/**
 * Stub de {@link ConversationSummaryPort}.
 * Retourne un résumé fixe et déterministe pour les assertions de test.
 */
@Component
public class StubConversationSummaryPort implements ConversationSummaryPort {

    public static final String FIXED_SUMMARY = "Résumé généré par le stub de test.";

    public void reset() {
        // stateless
    }

    @Override
    public String generateSummary(String conversationId) {
        return FIXED_SUMMARY;
    }
}
