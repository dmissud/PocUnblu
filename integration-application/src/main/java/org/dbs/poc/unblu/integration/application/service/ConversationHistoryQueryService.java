package org.dbs.poc.unblu.integration.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.application.port.in.GetConversationHistoryUseCase;
import org.dbs.poc.unblu.integration.application.port.in.ListConversationHistoryUseCase;
import org.dbs.poc.unblu.integration.application.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryQueryService
        implements ListConversationHistoryUseCase, GetConversationHistoryUseCase {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public ConversationHistoryPage listConversations(ListConversationHistoryQuery query) {
        return conversationHistoryRepository.findPage(query.page(), query.size(), query.sortField(), query.sortDirection());
    }

    @Override
    public Optional<ConversationHistory> getByConversationId(String conversationId) {
        return conversationHistoryRepository.findByConversationId(conversationId);
    }
}
