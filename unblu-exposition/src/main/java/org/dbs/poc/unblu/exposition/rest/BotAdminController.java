package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.CreateBotUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin/bots")
@RequiredArgsConstructor
public class BotAdminController {

    private final CreateBotUseCase createBotUseCase;

    @PostMapping
    public ResponseEntity<Map<String, String>> createSummaryBot(
            @RequestParam(defaultValue = "SummaryBot") String name,
            @RequestParam(defaultValue = "Bot d'envoi des résumés de conversation") String description) {

        String botPersonId = createBotUseCase.createSummaryBot(name, description);
        return ResponseEntity.ok(Map.of(
                "botPersonId", botPersonId,
                "info", "Configurez unblu.api.summary-bot-person-id=" + botPersonId
        ));
    }
}
