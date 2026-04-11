package org.dbs.poc.unblu.integration.application.service;

import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.webhook.ConversationData;
import org.dbs.poc.unblu.integration.domain.model.webhook.UnbluWebhookPayload;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryServiceTest {

    @Mock ConversationHistoryRepository conversationHistoryRepository;
    @InjectMocks ConversationHistoryService service;

    @Test
    void onConversationCreated_savesHistory() {
        // GIVEN
        var payload = new UnbluWebhookPayload(
                "ConversationCreatedEvent", "conversation.created",
                Instant.now().toEpochMilli(), "account-1", "conv-123",
                new ConversationData("conv-123", "Test Topic"), null, null);
        when(conversationHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        service.onConversationCreated(payload);

        // THEN
        ArgumentCaptor<ConversationHistory> captor = ArgumentCaptor.forClass(ConversationHistory.class);
        verify(conversationHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().conversationId()).isEqualTo("conv-123");
        assertThat(captor.getValue().topic()).isEqualTo("Test Topic");
    }

    @Test
    void onConversationCreated_withNullConversationId_logsAndReturnsGracefully() {
        // GIVEN
        var payload = new UnbluWebhookPayload("ConversationCreatedEvent", null,
                null, null, null, null, null, null);

        // WHEN / THEN : no exception, no save
        service.onConversationCreated(payload);
        verify(conversationHistoryRepository, never()).save(any());
    }

    @Test
    void onConversationEnded_closesExistingHistory() {
        // GIVEN
        String convId = "conv-456";
        ConversationHistory existing = ConversationHistory.create(convId, "topic", Instant.now().minusSeconds(60));
        when(conversationHistoryRepository.findByConversationId(convId)).thenReturn(Optional.of(existing));
        when(conversationHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var payload = new UnbluWebhookPayload("ConversationEndedEvent", "conversation.ended",
                Instant.now().toEpochMilli(), "acc", convId,
                new ConversationData(convId, null), null, "NORMAL");

        // WHEN
        service.onConversationEnded(payload);

        // THEN
        verify(conversationHistoryRepository).save(argThat(h -> h.isEnded()));
    }
}
