package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchBotsUseCase;
import org.dbs.poc.unblu.exposition.rest.dto.BotResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supervision")
@RequiredArgsConstructor
@Tag(name = "Supervision", description = "Supervision des bots Unblu")
public class SupervisionController {

    private final SearchBotsUseCase searchBotsUseCase;

    @GetMapping("/bots")
    @Operation(summary = "Liste tous les bots Unblu configurés")
    public ResponseEntity<List<BotResponse>> listBots() {
        var bots = searchBotsUseCase.listBots().stream()
                .map(b -> new BotResponse(
                        b.id(),
                        b.name(),
                        b.onboardingFilter(),
                        b.onboardingOrder(),
                        b.webhookStatus(),
                        b.webhookEndpoint()
                ))
                .toList();
        return ResponseEntity.ok(bots);
    }
}
