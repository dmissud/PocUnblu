# Architecture — Outillage

> Ce document couvre les outils et configurations transverses : tunnels ngrok, Docker Compose,
> profils Spring, et pièges connus (gzip, exceptions Camel).

---

## Ngrok — tunnels HTTPS

Unblu (plateforme SaaS) doit pouvoir appeler des endpoints locaux. Ngrok crée des tunnels HTTPS
publics vers les services locaux.

### Deux tunnels distincts

| Tunnel | Nom | Port local | Usage |
|--------|-----|-----------|-------|
| `webhook-entrypoint` | `webhook-entrypoint` | 8083 | Réception des webhooks Unblu |
| `livekit` | `livekit` | 8082 | Outbound requests bot (Unblu-Hookshot) |

Les deux tunnels sont démarrés automatiquement par `NgrokManager` lors de l'appel à
`POST /api/v1/webhooks/setup`. **Ne pas démarrer ngrok manuellement.**

```bash
# Démarrer l'application principale
mvn spring-boot:run -pl unblu-configuration

# Déclencher le setup (démarre ngrok + enregistre le webhook dans Unblu)
curl -X POST http://localhost:8081/api/v1/webhooks/setup

# Vérifier le statut
curl http://localhost:8081/api/v1/webhooks/status

# Arrêter et dé-enregistrer
curl -X POST "http://localhost:8081/api/v1/webhooks/teardown?deleteWebhook=true"
```

Les URLs ngrok générées sont visibles dans les logs et enregistrées dans Unblu automatiquement.

### Architecture du composant ngrok

```
NgrokManager
  ├── Démarre le processus ngrok (CLI)
  ├── Attend que l'API locale ngrok (port 4040) soit disponible
  ├── Interroge l'API pour récupérer les URLs publiques
  └── Arrête le processus proprement à la destruction du bean Spring

StaticTunnelAdapter
  └── Implémente TunnelPort (port OUT du domaine)
        → fournit les URLs de tunnel aux services d'enregistrement
```

### Dashboard local ngrok

`http://localhost:4040` — inspection des requêtes en temps réel, replay de webhooks.

### Prérequis

```bash
# Installer ngrok et configurer le token
ngrok config add-authtoken <VOTRE_TOKEN>
```

Token disponible sur [https://dashboard.ngrok.com](https://dashboard.ngrok.com).

---

## Problème gzip (Unblu-Hookshot)

### Symptôme

Le bot ne répond jamais. Dans le dashboard ngrok, la requête est reçue mais Unblu ne reçoit pas
de réponse exploitable.

### Cause

Unblu-Hookshot envoie `Accept-Encoding: gzip` sur les outbound requests bot. Si la compression
HTTP est activée côté serveur (comportement par défaut de Spring Boot / Tomcat au-delà d'un seuil
de taille), la réponse est gzip-compressée. **Unblu-Hookshot ne décompresse pas cette réponse.**

### Solution en deux points

**1. Désactiver la compression dans `livekit`** (`livekit/src/main/resources/application.yml`) :

```yaml
server:
  port: 8082
  compression:
    enabled: false
```

**2. Filtrer `Accept-Encoding` dans les proxies** (`ProxyHeaders.java`) :

```java
private static final Set<String> EXCLUDED = Set.of(
    "host", "content-length", "transfer-encoding",
    "connection",
    "accept-encoding"   // évite la réponse gzip vers les backends
);
```

`BotOutboundProxyController` utilise `ProxyHeaders.extract()` avant de transmettre la requête à
`livekit`.

> **Règle :** ne jamais activer `server.compression.enabled: true` dans `livekit/application.yml`.

---

## Docker Compose

`docker-compose.yml` démarre l'infrastructure partagée des deux blocs :

| Service | Image | Port | Usage |
|---------|-------|------|-------|
| `postgres` | `postgres:16-alpine` | 5432 | Base de données commune (historique) |
| `zookeeper` | `confluentinc/cp-zookeeper:7.5.0` | 2181 | Coordination Kafka |
| `kafka` | `confluentinc/cp-kafka:7.5.0` | 9092 | Bus d'événements webhook |
| `kafka-init` | (script) | — | Création des topics au démarrage |

```bash
docker compose -f docker-compose.yml up -d
```

Les credentials PostgreSQL sont lus depuis `.env` (jamais commité — voir `.env.example`).

---

## Profils Spring Boot

| Profil | Activation | Effet |
|--------|-----------|-------|
| (défaut) | toujours | Config de base, logs INFO |
| `debug` | `-Dspring-boot.run.profiles=debug` | Logs SQL détaillés, verbose Camel |
| `local` | (config séparée) | `show-sql: true`, `format_sql: true` — uniquement en local |
| `ngrok` | implicite via config | Active les proxies `LiveKitProxyController`, `BotOutboundProxyController` |

---

## Gestion des exceptions Camel

### Règles essentielles

**`onException()` doit être déclaré avant tout `from()`** dans `configure()`. Déclaré après, Camel
l'ignore silencieusement pour les routes déjà enregistrées.

```java
@Override
public void configure() {
    // TOUJOURS en premier
    onException(MyException.class)
        .handled(true)
        .log(LoggingLevel.ERROR, "...");

    from("direct:my-route")
        .process(myProcessor);
}
```

### Trois mécanismes complémentaires

| Mécanisme | Portée | Usage |
|-----------|--------|-------|
| `onException()` | Toutes les routes du `RouteBuilder` | Gestion globale par type d'exception |
| `doTry() / doCatch()` | Segment précis d'une route | Fallback local, pas de retry |
| `errorHandler()` | Par défaut pour toutes les routes | Dead Letter Channel global |

### Pattern dans ce projet

`WebhookEventRoute` et `KafkaWebhookConsumerRoute` suivent le même pattern :

```
onException(DataIntegrityViolationException) → handled(true), log WARN  [doublon idempotent]
onException(IllegalArgumentException)        → handled(true), log ERROR  [payload invalide]
onException(Exception)                       → retry x3, backoff 2s×2, puis DLQ
```

---

## Liquibase — migrations de schéma

Liquibase gère les migrations de base de données dans `integration-infrastructure` :

```
integration-infrastructure/src/main/resources/db/changelog/
  db.changelog-master.yaml
  migrations/
    001-initial-schema.sql
```

`ddl-auto` est retiré de la config principale. Les schémas évoluent uniquement via des migrations
versionnées.

---

## Ports utilisés

| Port | Service | Description |
|------|---------|-------------|
| 8081 | `unblu-configuration` | Application principale + frontend Angular |
| 8082 | `livekit` | Microservice bot |
| 8083 | `webhook-entrypoint` | Réception webhooks Unblu → Kafka |
| 8084 | `engine` | Consumer Kafka + API historique |
| 4040 | ngrok (dashboard) | Inspection des requêtes en temps réel |
| 5432 | PostgreSQL | Base de données |
| 9092 | Kafka | Bus d'événements |