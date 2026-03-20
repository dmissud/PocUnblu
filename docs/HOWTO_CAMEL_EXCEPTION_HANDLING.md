# HowTo — Gestion des exceptions avec Apache Camel

## Contexte

Apache Camel ne gère pas les exceptions comme Spring le ferait avec `@ControllerAdvice`. Chaque route est un pipeline de traitement de messages, et une exception non gérée fait échouer l'échange en cours. Camel offre plusieurs mécanismes complémentaires, chacun avec une portée et un comportement différents.

---

## 1. Les trois mécanismes principaux

### 1.1 `onException()` — Le plus courant

Déclaré dans un `RouteBuilder`, il s'applique à **toutes les routes définies dans ce même RouteBuilder**.

```java
@Override
public void configure() {
    // TOUJOURS déclarer avant les from()
    onException(MyBusinessException.class)
        .handled(true)
        .log(LoggingLevel.ERROR, "Erreur métier: ${exception.message}");

    from("direct:my-route")
        .process(myProcessor);
}
```

> **Règle critique :** `onException()` doit être déclaré **avant** tout `from()` dans `configure()`.
> Si déclaré après, Camel l'ignore silencieusement pour les routes déjà enregistrées.

---

### 1.2 `doTry() / doCatch() / doFinally()` — Gestion locale dans une route

Équivalent du `try/catch` Java, mais dans le DSL Camel. Utilisé pour gérer une exception **sur un segment précis** d'une route.

```java
from("direct:my-route")
    .doTry()
        .to("direct:risky-operation")
    .doCatch(TimeoutException.class)
        .log(LoggingLevel.WARN, "Timeout, using fallback")
        .to("direct:fallback")
    .doFinally()
        .log("Cleanup done")
    .end();
```

> Différence avec `onException()` : `doTry/doCatch` ne peut pas configurer de retry.
> À réserver pour les cas où on veut un fallback local sans retry.

---

### 1.3 `errorHandler()` — Configuration globale du ErrorHandler

Définit le comportement **par défaut** pour toutes les routes quand aucun `onException()` ne correspond.

```java
// Dans un RouteBuilder dédié à la configuration
errorHandler(deadLetterChannel("jms:queue:dead-letter")
    .maximumRedeliveries(3)
    .redeliveryDelay(1000)
    .logExhaustedMessageHistory(true));
```

En l'absence de configuration explicite, Camel utilise le `DefaultErrorHandler` qui logue l'erreur et ne fait **aucun retry**.

---

## 2. Les paramètres clés de `onException()`

### 2.1 `handled()` vs `continued()`

C'est le paramètre le plus important — il détermine ce qui se passe après le handler.

| Paramètre | Comportement | Usage |
|-----------|-------------|-------|
| `.handled(true)` | L'exception est absorbée. L'échange continue normalement après le handler, **sans propager l'erreur** à l'appelant. | Cas nominal : on a traité l'erreur, tout va bien. |
| `.handled(false)` | Le handler s'exécute, puis l'exception est **re-propagée**. L'appelant reçoit l'erreur. | Logging ou enrichissement avant de propager. |
| `.continued(true)` | L'exception est absorbée ET le traitement **reprend là où il s'est arrêté** dans la route originale. | Rare — pour ignorer une étape qui a échoué et continuer. |

```java
// Absorber l'erreur proprement
onException(BusinessException.class)
    .handled(true)
    .log("Erreur absorbée, réponse par défaut envoyée");

// Logger et re-propager
onException(TechnicalException.class)
    .handled(false)
    .log(LoggingLevel.ERROR, "Erreur technique: ${exception.message}");
```

---

### 2.2 Retry et backoff exponentiel

```java
onException(IOException.class)
    .maximumRedeliveries(3)        // nombre de tentatives après la première
    .redeliveryDelay(2000)         // délai initial en ms (2 secondes)
    .backOffMultiplier(2)          // multiplicateur entre chaque tentative
    .useExponentialBackOff()       // active l'exponentiel (nécessite backOffMultiplier)
    .maximumRedeliveryDelay(30000) // plafond du délai (30 secondes max)
    .retryAttemptedLogLevel(LoggingLevel.WARN) // niveau de log pour chaque retry
    .handled(true)
    .log(LoggingLevel.ERROR, "Échec définitif après retries");
```

Avec cette configuration, les délais seront : 2s → 4s → 8s (puis abandon).

> **Attention :** le retry re-exécute **l'échange complet depuis le début de la route**,
> pas seulement l'étape qui a échoué. Les opérations non idempotentes (INSERT) peuvent
> créer des doublons. Toujours protéger les opérations persistantes avec de l'idempotence.

---

### 2.3 Conditions de retry avec `onWhen()`

Pour ne retenter que sur certaines conditions :

```java
onException(HttpOperationFailedException.class)
    .onWhen(simple("${exception.statusCode} >= 500"))
    .maximumRedeliveries(3)
    .redeliveryDelay(1000)
    .handled(true);

onException(HttpOperationFailedException.class)
    .onWhen(simple("${exception.statusCode} < 500"))
    .handled(true)
    .log(LoggingLevel.ERROR, "Erreur client HTTP, pas de retry: ${exception.statusCode}");
```

---

## 3. Ordre de priorité des handlers

Camel évalue les `onException()` dans l'ordre de leur déclaration et applique le **plus spécifique en premier** (hiérarchie de classes Java).

```java
// ✅ Ordre correct : du plus spécifique au plus général
onException(DataIntegrityViolationException.class) // sous-classe de RuntimeException
    .handled(true)
    .log(LoggingLevel.WARN, "Doublon ignoré");

onException(IllegalArgumentException.class)        // sous-classe de RuntimeException
    .handled(true)
    .log(LoggingLevel.ERROR, "Données invalides");

onException(Exception.class)                       // catch-all en dernier
    .maximumRedeliveries(3)
    .redeliveryDelay(2000)
    .handled(true)
    .log(LoggingLevel.ERROR, "Erreur inattendue");
```

> Si `Exception.class` est déclaré en premier, il capte tout — les handlers suivants
> ne sont jamais atteints.

---

## 4. Portée des handlers

### Portée RouteBuilder (recommandée)

```java
public class MyRouteBuilder extends RouteBuilder {
    @Override
    public void configure() {
        // S'applique à TOUTES les routes de ce RouteBuilder
        onException(Exception.class).handled(true).log("...");

        from("direct:route-a")...;
        from("direct:route-b")...;
    }
}
```

### Portée globale avec `RouteConfigurationBuilder`

Depuis Camel 3.12, on peut définir des handlers partagés entre plusieurs RouteBuilders :

```java
@Component
public class GlobalErrorConfiguration extends RouteConfigurationBuilder {
    @Override
    public void configuration() throws Exception {
        routeConfiguration("global-error-config")
            .onException(Exception.class)
            .maximumRedeliveries(3)
            .handled(true)
            .log(LoggingLevel.ERROR, "Erreur globale: ${exception.message}");
    }
}
```

Puis dans les RouteBuilders qui doivent l'utiliser :

```java
from("direct:my-route")
    .routeConfigurationId("global-error-config")
    ...;
```

---

## 5. Cas d'usage — Application dans ce projet

### Cas 1 : Idempotence des webhooks

Un webhook Unblu peut être re-livré. Si la conversation est déjà en base, on absorbe silencieusement :

```java
onException(DataIntegrityViolationException.class)
    .handled(true)
    .log(LoggingLevel.WARN, "webhook-event-processor",
        "Duplicate webhook event ignored (already persisted): ${exception.message}");
```

### Cas 2 : Données métier invalides

Si une personne Unblu est introuvable (`IllegalArgumentException`), un retry ne servira à rien — on log et on absorbe :

```java
onException(IllegalArgumentException.class)
    .handled(true)
    .log(LoggingLevel.ERROR, "webhook-event-processor",
        "Webhook event rejected due to invalid data: ${exception.message}");
```

### Cas 3 : Failure transiente (réseau, DB momentanément indisponible)

On retente 3 fois avec backoff exponentiel avant d'abandonner :

```java
onException(Exception.class)
    .maximumRedeliveries(3)
    .redeliveryDelay(2000)
    .backOffMultiplier(2)
    .useExponentialBackOff()
    .retryAttemptedLogLevel(LoggingLevel.WARN)
    .handled(true)
    .log(LoggingLevel.ERROR, "webhook-dead-letter",
        "Webhook event processing failed after retries: ${exception.message}");
```

---

## 6. Évolution vers un Dead Letter Channel réel

En production, le log de "dead-letter" ne suffit pas — les événements perdus doivent être
rejoués manuellement. La prochaine étape est d'utiliser un vrai Dead Letter Channel :

```java
// Avec une queue JMS ou une table de base de données
onException(Exception.class)
    .maximumRedeliveries(3)
    .redeliveryDelay(2000)
    .useExponentialBackOff()
    .handled(true)
    .to("jms:queue:webhook-dead-letter");  // ou "sql:INSERT INTO dead_letter..."
```

Pour ce projet, une table PostgreSQL `webhook_dead_letter` serait cohérente avec
l'infrastructure existante :

```sql
CREATE TABLE webhook_dead_letter (
    id          BIGSERIAL PRIMARY KEY,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    event_type  VARCHAR(255),
    payload     TEXT NOT NULL,
    error       TEXT,
    retry_count INT  DEFAULT 0
);
```

---

## 7. Anti-patterns à éviter

| Anti-pattern | Problème | Correction |
|---|---|---|
| `onException()` après `from()` | Ignoré silencieusement par Camel | Toujours déclarer avant `from()` |
| `@Transactional` sur méthode `private` dans un `Processor` | Spring AOP ignore les méthodes `private` | Extraire dans un `@Service` séparé |
| Retry sur `IllegalArgumentException` | Les données invalides ne s'améliorent pas avec le temps | `handled(true)` sans retry |
| Catch-all `Exception` en premier | Masque tous les handlers spécifiques | Toujours mettre `Exception` en dernier |
| Pas de `handled(true)` | L'exception est propagée à l'appelant HTTP → le webhook Unblu reçoit une erreur et re-livre | Toujours gérer explicitement |
