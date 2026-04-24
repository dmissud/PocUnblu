package org.dbs.poc.unblu.integration.domain.model.statistics;

import java.time.LocalDate;
import java.util.Map;

import lombok.Builder;

/**
 * Statistiques agrégées sur les conversations (Bloc 1 — Integration).
 *
 * <p>Représente les métriques calculées pour une période donnée, incluant
 * le nombre de conversations par jour et les moyennes hebdomadaires, mensuelles et totales.</p>
 */
@Builder
public record ConversationStatistics(
        /**
         * Date de génération des statistiques
         */
        LocalDate generatedAt,

        /**
         * Nombre total de conversations dans l'historique
         */
        long totalConversations,

        /**
         * Nombre de conversations par jour (clé: date, valeur: nombre de conversations)
         */
        Map<LocalDate, Long> conversationsPerDay,

        /**
         * Moyenne de conversations par jour sur la dernière semaine
         */
        double weeklyAverage,

        /**
         * Moyenne de conversations par jour sur le dernier mois
         */
        double monthlyAverage,

        /**
         * Moyenne de conversations par jour sur toute la période
         */
        double overallAverage,

        /**
         * Date de la première conversation dans l'historique
         */
        LocalDate firstConversationDate,

        /**
         * Date de la dernière conversation dans l'historique
         */
        LocalDate lastConversationDate
) {
    /**
     * Crée des statistiques vides.
     */
    public static ConversationStatistics empty(LocalDate generatedAt) {
        return ConversationStatistics.builder()
                .generatedAt(generatedAt)
                .totalConversations(0L)
                .conversationsPerDay(Map.of())
                .weeklyAverage(0.0)
                .monthlyAverage(0.0)
                .overallAverage(0.0)
                .build();
    }
}

// Made with Bob
