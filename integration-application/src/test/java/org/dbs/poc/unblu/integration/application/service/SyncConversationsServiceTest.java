package org.dbs.poc.unblu.integration.application.service;

import org.dbs.poc.unblu.integration.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncConversationsServiceTest {

    @Mock IntegrationUnbluPort unbluPort;
    @Mock ConversationHistoryRepository conversationHistoryRepository;
    @InjectMocks SyncConversationsService service;

    @Test
    void syncAll_newConversations_persistsAll() {
        // GIVEN
        var summary1 = new UnbluConversationSummary("conv-1", "Topic 1", "ACTIVE", Instant.now(), null);
        var summary2 = new UnbluConversationSummary("conv-2", "Topic 2", "COMPLETE", Instant.now().minusSeconds(60), Instant.now());
        when(unbluPort.listAllConversations()).thenReturn(List.of(summary1, summary2));
        when(conversationHistoryRepository.existsByConversationId(any())).thenReturn(false);
        when(conversationHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        var result = service.syncAll();

        // THEN
        assertThat(result.totalScanned()).isEqualTo(2);
        assertThat(result.newlyPersisted()).isEqualTo(2);
        assertThat(result.alreadyExisting()).isEqualTo(0);
        assertThat(result.errors()).isEqualTo(0);
        verify(conversationHistoryRepository, times(2)).save(any());
    }

    @Test
    void syncAll_existingConversations_skipsCreation() {
        // GIVEN
        var summary = new UnbluConversationSummary("existing", "T", "ACTIVE", Instant.now(), null);
        when(unbluPort.listAllConversations()).thenReturn(List.of(summary));
        when(conversationHistoryRepository.existsByConversationId("existing")).thenReturn(true);

        // WHEN
        var result = service.syncAll();

        // THEN
        assertThat(result.alreadyExisting()).isEqualTo(1);
        assertThat(result.newlyPersisted()).isEqualTo(0);
    }

    @Test
    void syncAll_onError_tracksErrorIds() {
        // GIVEN
        when(unbluPort.listAllConversations()).thenReturn(
                List.of(new UnbluConversationSummary("bad", null, "ACTIVE", Instant.now(), null)));
        when(conversationHistoryRepository.existsByConversationId("bad")).thenThrow(new RuntimeException("DB error"));

        // WHEN
        var result = service.syncAll();

        // THEN
        assertThat(result.errors()).isEqualTo(1);
        assertThat(result.errorConversationIds()).contains("bad");
    }
}
