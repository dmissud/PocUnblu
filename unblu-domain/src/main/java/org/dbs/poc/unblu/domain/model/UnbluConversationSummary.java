package org.dbs.poc.unblu.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Résumé immuable d'une conversation existante dans Unblu.
 *
 * <p>Retourné lors d'un scan complet de la plateforme, ce value object encapsule
 * les informations essentielles d'une conversation : identité, sujet, état et cycle de vie.
 *
 * <p>L'état métier "terminée" est dérivé de la présence d'une {@code endedAt},
 * sans dépendre d'un état string externe.
 */
public record UnbluConversationSummary(
        String id,
        String topic,
        String state,
        Instant createdAt,
        Instant endedAt) {

    public UnbluConversationSummary {
        Objects.requireNonNull(id, "Conversation id cannot be null");
        Objects.requireNonNull(state, "Conversation state cannot be null");
        Objects.requireNonNull(createdAt, "Conversation createdAt cannot be null");
    }

    /**
     * Indique si la conversation est terminée, c'est-à-dire si un horodatage de fin est présent.
     */
    public boolean isEnded() {
        return endedAt != null;
    }
}