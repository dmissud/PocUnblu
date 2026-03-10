package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationRequest;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final StartConversationUseCase startConversationUseCase;

    @PostMapping("/start")
    public ResponseEntity<StartConversationResponse> startConversation(@RequestBody StartConversationRequest request) {
        
        // Map DTO to Command
        StartConversationCommand command = StartConversationCommand.builder()
                .clientId(request.getClientId())
                .subject(request.getSubject())
                .origin(request.getOrigin())
                .build();
                
        // Execute Use Case
        ConversationContext context = startConversationUseCase.startConversation(command);
        
        // Map Context to DTO
        StartConversationResponse response = StartConversationResponse.builder()
                .unbluConversationId(context.getUnbluConversationId())
                .unbluJoinUrl(context.getUnbluJoinUrl())
                .status("CREATED")
                .message("Conversation successfully created.")
                .build();
                
        return ResponseEntity.ok(response);
    }
}
