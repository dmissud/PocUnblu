package org.dbs.poc.unblu.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "org.dbs.poc.unblu.engine",
        "org.dbs.poc.unblu.integration"
})
@EnableJpaRepositories(basePackages = "org.dbs.poc.unblu.integration.infrastructure.persistence.repository")
@EntityScan(basePackages = "org.dbs.poc.unblu.integration.infrastructure.persistence.entity")
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }
}
