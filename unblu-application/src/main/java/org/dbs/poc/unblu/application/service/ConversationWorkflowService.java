package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.*;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Spring service encapsulating the business steps shared across conversation workflows.
 * Routes delegate to this service — they only orchestrate the flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationWorkflowService {

    private final UnbluPort unbluPort;


    /**
     * Finds a VIRTUAL person by sourceId.
     *
     * @throws IllegalArgumentException if the person is not found
     */
    public PersonInfo findVirtualPerson(String sourceId) {
        List<PersonInfo> persons = unbluPort.searchPersons(sourceId, PersonSource.VIRTUAL);
        if (persons == null || persons.isEmpty()) {
            throw new IllegalArgumentException("Personne VIRTUAL introuvable pour sourceId: " + sourceId);
        }
        return persons.getFirst();
    }

    /**
     * Finds a USER_DB agent by sourceId.
     *
     * @throws IllegalArgumentException if the agent is not found
     */
    public PersonInfo findAgentPerson(String sourceId) {
        List<PersonInfo> persons = unbluPort.searchPersons(sourceId, PersonSource.USER_DB);
        if (persons == null || persons.isEmpty()) {
            throw new IllegalArgumentException("Agent introuvable pour sourceId: " + sourceId);
        }
        return persons.getFirst();
    }

    /**
     * Validates that the routing decision authorizes chat access.
     *
     * @throws ChatAccessDeniedException if access is denied
     */
    public void validateAuthorization(ChatRoutingDecision decision) {
        if (!decision.isAuthorized()) {
            throw new ChatAccessDeniedException("Accès refusé", decision.routingReason());
        }
    }

    /**
     * Creates a direct conversation between a virtual visitor and an agent.
     */
    public UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
        return unbluPort.createDirectConversation(virtualPerson, agentPerson, subject);
    }
}
