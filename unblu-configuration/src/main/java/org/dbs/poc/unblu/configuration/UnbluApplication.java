package org.dbs.poc.unblu.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.dbs.poc.unblu")
public class UnbluApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnbluApplication.class, args);
    }

}
