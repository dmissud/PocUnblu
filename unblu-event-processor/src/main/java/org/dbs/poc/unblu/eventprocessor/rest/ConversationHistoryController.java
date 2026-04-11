package org.dbs.poc.unblu.eventprocessor.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.eventprocessor.rest.dto.ConversationHistoryDetailResponse;
import org.dbs.poc.unblu.eventprocessor.rest.dto.ConversationHistoryPageResponse;
import org.dbs.poc.unblu.eventprocessor.rest.mapper.ConversationHistoryMapper;
import org.dbs.poc.unblu.integration.application.port.in.EnrichConversationUseCase;
import org.dbs.poc.unblu.integration.application.port.in.GetConversationHistoryUseCase;
import org.dbs.poc.unblu.integration.application.port.in.ListConversationHistoryUseCase;
import org.dbs.poc.unblu.integration.application.port.in.SyncConversationsUseCase;
import org.dbs.poc.unblu.integration.application.port.in.query.ListConversationHistoryQuery;
import org.dbs.poc.unblu.integration.domain.model.ConversationSyncResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "Conversation History", description = "Bloc 1 — Integration: read/sync conversation history")
public class ConversationHistoryController {

    private final ListConversationHistoryUseCase listUseCase;
    private final GetConversationHistoryUseCase getUseCase;
    private final SyncConversationsUseCase syncUseCase;
    private final EnrichConversationUseCase enrichUseCase;
    private final ConversationHistoryMapper mapper;

    @GetMapping("/conversations")
    @Operation(summary = "List conversation history (paginated)")
    public ResponseEntity<ConversationHistoryPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "CREATED_AT") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        ListConversationHistoryQuery query = ListConversationHistoryQuery.of(page, size, sortField, sortDir);
        return ResponseEntity.ok(mapper.toPageResponse(listUseCase.listConversations(query)));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Get full conversation history detail")
    public ResponseEntity<ConversationHistoryDetailResponse> get(@PathVariable String conversationId) {
        return getUseCase.getByConversationId(conversationId)
                .map(mapper::toDetailResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sync")
    @Operation(summary = "Trigger full sync from Unblu to database")
    public ResponseEntity<ConversationSyncResult> sync() {
        return ResponseEntity.ok(syncUseCase.syncAll());
    }

    @PostMapping("/conversations/{conversationId}/enrich")
    @Operation(summary = "Enrich a conversation with messages and participants from Unblu")
    public ResponseEntity<ConversationHistoryDetailResponse> enrich(@PathVariable String conversationId) {
        return ResponseEntity.ok(mapper.toDetailResponse(enrichUseCase.enrichOne(conversationId)));
    }
}
