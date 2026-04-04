package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.port.in.*;
import org.dbs.poc.unblu.domain.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.domain.port.in.query.SearchConversationsByStateQuery;
import org.dbs.poc.unblu.exposition.rest.dto.*;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationHistoryQueryMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationSearchMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.SyncConversationsMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Gestion des conversations Unblu")
public class ConversationController {

    private final StartConversationUseCase startConversationUseCase;
    private final StartDirectConversationUseCase startDirectConversationUseCase;
    private final SyncConversationsUseCase syncConversationsUseCase;
    private final ListConversationHistoryUseCase listConversationHistoryUseCase;
    private final GetConversationHistoryUseCase getConversationHistoryUseCase;
    private final EnrichConversationUseCase enrichConversationUseCase;
    private final SearchConversationsByStateUseCase searchConversationsByStateUseCase;

    private final ConversationMapper conversationMapper;
    private final ConversationHistoryQueryMapper historyMapper;
    private final ConversationSearchMapper searchMapper;
    private final SyncConversationsMapper syncMapper;

    @PostMapping("/start")
    @Operation(summary = "Démarre une conversation avec une équipe")
    public ResponseEntity<StartConversationResponse> start(@RequestBody @Valid StartConversationRequest request) {
        var command = conversationMapper.toCommand(request);
        var state = startConversationUseCase.startConversation(command);
        return ResponseEntity.ok(conversationMapper.toResponse(state));
    }

    @PostMapping("/direct")
    @Operation(summary = "Démarre une conversation directe")
    public ResponseEntity<StartConversationResponse> startDirect(@RequestBody @Valid StartDirectConversationRequest request) {
        var command = conversationMapper.toCommand(request);
        var info = startDirectConversationUseCase.startDirectConversation(command);
        return ResponseEntity.ok(conversationMapper.toResponse(info));
    }

    @PostMapping("/sync")
    @Operation(summary = "Synchronise les conversations depuis Unblu")
    public ResponseEntity<SyncConversationsResponse> sync() {
        var result = syncConversationsUseCase.syncAll();
        return ResponseEntity.ok(syncMapper.toResponse(result));
    }

    @GetMapping("/history")
    @Operation(summary = "Liste l'historique des conversations")
    public ResponseEntity<ConversationHistoryPageResponse> listHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "CREATED_AT") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        var query = ListConversationHistoryQuery.of(page, size, sortField, sortDir);
        var result = listConversationHistoryUseCase.listConversations(query);
        return ResponseEntity.ok(historyMapper.toPageResponse(result));
    }

    @GetMapping("/history/{conversationId}")
    @Operation(summary = "Détail d'une conversation historisée")
    public ResponseEntity<ConversationHistoryDetailResponse> getHistory(@PathVariable String conversationId) {
        return getConversationHistoryUseCase.getByConversationId(conversationId)
                .map(historyMapper::toDetailResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/history/{conversationId}/enrich")
    @Operation(summary = "Enrichit une conversation historisée")
    public ResponseEntity<ConversationHistoryDetailResponse> enrich(@PathVariable String conversationId) {
        var history = enrichConversationUseCase.enrichOne(conversationId);
        return ResponseEntity.ok(historyMapper.toDetailResponse(history));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche des conversations par état")
    public ResponseEntity<ConversationSearchResponse> searchByState(@RequestParam String state) {
        var query = new SearchConversationsByStateQuery(state);
        var summaries = searchConversationsByStateUseCase.searchByState(query);
        return ResponseEntity.ok(searchMapper.toResponse(summaries, state));
    }
}
