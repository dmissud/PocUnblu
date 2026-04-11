# language: fr
# Bloc 2 — Supervisor: BDD validation
Feature: Supervision sans persistance propre (Bloc 2)

  Scenario: Le Bloc 2 accède à l'historique via le proxy sans base de données
    Given le Bloc 1 (event-processor) expose une API de listing des conversations
    And le Bloc 2 (superviseur) n'a pas de connexion directe à la base de données de l'intégration
    When l'interface Angular demande la liste des conversations historisées
    Then le Bloc 2 proxifie la requête vers le Bloc 1
    And retourne les données sans les persister lui-même

  Scenario: Le Bloc 2 peut lister les NamedAreas depuis Unblu directement
    Given l'API Unblu est disponible
    When une requête sur les NamedAreas est effectuée
    Then les données sont récupérées en temps réel depuis Unblu via le port SupervisorUnbluPort
    And aucune donnée n'est persistée localement par le Bloc 2

  Scenario: Le Bloc 2 fonctionne si le Bloc 1 est indisponible pour les opérations live
    Given le Bloc 1 est temporairement indisponible
    And l'API Unblu est disponible
    When une requête de liste des NamedAreas est effectuée
    Then le Bloc 2 répond avec les données Unblu directement
    And un message d'information est loggé concernant l'indisponibilité du Bloc 1
