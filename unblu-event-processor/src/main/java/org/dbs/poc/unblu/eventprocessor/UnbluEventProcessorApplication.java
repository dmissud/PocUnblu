package org.dbs.poc.unblu.eventprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "org.dbs.poc.unblu.eventprocessor",
        "org.dbs.poc.unblu.integration"
})
@EnableJpaRepositories(basePackages = "org.dbs.poc.unblu.integration.infrastructure.persistence.repository")
@EntityScan(basePackages = "org.dbs.poc.unblu.integration.infrastructure.persistence.entity")
public class UnbluEventProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnbluEventProcessorApplication.class, args);
    }
}
