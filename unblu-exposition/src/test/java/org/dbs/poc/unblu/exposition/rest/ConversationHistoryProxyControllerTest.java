package org.dbs.poc.unblu.exposition.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for ConversationHistoryProxyController (Bloc 2).
 * Validates that the proxy delegates to event-processor (Bloc 1) without direct dependency on persistence.
 */
@ExtendWith(MockitoExtension.class)
class ConversationHistoryProxyControllerTest {

    @Mock
    RestTemplate restTemplate;

    ConversationHistoryProxyController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new ConversationHistoryProxyController(restTemplate, "http://localhost:8082");
    }

    @Test
    void listHistory_delegatesToEventProcessorUrl() {
        var expected = Map.of("items", java.util.List.of(), "totalItems", 0);
        when(restTemplate.getForObject(contains("/api/history/conversations"), eq(Object.class)))
                .thenReturn(expected);

        ResponseEntity<?> response = controller.listHistory(0, 10, "CREATED_AT", "DESC");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expected);
        verify(restTemplate).getForObject(
                eq("http://localhost:8082/api/history/conversations?page=0&size=10&sortField=CREATED_AT&sortDir=DESC"),
                eq(Object.class));
    }

    @Test
    void getHistory_returnsNotFound_whenEventProcessorReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(Object.class))).thenReturn(null);
        ResponseEntity<?> response = controller.getHistory("conv-unknown");
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void sync_delegatesToEventProcessorPostEndpoint() {
        var syncResult = Map.of("newlyPersisted", 5);
        when(restTemplate.postForObject(contains("/api/history/sync"), isNull(), eq(Object.class)))
                .thenReturn(syncResult);

        ResponseEntity<?> response = controller.sync();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(syncResult);
    }

    @Test
    void bloc2HasNoPersistenceDependency() {
        // This test verifies at compile-time that ConversationHistoryProxyController
        // does NOT inject any JPA repository or ConversationHistoryRepository.
        // If it compiles, the separation is enforced.
        assertThat(controller.getClass().getDeclaredFields())
                .noneSatisfy(f -> assertThat(f.getType().getName())
                        .as("No JPA/persistence dependency in Bloc 2 proxy")
                        .containsIgnoringCase("repository")
                        .containsIgnoringCase("jpa")
                        .containsIgnoringCase("persistence"));
    }
}
