# Monitoring des Jobs Quartz

## Vue d'ensemble

Un nouvel onglet "⚙️ Jobs" a été ajouté au frontend Angular pour monitorer en temps réel les jobs Quartz du module `jobs` (port 8085).

## Fonctionnalités

### 1. Statut du Scheduler
- Nom et ID de l'instance du scheduler
- État (démarré/arrêté)
- Mode standby
- Horodatage de la dernière mise à jour

### 2. Jobs en cours d'exécution
- Liste des jobs actuellement en cours
- Heure de démarrage
- Durée d'exécution en millisecondes
- Compteur du nombre de jobs actifs

### 3. Jobs planifiés
- Liste complète de tous les jobs configurés
- Informations détaillées pour chaque job :
  - Nom et groupe
  - Description
  - Classe Java
  - Triggers associés avec :
    - État (NORMAL, PAUSED, BLOCKED, ERROR)
    - Expression cron
    - Dernière exécution
    - Prochaine exécution planifiée

### 4. Rafraîchissement automatique
- Auto-refresh toutes les 5 secondes (activable/désactivable)
- Bouton de rafraîchissement manuel
- Indicateurs visuels de l'état des jobs

## Architecture

### Backend (Module jobs - Port 8085)

**Endpoints REST:**
- `GET /api/jobs/status` - Récupère le statut complet du scheduler et de tous les jobs
- `GET /api/jobs/running` - Récupère uniquement les jobs en cours d'exécution

**Contrôleur:**
- [`JobMonitoringController.java`](../../jobs/src/main/java/org/dbs/poc/unblu/jobs/rest/JobMonitoringController.java:22)

**Proxy (Module unblu-configuration - Port 8081):**
- [`JobsProxyController.java`](../../unblu-exposition/src/main/java/org/dbs/poc/unblu/exposition/rest/JobsProxyController.java:1) - Reverse proxy transparent qui forwarde `/api/jobs/**` vers le service jobs (port 8085)

### Frontend (Angular)

**Composant:**
- [`jobs-monitoring.component.ts`](../../unblu-frontend/src/app/components/jobs-monitoring/jobs-monitoring.component.ts:1)
- [`jobs-monitoring.component.html`](../../unblu-frontend/src/app/components/jobs-monitoring/jobs-monitoring.component.html:1)
- [`jobs-monitoring.component.css`](../../unblu-frontend/src/app/components/jobs-monitoring/jobs-monitoring.component.css:1)

**Modèles TypeScript:**
- [`job.model.ts`](../../unblu-frontend/src/app/models/job.model.ts:1)

**Service API:**
- Méthodes ajoutées dans [`api.service.ts`](../../unblu-frontend/src/app/services/api.service.ts:1):
  - `getJobsStatus()`
  - `getRunningJobs()`

**Configuration proxy:**
- [`proxy.conf.json`](../../unblu-frontend/proxy.conf.json:1) - Route `/api/jobs/**` vers `http://localhost:8085`

## Utilisation

1. Démarrer le module jobs:
   ```bash
   cd jobs
   mvn spring-boot:run
   ```

2. Démarrer le frontend (en mode dev):
   ```bash
   cd unblu-frontend
   npm start
   ```

3. Accéder à l'interface:
   - Ouvrir http://localhost:4200
   - Cliquer sur l'onglet "⚙️ Jobs"

4. Ou accéder via l'application principale (après build):
   ```bash
   cd unblu-configuration
   mvn spring-boot:run
   ```
   - Ouvrir http://localhost:8081
   - Cliquer sur l'onglet "⚙️ Jobs"

## Jobs actuellement configurés

### ConversationStatisticsJob
- **Groupe:** statistics
- **Description:** Génère les statistiques de conversations
- **Planification:** Toutes les 30 minutes (cron: `0 0/30 * * * ?`)
- **Classe:** [`ConversationStatisticsJob.java`](../../jobs/src/main/java/org/dbs/poc/unblu/jobs/quartz/ConversationStatisticsJob.java:1)

## Interface utilisateur

### Indicateurs visuels

**États des triggers:**
- 🟢 NORMAL - Job actif et planifié normalement
- ⏸️ PAUSED - Job en pause
- 🚫 BLOCKED - Job bloqué
- ❌ ERROR - Job en erreur
- ❓ UNKNOWN - État inconnu

**Boutons d'action:**
- 🔄 Rafraîchir - Recharge manuellement les données
- ▶️/⏸️ Auto-refresh - Active/désactive le rafraîchissement automatique (5s)

### Sections de l'interface

1. **En-tête** - Actions de rafraîchissement
2. **Statut du Scheduler** - Informations globales
3. **Jobs en cours** - Liste des exécutions actives
4. **Jobs planifiés** - Configuration détaillée de chaque job

## Configuration

### Port du service jobs
Défini dans [`application.yml`](../../jobs/src/main/resources/application.yml:2):
```yaml
server:
  port: 8085
```

### Configuration du proxy
Définie dans [`application.yml`](../../unblu-configuration/src/main/resources/application.yml:124) du module unblu-configuration:
```yaml
jobs:
  base-url: ${JOBS_BASE_URL:http://localhost:8085}
```

Le contrôleur proxy [`JobsProxyController`](../../unblu-exposition/src/main/java/org/dbs/poc/unblu/exposition/rest/JobsProxyController.java:1) utilise cette configuration pour rediriger les appels du frontend (qui arrive sur le port 8081) vers le service jobs (port 8085).

### Quartz Configuration
Définie dans [`QuartzConfig.java`](../../jobs/src/main/java/org/dbs/poc/unblu/jobs/config/QuartzConfig.java:1):
- Job store: Memory
- Thread pool: 5 threads
- Scheduler name: UnbluJobsScheduler

## Dépannage

### Le module jobs ne démarre pas
- Vérifier que le port 8085 est disponible
- Vérifier les variables d'environnement de la base de données

### Les données ne s'affichent pas
- Vérifier que le module jobs est démarré sur le port 8085
- Vérifier la configuration du proxy dans `proxy.conf.json`
- Consulter la console du navigateur pour les erreurs

### Auto-refresh ne fonctionne pas
- Vérifier que le bouton Auto-refresh est activé (vert)
- Vérifier la console pour les erreurs réseau

## Évolutions futures possibles

- Ajout de la possibilité de déclencher manuellement un job
- Ajout de la possibilité de mettre en pause/reprendre un job
- Historique des exécutions avec succès/échecs
- Graphiques de performance
- Notifications en cas d'erreur
- Export des statistiques d'exécution
