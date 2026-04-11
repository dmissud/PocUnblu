package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchConversationsByStateUseCase;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.application.port.in.query.SearchConversationsByStateQuery;
import org.dbs.poc.unblu.exposition.rest.dto.*;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationSearchMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Bloc 2 — Supervisor: conversation live operations (start, direct, search by state).
 * History endpoints have been removed — they proxify to event-processor (Bloc 1).
 * @see ConversationHistoryProxyController
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Gestion des conversations Unblu (Bloc 2 - Supervisor)")
public class ConversationController {

    private final StartConversationUseCase startConversationUseCase;
    private final StartDirectConversationUseCase startDirectConversationUseCase;
    private final SearchConversationsByStateUseCase searchConversationsByStateUseCase;

    private final ConversationMapper conversationMapper;
    private final ConversationSearchMapper searchMapper;

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

    @GetMapping("/search")
    @Operation(summary = "Recherche des conversations par état (données live Unblu)")
    public ResponseEntity<ConversationSearchResponse> searchByState(@RequestParam String state) {
        var query = new SearchConversationsByStateQuery(state);
        var summaries = searchConversationsByStateUseCase.searchByState(query);
        return ResponseEntity.ok(searchMapper.toResponse(summaries, state));
    }
}
