package org.dbs.poc.unblu.jobs.config;

import org.dbs.poc.unblu.jobs.quartz.ConversationStatisticsJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Quartz pour la planification des jobs.
 *
 * <p>Configure le job de génération de statistiques pour s'exécuter
 * toutes les 30 minutes.</p>
 */
@Configuration
public class QuartzConfig {

    /**
     * Définit le job de génération de statistiques.
     */
    @Bean
    public JobDetail conversationStatisticsJobDetail() {
        return JobBuilder.newJob(ConversationStatisticsJob.class)
                .withIdentity("conversationStatisticsJob", "statistics")
                .withDescription("Génère les statistiques de conversations")
                .storeDurably()
                .build();
    }

    /**
     * Définit le trigger pour exécuter le job toutes les 30 minutes.
     */
    @Bean
    public Trigger conversationStatisticsJobTrigger(JobDetail conversationStatisticsJobDetail) {
        // Cron expression: toutes les 30 minutes (à 0 et 30 minutes de chaque heure)
        return TriggerBuilder.newTrigger()
                .forJob(conversationStatisticsJobDetail)
                .withIdentity("conversationStatisticsJobTrigger", "statistics")
                .withDescription("Déclenche le job de statistiques toutes les 30 minutes")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/30 * * * ?"))
                .build();
    }
}

// Made with Bob
