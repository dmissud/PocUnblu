Feature: Démarrer une conversation avec une équipe

  Scenario: Client standard autorisé — conversation créée avec résumé
    Given un client avec l'identifiant "client-001" et le segment "STANDARD"
    And Unblu retournera l'identifiant de conversation "conv-std-001"
    When je démarre une conversation pour le client "client-001" depuis l'origine "WEB" vers l'équipe "team-001"
    Then la conversation est créée avec succès
    And l'identifiant Unblu de la conversation est "conv-std-001"
    And un résumé a été posté dans la conversation "conv-std-001"

  Scenario: Client VIP autorisé — conversation créée avec résumé
    Given un client avec l'identifiant "VIP-007" et le segment "VIP"
    And Unblu retournera l'identifiant de conversation "conv-vip-007"
    When je démarre une conversation pour le client "VIP-007" depuis l'origine "APP_MOBILE" vers l'équipe "team-vip"
    Then la conversation est créée avec succès
    And l'identifiant Unblu de la conversation est "conv-vip-007"
    And un résumé a été posté dans la conversation "conv-vip-007"

  Scenario: Client banni — conversation créée malgré le refus de routage
    Given un client avec l'identifiant "BANNED-042" et le segment "BANNED"
    And Unblu retournera l'identifiant de conversation "conv-banned-042"
    When je démarre une conversation pour le client "BANNED-042" depuis l'origine "WEB" vers l'équipe "team-001"
    Then la conversation est créée avec succès
    And la décision de routage indique que l'accès est refusé

  Scenario: Unblu indisponible — conversation en mode dégradé OFFLINE-PENDING
    Given un client avec l'identifiant "client-002" et le segment "STANDARD"
    And Unblu est indisponible
    When je démarre une conversation pour le client "client-002" depuis l'origine "WEB" vers l'équipe "team-001"
    Then la conversation est en mode dégradé OFFLINE-PENDING
    And aucun résumé n'a été posté
