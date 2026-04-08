package org.dbs.poc.unblu.application.bdd.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring minimale pour les tests BDD.
 *
 * <p>Charge uniquement :
 * <ul>
 *   <li>Les services use case de {@code unblu-application}</li>
 *   <li>Les stubs de ports secondaires (dans le package {@code bdd.stub})</li>
 * </ul>
 *
 * <p>Exclut explicitement JPA et DataSource — les stubs remplacent toute dépendance infra.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "org.dbs.poc.unblu.application.service",
        "org.dbs.poc.unblu.application.bdd.stub",
        "org.dbs.poc.unblu.application.bdd.glue"
})
public class BddTestConfig {
}
