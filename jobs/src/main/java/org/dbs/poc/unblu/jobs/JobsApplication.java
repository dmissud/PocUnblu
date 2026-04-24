package org.dbs.poc.unblu.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application Spring Boot pour les tâches planifiées de statistiques.
 *
 * <p>Ce module exécute des jobs Quartz périodiques pour générer des statistiques
 * sur les conversations Unblu stockées en base de données et les persister dans des fichiers.</p>
 *
 * <p>Architecture hexagonale - utilise les modules integration-* du Bloc 1.</p>
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "org.dbs.poc.unblu.jobs",
        "org.dbs.poc.unblu.integration"
})
@EnableJpaRepositories(basePackages = "org.dbs.poc.unblu.integration.infrastructure.persistence.repository")
@EntityScan(basePackages = "org.dbs.poc.unblu.integration.infrastructure.persistence.entity")
public class JobsApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobsApplication.class, args);
    }
}

// Made with Bob
