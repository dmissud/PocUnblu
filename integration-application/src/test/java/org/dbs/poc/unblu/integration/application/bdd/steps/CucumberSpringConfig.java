package org.dbs.poc.unblu.integration.application.bdd.steps;

import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;
import org.dbs.poc.unblu.integration.domain.port.out.StatisticsPersistencePort;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(classes = CucumberSpringConfig.TestApplication.class)
public class CucumberSpringConfig {

    @MockitoBean
    ConversationHistoryRepository conversationHistoryRepository;

    @MockitoBean
    IntegrationUnbluPort integrationUnbluPort;

    @MockitoBean
    StatisticsPersistencePort statisticsPersistencePort;

    @SpringBootApplication(scanBasePackages = "org.dbs.poc.unblu.integration.application")
    static class TestApplication {
    }
}
