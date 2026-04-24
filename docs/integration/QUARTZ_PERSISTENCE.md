# Configuration de la persistance Quartz

## Vue d'ensemble

Quartz Scheduler peut fonctionner en deux modes:
1. **Mode mémoire** (par défaut) - Les jobs sont perdus au redémarrage
2. **Mode JDBC** - Les jobs sont persistés en base de données PostgreSQL

## Configuration actuelle

Le module `jobs` est configuré pour utiliser la **persistance JDBC** avec PostgreSQL.

### Fichier de configuration

[`jobs/src/main/resources/application.yml`](../../jobs/src/main/resources/application.yml:32)

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always
    properties:
      org:
        quartz:
          scheduler:
            instanceName: UnbluJobsScheduler
            instanceId: AUTO
          threadPool:
            threadCount: 5
          jobStore:
            class: org.springframework.scheduling.quartz.LocalDataSourceJobStore
            driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
            useProperties: false
            tablePrefix: QRTZ_
            isClustered: true
            clusterCheckinInterval: 20000
```

## Avantages de la persistance JDBC

### 1. Persistance des jobs
- Les jobs et triggers survivent aux redémarrages de l'application
- Pas de perte de planification en cas de crash

### 2. Clustering
- Plusieurs instances peuvent partager la même base de données
- Load balancing automatique des jobs entre instances
- Haute disponibilité (failover automatique)

### 3. Historique
- Les informations d'exécution sont conservées
- Possibilité d'audit et de monitoring avancé

## Initialisation automatique du schéma

Spring Boot gère automatiquement la création des tables Quartz grâce à:

```yaml
spring:
  quartz:
    jdbc:
      initialize-schema: always
```

### Options disponibles:
- `always` - Crée/recrée les tables à chaque démarrage (⚠️ développement uniquement)
- `never` - Ne crée jamais les tables (production, gérées par Liquibase/Flyway)
- `embedded` - Crée uniquement pour les bases embarquées (H2, HSQL)

### Tables créées automatiquement

Spring Boot utilise les scripts SQL fournis par Quartz:
- `org/quartz/impl/jdbcjobstore/tables_postgres.sql`

Tables principales:
- `QRTZ_JOB_DETAILS` - Définitions des jobs
- `QRTZ_TRIGGERS` - Triggers associés aux jobs
- `QRTZ_CRON_TRIGGERS` - Triggers de type cron
- `QRTZ_SIMPLE_TRIGGERS` - Triggers simples
- `QRTZ_FIRED_TRIGGERS` - Jobs en cours d'exécution
- `QRTZ_SCHEDULER_STATE` - État des instances du scheduler
- `QRTZ_LOCKS` - Verrous pour la synchronisation en cluster
- `QRTZ_CALENDARS` - Calendriers personnalisés
- `QRTZ_PAUSED_TRIGGER_GRPS` - Groupes de triggers en pause
- `QRTZ_BLOB_TRIGGERS` - Triggers avec données binaires
- `QRTZ_SIMPROP_TRIGGERS` - Triggers avec propriétés simples

## Configuration du clustering

### Paramètres clés

```yaml
jobStore:
  isClustered: true
  clusterCheckinInterval: 20000  # 20 secondes
```

- `isClustered: true` - Active le mode cluster
- `clusterCheckinInterval` - Fréquence de vérification entre instances (en ms)

### Fonctionnement du cluster

1. Chaque instance s'enregistre dans `QRTZ_SCHEDULER_STATE`
2. Les instances se synchronisent via `QRTZ_LOCKS`
3. Si une instance tombe, les autres reprennent ses jobs
4. Load balancing automatique des exécutions

## Migration vers la persistance

### Depuis le mode mémoire

Si vous aviez `job-store-type: memory`, changez simplement vers `jdbc`:

```yaml
spring:
  quartz:
    job-store-type: jdbc  # Était: memory
    jdbc:
      initialize-schema: always
```

### Première exécution

Au premier démarrage avec `initialize-schema: always`:
1. Spring Boot détecte PostgreSQL
2. Charge le script `tables_postgres.sql` de Quartz
3. Crée toutes les tables avec le préfixe `QRTZ_`
4. Les jobs définis dans [`QuartzConfig.java`](../../jobs/src/main/java/org/dbs/poc/unblu/jobs/config/QuartzConfig.java:1) sont automatiquement persistés

## Production

### Recommandations

Pour la production, utilisez `initialize-schema: never` et gérez le schéma via Liquibase:

```yaml
spring:
  quartz:
    jdbc:
      initialize-schema: never  # Production
```

Puis créez une migration Liquibase qui exécute le script SQL de Quartz.

### Monitoring

Les tables Quartz peuvent être interrogées pour le monitoring:

```sql
-- Jobs planifiés
SELECT job_name, job_group, description
FROM qrtz_job_details
WHERE sched_name = 'UnbluJobsScheduler';

-- Prochaines exécutions
SELECT trigger_name, trigger_group, next_fire_time
FROM qrtz_triggers
WHERE sched_name = 'UnbluJobsScheduler'
ORDER BY next_fire_time;

-- Instances actives du cluster
SELECT instance_name, last_checkin_time, checkin_interval
FROM qrtz_scheduler_state
WHERE sched_name = 'UnbluJobsScheduler';
```

## Dépannage

### Les tables ne sont pas créées

Vérifiez:
1. La connexion à PostgreSQL est correcte
2. `spring.quartz.jdbc.initialize-schema` est défini
3. Les logs Spring Boot au démarrage

### Jobs perdus après redémarrage

Si les jobs disparaissent:
1. Vérifiez que `job-store-type: jdbc` (pas `memory`)
2. Vérifiez que les jobs sont marqués `storeDurably()` dans [`QuartzConfig`](../../jobs/src/main/java/org/dbs/poc/unblu/jobs/config/QuartzConfig.java:25)

### Problèmes de clustering

Si le clustering ne fonctionne pas:
1. Vérifiez `isClustered: true`
2. Toutes les instances doivent pointer vers la même base
3. Les horloges des serveurs doivent être synchronisées (NTP)
4. Vérifiez les logs pour les erreurs de lock

## Différences avec Spring Batch

| Aspect | Quartz | Spring Batch |
|--------|--------|--------------|
| Initialisation schéma | Automatique par Spring Boot | Manuel via Liquibase/Flyway |
| Scripts SQL | Fournis par Quartz | Fournis par Spring Batch |
| Configuration | `spring.quartz.jdbc.initialize-schema` | Migrations manuelles |
| Tables | Préfixe `QRTZ_` | Préfixe `BATCH_` |

**Quartz** gère son schéma automatiquement, contrairement à **Spring Batch** qui nécessite des migrations explicites.

## Références

- [Documentation Quartz JDBC JobStore](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-09.html)
- [Spring Boot Quartz Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.quartz)
- [Quartz Clustering](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/ConfigJDBCJobStoreClustering.html)
