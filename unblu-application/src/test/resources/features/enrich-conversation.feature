Feature: Enrichir une conversation depuis Unblu

  Scenario: Conversation existante enrichie avec participants et messages
    Given une conversation "conv-enrich-001" existe en base avec le sujet "Aide en ligne"
    And Unblu dispose des participants suivants pour cette conversation :
      | personId   | displayName  | personSource |
      | person-001 | Alice Dupont | USER_DB      |
      | person-002 | Bob Martin   | VIRTUAL      |
    And Unblu dispose des messages suivants pour cette conversation :
      | messageId | text                   | senderPersonId |
      | msg-001   | Bonjour, puis-je vous aider ? | person-001 |
      | msg-002   | Oui, j'ai une question | person-002 |
    When j'enrichis la conversation "conv-enrich-001"
    Then la conversation enrichie contient 2 participants
    And la conversation enrichie contient 2 messages
    And la conversation enrichie est persistée en base

  Scenario: Conversation inexistante — exception levée
    Given aucune conversation n'existe en base pour l'identifiant "conv-inexistante"
    When j'enrichis la conversation "conv-inexistante"
    Then une exception IllegalArgumentException est levée avec le message "Conversation introuvable en base : conv-inexistante"

  Scenario: Enrichissement idempotent — message dupliqué ignoré
    Given une conversation "conv-idem-001" existe en base avec le sujet "Test idempotence"
    And Unblu dispose des messages suivants pour cette conversation :
      | messageId | text           | senderPersonId |
      | msg-dup   | Premier message | person-001    |
      | msg-dup   | Premier message | person-001    |
    When j'enrichis la conversation "conv-idem-001"
    Then la conversation enrichie contient 1 messages
