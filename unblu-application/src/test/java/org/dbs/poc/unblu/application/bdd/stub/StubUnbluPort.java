package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.model.*;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Stub de {@link UnbluPort} configurable par scénario.
 *
 * <p>Permet de :
 * <ul>
 *   <li>Configurer l'identifiant de conversation retourné par {@code createConversation}</li>
 *   <li>Simuler l'indisponibilité d'Unblu (retourne OFFLINE-PENDING)</li>
 *   <li>Enregistrer les appels à {@code addSummaryToConversation} pour assertion</li>
 *   <li>Fournir des participants et messages pré-configurés pour l'enrichissement</li>
 * </ul>
 */
@Component("unbluApiAdapter")
public class StubUnbluPort implements UnbluPort {

    private String nextConversationId = "conv-stub-default";
    private boolean offline = false;

    private final List<String> summariesPosted = new ArrayList<>();
    private final List<UnbluParticipantData> participants = new ArrayList<>();
    private final List<UnbluMessageData> messages = new ArrayList<>();

    // --- Configuration ---

    public void willReturnConversationId(String conversationId) {
        this.nextConversationId = conversationId;
        this.offline = false;
    }

    public void willBeOffline() {
        this.offline = true;
    }

    public void addParticipant(UnbluParticipantData participant) {
        participants.add(participant);
    }

    public void addMessage(UnbluMessageData message) {
        messages.add(message);
    }

    public void reset() {
        nextConversationId = "conv-stub-default";
        offline = false;
        summariesPosted.clear();
        participants.clear();
        messages.clear();
    }

    // --- Assertions ---

    public boolean summaryWasPostedTo(String conversationId) {
        return summariesPosted.contains(conversationId);
    }

    public List<String> allSummariesPosted() {
        return Collections.unmodifiableList(summariesPosted);
    }

    // --- UnbluPort implementation ---

    @Override
    public UnbluConversationInfo createConversation(ConversationCreationRequest request) {
        if (offline) {
            return new UnbluConversationInfo("OFFLINE-PENDING", null);
        }
        return new UnbluConversationInfo(nextConversationId, "https://stub.unblu/join/" + nextConversationId);
    }

    @Override
    public void addSummaryToConversation(String conversationId, String summary) {
        summariesPosted.add(conversationId);
    }

    @Override
    public UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
        if (offline) {
            return new UnbluConversationInfo("OFFLINE-PENDING", null);
        }
        return new UnbluConversationInfo(nextConversationId, "https://stub.unblu/join/" + nextConversationId);
    }

    @Override
    public List<UnbluParticipantData> fetchConversationParticipants(String conversationId) {
        return List.copyOf(participants);
    }

    @Override
    public List<UnbluMessageData> fetchConversationMessages(String conversationId) {
        return List.copyOf(messages);
    }

    @Override
    public List<PersonInfo> searchPersons(String sourceId, PersonSource personSource) {
        return List.of();
    }

    @Override
    public PersonInfo getPersonBySource(PersonSource personSource, String sourceId) {
        return new PersonInfo(sourceId, sourceId, sourceId, null, null, null, personSource.name());
    }

    @Override
    public List<TeamInfo> searchTeams() {
        return List.of();
    }

    @Override
    public List<NamedAreaInfo> searchNamedAreas() {
        return List.of();
    }

    @Override
    public List<PersonInfo> searchAgentsByNamedArea(String namedAreaId) {
        return List.of();
    }

    @Override
    public String createBot(String name, String description) {
        return "bot-stub-id";
    }

    @Override
    public List<UnbluConversationSummary> listAllConversations() {
        return List.of();
    }

    @Override
    public List<UnbluConversationSummary> searchConversationsByState(String state) {
        return List.of();
    }
}
