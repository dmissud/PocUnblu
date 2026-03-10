package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnbluCamelAdapterPort implements UnbluPort {

    private final ProducerTemplate producerTemplate;

    @Override
    public UnbluConversationInfo createConversation(ConversationContext context) {
        ConversationContext result = producerTemplate.requestBody("direct:unblu-adapter-resilient", context, ConversationContext.class);
        return new UnbluConversationInfo(result.getUnbluConversationId(), result.getUnbluJoinUrl());
    }
}
