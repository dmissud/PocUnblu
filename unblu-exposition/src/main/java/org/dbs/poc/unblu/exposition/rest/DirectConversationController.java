package org.dbs.poc.unblu.exposition.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.dbs.poc.unblu.exposition.rest.dto.StartDirectConversationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class DirectConversationController {

    private final StartDirectConversationUseCase startDirectConversationUseCase;

    @PostMapping("/direct")
    public ResponseEntity<StartConversationResponse> startDirectConversation(
            @Valid @RequestBody StartDirectConversationRequest request) {

        StartDirectConversationCommand command = new StartDirectConversationCommand(
                request.getVirtualParticipantSourceId(),
                request.getAgentParticipantSourceId(),
                request.getSubject());

        UnbluConversationInfo info = startDirectConversationUseCase.startDirectConversation(command);

        StartConversationResponse response = StartConversationResponse.builder()
                .unbluConversationId(info.unbluConversationId())
                .unbluJoinUrl(info.unbluJoinUrl())
                .status("CREATED")
                .message("Conversation directe créée avec succès.")
                .build();

        return ResponseEntity.ok(response);
    }
}
