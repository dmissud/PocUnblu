package org.dbs.poc.unblu.livekit.service;

import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

/**
 * Cas d'utilisation : démarrage d'une conversation simplifiée pour LiveKit.
 */
public interface StartLiveKitConversationUseCase {
    /**
     * Démarre une conversation Unblu avec la zone nommée CH_HB_PREMIUM.
     *
     * @param clientId Identifiant du client issu de l'IHM
     * @return Informations sur la conversation créée
     */
    UnbluConversationInfo startConversation(String clientId);
}
