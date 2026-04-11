# Kafka — Mise en œuvre

Kafka est le canal d'intégration asynchrone entre `webhook-entrypoint` (producteur) et
`engine` (consommateur). Il découple la réception des webhooks Unblu de leur traitement métier.

---

## Topics

| Topic | Partitions | Rôle |
|-------|:----------:|------|
| `unblu.webhook.events` | 3 | Événements webhook Unblu bruts (JSON) |
| `unblu.webhook.events.dlq` | 1 | Dead Letter Queue — messages en échec après retry |

### Pourquoi 3 partitions sur le topic principal ?

3 partitions permettent d'avoir jusqu'à 3 instances du consumer `engine` en parallèle
(une instance par partition dans le même consumer group). Avec 1 seule instance aujourd'hui
c'est transparent, mais cela ne nécessite pas de re-partitionnement pour scaler.

### Pourquoi 1 partition sur la DLQ ?

Les messages en DLQ sont rares et traités manuellement (investigation, replay). L'ordre
de réception suffit, le parallélisme n'apporte rien ici.

---

## Création des topics

Les topics sont créés au démarrage par le service `kafka-init` dans `docker-compose.yml`.
`KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"` est positionné sur le broker — les topics
doivent exister explicitement avant que les services démarrent.

```yaml
# docker-compose.yml — extrait
kafka-init:
  image: confluentinc/cp-kafka:7.5.0
  command: >
    bash -c "
      cub kafka-ready -b kafka:29092 1 60 &&
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092
        --partitions 3 --replication-factor 1 --topic unblu.webhook.events &&
      kafka-topics --create --if-not-exists --bootstrap-server kafka:29092
        --partitions 1 --replication-factor 1 --topic unblu.webhook.events.dlq
    "
```

`cub kafka-ready` attend que le broker soit prêt avant de créer les topics.
`--if-not-exists` rend la commande idempotente (relance sans erreur si les topics existent déjà).

---

## Producteur — `webhook-entrypoint`

**Fichier de configuration :** `webhook-entrypoint/src/main/resources/application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3

kafka:
  topic:
    webhook-events: ${KAFKA_TOPIC_WEBHOOK_EVENTS:unblu.webhook.events}
```

**Classe productrice :** `WebhookReceiverController`

```java
kafkaTemplate.send(topic, eventType, body);
//                         ↑ clé      ↑ valeur
//                    X-Unblu-Event  JSON brut
```

- **Clé** : le type d'événement Unblu (`X-Unblu-Event` header), par exemple `conversation.created`
- **Valeur** : le JSON brut du payload Unblu, sans transformation
- **`acks: all`** : le broker confirme l'écriture sur toutes les répliques avant d'acquitter
- **`retries: 3`** : 3 tentatives en cas d'erreur réseau transitoire côté producteur

---

## Consommateur — `engine`

**Fichier de configuration :** `engine/src/main/resources/application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

kafka:
  topic:
    webhook-events: ${KAFKA_TOPIC_WEBHOOK_EVENTS:unblu.webhook.events}
    webhook-events-dlq: ${KAFKA_TOPIC_WEBHOOK_EVENTS_DLQ:unblu.webhook.events.dlq}
  consumer:
    group-id: ${KAFKA_CONSUMER_GROUP_ID:unblu-engine}
```

**Classe consommatrice :** `KafkaWebhookConsumerRoute` (Apache Camel)

```
from("kafka:unblu.webhook.events
       ?brokers=localhost:9092
       &groupId=unblu-engine
       &autoOffsetReset=earliest
       &autoCommitEnable=true")
```

- **`autoOffsetReset=earliest`** : au premier démarrage (offset inconnu), repart depuis le début du topic
- **`autoCommitEnable=true`** : l'offset est commité automatiquement après traitement

---

## Format des messages

| Champ Kafka | Contenu |
|-------------|---------|
| Clé (`key`) | Type d'événement Unblu — ex. `conversation.created`, `person.registered` |
| Valeur (`value`) | JSON brut du payload Unblu (`UnbluWebhookPayload`) |
| Header Camel `kafka.KEY` | Recopie de la clé, accessible dans la route |

Exemple de payload valeur (JSON) :

```json
{
  "type": "conversation.created",
  "eventType": "conversation.created",
  "conversation": {
    "id": "abc123",
    "topic": "Demande client",
    "state": "ACTIVE"
  }
}
```

---

## Gestion des erreurs et DLQ

Deux catégories d'erreur dans `KafkaWebhookConsumerRoute` :

| Erreur | Comportement | Topic cible |
|--------|-------------|-------------|
| `IllegalArgumentException` — payload vide ou non parseable | DLQ immédiat, sans retry | `unblu.webhook.events.dlq` |
| `Exception` générale (erreur transitoire) | 3 retries, back-off exponentiel (2s × 2), puis DLQ | `unblu.webhook.events.dlq` |

Les messages en DLQ sont enrichis avec les headers suivants :

| Header | Contenu |
|--------|---------|
| `dlq.original.topic` | Topic source (`unblu.webhook.events`) |
| `dlq.error.message` | Message de l'exception |
| `dlq.error.class` | Classe Java de l'exception |
| `dlq.failed.at` | Timestamp ISO-8601 de l'échec |
| `dlq.retry.count` | Nombre de tentatives effectuées |

---

## Variables d'environnement

| Variable | Défaut | Utilisée par |
|----------|--------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | `webhook-entrypoint`, `engine` |
| `KAFKA_TOPIC_WEBHOOK_EVENTS` | `unblu.webhook.events` | `webhook-entrypoint`, `engine` |
| `KAFKA_TOPIC_WEBHOOK_EVENTS_DLQ` | `unblu.webhook.events.dlq` | `engine` |
| `KAFKA_CONSUMER_GROUP_ID` | `unblu-engine` | `engine` |

En Docker Compose, `KAFKA_BOOTSTRAP_SERVERS` est surchargé à `kafka:29092` (réseau interne).
En développement local, la valeur par défaut `localhost:9092` s'applique.

---

## Supervision locale — Kafka UI

Un dashboard Kafka UI est disponible lors du démarrage via Docker Compose :

```
http://localhost:8080
```

Il permet de visualiser les topics, les consumer groups, les offsets, et d'inspecter
les messages en DLQ.

```yaml
# docker-compose.yml — extrait
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  environment:
    KAFKA_CLUSTERS_0_NAME: unblu-local
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
  ports:
    - "8080:8080"
```
