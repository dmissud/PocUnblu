package org.dbs.poc.unblu.livekit.service;

import org.dbs.poc.unblu.integration.domain.model.UnbluConversationInfo;

/**
 * Cas d'utilisation : démarrage d'une conversation simplifiée pour LiveKit.
 */
public interface StartLiveKitConversationUseCase {
    /**
     * Démarre une conversation Unblu avec la zone nommée CH_HB_PREMIUM.
     *
     * @param personId Identifiant Unblu (UUID) de la personne sélectionnée dans l'IHM
     * @return Informations sur la conversation créée
     */
    UnbluConversationInfo startConversation(String personId);
}
