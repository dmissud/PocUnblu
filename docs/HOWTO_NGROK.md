# Guide Ngrok - Configuration et Supervision

## 1. Création du compte Ngrok

1. Aller sur [https://ngrok.com](https://ngrok.com)
2. Cliquer sur **Sign up** (ou **Get started for free**)
3. S'inscrire avec un email ou via GitHub/Google
4. Confirmer l'email de vérification

## 2. Installation de Ngrok

### macOS (via Homebrew)

```bash
brew install ngrok/ngrok/ngrok
```

### Linux/Windows

Télécharger depuis [https://ngrok.com/download](https://ngrok.com/download) et suivre les instructions.

## 3. Configuration de l'Authtoken

### Récupération du token

1. Se connecter sur [https://dashboard.ngrok.com](https://dashboard.ngrok.com)
2. Aller dans **Your Authtoken** (menu de gauche)
3. Copier le token affiché (format : `2abcDEFghiJKLmno3PQRstuVWXyz4ABC_5defGHIjklMNOpqrSTUv`)

### Enregistrement du token

```bash
ngrok config add-authtoken VOTRE_TOKEN_ICI
```

Le token est stocké dans `~/.config/ngrok/ngrok.yml` (macOS/Linux) ou `%USERPROFILE%\.ngrok2\ngrok.yml` (Windows).

## 4. Démarrage du tunnel

⚠️ **IMPORTANT** : **NE PAS lancer ngrok manuellement** avec `ngrok http 8081`

Le tunnel ngrok est **démarré par l'application PocUnblu** via le composant `NgrokManager` lorsque vous appelez
l'endpoint de setup du webhook :

```bash
# Démarrer l'application
mvn spring-boot:run -pl unblu-configuration

# Dans un autre terminal, déclencher le setup du webhook (qui démarre ngrok)
curl -X POST http://localhost:8081/api/v1/webhooks/setup
```

### Résultat attendu dans les logs de l'application

```
INFO  NgrokManager - Starting ngrok tunnels (webhook-entrypoint:8083, livekit:8082)...
INFO  NgrokManager - Ngrok tunnels ready — webhook-entrypoint: https://abc123.ngrok-free.app, livekit: https://def456.ngrok-free.app
```

Deux tunnels indépendants, accès direct sans proxy :
- `https://abc123.ngrok-free.app` → webhook-entrypoint (8083)
- `https://def456.ngrok-free.app` → livekit/bot (8082)

**URL publique générée** : visible dans les logs et enregistrée automatiquement dans Unblu

## 5. Supervision locale - Dashboard Web

### Accès au dashboard

Une fois ngrok lancé, ouvrir dans un navigateur :

```
http://localhost:4040
```

### Fonctionnalités du dashboard

- **Requests** : visualisation en temps réel de toutes les requêtes HTTP reçues
- **Replay** : renvoi d'une requête reçue (utile pour tester)
- **Status** : état du tunnel et statistiques

### Détail d'une requête

Cliquer sur une requête pour voir :

- Headers HTTP (request/response)
- Body JSON complet
- Timing et status code
- Option **Replay** pour rejouer la requête

## 6. Supervision en ligne - Cloud Dashboard

### Accès au dashboard cloud

1. Se connecter sur [https://dashboard.ngrok.com](https://dashboard.ngrok.com)
2. Aller dans **Endpoints** → **Cloud Edge** → **Endpoints**

### Informations disponibles

- **Status** : tunnels actifs/inactifs
- **Endpoints** : liste des URLs publiques actives
- **Events** : log des connexions et requêtes (historique limité selon le plan)
- **Traffic Inspector** : analyse du trafic (plans payants uniquement)

### Limite du plan gratuit

- 3 endpoints simultanés (2 tunnels suffisent pour ce projet)
- URLs aléatoires (changent à chaque redémarrage)
- Inspection du trafic limitée

## 7. Intégration avec PocUnblu

### Configuration dans `.env` (optionnelle)

```bash
UNBLU_WEBHOOK_NAME=unblu-poc-webhook
UNBLU_WEBHOOK_LOCAL_PORT=8081
UNBLU_WEBHOOK_EVENTS=conversation.created
UNBLU_WEBHOOK_ENDPOINT_PATH=/api/webhooks/unblu
```

### Workflow complet

```bash
# 1. Démarrer l'application
mvn spring-boot:run -pl unblu-configuration

# 2. Déclencher le setup du webhook (démarre ngrok + enregistre dans Unblu)
curl -X POST http://localhost:8081/api/v1/webhooks/setup

# 3. Vérifier le status
curl http://localhost:8081/api/v1/webhooks/status

# 4. Quand terminé, arrêter le tunnel
curl -X POST http://localhost:8081/api/v1/webhooks/teardown?deleteWebhook=true
```

## 8. Arrêt du tunnel

Le tunnel ngrok s'arrête **automatiquement** lors de l'arrêt de l'application PocUnblu (Ctrl+C dans le terminal Maven).

Le `NgrokManager` se charge de fermer proprement le processus ngrok.

## 9. Troubleshooting

### Bot ne répond pas (ngrok reçoit l'appel mais pas de réponse)

Unblu envoie `Accept-Encoding: gzip` sur les outbound requests bot. Si la compression est activée
côté livekit, la réponse est gzip-compressée et Unblu ne peut pas la parser.

→ Voir [`HOWTO_BOT_OUTBOUND_GZIP.md`](./HOWTO_BOT_OUTBOUND_GZIP.md) pour le détail et la solution.

### Erreur "authtoken not found"

```bash
ngrok config add-authtoken VOTRE_TOKEN_ICI
```

### Port 8081 déjà utilisé

```bash
lsof -i :8081
# Ou changer le port dans application.properties
```

### URL ngrok non accessible depuis Unblu

- Vérifier que le firewall autorise les connexions entrantes
- Tester l'URL publique depuis un navigateur externe
- Vérifier les logs ngrok (`http://localhost:4040`)

### Tunnel déconnecté fréquemment

- Plan gratuit : timeout après inactivité
- Solution : passer à un plan payant ou relancer le tunnel

## 10. Commandes utiles

```bash
# Version de ngrok (vérifier l'installation)
ngrok version

# Configuration actuelle (vérifier le token)
ngrok config check
```

⚠️ **Note** : Les commandes `ngrok http 8081` ne sont **pas nécessaires** car le tunnel est géré automatiquement par
l'application.

## Références

- Documentation officielle : [https://ngrok.com/docs](https://ngrok.com/docs)
- Dashboard : [https://dashboard.ngrok.com](https://dashboard.ngrok.com)
- Pricing : [https://ngrok.com/pricing](https://ngrok.com/pricing)
