package org.dbs.poc.unblu.integration.application.service;

import org.dbs.poc.unblu.integration.application.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.integration.domain.model.history.*;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryQueryServiceTest {

    @Mock ConversationHistoryRepository conversationHistoryRepository;
    @InjectMocks ConversationHistoryQueryService service;

    @Test
    void listConversations_delegatesToRepository() {
        // GIVEN
        var history = ConversationHistory.create("conv-1", "Topic", Instant.now());
        var page = new ConversationHistoryPage(List.of(history), 1, 0, 10, 1);
        when(conversationHistoryRepository.findPage(0, 10, ConversationSortField.CREATED_AT, ConversationSortDirection.DESC))
                .thenReturn(page);

        // WHEN
        var result = service.listConversations(ListConversationHistoryQuery.of(0, 10, "CREATED_AT", "DESC"));

        // THEN
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void getByConversationId_returnsEmpty_whenNotFound() {
        when(conversationHistoryRepository.findByConversationId("unknown")).thenReturn(Optional.empty());
        assertThat(service.getByConversationId("unknown")).isEmpty();
    }

    @Test
    void getByConversationId_returnsHistory_whenFound() {
        var history = ConversationHistory.create("conv-99", "Topic 99", Instant.now());
        when(conversationHistoryRepository.findByConversationId("conv-99")).thenReturn(Optional.of(history));
        assertThat(service.getByConversationId("conv-99")).isPresent();
    }
}
