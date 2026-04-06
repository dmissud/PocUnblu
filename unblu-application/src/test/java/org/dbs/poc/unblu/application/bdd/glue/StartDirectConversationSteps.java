package org.dbs.poc.unblu.application.bdd.glue;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.dbs.poc.unblu.application.bdd.stub.StubUnbluPort;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.application.port.in.command.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class StartDirectConversationSteps {

    @Autowired private StartDirectConversationUseCase startDirectConversationUseCase;
    @Autowired private StubUnbluPort stubUnbluPort;
    @Autowired private ScenarioState state;

    @When("je démarre une conversation directe entre le visiteur {string} et l'agent {string} avec le sujet {string}")
    public void jeDemarreUneConversationDirecte(String visitorSourceId, String agentSourceId, String subject) {
        try {
            var command = new StartDirectConversationCommand(
                    visitorSourceId + "-id", visitorSourceId,
                    agentSourceId + "-id", agentSourceId,
                    subject);
            state.directConversationInfo = startDirectConversationUseCase.startDirectConversation(command);
        } catch (ChatAccessDeniedException e) {
            state.capturedException = e;
        }
    }

    @Then("la conversation directe est créée avec succès")
    public void laConversationDirecteEstCreee() {
        assertThat(state.capturedException)
                .as("Aucune exception ne devrait avoir été levée")
                .isNull();
        assertThat(state.directConversationInfo).isNotNull();
        assertThat(state.directConversationInfo.unbluConversationId())
                .isNotNull()
                .isNotEqualTo("OFFLINE-PENDING");
    }

    @And("l'identifiant de la conversation directe est {string}")
    public void lIdentifiantDeLaConversationDirecteEst(String expectedId) {
        assertThat(state.directConversationInfo.unbluConversationId()).isEqualTo(expectedId);
    }

    @Then("une exception ChatAccessDeniedException est levée")
    public void uneExceptionChatAccessDeniedEstLevee() {
        assertThat(state.capturedException)
                .isNotNull()
                .isInstanceOf(ChatAccessDeniedException.class);
    }

    @And("aucune conversation directe n'est créée dans Unblu")
    public void aucuneConversationDirecteCreee() {
        assertThat(stubUnbluPort.allSummariesPosted()).isEmpty();
        assertThat(state.directConversationInfo).isNull();
    }
}
