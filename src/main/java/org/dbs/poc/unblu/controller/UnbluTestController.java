package org.dbs.poc.unblu.controller;

import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.service.UnbluService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/unblu")
@RequiredArgsConstructor
@Tag(name = "Unblu", description = "API pour interagir avec Unblu")
public class UnbluTestController {

    private final UnbluService unbluService;

    @GetMapping("/current-account")
    @Operation(summary = "Récupérer le compte actuel")
    public ResponseEntity<Account> getCurrentAccount() {
        Account account = unbluService.getCurrentAccount();
        return ResponseEntity.ok(account);
    }

    @GetMapping("/accounts")
    @Operation(summary = "Lister tous les comptes")
    public ResponseEntity<AccountResult> getAccounts() {
        AccountResult accounts = unbluService.listAccounts();
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/conversations")
    @Operation(summary = "Créer une nouvelle conversation")
    public ResponseEntity<ConversationData> createConversation(@RequestBody ConversationCreationData conversationData) {
        ConversationData conversation = unbluService.createConversation(conversationData);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/agents/search")
    @Operation(summary = "Rechercher les agents disponibles")
    public ResponseEntity<PersonResult> searchAgents(@RequestBody PersonTypedQuery query) {
        PersonResult agents = unbluService.searchAgents(query);
        return ResponseEntity.ok(agents);
    }

    @PostMapping("/agents/search-by-state")
    @Operation(summary = "Rechercher les agents par état")
    public ResponseEntity<AgentPersonStateResult> searchAgentsByState(@RequestBody AgentStateQuery query) {
        AgentPersonStateResult agents = unbluService.searchAgentsByState(query);
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/persons/by-source")
    @Operation(summary = "Vérifier si un client est connu dans Unblu")
    public ResponseEntity<PersonData> getPersonBySource(
            @RequestParam EPersonSource personSource,
            @RequestParam String sourceId) {
        PersonData person = unbluService.getPersonBySource(personSource, sourceId);
        return ResponseEntity.ok(person);
    }

    @PostMapping("/webhooks/search")
    @Operation(summary = "Rechercher les webhooks enregistrés")
    public ResponseEntity<WebhookRegistrationResult> searchWebhooks(@RequestBody WebhookRegistrationQuery query) {
        WebhookRegistrationResult webhooks = unbluService.searchWebhooks(query);
        return ResponseEntity.ok(webhooks);
    }

    @GetMapping("/webhooks/{registrationId}")
    @Operation(summary = "Récupérer un webhook par son ID")
    public ResponseEntity<WebhookRegistration> getWebhookById(@PathVariable String registrationId) {
        WebhookRegistration webhook = unbluService.getWebhookById(registrationId);
        return ResponseEntity.ok(webhook);
    }

    @GetMapping("/webhooks/by-name")
    @Operation(summary = "Récupérer un webhook par son nom")
    public ResponseEntity<WebhookRegistration> getWebhookByName(@RequestParam String name) {
        WebhookRegistration webhook = unbluService.getWebhookByName(name);
        return ResponseEntity.ok(webhook);
    }
}
