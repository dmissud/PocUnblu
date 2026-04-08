Feature: Démarrer une conversation directe entre un visiteur et un agent

  Scenario: Client autorisé — conversation directe créée avec résumé
    Given un client avec l'identifiant "visitor-001" et le segment "STANDARD"
    And Unblu retournera l'identifiant de conversation "conv-direct-001"
    When je démarre une conversation directe entre le visiteur "visitor-001" et l'agent "agent-001" avec le sujet "Question sur mon contrat"
    Then la conversation directe est créée avec succès
    And l'identifiant de la conversation directe est "conv-direct-001"
    And un résumé a été posté dans la conversation "conv-direct-001"

  Scenario: Client banni — accès refusé avec exception
    Given un client avec l'identifiant "BANNED-visitor" et le segment "BANNED"
    When je démarre une conversation directe entre le visiteur "BANNED-visitor" et l'agent "agent-001" avec le sujet "Test"
    Then une exception ChatAccessDeniedException est levée
    And aucune conversation directe n'est créée dans Unblu
