package org.dbs.poc.unblu.livekit.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.livekit.dto.LiveKitStartRequest;
import org.dbs.poc.unblu.livekit.dto.LiveKitStartResponse;
import org.dbs.poc.unblu.livekit.service.StartLiveKitConversationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/livekit/conversations")
@RequiredArgsConstructor
@Tag(name = "LiveKit", description = "Endpoints de travail pour LiveKit")
public class LiveKitConversationController {

    private final StartLiveKitConversationUseCase startUseCase;

    @PostMapping("/start")
    @Operation(summary = "Démarre une conversation simplifiée pour LiveKit")
    public ResponseEntity<LiveKitStartResponse> start(@RequestBody @Valid LiveKitStartRequest request) {
        var info = startUseCase.startConversation(request.getPersonId());
        return ResponseEntity.ok(LiveKitStartResponse.builder()
                .conversationId(info.unbluConversationId())
                .joinUrl(info.unbluJoinUrl())
                .build());
    }
}
