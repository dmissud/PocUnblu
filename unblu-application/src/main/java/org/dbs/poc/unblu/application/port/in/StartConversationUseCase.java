package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;

/**
 * Cas d'utilisation principal : démarrage d'une conversation avec une équipe Unblu.
 * Orchestre le workflow complet : enrichissement ERP, règles métier et création Unblu.
 */
public interface StartConversationUseCase {
    /**
     * Orchestrates the startup of a new conversation (ERP -> Rules -> Unblu).
     * @param command The initial command
     * @return The orchestration state holding the Unblu conversation result
     */
    ConversationOrchestrationState startConversation(StartConversationCommand command);
}
