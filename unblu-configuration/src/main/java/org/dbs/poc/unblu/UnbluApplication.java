package org.dbs.poc.unblu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the PocUnblu application.
 *
 * <p>Scans all modules under {@code org.dbs.poc.unblu} and delegates
 * component assembly to the Spring container.
 */
@SpringBootApplication(scanBasePackages = "org.dbs.poc.unblu")
public class UnbluApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnbluApplication.class, args);
    }

}
