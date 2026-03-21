package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UnbluCamelAdapterPort implements UnbluPort {

    private final ProducerTemplate producerTemplate;
    private final UnbluService unbluService;

    @Override
    public UnbluConversationInfo createConversation(ConversationContext context) {
        ConversationContext result = producerTemplate.requestBody(UnbluResilientRoute.DIRECT_UNBLU_ADAPTER_RESILIENT, context, ConversationContext.class);
        return new UnbluConversationInfo(result.getUnbluConversationId(), result.getUnbluJoinUrl());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PersonInfo> searchPersons(String sourceId, PersonSource personSource) {
        return producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_PERSONS,
                new PersonSearchRequest(sourceId, personSource), List.class);
    }

    public record PersonSearchRequest(String sourceId, PersonSource personSource) {}

    @Override
    public UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
        DirectConversationRequest req = new DirectConversationRequest(virtualPerson, agentPerson, subject);
        com.unblu.webapi.model.v4.ConversationData result =
                producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION, req,
                        com.unblu.webapi.model.v4.ConversationData.class);
        return new UnbluConversationInfo(result.getId(), result.getId());
    }

    public record DirectConversationRequest(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {}

    @Override
    public void addSummaryToConversation(String conversationId, String summary) {
        producerTemplate.sendBody(UnbluCamelAdapter.DIRECT_UNBLU_ADD_SUMMARY, new SummaryRequest(conversationId, summary));
    }

    public record SummaryRequest(String conversationId, String summary) {}

    @Override
    public String createBot(String name, String description) {
        return unbluService.createBot(name, description);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TeamInfo> searchTeams() {
        return producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_TEAMS, null, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<org.dbs.poc.unblu.domain.model.NamedAreaInfo> searchNamedAreas() {
        return producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_NAMED_AREAS, null, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PersonInfo> searchAgentsByNamedArea(String namedAreaId) {
        return producerTemplate.requestBody(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_AGENTS_BY_NAMED_AREA, namedAreaId, List.class);
    }
}
