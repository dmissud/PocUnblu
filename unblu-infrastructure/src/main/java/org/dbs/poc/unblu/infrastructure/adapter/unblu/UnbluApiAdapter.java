package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.model.v4.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.*;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Adaptateur d'infrastructure pour l'API Unblu.
 * Implémente {@link UnbluPort} en appelant directement les services techniques
 * et en appliquant la résilience via Resilience4j.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class UnbluApiAdapter implements UnbluPort {

    private final UnbluConversationService conversationService;
    private final UnbluPersonService personService;
    private final UnbluService unbluService;
    private final UnbluBotService unbluBotService;

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackCreateConversation")
    @Retry(name = "unblu")
    public UnbluConversationInfo createConversation(ConversationCreationRequest request) {
        var creationData = new ConversationCreationData();
        creationData.setTopic(request.topic());
        creationData.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);
        creationData.setVisitorData(request.visitorData());
        if (request.namedAreaId() != null) {
            var recipient = new ConversationCreationRecipientData();
            recipient.setType(EConversationRecipientType.NAMED_AREA);
            recipient.setId(request.namedAreaId());
            creationData.setRecipient(recipient);
        }
        creationData.setConversationTemplateId(request.conversationTemplate());

        if (request.participants() != null) {
            request.participants().forEach(p -> {
                var participant = new ConversationCreationParticipantData();
                participant.setPersonId(p.personId());
                participant.setParticipationType(EConversationRealParticipationType.valueOf(p.participationType()));
                creationData.addParticipantsItem(participant);
            });
        }

        var data = conversationService.createConversation(creationData);
        String joinUrl = conversationService.generateVisitorJoinUrl(data.getId());
        return new UnbluConversationInfo(data.getId(), joinUrl);
    }

    private UnbluConversationInfo fallbackCreateConversation(ConversationCreationRequest request, Throwable t) {
        log.warn("Unblu indisponible pour createConversation : {}", t.getMessage());
        return new UnbluConversationInfo("OFFLINE-PENDING", null);
    }

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackSearchPersons")
    public List<PersonInfo> searchPersons(String sourceId, PersonSource personSource) {
        return personService.searchPersons(sourceId, personSource);
    }

    private List<PersonInfo> fallbackSearchPersons(String sourceId, PersonSource personSource, Throwable t) {
        log.warn("Unblu indisponible pour searchPersons : {}", t.getMessage());
        return Collections.emptyList();
    }

    @Override
    public PersonInfo getPersonBySource(PersonSource personSource, String sourceId) {
        com.unblu.webapi.model.v4.EPersonSource ePersonSource =
                com.unblu.webapi.model.v4.EPersonSource.valueOf(personSource.name());
        var data = personService.getPersonBySource(ePersonSource, sourceId);
        return personService.toPersonInfo(data);
    }

    @Override
    public List<TeamInfo> searchTeams() {
        return unbluService.searchTeams();
    }

    @Override
    public List<NamedAreaInfo> searchNamedAreas() {
        return unbluService.searchNamedAreas();
    }

    @Override
    public List<PersonInfo> searchAgentsByNamedArea(String namedAreaId) {
        return personService.searchAgentsByNamedArea(namedAreaId);
    }

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackCreateDirectConversation")
    public UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
        var data = conversationService.createDirectConversation(virtualPerson, agentPerson, subject);
        return new UnbluConversationInfo(data.getId(), data.getId());
    }

    private UnbluConversationInfo fallbackCreateDirectConversation(PersonInfo v, PersonInfo a, String s, Throwable t) {
        log.warn("Unblu indisponible pour createDirectConversation : {}", t.getMessage());
        return new UnbluConversationInfo("OFFLINE-PENDING", null);
    }

    @Override
    @CircuitBreaker(name = "unblu")
    public void addSummaryToConversation(String conversationId, String summary) {
        conversationService.addSummaryToConversation(conversationId, summary);
    }

    @Override
    public String createBot(String name, String description) {
        return unbluBotService.createBot(name, description);
    }

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackListAllConversations")
    public List<UnbluConversationSummary> listAllConversations() {
        return conversationService.listAllConversations();
    }

    private List<UnbluConversationSummary> fallbackListAllConversations(Throwable t) {
        log.warn("Unblu indisponible pour listAllConversations : {}", t.getMessage());
        return Collections.emptyList();
    }

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackFetchMessages")
    public List<UnbluMessageData> fetchConversationMessages(String conversationId) {
        return conversationService.fetchMessages(conversationId);
    }

    private List<UnbluMessageData> fallbackFetchMessages(String conversationId, Throwable t) {
        log.warn("Unblu indisponible pour fetchMessages : {}", t.getMessage());
        return Collections.emptyList();
    }

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackFetchParticipants")
    public List<UnbluParticipantData> fetchConversationParticipants(String conversationId) {
        return conversationService.fetchParticipants(conversationId);
    }

    private List<UnbluParticipantData> fallbackFetchParticipants(String conversationId, Throwable t) {
        log.warn("Unblu indisponible pour fetchParticipants : {}", t.getMessage());
        return Collections.emptyList();
    }

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackSearchByState")
    public List<UnbluConversationSummary> searchConversationsByState(String state) {
        return conversationService.searchConversationsByState(state);
    }

    private List<UnbluConversationSummary> fallbackSearchByState(String state, Throwable t) {
        log.warn("Unblu indisponible pour searchByState : {}", t.getMessage());
        return Collections.emptyList();
    }

    @Override
    public List<BotInfo> listBots() {
        return unbluBotService.listBots();
    }
}
