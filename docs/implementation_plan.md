# Objectif
Restructurer l'application monolithique Spring Boot actuelle (`org.dbs.poc.unblu`) en un projet Maven multi-modules respectant les principes de l'Architecture Hexagonale et intégrant Apache Camel dans la couche d'infrastructure.

## Modifications Proposées

---

### Refonte du Projet Base (Parent)
Transformation du projet racine actuel en projet de type `pom` pour agréger les sous-modules. Déplacement des fichiers sources actuels vers les futurs sous-modules.

#### [MODIFY] pom.xml (Racine)
- Changement du `<packaging>` vers `pom`.
- Ajout de la section `<modules>` : `unblu-domain`, `unblu-infrastructure`, `unblu-application`.
- Ajout de la section `<dependencyManagement>` pour les versions globales (Unblu OpenAPI, Camel, etc.).

---

### Module : `unblu-domain`
Ce module ne contiendra aucune dépendance Spring ou Camel. Il sera du pur Java décrivant le métier.

#### [NEW] unblu-domain/pom.xml
Dépendances : Uniquement `lombok`. (Les DTOs d'Unblu générés par OpenAPI seront considérés hors du domaine pur, le domaine liera à un DTO interne, ou on fera le pont au niveau infrastructure).

#### [NEW] unblu-domain/src/main/java/org/dbs/poc/unblu/domain/model/*
- `ConversationContext.java`
- `CustomerProfile.java`

#### [NEW] unblu-domain/src/main/java/org/dbs/poc/unblu/domain/port/primary/
Points d'entrées (Use Cases) de l'application (ex: `StartConversationUseCase`).

#### [NEW] unblu-domain/src/main/java/org/dbs/poc/unblu/domain/port/secondary/
Interfaces requises par le domaine pour communiquer avec l'extérieur (ex: `UnbluConversationGateway`).

---

### Module : `unblu-infrastructure`
Ce module contiendra les dépendances web, REST, Camel, et le SDK Unblu OpenAPI. Il implémente les interfaces décrites dans `unblu-domain`.

#### [NEW] unblu-infrastructure/pom.xml
Dépendances : `unblu-domain`, `spring-boot-starter-web`, `camel-spring-boot-starter`, `jersey3-client-v4` (Unblu), etc.

#### [NEW] unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/orchestration/*
- Les Routes Camel (`MainOrchestratorRoute`).

#### [NEW] unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/rest/*
- `UnbluTestController` et `HomeController` (déplacés depuis leur emplacement actuel).

#### [NEW] unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/unblu/*
- Remplacement ciblé de `UnbluService.java` par `UnbluApiAdapter` (implémentant les éventuels ports secondaires de la classe via Camel et `ApiClient`).
- Déplacement des classes `config/` (`UnbluClientConfig`, `UnbluProperties`).
- Déplacement de `exception/` (`GlobalExceptionHandler`).

---

### Module : `unblu-application`
L'exécutable final. Le module qui assemble le domaine et l'infrastructure via le conteneur IoC Spring.

#### [NEW] unblu-application/pom.xml
Dépendances : `unblu-domain`, `unblu-infrastructure`, `spring-boot-starter`. Configuration du plugin `spring-boot-maven-plugin`.

#### [NEW] unblu-application/src/main/java/org/dbs/poc/unblu/UnbluApplication.java
Déplacé depuis le répertoire d'origine. C'est la classe contenant le `@SpringBootApplication`.

#### [NEW] unblu-application/src/main/resources/application.yml
Déplacé (ou copié) depuis le projet d'origine.

---

## Plan de Vérification

### Compilation
- `mvn clean install -DskipTests` pour s'assurer que la restructuration multi-modules compile parfaitement et que les dépendances cycliques sont évitées.

### Tests Unitaires
- Les tests existants (`UnbluApplicationTests` et `UnbluProxyConfigTest`) seront déplacés vers les sous-modules appropriés (le test de config ira dans `unblu-infrastructure` ou `unblu-application`).
- Lancement de `mvn test` pour valider que tous les contextes Spring chargent bien l'architecture éclatée.

### Vérification Manuelle
- Le développeur vérifiera en exécutant le Spring Boot généré (`mvn spring-boot:run` dans le module `unblu-application`) que le Swagger remonte bien, indiquant que les RestControllers du module infrastructure sont bien mappés.
