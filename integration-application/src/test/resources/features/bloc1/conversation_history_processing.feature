
# Bloc 1 — Integration Layer: BDD validation
# Validates that the integration layer processes webhook events independently of Unblu API
Feature: Traitement des événements webhook (Bloc 1)

  Background:
    Given le repository de conversation est vide

  Scenario: Création d'une conversation lors d'un événement webhook
    Given un événement webhook de type "conversation.created" pour la conversation "conv-001" avec le sujet "Support Client"
    When le service ConversationHistory traite l'événement
    Then la conversation "conv-001" est persistée avec le sujet "Support Client"
    And la conversation n'est pas encore terminée

  Scenario: Fin de conversation lors d'un événement webhook
    Given la conversation "conv-002" existe en base avec le sujet "Finance"
    And un événement webhook de type "conversation.ended" pour la conversation "conv-002"
    When le service ConversationHistory traite l'événement
    Then la conversation "conv-002" est marquée comme terminée

  Scenario: Synchronisation complète depuis Unblu
    Given Unblu retourne 3 conversations actives
    When la synchronisation est déclenchée
    Then 3 conversations sont persistées en base
    And le résultat de sync indique 3 nouvelles conversations

  Scenario: Le Bloc 1 fonctionne sans dépendance au Bloc 2
    Given le Bloc 2 est indisponible
    And la base de données Bloc 1 est accessible
    When une requête de listing des conversations est effectuée
    Then les conversations sont retournées depuis la base de données Bloc 1
    And aucune dépendance au Bloc 2 n'est requise
