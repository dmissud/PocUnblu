# Problème gzip sur les outbound requests bot Unblu

## Contexte

Unblu appelle le endpoint `/api/bot/outbound` de façon synchrone lors des événements de dialog
(onboarding, message, dialog ouvert/fermé). Ces appels sont émis par le composant **Unblu-Hookshot**
avec le header suivant :

```
Accept-Encoding: gzip
```

## Le problème

Si la compression HTTP est activée côté serveur (comportement par défaut de Spring Boot / Tomcat
dès que la réponse dépasse un certain seuil), Tomcat répond avec un body **gzip-compressé** et
ajoute `Content-Encoding: gzip` à la réponse.

**Unblu-Hookshot ne décompresse pas cette réponse.** Il interprète les bytes gzip comme du JSON
plain → échec de parsing → Unblu considère l'appel sans réponse valide et le bot ne répond jamais.

Symptôme observable côté ngrok : la requête apparaît dans le dashboard ngrok avec un statut reçu,
mais Unblu ne reçoit pas de réponse exploitable.

## Solution

### 1. Désactivation de la compression dans livekit

Dans `livekit/src/main/resources/application.yml` :

```yaml
server:
  port: 8082
  compression:
    enabled: false  # Voir HOWTO_BOT_OUTBOUND_GZIP.md
```

Cela garantit que livekit répond toujours en JSON plain, que l'appel arrive directement d'Unblu
ou via le proxy reverse de `unblu-configuration`.

### 2. Exclusion de `Accept-Encoding` dans les proxy controllers

Lorsque les appels transitent par `unblu-configuration` (profil `ngrok`), le proxy
`BotOutboundProxyController` utilise `ProxyHeaders.extract()` qui exclut `Accept-Encoding`
avant de transmettre la requête à livekit.

Classe concernée : `unblu-exposition/.../rest/config/ProxyHeaders.java`

```java
private static final Set<String> EXCLUDED = Set.of(
    "host",
    "content-length",
    "transfer-encoding",
    "connection",
    "accept-encoding"   // ← évite la réponse gzip vers les backends
);
```

## Règle à respecter

> **Ne jamais activer `server.compression.enabled=true` dans `livekit/application.yml`**
> sans avoir vérifié que le client Unblu-Hookshot est capable de décompresser les réponses gzip.

## Headers Unblu-Hookshot typiques (référence)

Extrait d'une requête réelle reçue sur `/api/bot/outbound` :

```
User-Agent: Unblu-Hookshot
Content-Type: application/json; charset=UTF-8
X-Unblu-Service-Name: outbound.bot.onboarding_offer
X-Unblu-Version: v4
Accept-Encoding: gzip          ← déclencheur du problème
```

## Fichiers impliqués

| Fichier | Rôle |
|---|---|
| `livekit/src/main/resources/application.yml` | Compression désactivée |
| `unblu-exposition/.../rest/config/ProxyHeaders.java` | Filtre `accept-encoding` en mode proxy |
| `unblu-exposition/.../rest/BotOutboundProxyController.java` | Proxy `/api/bot/outbound` → livekit:8082 |
