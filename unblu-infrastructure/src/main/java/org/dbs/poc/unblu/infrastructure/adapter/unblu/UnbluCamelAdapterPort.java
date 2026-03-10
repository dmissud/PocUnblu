package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UnbluCamelAdapterPort implements UnbluPort {

    private final ProducerTemplate producerTemplate;

    @Override
    public UnbluConversationInfo createConversation(ConversationContext context) {
        ConversationContext result = producerTemplate.requestBody("direct:unblu-adapter-resilient", context, ConversationContext.class);
        return new UnbluConversationInfo(result.getUnbluConversationId(), result.getUnbluJoinUrl());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PersonInfo> searchPersons(String sourceId) {
        return producerTemplate.requestBody("direct:unblu-search-persons", sourceId, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TeamInfo> searchTeams() {
        return producerTemplate.requestBody("direct:unblu-search-teams", null, List.class);
    }
}
