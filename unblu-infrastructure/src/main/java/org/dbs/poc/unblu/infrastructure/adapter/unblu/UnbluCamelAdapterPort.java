package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;
import org.dbs.poc.unblu.domain.model.*;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptateur secondaire implémentant {@link UnbluPort} en déléguant chaque opération
 * à une route Camel résiliente via {@link ProducerTemplate}.
 * Joue le rôle de pont entre la couche application et l'infrastructure Unblu.
 */
@Component
@RequiredArgsConstructor
public class UnbluCamelAdapterPort implements UnbluPort {

    private final ProducerTemplate producerTemplate;
    private final UnbluBotService unbluBotService;
    private final UnbluPersonService unbluPersonService;

    /**
     * {@inheritDoc}
     */
    @Override
    public UnbluConversationInfo createConversation(ConversationContext context) {
        ConversationOrchestrationState state = new ConversationOrchestrationState(context);
        ConversationOrchestrationState result = producerTemplate.requestBody(
                UnbluResilientRoute.DIRECT_UNBLU_ADAPTER_RESILIENT, state, ConversationOrchestrationState.class);
        return new UnbluConversationInfo(result.unbluConversationId(), result.unbluJoinUrl());
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<PersonInfo> searchPersons(String sourceId, PersonSource personSource) {
        return producerTemplate.requestBody(UnbluResilientRoute.DIRECT_UNBLU_SEARCH_PERSONS_RESILIENT,
                new PersonSearchRequest(sourceId, personSource), List.class);
    }

    /** {@inheritDoc} */
    @Override
    public UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
        DirectConversationRequest req = new DirectConversationRequest(virtualPerson, agentPerson, subject);
        com.unblu.webapi.model.v4.ConversationData result =
                producerTemplate.requestBody(UnbluResilientRoute.DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION_RESILIENT, req,
                        com.unblu.webapi.model.v4.ConversationData.class);
        return new UnbluConversationInfo(result.getId(), result.getId());
    }

    /** {@inheritDoc} */
    @Override
    public void addSummaryToConversation(String conversationId, String summary) {
        producerTemplate.sendBody(UnbluResilientRoute.DIRECT_UNBLU_ADD_SUMMARY_RESILIENT, new SummaryRequest(conversationId, summary));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersonInfo getPersonBySource(PersonSource personSource, String sourceId) {
        com.unblu.webapi.model.v4.EPersonSource ePersonSource =
                com.unblu.webapi.model.v4.EPersonSource.valueOf(personSource.name());
        com.unblu.webapi.model.v4.PersonData personData =
                unbluPersonService.getPersonBySource(ePersonSource, sourceId);
        return unbluPersonService.toPersonInfo(personData);
    }

    /** {@inheritDoc} */
    @Override
    public String createBot(String name, String description) {
        return unbluBotService.createBot(name, description);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<TeamInfo> searchTeams() {
        return producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_TEAMS, null, List.class);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<org.dbs.poc.unblu.domain.model.NamedAreaInfo> searchNamedAreas() {
        return producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_NAMED_AREAS, null, List.class);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<PersonInfo> searchAgentsByNamedArea(String namedAreaId) {
        return producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_AGENTS_BY_NAMED_AREA, namedAreaId, List.class);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public List<UnbluConversationSummary> listAllConversations() {
        return producerTemplate.requestBody(
                UnbluResilientRoute.DIRECT_UNBLU_LIST_CONVERSATIONS_RESILIENT, null, List.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<UnbluMessageData> fetchConversationMessages(String conversationId) {
        return producerTemplate.requestBody(
                UnbluResilientRoute.DIRECT_UNBLU_FETCH_MESSAGES_RESILIENT, conversationId, List.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<UnbluParticipantData> fetchConversationParticipants(String conversationId) {
        return producerTemplate.requestBody(
                UnbluResilientRoute.DIRECT_UNBLU_FETCH_PARTICIPANTS_RESILIENT, conversationId, List.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<UnbluConversationSummary> searchConversationsByState(String state) {
        return producerTemplate.requestBody(
                UnbluResilientRoute.DIRECT_UNBLU_SEARCH_CONVERSATIONS_BY_STATE_RESILIENT, state, List.class);
    }

    /**
     * Requête de recherche de personnes transmise à la route Camel Unblu.
     *
     * @param sourceId     identifiant source (peut être {@code null})
     * @param personSource type de source ({@code VIRTUAL} ou {@code USER_DB}, peut être {@code null})
     */
    public record PersonSearchRequest(String sourceId, PersonSource personSource) {
    }

    /**
     * Requête de création de conversation directe transmise à la route Camel Unblu.
     *
     * @param virtualPerson la personne virtuelle (visiteur)
     * @param agentPerson   l'agent assigné
     * @param subject       le sujet de la conversation
     */
    public record DirectConversationRequest(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
    }

    /**
     * Requête d'ajout de résumé transmise à la route Camel Unblu.
     *
     * @param conversationId l'identifiant de la conversation cible
     * @param summary        le texte du résumé
     */
    public record SummaryRequest(String conversationId, String summary) {
    }
}
