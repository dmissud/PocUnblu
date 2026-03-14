package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final StartDirectConversationUseCase startDirectConversationUseCase;
    private final StartConversationUseCase startConversationUseCase;

    @PostMapping("/direct")
    public ResponseEntity<UnbluConversationInfo> startDirectConversation(
            @RequestBody DirectConversationRequest request) {
        
        StartDirectConversationCommand command = new StartDirectConversationCommand(
                request.clientSourceId(),
                request.agentSourceId(),
                request.subject()
        );
        
        UnbluConversationInfo result = startDirectConversationUseCase.startDirectConversation(command);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/team")
    public ResponseEntity<ConversationContext> startTeamConversation(
            @RequestBody TeamConversationRequest request) {

        if (request.teamId() == null || request.teamId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        StartConversationCommand command = new StartConversationCommand(
                request.clientId(),
                request.subject(),
                "FRONTEND_TEST",
                request.teamId()
        );

        ConversationContext result = startConversationUseCase.startConversation(command);
        return ResponseEntity.ok(result);
    }

    public record DirectConversationRequest(
            String clientSourceId,
            String agentSourceId,
            String subject
    ) {}

    public record TeamConversationRequest(
            String clientId,
            String subject,
            String teamId
    ) {}
}
