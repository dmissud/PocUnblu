package org.dbs.poc.unblu.integration.application.bdd.steps;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;

@CucumberContextConfiguration
@SpringBootTest(classes = CucumberSpringConfig.TestApplication.class)
public class CucumberSpringConfig {
    @SpringBootApplication(scanBasePackages = "org.dbs.poc.unblu.integration.application")
    static class TestApplication {
        @MockBean
        ConversationHistoryRepository conversationHistoryRepository;
        
        @MockBean
        IntegrationUnbluPort integrationUnbluPort;
    }
}
