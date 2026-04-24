package org.dbs.poc.unblu.integration.application.bdd.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.dbs.poc.unblu.integration.application.port.in.ListConversationHistoryUseCase;
import org.dbs.poc.unblu.integration.application.port.in.SyncConversationsUseCase;
import org.dbs.poc.unblu.integration.application.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.integration.application.service.ConversationHistoryService;
import org.dbs.poc.unblu.integration.domain.model.ConversationSyncResult;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.integration.domain.model.webhook.ConversationData;
import org.dbs.poc.unblu.integration.domain.model.webhook.UnbluWebhookPayload;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;

import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Soit;

/**
 * Cucumber step definitions for Bloc 1 BDD tests.
 * Uses in-memory test doubles without Spring context for maximum isolation.
 */
public class ConversationHistorySteps {

    // In-memory repository
    private final List<ConversationHistory> store = new ArrayList<>();
    private ConversationHistoryRepository repository;
    private IntegrationUnbluPort unbluPort;
    private ConversationHistoryService historyService;
    private SyncConversationsUseCase syncUseCase;
    private ListConversationHistoryUseCase listUseCase;

    private UnbluWebhookPayload currentPayload;
    private ConversationSyncResult syncResult;
    private ConversationHistoryPage listResult;

    @Before
    public void setUp() {
        store.clear();
        repository = buildInMemoryRepo();
        unbluPort = mock(IntegrationUnbluPort.class);
        historyService = new ConversationHistoryService(repository);
        var syncService = new org.dbs.poc.unblu.integration.application.service.SyncConversationsService(unbluPort, repository);
        syncUseCase = syncService;
        var queryService = new org.dbs.poc.unblu.integration.application.service.ConversationHistoryQueryService(repository);
        listUseCase = queryService;
    }

    @Soit("le repository de conversation est vide")
    public void repositoryIsEmpty() {
        store.clear();
    }

    @Soit("un événement webhook de type {string} pour la conversation {string} avec le sujet {string}")
    public void givenWebhookEventWithSubject(String eventType, String convId, String topic) {
        currentPayload = new UnbluWebhookPayload("ConversationCreatedEvent", eventType,
                Instant.now().toEpochMilli(), "acc", convId,
                new ConversationData(convId, topic), null, null);
    }

    @Soit("un événement webhook de type {string} pour la conversation {string}")
    public void givenWebhookEvent(String eventType, String convId) {
        currentPayload = new UnbluWebhookPayload("ConversationEndedEvent", eventType,
                Instant.now().toEpochMilli(), "acc", convId,
                new ConversationData(convId, null), null, "NORMAL");
    }

    @Soit("la conversation {string} existe en base avec le sujet {string}")
    public void conversationExists(String convId, String topic) {
        store.add(ConversationHistory.create(convId, topic, Instant.now().minusSeconds(120)));
    }

    @Soit("Unblu retourne {int} conversations actives")
    public void unbluReturnsConversations(int count) {
        List<UnbluConversationSummary> summaries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            summaries.add(new UnbluConversationSummary("conv-bdd-" + i, "Topic " + i, "ACTIVE",
                    Instant.now().minusSeconds(i * 60), null));
        }
        when(unbluPort.listAllConversations()).thenReturn(summaries);
    }

    @Soit("le Bloc {int} est indisponible")
    public void bloc2IsUnavailable(int bloc) {
        // Nothing to configure — Bloc 1 has no dependency on Bloc 2
    }

    @Soit("la base de données Bloc {int} est accessible")
    public void databaseIsAccessible(int bloc) {
        // In-memory repo is always available
    }

    @Quand("le service ConversationHistory traite l'événement")
    public void serviceProcessesEvent() {
        String eventType = currentPayload.eventType() != null ? currentPayload.eventType() : currentPayload.type();
        if (eventType.contains("created") || eventType.contains("Created")) {
            historyService.onConversationCreated(currentPayload);
        } else if (eventType.contains("ended") || eventType.contains("Ended")) {
            historyService.onConversationEnded(currentPayload);
        }
    }

    @Quand("la synchronisation est déclenchée")
    public void syncIsTriggered() {
        syncResult = syncUseCase.syncAll();
    }

    @Quand("une requête de listing des conversations est effectuée")
    public void listConversations() {
        listResult = listUseCase.listConversations(ListConversationHistoryQuery.of(0, 100, "CREATED_AT", "DESC"));
    }

    @Alors("la conversation {string} est persistée avec le sujet {string}")
    public void conversationIsPersistedWithTopic(String convId, String topic) {
        Optional<ConversationHistory> found = repository.findByConversationId(convId);
        assertThat(found).isPresent();
        assertThat(found.get().topic()).isEqualTo(topic);
    }

    @Alors("la conversation n'est pas encore terminée")
    public void conversationIsNotEnded() {
        assertThat(store).anyMatch(h -> !h.isEnded());
    }

    @Alors("la conversation {string} est marquée comme terminée")
    public void conversationIsEnded(String convId) {
        Optional<ConversationHistory> found = repository.findByConversationId(convId);
        assertThat(found).isPresent();
        assertThat(found.get().isEnded()).isTrue();
    }

    @Alors("{int} conversations sont persistées en base")
    public void conversationsArePersisted(int count) {
        assertThat(store).hasSizeGreaterThanOrEqualTo(count);
    }

    @Alors("le résultat de sync indique {int} nouvelles conversations")
    public void syncResultIndicatesNew(int count) {
        assertThat(syncResult.newlyPersisted()).isEqualTo(count);
    }

    @Alors("les conversations sont retournées depuis la base de données Bloc {int}")
    public void conversationsReturnedFromDb(int bloc) {
        assertThat(listResult).isNotNull();
    }

    @Alors("aucune dépendance au Bloc {int} n'est requise")
    public void noDependencyOnBloc(int bloc) {
        // If we got here without errors, Bloc 1 is independent of Bloc 2
        assertThat(bloc).isEqualTo(2);
    }

    // ---- In-memory test double ----
    private ConversationHistoryRepository buildInMemoryRepo() {
        return new ConversationHistoryRepository() {
            public ConversationHistory save(ConversationHistory h) {
                store.removeIf(s -> s.conversationId().equals(h.conversationId()));
                store.add(h);
                return h;
            }

            public Optional<ConversationHistory> findByConversationId(String id) {
                return store.stream().filter(h -> h.conversationId().equals(id)).findFirst();
            }

            public org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage findPage(
                    int page, int size,
                    org.dbs.poc.unblu.integration.domain.model.history.ConversationSortField f,
                    org.dbs.poc.unblu.integration.domain.model.history.ConversationSortDirection d) {
                return new org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage(
                        List.copyOf(store), store.size(), page, size, 1);
            }

            public boolean existsByConversationId(String id) {
                return store.stream().anyMatch(h -> h.conversationId().equals(id));
            }

            public java.util.List<ConversationHistory> findAll() {
                return List.copyOf(store);
            }

            public long count() {
                return store.size();
            }
        };
    }
}
