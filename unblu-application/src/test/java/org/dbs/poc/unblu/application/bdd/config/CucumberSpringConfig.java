package org.dbs.poc.unblu.application.bdd.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Point d'entrée Cucumber-Spring.
 * Lie le contexte Spring aux scénarios Cucumber.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = BddTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CucumberSpringConfig {
}
