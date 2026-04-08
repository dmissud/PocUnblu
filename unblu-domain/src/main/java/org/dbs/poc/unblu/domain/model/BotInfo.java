package org.dbs.poc.unblu.domain.model;

/**
 * Informations de supervision d'un bot Unblu.
 *
 * @param id               identifiant Unblu du bot
 * @param name             nom du bot
 * @param onboardingFilter filtre d'onboarding (VISITORS, ALL, NONE)
 * @param onboardingOrder  ordre de priorité pour l'onboarding
 * @param webhookStatus    statut du webhook (ACTIVE, INACTIVE)
 * @param webhookEndpoint  URL de l'endpoint webhook du bot
 */
public record BotInfo(
        String id,
        String name,
        String onboardingFilter,
        Integer onboardingOrder,
        String webhookStatus,
        String webhookEndpoint
) {
}
