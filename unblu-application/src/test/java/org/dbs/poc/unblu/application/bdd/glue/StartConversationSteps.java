package org.dbs.poc.unblu.application.bdd.glue;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.dbs.poc.unblu.application.bdd.stub.StubErpPort;
import org.dbs.poc.unblu.application.bdd.stub.StubUnbluPort;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.application.port.in.command.StartConversationCommand;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class StartConversationSteps {

    @Autowired private StartConversationUseCase startConversationUseCase;
    @Autowired private StubErpPort stubErpPort;
    @Autowired private StubUnbluPort stubUnbluPort;
    @Autowired private ScenarioState state;

    @Given("un client avec l'identifiant {string} et le segment {string}")
    public void unClientAvecIdentifiantEtSegment(String clientId, String segment) {
        stubErpPort.registerProfile(clientId, segment);
    }

    @And("Unblu retournera l'identifiant de conversation {string}")
    public void unbluRetourneraIdentifiantConversation(String conversationId) {
        stubUnbluPort.willReturnConversationId(conversationId);
    }

    @And("Unblu est indisponible")
    public void unbluEstIndisponible() {
        stubUnbluPort.willBeOffline();
    }

    @When("je démarre une conversation pour le client {string} depuis l'origine {string} vers l'équipe {string}")
    public void jeDemarreUneConversation(String clientId, String origin, String teamId) {
        var command = new StartConversationCommand(clientId, "Sujet test", origin, teamId);
        state.orchestrationState = startConversationUseCase.startConversation(command);
    }

    @Then("la conversation est créée avec succès")
    public void laConversationEstCreeeAvecSucces() {
        assertThat(state.orchestrationState.isSuccess()).isTrue();
    }

    @Then("la conversation est en mode dégradé OFFLINE-PENDING")
    public void laConversationEstEnModeDegradé() {
        assertThat(state.orchestrationState.unbluConversationId()).isEqualTo("OFFLINE-PENDING");
        assertThat(state.orchestrationState.isSuccess()).isFalse();
    }

    @And("l'identifiant Unblu de la conversation est {string}")
    public void lIdentifiantUnbluDeLaConversationEst(String expectedId) {
        assertThat(state.orchestrationState.unbluConversationId()).isEqualTo(expectedId);
    }

    @And("un résumé a été posté dans la conversation {string}")
    public void unResumeAEtePoste(String conversationId) {
        assertThat(stubUnbluPort.summaryWasPostedTo(conversationId))
                .as("Un résumé devrait avoir été posté dans la conversation %s", conversationId)
                .isTrue();
    }

    @And("aucun résumé n'a été posté")
    public void aucunResumeNAEtePoste() {
        assertThat(stubUnbluPort.allSummariesPosted()).isEmpty();
    }

    @And("la décision de routage indique que l'accès est refusé")
    public void laDecisionDeRoutageIndiqueRefus() {
        assertThat(state.orchestrationState.context().routingDecision().isAuthorized()).isFalse();
    }
}
