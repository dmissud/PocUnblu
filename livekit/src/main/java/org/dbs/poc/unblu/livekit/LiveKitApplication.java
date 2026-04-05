package org.dbs.poc.unblu.livekit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.dbs.poc.unblu.livekit",
        "org.dbs.poc.unblu.infrastructure",
        "org.dbs.poc.unblu.application",
        "org.dbs.poc.unblu.domain"
})
@EntityScan(basePackages = "org.dbs.poc.unblu.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "org.dbs.poc.unblu.infrastructure.persistence.repository")
public class LiveKitApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiveKitApplication.class, args);
    }
}
