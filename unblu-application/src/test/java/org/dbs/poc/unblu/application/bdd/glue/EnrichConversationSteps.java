package org.dbs.poc.unblu.application.bdd.glue;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.dbs.poc.unblu.application.bdd.stub.InMemoryConversationHistoryRepository;
import org.dbs.poc.unblu.application.bdd.stub.StubUnbluPort;
import org.dbs.poc.unblu.application.port.in.EnrichConversationUseCase;
import org.dbs.poc.unblu.domain.model.UnbluMessageData;
import org.dbs.poc.unblu.domain.model.UnbluParticipantData;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrichConversationSteps {

    @Autowired private EnrichConversationUseCase enrichConversationUseCase;
    @Autowired private InMemoryConversationHistoryRepository historyRepository;
    @Autowired private StubUnbluPort stubUnbluPort;
    @Autowired private ScenarioState state;

    @Given("une conversation {string} existe en base avec le sujet {string}")
    public void uneConversationExisteEnBase(String conversationId, String topic) {
        historyRepository.seed(ConversationHistory.create(conversationId, topic, Instant.now()));
    }

    @Given("aucune conversation n'existe en base pour l'identifiant {string}")
    public void aucuneConversationExiste(String conversationId) {
        // le repo est vide par défaut après reset
    }

    @And("Unblu dispose des participants suivants pour cette conversation :")
    public void unbluDisposeDesParticipants(io.cucumber.datatable.DataTable dataTable) {
        dataTable.asMaps().forEach(row -> stubUnbluPort.addParticipant(
                new UnbluParticipantData(row.get("personId"), row.get("displayName"), row.get("personSource"))));
    }

    @And("Unblu dispose des messages suivants pour cette conversation :")
    public void unbluDisposeDesMessages(io.cucumber.datatable.DataTable dataTable) {
        dataTable.asMaps().forEach(row -> stubUnbluPort.addMessage(
                new UnbluMessageData(row.get("messageId"), row.get("text"), row.get("senderPersonId"), Instant.now())));
    }

    @When("j'enrichis la conversation {string}")
    public void jenrichisLaConversation(String conversationId) {
        try {
            state.enrichedHistory = enrichConversationUseCase.enrichOne(conversationId);
        } catch (Exception e) {
            state.capturedException = e;
        }
    }

    @Then("la conversation enrichie contient {int} participants")
    public void laConversationEnrichieContientParticipants(int expectedCount) {
        assertThat(state.enrichedHistory.participants()).hasSize(expectedCount);
    }

    @Then("la conversation enrichie contient {int} messages")
    public void laConversationEnrichieContientMessages(int expectedCount) {
        long messageCount = state.enrichedHistory.events().stream()
                .filter(e -> e.eventType() == org.dbs.poc.unblu.domain.model.history.ConversationEventHistory.EventType.MESSAGE)
                .count();
        assertThat(messageCount).isEqualTo(expectedCount);
    }

    @And("la conversation enrichie est persistée en base")
    public void laConversationEnrichieEstPersistee() {
        assertThat(historyRepository.findByConversationId(state.enrichedHistory.conversationId())).isPresent();
    }

    @Then("une exception IllegalArgumentException est levée avec le message {string}")
    public void uneExceptionIllegalArgumentEstLevee(String expectedMessage) {
        assertThat(state.capturedException)
                .isNotNull()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }
}
