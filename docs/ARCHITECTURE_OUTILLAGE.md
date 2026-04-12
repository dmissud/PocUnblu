# Architecture — Outillage

> Ce document couvre les outils et configurations transverses : tunnels ngrok, Docker Compose,
> profils Spring, et pièges connus (gzip, exceptions Camel).

---

## Ngrok — tunnels HTTPS

Unblu (plateforme SaaS) doit pouvoir appeler des endpoints locaux. Ngrok crée un tunnel HTTPS
public vers `unblu-configuration` (port 8081), qui proxy ensuite les requêtes vers les services
internes.

### Un seul tunnel sur 8081

| Tunnel | Nom | Port local | Usage |
|--------|-----|-----------|-------|
| `main` | `main` | 8081 | Webhook Unblu + Outbound requests bot |

Le routing est assuré par les proxy controllers d'`unblu-exposition` :

| Route publique | Proxy controller | Backend |
|----------------|-----------------|---------|
| `POST /api/webhooks/unblu` | `WebhookEntrypointProxyController` | `webhook-entrypoint` :8083 |
| `POST /api/bot/outbound` | `BotOutboundProxyController` | `livekit` :8082 |

Le tunnel est démarré automatiquement par `NgrokManager` lors de l'appel à
`POST /api/v1/webhooks/setup`. **Ne pas démarrer ngrok manuellement.**

> **Pourquoi un seul tunnel ?**
> Avec un compte ngrok gratuit, un seul domaine statique est disponible. Deux tunnels nommés sans
> `domain` explicite obtiennent tous deux le même domaine → routage non déterministe côté ngrok.
> Un tunnel unique sur 8081 élimine ce problème ; les deux routes sont distinguées par leur chemin.

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

## Problèmes connus avec les proxies reverse (Unblu-Hookshot)

### Problème 1 — gzip : réponse illisible côté Unblu

**Symptôme :** le bot ne répond jamais ; la requête est reçue par ngrok mais Unblu ne peut pas
parser la réponse.

**Cause :** Unblu-Hookshot envoie `Accept-Encoding: gzip`. Si la compression est activée,
Spring/Tomcat gzip-encode la réponse. **Unblu-Hookshot ne décompresse pas.**

**Solution :**

1. Désactiver la compression sur `unblu-configuration` et `livekit` :
```yaml
server:
  compression:
    enabled: false
```

2. Filtrer `Accept-Encoding` dans `ProxyHeaders.java` pour ne pas le transmettre aux backends :
```java
private static final Set<String> EXCLUDED = Set.of(
    "host", "content-length", "transfer-encoding", "connection",
    "accept-encoding"   // évite la réponse gzip vers les backends
);
```

> **Règle :** ne jamais activer `server.compression.enabled: true` sur `unblu-configuration`
> ni sur `livekit`.

---

### Problème 2 — ERR_NGROK_3004 : réponse HTTP malformée

**Symptôme :** le proxy Spring retourne bien 200 OK (visible dans les logs), mais ngrok affiche
`ERR_NGROK_3004` et Unblu ne reçoit rien. Unblu réessaie en boucle (~1 s d'intervalle).

**Cause :** `restTemplate.exchange(…, byte[].class)` retourne un `ResponseEntity<byte[]>` qui
inclut **tous les headers de réponse du backend** (notamment `Content-Length`, `Transfer-Encoding`).
Quand Spring sur 8081 renvoie ces headers à Unblu via ngrok, il ajoute ses propres valeurs →
headers dupliqués ou incohérents → réponse HTTP invalide que ngrok ne peut pas transmettre.

**Solution :** dans chaque proxy controller, ne propager que `Content-Type` ; laisser Spring
calculer `Content-Length` et gérer `Transfer-Encoding` :

```java
HttpHeaders responseHeaders = new HttpHeaders();
if (response.getHeaders().getContentType() != null) {
    responseHeaders.setContentType(response.getHeaders().getContentType());
}
return ResponseEntity.status(response.getStatusCode())
        .headers(responseHeaders)
        .body(response.getBody());
```

Appliqué dans `BotOutboundProxyController` et `WebhookEntrypointProxyController`.

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