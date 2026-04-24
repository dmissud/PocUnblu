package org.dbs.poc.unblu.integration.application.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dbs.poc.unblu.integration.application.port.in.GenerateConversationStatisticsUseCase;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.statistics.ConversationStatistics;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.domain.port.out.StatisticsPersistencePort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service de génération de statistiques sur les conversations (Bloc 1 — Integration).
 *
 * <p>Calcule les métriques suivantes:
 * <ul>
 *   <li>Nombre de conversations par jour</li>
 *   <li>Moyenne hebdomadaire (7 derniers jours)</li>
 *   <li>Moyenne mensuelle (30 derniers jours)</li>
 *   <li>Moyenne totale</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationStatisticsService implements GenerateConversationStatisticsUseCase {

    private final ConversationHistoryRepository conversationHistoryRepository;
    private final StatisticsPersistencePort statisticsPersistencePort;

    @Override
    public ConversationStatistics generate() {
        log.info("Génération des statistiques de conversations...");

        List<ConversationHistory> allConversations = conversationHistoryRepository.findAll();

        if (allConversations.isEmpty()) {
            log.warn("Aucune conversation trouvée pour générer des statistiques");
            ConversationStatistics emptyStats = ConversationStatistics.empty(LocalDate.now());
            statisticsPersistencePort.save(emptyStats);
            return emptyStats;
        }

        // Calculer les conversations par jour
        Map<LocalDate, Long> conversationsPerDay = calculateConversationsPerDay(allConversations);

        // Trouver les dates min/max
        LocalDate firstDate = conversationsPerDay.keySet().stream().min(LocalDate::compareTo).orElse(null);
        LocalDate lastDate = conversationsPerDay.keySet().stream().max(LocalDate::compareTo).orElse(null);

        // Calculer les moyennes
        double weeklyAverage = calculateWeeklyAverage(conversationsPerDay);
        double monthlyAverage = calculateMonthlyAverage(conversationsPerDay);
        double overallAverage = calculateOverallAverage(conversationsPerDay);

        ConversationStatistics statistics = ConversationStatistics.builder()
                .generatedAt(LocalDate.now())
                .totalConversations((long) allConversations.size())
                .conversationsPerDay(conversationsPerDay)
                .weeklyAverage(weeklyAverage)
                .monthlyAverage(monthlyAverage)
                .overallAverage(overallAverage)
                .firstConversationDate(firstDate)
                .lastConversationDate(lastDate)
                .build();

        log.info("Statistiques générées: {} conversations totales, moyenne hebdo={}, moyenne mensuelle={}, moyenne totale={}",
                statistics.totalConversations(),
                String.format("%.2f", weeklyAverage),
                String.format("%.2f", monthlyAverage),
                String.format("%.2f", overallAverage));

        // Persister les statistiques
        statisticsPersistencePort.save(statistics);

        return statistics;
    }

    /**
     * Calcule le nombre de conversations par jour.
     */
    private Map<LocalDate, Long> calculateConversationsPerDay(List<ConversationHistory> conversations) {
        Map<LocalDate, Long> conversationsPerDay = new HashMap<>();

        for (ConversationHistory conversation : conversations) {
            if (conversation.startedAt() != null) {
                LocalDate date = conversation.startedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                conversationsPerDay.merge(date, 1L, Long::sum);
            }
        }

        return conversationsPerDay;
    }

    /**
     * Calcule la moyenne de conversations par jour sur les 7 derniers jours.
     */
    private double calculateWeeklyAverage(Map<LocalDate, Long> conversationsPerDay) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        long totalConversations = conversationsPerDay.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(weekAgo) && !entry.getKey().isAfter(today))
                .mapToLong(Map.Entry::getValue)
                .sum();

        return totalConversations / 7.0;
    }

    /**
     * Calcule la moyenne de conversations par jour sur les 30 derniers jours.
     */
    private double calculateMonthlyAverage(Map<LocalDate, Long> conversationsPerDay) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);

        long totalConversations = conversationsPerDay.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(monthAgo) && !entry.getKey().isAfter(today))
                .mapToLong(Map.Entry::getValue)
                .sum();

        return totalConversations / 30.0;
    }

    /**
     * Calcule la moyenne de conversations par jour sur toute la période.
     */
    private double calculateOverallAverage(Map<LocalDate, Long> conversationsPerDay) {
        if (conversationsPerDay.isEmpty()) {
            return 0.0;
        }

        LocalDate firstDate = conversationsPerDay.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate lastDate = conversationsPerDay.keySet().stream().max(LocalDate::compareTo).orElse(LocalDate.now());

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate) + 1;
        long totalConversations = conversationsPerDay.values().stream().mapToLong(Long::longValue).sum();

        return daysBetween > 0 ? (double) totalConversations / daysBetween : 0.0;
    }
}

// Made with Bob
