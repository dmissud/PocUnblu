package org.dbs.poc.unblu.jobs.quartz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.application.port.in.GenerateConversationStatisticsUseCase;
import org.dbs.poc.unblu.integration.domain.model.statistics.ConversationStatistics;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * Job Quartz pour générer les statistiques de conversations périodiquement.
 *
 * <p>Exécuté toutes les 30 minutes pour calculer et persister les statistiques
 * sur les conversations Unblu.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationStatisticsJob implements Job {

    private final GenerateConversationStatisticsUseCase generateStatisticsUseCase;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("=== Démarrage du job de génération de statistiques ===");

        try {
            ConversationStatistics statistics = generateStatisticsUseCase.generate();

            log.info("Job terminé avec succès. Statistiques générées: {} conversations totales",
                    statistics.totalConversations());

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution du job de statistiques", e);
            throw new JobExecutionException("Échec de la génération des statistiques", e);
        }
    }
}

// Made with Bob
