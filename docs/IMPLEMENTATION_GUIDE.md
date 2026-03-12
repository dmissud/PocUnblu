# Implementation Guide: Strict 5-Module Hexagonal Architecture

This guide provides exact, step-by-step instructions for an AI agent to reproduce all changes made
to the codebase from commit `cdf2c23` (docs: add orchestration architecture comparison) through to
the current state of the `feature/strict-hexagonal-architecture` branch.

Apply every change described here to a fresh checkout at commit `cdf2c23`.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Step 1 — Root pom.xml](#step-1--root-pomxml)
4. [Step 2 — unblu-domain module](#step-2--unblu-domain-module)
5. [Step 3 — unblu-application module](#step-3--unblu-application-module)
6. [Step 4 — unblu-infrastructure module](#step-4--unblu-infrastructure-module)
7. [Step 5 — New module: unblu-exposition](#step-5--new-module-unblu-exposition)
8. [Step 6 — New module: unblu-configuration](#step-6--new-module-unblu-configuration)
9. [Step 7 — Configuration files](#step-7--configuration-files)
10. [Key Constraints and Unblu API Limitations](#key-constraints-and-unblu-api-limitations)

---

## Overview

Before commit `cdf2c23` the project had three Maven modules:
- `unblu-domain`
- `unblu-infrastructure`
- `unblu-application` (depended on `unblu-infrastructure`, contained the Spring Boot main class, had Camel and the Spring Boot Maven plugin)

After the changes:
- The project has **five** Maven modules following a strict layered hexagonal architecture.
- Two new modules are created: `unblu-exposition` and `unblu-configuration`.
- `unblu-application` is purified: it depends only on `unblu-domain`, has no Camel, no Spring Boot plugin, and no main class.
- `unblu-configuration` becomes the sole Spring Boot runnable module (contains `@SpringBootApplication`, has the Spring Boot Maven plugin).
- `unblu-exposition` contains all REST controllers and DTOs.
- `unblu-infrastructure` gains new adapters for persons/teams search, direct conversations, summary messaging, and bot creation.
- `unblu-domain` gains new model classes and port interfaces required by the new use cases.

---

## Architecture Diagram

```
HTTP Clients
     |
     v
+--------------------+
|  unblu-exposition  |  REST Controllers, DTOs
|  (port 8081)       |  @RestController beans
+--------------------+
          |
          | calls use case interfaces (ports in)
          v
+--------------------+
| unblu-application  |  Use Cases, Application Services
|                    |  @Service beans
+--------------------+
     |         |
     |         | calls secondary ports (interfaces)
     |         v
     |  +--------------------+
     |  |   unblu-domain     |  Domain models, port interfaces
     |  |                    |  Plain Java (no Spring)
     |  +--------------------+
     |         ^
     |         | implements secondary ports
     v         |
+--------------------+
| unblu-infrastructure|  Camel routes, Unblu SDK adapters,
|                    |  mock adapters, exception handlers
+--------------------+
          |
          v
+--------------------+
| unblu-configuration|  Spring Boot main class, assembles all
|  (runnable JAR)    |  modules via component scan
+--------------------+

Dependency direction (compile):
  exposition  -> application -> domain
  infrastructure              -> domain
  configuration -> all four modules
```

---

## Step 1 — Root pom.xml

**File:** `pom.xml`

Replace the `<modules>` block and `<dependencyManagement>` section.

### 1a. Replace `<modules>` block

**Before:**
```xml
<modules>
    <module>unblu-domain</module>
    <module>unblu-infrastructure</module>
    <module>unblu-application</module>
</modules>
```

**After:**
```xml
<modules>
    <module>unblu-domain</module>
    <module>unblu-infrastructure</module>
    <module>unblu-application</module>
    <module>unblu-exposition</module>
    <module>unblu-configuration</module>
</modules>
```

### 1b. Add missing artifacts to `<dependencyManagement>`

Inside `<dependencyManagement><dependencies>`, after the existing `unblu-infrastructure` entry, add:

```xml
<dependency>
    <groupId>org.dbs.poc</groupId>
    <artifactId>unblu-application</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>org.dbs.poc</groupId>
    <artifactId>unblu-exposition</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>org.dbs.poc</groupId>
    <artifactId>unblu-configuration</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 1c. Full resulting root pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.11</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>org.dbs.poc</groupId>
    <artifactId>unblu</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>unblu-parent</name>
    <description>Unblu Integration PoC with Hexagonal Architecture</description>
    <packaging>pom</packaging>

    <properties>
        <java.version>21</java.version>
        <unblu.version>8.30.1</unblu.version>
        <camel.version>4.18.0</camel.version>
        <springdoc.version>2.8.0</springdoc.version>
        <dotenv.version>4.0.0</dotenv.version>
    </properties>

    <modules>
        <module>unblu-domain</module>
        <module>unblu-infrastructure</module>
        <module>unblu-application</module>
        <module>unblu-exposition</module>
        <module>unblu-configuration</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- Modules Internes -->
            <dependency>
                <groupId>org.dbs.poc</groupId>
                <artifactId>unblu-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.dbs.poc</groupId>
                <artifactId>unblu-infrastructure</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.dbs.poc</groupId>
                <artifactId>unblu-application</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.dbs.poc</groupId>
                <artifactId>unblu-exposition</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.dbs.poc</groupId>
                <artifactId>unblu-configuration</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- Camel -->
            <dependency>
                <groupId>org.apache.camel.springboot</groupId>
                <artifactId>camel-spring-boot-starter</artifactId>
                <version>${camel.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.camel.springboot</groupId>
                <artifactId>camel-rest-starter</artifactId>
                <version>${camel.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.camel.springboot</groupId>
                <artifactId>camel-jackson-starter</artifactId>
                <version>${camel.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.camel.springboot</groupId>
                <artifactId>camel-resilience4j-starter</artifactId>
                <version>${camel.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.camel.springboot</groupId>
                <artifactId>camel-http-starter</artifactId>
                <version>${camel.version}</version>
            </dependency>

            <!-- Unblu OpenAPI -->
            <dependency>
                <groupId>com.unblu.openapi</groupId>
                <artifactId>jersey3-client-v4</artifactId>
                <version>${unblu.version}</version>
            </dependency>

            <!-- Springdoc OpenAPI (Swagger) -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>

            <!-- Dotenv support -->
            <dependency>
                <groupId>me.paulschwarz</groupId>
                <artifactId>spring-dotenv</artifactId>
                <version>${dotenv.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

</project>
```

---

## Step 2 — unblu-domain module

The domain module's `pom.xml` does **not** change. Only new Java source files are added.

### 2a. unblu-domain/pom.xml (unchanged — for reference)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.dbs.poc</groupId>
        <artifactId>unblu</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>unblu-domain</artifactId>
    <name>unblu-domain</name>
    <description>Core Business Logic for Unblu Orchestration</description>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
```

### 2b. New domain model files

Base package: `org.dbs.poc.unblu.domain.model`
Base source path: `unblu-domain/src/main/java/org/dbs/poc/unblu/domain/model/`

#### `PersonSource.java`

```java
package org.dbs.poc.unblu.domain.model;

public enum PersonSource {
    USER_DB,
    VIRTUAL
}
```

#### `PersonInfo.java`

```java
package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonInfo {
    private String id;
    private String sourceId;
    private String displayName;
    private String email;
}
```

#### `TeamInfo.java`

```java
package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamInfo {
    private String id;
    private String name;
    private String description;
}
```

#### `UnbluConversationInfo.java`

```java
package org.dbs.poc.unblu.domain.model;

public record UnbluConversationInfo(String unbluConversationId, String unbluJoinUrl) {
}
```

#### Existing files that must already exist (verify they are present at `cdf2c23`)

- `ChatAccessDeniedException.java`
- `ChatRoutingDecision.java`
- `ConversationContext.java`
- `CustomerProfile.java`

If any are missing, create them:

**`ChatAccessDeniedException.java`**
```java
package org.dbs.poc.unblu.domain.model;

public class ChatAccessDeniedException extends RuntimeException {

    private final String reason;

    public ChatAccessDeniedException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
```

**`ChatRoutingDecision.java`**
```java
package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoutingDecision {
    private boolean isAuthorized;
    private String unbluAssignedGroupId;
    private String routingReason;
}
```

**`ConversationContext.java`**
```java
package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private String initialClientId;
    private String originApplication;

    // Enriched during orchestration
    private CustomerProfile customerProfile;
    private ChatRoutingDecision routingDecision;

    // Final Unblu result
    private String unbluConversationId;
    private String unbluJoinUrl;
}
```

**`CustomerProfile.java`**
```java
package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile {
    private String customerId;
    private String firstName;
    private String lastName;
    private String customerSegment; // e.g. "VIP", "STANDARD", "PRO"
    private boolean isKnown;
}
```

### 2c. New secondary port interfaces

Base package: `org.dbs.poc.unblu.domain.port.secondary`
Base source path: `unblu-domain/src/main/java/org/dbs/poc/unblu/domain/port/secondary/`

#### `ConversationSummaryPort.java`

```java
package org.dbs.poc.unblu.domain.port.secondary;

public interface ConversationSummaryPort {
    /**
     * Generates a summary for a conversation.
     * @param conversationId the Unblu conversation ID
     * @return a summary text
     */
    String generateSummary(String conversationId);
}
```

#### `ErpPort.java`

```java
package org.dbs.poc.unblu.domain.port.secondary;

import org.dbs.poc.unblu.domain.model.CustomerProfile;

public interface ErpPort {
    /**
     * Retrieves the customer profile from the ERP system.
     * @param clientId The ID of the client to retrieve
     * @return The customer profile
     */
    CustomerProfile getCustomerProfile(String clientId);
}
```

#### `RuleEnginePort.java`

```java
package org.dbs.poc.unblu.domain.port.secondary;

import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;

public interface RuleEnginePort {
    /**
     * Evaluates the routing decision based on the conversation context.
     * @param context The current conversation context including customer profile
     * @return The routing decision
     */
    ChatRoutingDecision evaluateRouting(ConversationContext context);
}
```

#### `UnbluPort.java`

```java
package org.dbs.poc.unblu.domain.port.secondary;

import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

import java.util.List;

public interface UnbluPort {
    /**
     * Creates a new conversation in Unblu and returns its info.
     */
    UnbluConversationInfo createConversation(ConversationContext context);

    /**
     * Searches for persons in Unblu.
     * @param sourceId optional filter by source ID
     * @param personSource optional filter by person source (USER_DB or VIRTUAL)
     */
    List<PersonInfo> searchPersons(String sourceId, PersonSource personSource);

    List<TeamInfo> searchTeams();

    /**
     * Creates a direct conversation between a VIRTUAL person and a USER_DB agent.
     * @param virtualPerson the VIRTUAL participant
     * @param agentPerson   the USER_DB agent participant
     * @param subject       the conversation subject
     */
    UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject);

    /**
     * Sends the summary as a message in the conversation on behalf of the configured summary bot.
     */
    void addSummaryToConversation(String conversationId, String summary);

    String createBot(String name, String description);
}
```

---

## Step 3 — unblu-application module

### 3a. Replace unblu-application/pom.xml

The old pom.xml at `cdf2c23` depended on `unblu-infrastructure`, used `camel-spring-boot-starter`,
had the Spring Boot Maven plugin, and did not have Lombok. Replace it entirely:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.dbs.poc</groupId>
        <artifactId>unblu</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>unblu-application</artifactId>
    <name>unblu-application</name>
    <description>Application Layer: Use Cases and Services</description>

    <dependencies>
        <dependency>
            <groupId>org.dbs.poc</groupId>
            <artifactId>unblu-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <!-- Adding Lombok for Use Case Services -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

Key changes from prior version:
- Dependency changed from `unblu-infrastructure` to `unblu-domain`
- `camel-spring-boot-starter` removed; `spring-boot-starter` added
- `lombok` dependency added
- Spring Boot Maven plugin `<build>` section removed entirely

### 3b. Remove old application-layer files

At `cdf2c23`, the `unblu-application` module may have contained the `@SpringBootApplication` class
and possibly Camel-coupled services. Delete any such files. The correct package structure after this
step is:

```
unblu-application/src/main/java/org/dbs/poc/unblu/application/
  port/in/
    CreateBotUseCase.java
    SearchPersonsQuery.java
    SearchPersonsUseCase.java
    SearchTeamsUseCase.java
    StartConversationCommand.java
    StartConversationUseCase.java
    StartDirectConversationCommand.java
    StartDirectConversationUseCase.java
  service/
    BotAdminService.java
    ConversationOrchestratorService.java
    DirectConversationService.java
    PersonQueryService.java
    TeamQueryService.java
```

### 3c. Create port/in interfaces and commands

Base path: `unblu-application/src/main/java/org/dbs/poc/unblu/application/port/in/`

#### `CreateBotUseCase.java`

```java
package org.dbs.poc.unblu.application.port.in;

public interface CreateBotUseCase {
    String createSummaryBot(String name, String description);
}
```

#### `SearchPersonsQuery.java`

```java
package org.dbs.poc.unblu.application.port.in;

import lombok.Builder;
import lombok.Data;
import org.dbs.poc.unblu.domain.model.PersonSource;

@Data
@Builder
public class SearchPersonsQuery {
    private String sourceId;
    private PersonSource personSource;
}
```

#### `SearchPersonsUseCase.java`

```java
package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.PersonInfo;

import java.util.List;

public interface SearchPersonsUseCase {
    List<PersonInfo> searchPersons(SearchPersonsQuery query);
}
```

#### `SearchTeamsUseCase.java`

```java
package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.TeamInfo;

import java.util.List;

public interface SearchTeamsUseCase {
    List<TeamInfo> searchTeams();
}
```

#### `StartConversationCommand.java`

```java
package org.dbs.poc.unblu.application.port.in;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartConversationCommand {
    private String clientId;
    private String subject;
    private String origin;
}
```

#### `StartConversationUseCase.java`

```java
package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.ConversationContext;

public interface StartConversationUseCase {
    /**
     * Orchestrates the startup of a new conversation (ERP -> Rules -> Unblu).
     * @param command The initial command
     * @return The resulting conversation context
     */
    ConversationContext startConversation(StartConversationCommand command);
}
```

#### `StartDirectConversationCommand.java`

```java
package org.dbs.poc.unblu.application.port.in;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartDirectConversationCommand {
    private String virtualParticipantSourceId;
    private String agentParticipantSourceId;
    private String subject;
}
```

#### `StartDirectConversationUseCase.java`

```java
package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;

public interface StartDirectConversationUseCase {
    /**
     * Creates a direct conversation between a VIRTUAL participant (via ERP + Rule Engine)
     * and a USER_DB agent identified by sourceId.
     */
    UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command);
}
```

### 3d. Create application services

Base path: `unblu-application/src/main/java/org/dbs/poc/unblu/application/service/`

#### `BotAdminService.java`

```java
package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.CreateBotUseCase;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotAdminService implements CreateBotUseCase {

    private final UnbluPort unbluPort;

    @Override
    public String createSummaryBot(String name, String description) {
        return unbluPort.createBot(name, description);
    }
}
```

#### `ConversationOrchestratorService.java`

Orchestration flow: ERP → RuleEngine → (if authorized) Unblu createConversation → generateSummary → addSummaryToConversation.

```java
package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.ConversationSummaryPort;
import org.dbs.poc.unblu.domain.port.secondary.ErpPort;
import org.dbs.poc.unblu.domain.port.secondary.RuleEnginePort;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final ErpPort erpPort;
    private final RuleEnginePort ruleEnginePort;
    private final UnbluPort unbluPort;
    private final ConversationSummaryPort conversationSummaryPort;

    @Override
    public ConversationContext startConversation(StartConversationCommand command) {
        log.info("Démarrage de l'orchestration de conversation pour clientId: {}", command.getClientId());

        // 1. Initialiser le contexte
        ConversationContext context = ConversationContext.builder()
                .initialClientId(command.getClientId())
                .originApplication(command.getOrigin())
                .build();

        // 2. Appel ERP
        CustomerProfile profile = erpPort.getCustomerProfile(command.getClientId());
        context.setCustomerProfile(profile);

        // 3. Appel Moteur de Règles
        ChatRoutingDecision decision = ruleEnginePort.evaluateRouting(context);
        context.setRoutingDecision(decision);

        // 4. Décision Métier d'Orchestration (Autorisé ou Rejeté)
        if (!decision.isAuthorized()) {
            log.warn("Accès refusé par le moteur de règles pour clientId: {}. Motif: {}", command.getClientId(), decision.getRoutingReason());
            throw new ChatAccessDeniedException("Accès refusé", decision.getRoutingReason());
        }

        // 5. Appel à l'API Unblu
        UnbluConversationInfo unbluInfo = unbluPort.createConversation(context);
        context.setUnbluConversationId(unbluInfo.unbluConversationId());
        context.setUnbluJoinUrl(unbluInfo.unbluJoinUrl());

        // 6. Génération et ajout du résumé comme message système dans la conversation
        String summary = conversationSummaryPort.generateSummary(unbluInfo.unbluConversationId());
        unbluPort.addSummaryToConversation(unbluInfo.unbluConversationId(), summary);

        return context;
    }
}
```

#### `DirectConversationService.java`

Orchestration flow: searchPersons(VIRTUAL) → ERP → RuleEngine → searchPersons(USER_DB) → createDirectConversation → generateSummary → addSummaryToConversation.

```java
package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.ConversationSummaryPort;
import org.dbs.poc.unblu.domain.port.secondary.ErpPort;
import org.dbs.poc.unblu.domain.port.secondary.RuleEnginePort;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectConversationService implements StartDirectConversationUseCase {

    private final ErpPort erpPort;
    private final RuleEnginePort ruleEnginePort;
    private final UnbluPort unbluPort;
    private final ConversationSummaryPort conversationSummaryPort;

    @Override
    public UnbluConversationInfo startDirectConversation(StartDirectConversationCommand command) {
        log.info("Démarrage d'une conversation directe - VIRTUAL: {}, Agent: {}",
                command.getVirtualParticipantSourceId(), command.getAgentParticipantSourceId());

        // 1. Résoudre le participant VIRTUAL dans Unblu
        List<PersonInfo> virtualPersons = unbluPort.searchPersons(command.getVirtualParticipantSourceId(), PersonSource.VIRTUAL);
        if (virtualPersons.isEmpty()) {
            throw new IllegalArgumentException("Participant VIRTUAL introuvable dans Unblu pour sourceId: " + command.getVirtualParticipantSourceId());
        }
        PersonInfo virtualPerson = virtualPersons.getFirst();

        // 2. ERP — enrichissement du profil client
        CustomerProfile profile = erpPort.getCustomerProfile(command.getVirtualParticipantSourceId());

        // 3. Rule Engine — décision de routage
        ConversationContext context = ConversationContext.builder()
                .initialClientId(command.getVirtualParticipantSourceId())
                .customerProfile(profile)
                .build();
        ChatRoutingDecision decision = ruleEnginePort.evaluateRouting(context);

        if (!decision.isAuthorized()) {
            log.warn("Accès refusé par le moteur de règles pour sourceId: {}. Motif: {}",
                    command.getVirtualParticipantSourceId(), decision.getRoutingReason());
            throw new ChatAccessDeniedException("Accès refusé", decision.getRoutingReason());
        }

        // 4. Résoudre l'agent USER_DB dans Unblu
        List<PersonInfo> agentPersons = unbluPort.searchPersons(command.getAgentParticipantSourceId(), PersonSource.USER_DB);
        if (agentPersons.isEmpty()) {
            throw new IllegalArgumentException("Agent USER_DB introuvable dans Unblu pour sourceId: " + command.getAgentParticipantSourceId());
        }
        PersonInfo agentPerson = agentPersons.getFirst();

        // 5. Créer la conversation directe dans Unblu
        UnbluConversationInfo info = unbluPort.createDirectConversation(virtualPerson, agentPerson, command.getSubject());

        // 6. Génération et ajout du résumé comme message au nom du bot
        String summary = conversationSummaryPort.generateSummary(info.unbluConversationId());
        unbluPort.addSummaryToConversation(info.unbluConversationId(), summary);

        return info;
    }
}
```

#### `PersonQueryService.java`

```java
package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SearchPersonsQuery;
import org.dbs.poc.unblu.application.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonQueryService implements SearchPersonsUseCase {

    private final UnbluPort unbluPort;

    @Override
    public List<PersonInfo> searchPersons(SearchPersonsQuery query) {
        log.info("Recherche de personnes dans Unblu, sourceId: {}, personSource: {}", query.getSourceId(), query.getPersonSource());
        return unbluPort.searchPersons(query.getSourceId(), query.getPersonSource());
    }
}
```

#### `TeamQueryService.java`

```java
package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SearchTeamsUseCase;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamQueryService implements SearchTeamsUseCase {

    private final UnbluPort unbluPort;

    @Override
    public List<TeamInfo> searchTeams() {
        log.info("Récupération des équipes Unblu");
        return unbluPort.searchTeams();
    }
}
```

---

## Step 4 — unblu-infrastructure module

### 4a. unblu-infrastructure/pom.xml (unchanged — for reference)

The infrastructure pom.xml does not change. It already contains all required dependencies. For
reference, the full content is:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.dbs.poc</groupId>
        <artifactId>unblu</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>unblu-infrastructure</artifactId>
    <name>unblu-infrastructure</name>
    <description>Infrastructure Layer: Web, Camel Orchestration, Rest Adapters</description>

    <dependencies>
        <!-- Domain -->
        <dependency>
            <groupId>org.dbs.poc</groupId>
            <artifactId>unblu-domain</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Apache Camel Core & Components -->
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-rest-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-servlet-starter</artifactId>
            <version>${camel.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-jackson-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-resilience4j-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-openapi-java-starter</artifactId>
            <version>${camel.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-http-starter</artifactId>
        </dependency>

        <!-- Unblu OpenAPI -->
        <dependency>
            <groupId>com.unblu.openapi</groupId>
            <artifactId>jersey3-client-v4</artifactId>
        </dependency>

        <!-- Springdoc OpenAPI (Swagger) -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <!-- Dotenv support -->
        <dependency>
            <groupId>me.paulschwarz</groupId>
            <artifactId>spring-dotenv</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 4b. Update UnbluProperties.java

Add the `summaryBotPersonId` field. The full file content after the change:

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/config/UnbluProperties.java`

```java
package org.dbs.poc.unblu.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "unblu.api")
public class UnbluProperties {
    private String baseUrl;
    private String username;
    private String password;
    private String summaryBotPersonId;

    private ProxyProperties proxy = new ProxyProperties();

    @Data
    public static class ProxyProperties {
        private String host;
        private Integer port;
        private String username;
        private String password;
    }
}
```

### 4c. UnbluClientConfig.java (unchanged — for reference)

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/config/UnbluClientConfig.java`

```java
package org.dbs.poc.unblu.infrastructure.config;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UnbluClientConfig {

    private final UnbluProperties unbluProperties;

    @Bean
    public ApiClient unbluApiClient() {
        ApiClient apiClient = new ApiClient();
        String baseUrl = unbluProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://services8.unblu.com/app/rest/v4";
        }
        baseUrl = baseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        apiClient.setBasePath(baseUrl);
        apiClient.setUsername(unbluProperties.getUsername());
        apiClient.setPassword(unbluProperties.getPassword());

        configureProxy(apiClient);

        return apiClient;
    }

    private void configureProxy(ApiClient apiClient) {
        UnbluProperties.ProxyProperties proxyProps = unbluProperties.getProxy();
        if (proxyProps != null && proxyProps.getHost() != null && !proxyProps.getHost().isBlank()) {
            ClientConfig clientConfig = new ClientConfig();

            String proxyUri = String.format("http://%s:%d", proxyProps.getHost(),
                proxyProps.getPort() != null ? proxyProps.getPort() : 8080);

            clientConfig.property(ClientProperties.PROXY_URI, proxyUri);

            if (proxyProps.getUsername() != null && !proxyProps.getUsername().isBlank()) {
                clientConfig.property(ClientProperties.PROXY_USERNAME, proxyProps.getUsername());
                clientConfig.property(ClientProperties.PROXY_PASSWORD, proxyProps.getPassword());
            }

            Client client = ClientBuilder.newClient(clientConfig);
            apiClient.setHttpClient(client);
        }
    }
}
```

### 4d. New infrastructure adapter files

#### `ErpCamelAdapter.java`

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/erp/ErpCamelAdapter.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.erp;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.port.secondary.ErpPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErpCamelAdapter implements ErpPort {

    private final ProducerTemplate producerTemplate;

    @Override
    public CustomerProfile getCustomerProfile(String clientId) {
        // We create a temporary context just for the Camel route to extract the ID
        ConversationContext requestContext = ConversationContext.builder()
                .initialClientId(clientId)
                .build();

        // Call the resilient Camel route for ERP
        return producerTemplate.requestBody("direct:erp-adapter", requestContext, CustomerProfile.class);
    }
}
```

#### `RuleEngineCamelAdapter.java`

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/rules/RuleEngineCamelAdapter.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.rules;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.port.secondary.RuleEnginePort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuleEngineCamelAdapter implements RuleEnginePort {

    private final ProducerTemplate producerTemplate;

    @Override
    public ChatRoutingDecision evaluateRouting(ConversationContext context) {
        // Call the Rule Engine Camel route (which has retry/resilience config)
        return producerTemplate.requestBody("direct:rule-engine-adapter", context, ChatRoutingDecision.class);
    }
}
```

#### `ExternalSystemsMockAdapters.java`

This single Camel RouteBuilder provides both the mock ERP route (`direct:erp-adapter`) and the
mock Rule Engine route (`direct:rule-engine-adapter`).

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/mock/ExternalSystemsMockAdapters.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.mock;

import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class ExternalSystemsMockAdapters extends RouteBuilder {

    private static final List<String> HELLO_BANK_TEAM_IDS = List.of(
        "cAaYUeKyTZ25_OaA6jUeVA", // Hello bank! Premium
        "xanCWmO_Rluxt0DaUn_11w", // Hello bank! Classic
        "pf50ylVKRRWeMkttXKPwQw", // Hello bank! End2End
        "7iLOw0i9TVCpI8SDAaTXyA"  // Hello bank! Supervision
    );
    private static final Random RANDOM = new Random();

    @Override
    public void configure() throws Exception {

        // ==========================================
        // ADAPTER MOCK : ERP (Récupération Profil)
        // ==========================================
        from("direct:erp-adapter")
            .routeId("mock-erp-adapter")
            .log("Mock ERP appelé pour le client ID: ${body.initialClientId}")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                String clientId = ctx.getInitialClientId();

                // Simulation d'une logique métier ERP
                CustomerProfile profile = CustomerProfile.builder()
                        .customerId(clientId)
                        .firstName("Jean")
                        .lastName("Dupont")
                        .isKnown(true)
                        .customerSegment(clientId.startsWith("VIP") ? "VIP" : "STANDARD")
                        .build();

                exchange.getIn().setBody(profile);
            });


        // ==========================================
        // ADAPTER MOCK : Moteur de Règles
        // ==========================================
        from("direct:rule-engine-adapter")
            .routeId("mock-rule-engine-adapter")
            .log("Mock Moteur de Règles appelé pour le segment: ${body.customerProfile.customerSegment}")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                String segment = ctx.getCustomerProfile().getCustomerSegment();

                ChatRoutingDecision decision = new ChatRoutingDecision();

                if ("BANNED".equalsIgnoreCase(segment)) {
                    decision.setAuthorized(false);
                    decision.setRoutingReason("Client blacklisté - Accès au chat refusé.");
                } else {
                    decision.setAuthorized(true);
                    decision.setRoutingReason("Client éligible.");
                    String teamId = HELLO_BANK_TEAM_IDS.get(RANDOM.nextInt(HELLO_BANK_TEAM_IDS.size()));
                    decision.setUnbluAssignedGroupId(teamId);
                }

                exchange.getIn().setBody(decision);
            });
    }
}
```

#### `ConversationSummaryMockAdapter.java`

Implements `ConversationSummaryPort`. Generates a 2-line summary by randomly combining one line
from each of two predefined lists (5 options each).

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/summary/ConversationSummaryMockAdapter.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.summary;

import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.port.secondary.ConversationSummaryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class ConversationSummaryMockAdapter implements ConversationSummaryPort {

    private static final List<String> LINE1 = List.of(
            "Le client a contacté le service pour une demande d'information sur ses produits.",
            "Le client souhaite obtenir un accompagnement personnalisé sur ses placements financiers.",
            "Le client a signalé un problème technique lié à l'accès à son espace personnel.",
            "Le client demande une révision de son contrat en cours.",
            "Le client cherche des conseils pour optimiser sa situation patrimoniale."
    );

    private static final List<String> LINE2 = List.of(
            "Un conseiller spécialisé a été assigné pour traiter la demande en priorité.",
            "La demande nécessite une analyse approfondie avant toute réponse définitive.",
            "Le dossier a été transmis à l'équipe compétente pour prise en charge rapide.",
            "Une réponse personnalisée sera fournie dans les meilleurs délais.",
            "Le conseiller prendra contact avec le client sous 24 heures pour un suivi."
    );

    private final Random random = new Random();

    @Override
    public String generateSummary(String conversationId) {
        String summary = LINE1.get(random.nextInt(LINE1.size())) + "\n"
                + LINE2.get(random.nextInt(LINE2.size()));
        log.info("Résumé généré pour la conversation {}: {}", conversationId, summary);
        return summary;
    }
}
```

#### `UnbluCamelAdapterPort.java`

Implements `UnbluPort` using Camel `ProducerTemplate` and direct delegation to `UnbluService` for
bot creation. Inner records are used as typed Camel message payloads.

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/unblu/UnbluCamelAdapterPort.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UnbluCamelAdapterPort implements UnbluPort {

    private final ProducerTemplate producerTemplate;
    private final UnbluService unbluService;

    @Override
    public UnbluConversationInfo createConversation(ConversationContext context) {
        ConversationContext result = producerTemplate.requestBody("direct:unblu-adapter-resilient", context, ConversationContext.class);
        return new UnbluConversationInfo(result.getUnbluConversationId(), result.getUnbluJoinUrl());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PersonInfo> searchPersons(String sourceId, PersonSource personSource) {
        return producerTemplate.requestBody("direct:unblu-search-persons",
                new PersonSearchRequest(sourceId, personSource), List.class);
    }

    public record PersonSearchRequest(String sourceId, PersonSource personSource) {}

    @Override
    public UnbluConversationInfo createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
        DirectConversationRequest req = new DirectConversationRequest(virtualPerson, agentPerson, subject);
        com.unblu.webapi.model.v4.ConversationData result =
                producerTemplate.requestBody("direct:unblu-create-direct-conversation", req,
                        com.unblu.webapi.model.v4.ConversationData.class);
        return new UnbluConversationInfo(result.getId(), result.getId());
    }

    public record DirectConversationRequest(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {}

    @Override
    public void addSummaryToConversation(String conversationId, String summary) {
        producerTemplate.sendBody("direct:unblu-add-summary", new SummaryRequest(conversationId, summary));
    }

    public record SummaryRequest(String conversationId, String summary) {}

    @Override
    public String createBot(String name, String description) {
        return unbluService.createBot(name, description);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TeamInfo> searchTeams() {
        return producerTemplate.requestBody("direct:unblu-search-teams", null, List.class);
    }
}
```

#### `UnbluCamelAdapter.java`

Camel RouteBuilder implementing all Unblu-specific routes. Each route delegates to `UnbluService`.

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/unblu/UnbluCamelAdapter.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.DirectConversationRequest;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.PersonSearchRequest;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UnbluCamelAdapter extends RouteBuilder {

    private final UnbluService unbluService;

    @Override
    public void configure() throws Exception {

        // ==========================================
        // ADAPTER : Unblu (Appel au SDK)
        // ==========================================
        from("direct:unblu-adapter")
            .routeId("unblu-rest-adapter")
            .log("Création de la conversation Unblu pour la file d'attente : ${body.routingDecision.unbluAssignedGroupId}")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);

                // Configuration de la requête de création de conversation
                ConversationCreationData creationData = new ConversationCreationData();
                creationData.setTopic("Contact depuis " + ctx.getOriginApplication());
                creationData.setVisitorData(ctx.getInitialClientId());
                creationData.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);

                com.unblu.webapi.model.v4.ConversationCreationRecipientData recipient = new com.unblu.webapi.model.v4.ConversationCreationRecipientData();
                recipient.setType(com.unblu.webapi.model.v4.EConversationRecipientType.TEAM);
                recipient.setId(ctx.getRoutingDecision().getUnbluAssignedGroupId());
                creationData.setRecipient(recipient);

                // Ajout du client comme participant CONTEXT_PERSON
                ConversationCreationParticipantData participant = new ConversationCreationParticipantData();
                participant.setPersonId(ctx.getInitialClientId());
                participant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
                creationData.addParticipantsItem(participant);

                // Appel réel au SDK Unblu via le service
                ConversationData response = unbluService.createConversation(creationData);

                ctx.setUnbluConversationId(response.getId());
                ctx.setUnbluJoinUrl("https://server.unblu.com/join/" + response.getId());

                exchange.getIn().setBody(ctx);
            });

        // ==========================================
        // ADAPTER : Recherche de personnes Unblu
        // ==========================================
        from("direct:unblu-search-persons")
            .routeId("unblu-search-persons")
            .log("Recherche de personnes dans Unblu")
            .process(exchange -> {
                PersonSearchRequest req = exchange.getIn().getBody(PersonSearchRequest.class);
                List<PersonInfo> persons = unbluService.searchPersons(req.sourceId(), req.personSource());
                exchange.getIn().setBody(persons);
            });

        // ==========================================
        // ADAPTER : Conversation directe Unblu
        // ==========================================
        from("direct:unblu-create-direct-conversation")
            .routeId("unblu-create-direct-conversation")
            .log("Création d'une conversation directe dans Unblu")
            .process(exchange -> {
                DirectConversationRequest req = exchange.getIn().getBody(DirectConversationRequest.class);
                ConversationData result = unbluService.createDirectConversation(
                        req.virtualPerson(), req.agentPerson(), req.subject());
                exchange.getIn().setBody(result);
            });

        // ==========================================
        // ADAPTER : Ajout du résumé à une conversation Unblu
        // ==========================================
        from("direct:unblu-add-summary")
            .routeId("unblu-add-summary")
            .log("Ajout du résumé à la conversation Unblu")
            .process(exchange -> {
                SummaryRequest req = exchange.getIn().getBody(SummaryRequest.class);
                unbluService.addSummaryToConversation(req.conversationId(), req.summary());
            });

        // ==========================================
        // ADAPTER : Recherche des équipes Unblu
        // ==========================================
        from("direct:unblu-search-teams")
            .routeId("unblu-search-teams")
            .log("Récupération des équipes Unblu")
            .process(exchange -> {
                List<TeamInfo> teams = unbluService.searchTeams();
                exchange.getIn().setBody(teams);
            });
    }
}
```

#### `UnbluResilientRoute.java`

Circuit-breaker wrapper around `direct:unblu-adapter`. Timeout: 3 seconds. Fallback sets
`OFFLINE-PENDING` status on the context.

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/unblu/UnbluResilientRoute.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Component;

@Component
public class UnbluResilientRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // We wrap the base unblu rest adapter with a circuit breaker
        from("direct:unblu-adapter-resilient")
            .routeId("unblu-resilient-wrapper")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(3000).end()
                .to("direct:unblu-adapter")
            .onFallback()
                .log("L'API Unblu est injoignable ou a expiré. Déclenchement du Fallback.")
                .process(exchange -> {
                    ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                    ctx.setUnbluConversationId("OFFLINE-PENDING");
                    ctx.setUnbluJoinUrl("Le service de chat est temporairement indisponible.");
                    exchange.getIn().setBody(ctx);
                })
            .end();
    }
}
```

#### `UnbluService.java`

Core service that wraps the Unblu Jersey SDK. Only the methods used by the Camel routes are
essential; the remaining utility methods (listAccounts, searchAgents, etc.) can remain but are not
called by any adapter route.

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/unblu/UnbluService.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.api.PersonsApi;
import com.unblu.webapi.jersey.v4.api.TeamsApi;
import com.unblu.webapi.model.v4.*;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.infrastructure.config.UnbluProperties;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluService {

    private final ApiClient apiClient;
    private final UnbluProperties unbluProperties;

    /**
     * Create a new conversation
     */
    public ConversationData createConversation(ConversationCreationData conversationData) {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);

            log.info("Creating conversation in Unblu...");
            ConversationData result = conversationsApi.conversationsCreate(conversationData, null);
            log.info("Successfully created conversation with ID: {}", result.getId());

            return result;
        } catch (ApiException e) {
            log.error("Error creating conversation in Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour créer une conversation");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création de la conversation : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating conversation in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la création de la conversation", e);
        }
    }

    /**
     * Search for persons in Unblu with optional filters by sourceId and personSource.
     * PersonSource.name() maps directly to EPersonSource.valueOf() because the enum names are identical.
     */
    public List<PersonInfo> searchPersons(String sourceId, org.dbs.poc.unblu.domain.model.PersonSource personSource) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            PersonQuery query = new PersonQuery();

            if (personSource != null) {
                EPersonSource ePersonSource = EPersonSource.valueOf(personSource.name());
                EqualsPersonSourceOperator sourceOperator = new EqualsPersonSourceOperator();
                sourceOperator.setValue(ePersonSource);
                PersonSourcePersonSearchFilter sourceFilter = new PersonSourcePersonSearchFilter();
                sourceFilter.setField(EPersonSearchFilterField.PERSON_SOURCE);
                sourceFilter.setOperator(sourceOperator);
                query.addSearchFiltersItem(sourceFilter);
            }

            if (sourceId != null && !sourceId.isBlank()) {
                EqualsIdOperator sourceIdOperator = new EqualsIdOperator();
                sourceIdOperator.setValue(sourceId);
                SourceIdPersonSearchFilter sourceIdFilter = new SourceIdPersonSearchFilter();
                sourceIdFilter.setField(EPersonSearchFilterField.SOURCE_ID);
                sourceIdFilter.setOperator(sourceIdOperator);
                query.addSearchFiltersItem(sourceIdFilter);
            }

            log.info("Recherche de personnes dans Unblu, sourceId: {}", sourceId);
            PersonResult result = personsApi.personsSearch(query, null);
            log.info("Trouvé {} personne(s)", result.getItems().size());

            return result.getItems().stream()
                    .map(p -> PersonInfo.builder()
                            .id(p.getId())
                            .sourceId(p.getSourceId())
                            .displayName(p.getDisplayName())
                            .email(p.getEmail())
                            .build())
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la recherche de personnes dans Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche de personnes : " + e.getMessage());
        }
    }

    /**
     * Search all teams in Unblu
     */
    public List<TeamInfo> searchTeams() {
        try {
            TeamsApi teamsApi = new TeamsApi(apiClient);
            TeamQuery query = new TeamQuery();

            log.info("Récupération des équipes dans Unblu...");
            TeamResult result = teamsApi.teamsSearch(query, null);
            log.info("Trouvé {} équipe(s)", result.getItems().size());

            return result.getItems().stream()
                    .map(t -> TeamInfo.builder()
                            .id(t.getId())
                            .name(t.getName())
                            .description(t.getDescription())
                            .build())
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des équipes Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération des équipes : " + e.getMessage());
        }
    }

    /**
     * Create a direct conversation between a VIRTUAL person and a USER_DB agent.
     * VIRTUAL person receives CONTEXT_PERSON participation type.
     * USER_DB agent receives ASSIGNED_AGENT participation type.
     */
    public ConversationData createDirectConversation(
            org.dbs.poc.unblu.domain.model.PersonInfo virtualPerson,
            org.dbs.poc.unblu.domain.model.PersonInfo agentPerson,
            String subject) {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);

            ConversationCreationData data = new ConversationCreationData();
            data.setTopic(subject);
            data.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);
            data.setVisitorData(virtualPerson.getSourceId());

            ConversationCreationParticipantData virtualParticipant = new ConversationCreationParticipantData();
            virtualParticipant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
            virtualParticipant.setPersonId(virtualPerson.getId());

            ConversationCreationParticipantData agentParticipant = new ConversationCreationParticipantData();
            agentParticipant.setParticipationType(EConversationRealParticipationType.ASSIGNED_AGENT);
            agentParticipant.setPersonId(agentPerson.getId());

            data.addParticipantsItem(virtualParticipant);
            data.addParticipantsItem(agentParticipant);

            log.info("Création d'une conversation directe dans Unblu - VIRTUAL: {}, Agent: {}", virtualPerson.getId(), agentPerson.getId());
            ConversationData result = conversationsApi.conversationsCreate(data, null);
            log.info("Conversation directe créée avec ID: {}", result.getId());

            return result;
        } catch (ApiException e) {
            log.error("Erreur lors de la création de la conversation directe - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création de la conversation directe : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating direct conversation in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la création de la conversation directe", e);
        }
    }

    /**
     * Adds a bot participant (hidden) to a conversation then sends a text message on behalf of
     * that bot. The bot's person ID is read from unblu.api.summary-bot-person-id property.
     *
     * IMPORTANT: The bot must be added as a participant BEFORE calling botsSendMessage.
     * Sending a message without participation results in an API error.
     */
    public void addSummaryToConversation(String conversationId, String summary) {
        try {
            BotsApi botsApi = new BotsApi(apiClient);

            TextPostMessageData messageData = new TextPostMessageData();
            messageData.setType(EPostMessageType.TEXT);
            messageData.setText(summary);
            messageData.setFallbackText(summary);

            String botPersonId = unbluProperties.getSummaryBotPersonId();
            if (botPersonId == null || botPersonId.isBlank()) {
                log.warn("unblu.api.summary-bot-person-id non configuré — résumé non envoyé dans la conversation {}", conversationId);
                return;
            }

            BotPostMessage message = new BotPostMessage();
            message.setConversationId(conversationId);
            message.setSenderPersonId(botPersonId);
            message.setMessageData(messageData);

            // Add the bot as a hidden participant before sending the message
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            ConversationsAddParticipantBody addParticipantBody = new ConversationsAddParticipantBody();
            addParticipantBody.setPersonId(botPersonId);
            addParticipantBody.setHidden(true);
            conversationsApi.conversationsAddParticipant(conversationId, addParticipantBody, null);

            log.info("Envoi du résumé comme message bot dans la conversation {}", conversationId);
            botsApi.botsSendMessage(message);
            log.info("Résumé envoyé avec succès dans la conversation: {}", conversationId);
        } catch (ApiException e) {
            log.error("Erreur lors de l'envoi du résumé dans la conversation {} - Status: {}", conversationId, e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de l'envoi du résumé : " + e.getMessage());
        }
    }

    /**
     * Creates a bot person in Unblu then registers it as a CustomDialogBot.
     * Returns the botPersonId to be stored as unblu.api.summary-bot-person-id.
     *
     * Steps:
     *   1. personsCreateOrUpdateBot with sourceId="bot-{name}" → obtains botPersonId
     *   2. botsCreate with CustomDialogBotData using:
     *      - type=CUSTOM
     *      - onboardingFilter=NONE, offboardingFilter=NONE
     *      - webhookStatus=INACTIVE
     *      - webhookEndpoint="http://localhost/unused" (placeholder, required by API)
     *      - webhookApiVersion=V4
     *      - outboundTimeoutMillis=5000
     */
    public String createBot(String name, String description) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            BotsApi botsApi = new BotsApi(apiClient);

            // 1. Créer la personne bot (sourceId = identifiant unique externe)
            PersonData botPerson = new PersonData();
            botPerson.setDisplayName(name);
            botPerson.setSourceId("bot-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-"));
            PersonData createdPerson = personsApi.personsCreateOrUpdateBot(botPerson, null);
            String botPersonId = createdPerson.getId();
            log.info("Personne bot créée: id={}", botPersonId);

            // 2. Créer le bot avec l'ID de la personne
            CustomDialogBotData botData = new CustomDialogBotData();
            botData.setName(name);
            botData.setDescription(description);
            botData.setType(EBotType.CUSTOM);
            botData.setBotPersonId(botPersonId);
            botData.setOnboardingFilter(EBotDialogFilter.NONE);
            botData.setOffboardingFilter(EBotDialogFilter.NONE);
            botData.setWebhookStatus(ERegistrationStatus.INACTIVE);
            botData.setWebhookEndpoint("http://localhost/unused");
            botData.setWebhookApiVersion(EWebApiVersion.V4);
            botData.setOutboundTimeoutMillis(5000L);
            botsApi.botsCreate(botData);
            log.info("Bot créé: name={}, botPersonId={}", name, botPersonId);

            return botPersonId;
        } catch (ApiException e) {
            log.error("Erreur lors de la création du bot - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création du bot : " + e.getMessage());
        }
    }
}
```

#### `GlobalExceptionHandler.java`

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/exception/GlobalExceptionHandler.java`

```java
package org.dbs.poc.unblu.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnbluApiException.class)
    public ResponseEntity<Map<String, Object>> handleUnbluApiException(UnbluApiException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getStatusDescription());
        response.put("message", ex.getMessage());
        response.put("statusCode", ex.getStatusCode());

        HttpStatus status = switch (ex.getStatusCode()) {
            case 403 -> HttpStatus.FORBIDDEN;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(response);
    }
}
```

#### `UnbluApiException.java`

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/exception/UnbluApiException.java`

```java
package org.dbs.poc.unblu.infrastructure.exception;

import lombok.Getter;

@Getter
public class UnbluApiException extends RuntimeException {
    private final int statusCode;
    private final String statusDescription;

    public UnbluApiException(int statusCode, String statusDescription, String message) {
        super(message);
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
    }
}
```

#### `HomeController.java`

**File:** `unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/rest/HomeController.java`

```java
package org.dbs.poc.unblu.infrastructure.adapter.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui.html";
    }
}
```

---

## Step 5 — New module: unblu-exposition

Create the directory `unblu-exposition/` at the root of the project.

### 5a. Create unblu-exposition/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.dbs.poc</groupId>
        <artifactId>unblu</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>unblu-exposition</artifactId>
    <name>unblu-exposition</name>
    <description>Entry Points (REST Controllers) for Unblu Orchestration</description>

    <dependencies>
        <dependency>
            <groupId>org.dbs.poc</groupId>
            <artifactId>unblu-application</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 5b. Create DTOs

Base path: `unblu-exposition/src/main/java/org/dbs/poc/unblu/exposition/rest/dto/`

#### `PersonResponse.java`

```java
package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonResponse {
    private String id;
    private String sourceId;
    private String displayName;
    private String email;
}
```

#### `TeamResponse.java`

```java
package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamResponse {
    private String id;
    private String name;
    private String description;
}
```

#### `StartConversationRequest.java`

```java
package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Data;

@Data
public class StartConversationRequest {
    private String clientId;
    private String subject;
    private String origin;
}
```

#### `StartConversationResponse.java`

```java
package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartConversationResponse {
    private String unbluConversationId;
    private String unbluJoinUrl;
    private String status;
    private String message;
}
```

#### `StartDirectConversationRequest.java`

```java
package org.dbs.poc.unblu.exposition.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartDirectConversationRequest {
    @NotBlank
    private String virtualParticipantSourceId;
    @NotBlank
    private String agentParticipantSourceId;
    @NotBlank
    private String subject;
}
```

### 5c. Create REST Controllers

Base path: `unblu-exposition/src/main/java/org/dbs/poc/unblu/exposition/rest/`

#### `PersonController.java`

- Route: `GET /v1/persons`
- Query params: `sourceId` (optional), `personSource` (optional, enum `USER_DB` or `VIRTUAL`)

```java
package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchPersonsQuery;
import org.dbs.poc.unblu.application.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.exposition.rest.dto.PersonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final SearchPersonsUseCase searchPersonsUseCase;

    @GetMapping
    public ResponseEntity<List<PersonResponse>> searchPersons(
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) PersonSource personSource) {

        List<PersonResponse> response = searchPersonsUseCase
                .searchPersons(SearchPersonsQuery.builder().sourceId(sourceId).personSource(personSource).build())
                .stream()
                .map(p -> PersonResponse.builder()
                        .id(p.getId())
                        .sourceId(p.getSourceId())
                        .displayName(p.getDisplayName())
                        .email(p.getEmail())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
}
```

#### `TeamController.java`

- Route: `GET /v1/teams`

```java
package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchTeamsUseCase;
import org.dbs.poc.unblu.exposition.rest.dto.TeamResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final SearchTeamsUseCase searchTeamsUseCase;

    @GetMapping
    public ResponseEntity<List<TeamResponse>> searchTeams() {
        List<TeamResponse> response = searchTeamsUseCase.searchTeams()
                .stream()
                .map(t -> TeamResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .description(t.getDescription())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
}
```

#### `ConversationController.java`

- Route: `POST /v1/conversations/start`
- Body: `StartConversationRequest` (`clientId`, `subject`, `origin`)

```java
package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationRequest;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final StartConversationUseCase startConversationUseCase;

    @PostMapping("/start")
    public ResponseEntity<StartConversationResponse> startConversation(@RequestBody StartConversationRequest request) {

        // Map DTO to Command
        StartConversationCommand command = StartConversationCommand.builder()
                .clientId(request.getClientId())
                .subject(request.getSubject())
                .origin(request.getOrigin())
                .build();

        // Execute Use Case
        ConversationContext context = startConversationUseCase.startConversation(command);

        // Map Context to DTO
        StartConversationResponse response = StartConversationResponse.builder()
                .unbluConversationId(context.getUnbluConversationId())
                .unbluJoinUrl(context.getUnbluJoinUrl())
                .status("CREATED")
                .message("Conversation successfully created.")
                .build();

        return ResponseEntity.ok(response);
    }
}
```

#### `DirectConversationController.java`

- Route: `POST /v1/conversations/direct`
- Body: `StartDirectConversationRequest` (`virtualParticipantSourceId`, `agentParticipantSourceId`, `subject`)

```java
package org.dbs.poc.unblu.exposition.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationUseCase;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.dbs.poc.unblu.exposition.rest.dto.StartDirectConversationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class DirectConversationController {

    private final StartDirectConversationUseCase startDirectConversationUseCase;

    @PostMapping("/direct")
    public ResponseEntity<StartConversationResponse> startDirectConversation(
            @Valid @RequestBody StartDirectConversationRequest request) {

        StartDirectConversationCommand command = StartDirectConversationCommand.builder()
                .virtualParticipantSourceId(request.getVirtualParticipantSourceId())
                .agentParticipantSourceId(request.getAgentParticipantSourceId())
                .subject(request.getSubject())
                .build();

        UnbluConversationInfo info = startDirectConversationUseCase.startDirectConversation(command);

        StartConversationResponse response = StartConversationResponse.builder()
                .unbluConversationId(info.unbluConversationId())
                .unbluJoinUrl(info.unbluJoinUrl())
                .status("CREATED")
                .message("Conversation directe créée avec succès.")
                .build();

        return ResponseEntity.ok(response);
    }
}
```

#### `BotAdminController.java`

- Route: `POST /v1/admin/bots`
- Query params: `name` (default: `SummaryBot`), `description` (default: French description)
- Returns: `{ "botPersonId": "...", "info": "Configurez unblu.api.summary-bot-person-id=..." }`

```java
package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.CreateBotUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin/bots")
@RequiredArgsConstructor
public class BotAdminController {

    private final CreateBotUseCase createBotUseCase;

    @PostMapping
    public ResponseEntity<Map<String, String>> createSummaryBot(
            @RequestParam(defaultValue = "SummaryBot") String name,
            @RequestParam(defaultValue = "Bot d'envoi des résumés de conversation") String description) {

        String botPersonId = createBotUseCase.createSummaryBot(name, description);
        return ResponseEntity.ok(Map.of(
                "botPersonId", botPersonId,
                "info", "Configurez unblu.api.summary-bot-person-id=" + botPersonId
        ));
    }
}
```

---

## Step 6 — New module: unblu-configuration

Create the directory `unblu-configuration/` at the root of the project.

### 6a. Create unblu-configuration/pom.xml

This is the sole runnable Spring Boot module. It assembles all four other modules and carries the
Spring Boot Maven plugin.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.dbs.poc</groupId>
        <artifactId>unblu</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>unblu-configuration</artifactId>
    <name>unblu-configuration</name>
    <description>Application Assembler and Spring Boot configuration</description>

    <dependencies>
        <dependency>
            <groupId>org.dbs.poc</groupId>
            <artifactId>unblu-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dbs.poc</groupId>
            <artifactId>unblu-application</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dbs.poc</groupId>
            <artifactId>unblu-infrastructure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.dbs.poc</groupId>
            <artifactId>unblu-exposition</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-spring-boot-starter</artifactId>
        </dependency>

        <!-- Springdoc OpenAPI (Swagger) -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-reflect</artifactId>
        </dependency>

        <!-- Dotenv support -->
        <dependency>
            <groupId>me.paulschwarz</groupId>
            <artifactId>spring-dotenv</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

### 6b. Create Spring Boot main class

**File:** `unblu-configuration/src/main/java/org/dbs/poc/unblu/configuration/UnbluApplication.java`

```java
package org.dbs.poc.unblu.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.dbs.poc.unblu")
public class UnbluApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnbluApplication.class, args);
    }

}
```

The `scanBasePackages = "org.dbs.poc.unblu"` is essential: it tells Spring to scan all subpackages
across all five modules, making the single-application assembly possible.

---

## Step 7 — Configuration files

### 7a. application.properties

**File:** `unblu-configuration/src/main/resources/application.properties`

```properties
server.port=8081
camel.servlet.mapping.context-path=/api/*

# Swagger UI (Springdoc)
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
logging.level.org.dbs.poc.unblu=DEBUG
unblu.api.summary-bot-person-id=<BOT_PERSON_ID>
```

Replace `<BOT_PERSON_ID>` with the actual bot person ID obtained by calling
`POST /v1/admin/bots` after the first deployment.

### 7b. .env file (not committed to git)

Create a `.env` file at the project root. This file is loaded by `spring-dotenv` and must **not**
be committed to version control.

```
UNBLU_API_USERNAME=hb-admin
UNBLU_API_PASSWORD=<password>
UNBLU_API_BASE_URL=https://services8.unblu.com/app/rest/v4
```

These values map to `unblu.api.username`, `unblu.api.password`, and `unblu.api.base-url` via the
`UnbluProperties` configuration properties class.

---

## Key Constraints and Unblu API Limitations

### 1. PersonSource enum name mapping

`PersonSource` in the domain has two values: `USER_DB` and `VIRTUAL`. These names are intentionally
identical to the Unblu SDK's `EPersonSource` enum values. The mapping in `UnbluService.searchPersons`
uses:

```java
EPersonSource ePersonSource = EPersonSource.valueOf(personSource.name());
```

Do not rename these enum values; the mapping would break silently.

### 2. Bot messaging requires prior participation

The Unblu API rejects `botsSendMessage` if the sender bot is not already a participant of the
conversation. The correct sequence (implemented in `UnbluService.addSummaryToConversation`) is:

1. Call `conversationsAddParticipant` with `personId = botPersonId` and `hidden = true`
2. Only then call `botsSendMessage`

Skipping step 1 results in an API error.

### 3. botsSendMessage sender must be a bot person

`BotPostMessage.setSenderPersonId()` must receive the ID of a person created via
`personsCreateOrUpdateBot`. A VIRTUAL person's ID cannot be used as the sender in `botsSendMessage`.
This is why a dedicated summary bot must be created and its `botPersonId` configured separately.

### 4. Direct conversation participant types

When creating a direct conversation:
- The VIRTUAL person must have participation type `CONTEXT_PERSON`
- The USER_DB agent must have participation type `ASSIGNED_AGENT`

Using wrong participant types results in an API error or the conversation being created without the
expected routing behaviour.

### 5. Bot creation — required CustomDialogBotData fields

When calling `botsCreate`, the following fields are all required even though some appear optional
in documentation:
- `name`
- `type` = `EBotType.CUSTOM`
- `botPersonId` (from step 1 of `createBot`)
- `onboardingFilter` = `EBotDialogFilter.NONE`
- `offboardingFilter` = `EBotDialogFilter.NONE`
- `webhookStatus` = `ERegistrationStatus.INACTIVE`
- `webhookEndpoint` = `"http://localhost/unused"` (any valid URL; placeholder when bot is inactive)
- `webhookApiVersion` = `EWebApiVersion.V4`
- `outboundTimeoutMillis` = `5000L`

### 6. Summary bot one-time setup workflow

The bot is not created at startup. The required workflow is:

1. Deploy and start the application (without `summary-bot-person-id` configured)
2. Call `POST /v1/admin/bots?name=SummaryBot` once
3. Copy the returned `botPersonId` value
4. Set `unblu.api.summary-bot-person-id=<returned-id>` in `application.properties`
5. Restart the application

Until this is done, `addSummaryToConversation` logs a warning and skips summary injection.

### 7. Module dependency rules (enforced by Maven)

```
unblu-configuration  →  all four modules
unblu-exposition     →  unblu-application
unblu-application    →  unblu-domain (only)
unblu-infrastructure →  unblu-domain (only)
unblu-domain         →  no internal dependencies
```

The application layer must **never** depend on infrastructure. The domain layer must **never**
depend on any other internal module. These rules ensure that business logic can be tested in
isolation without any infrastructure on the classpath.

### 8. Component scan must be set explicitly

The `@SpringBootApplication` class is in package `org.dbs.poc.unblu.configuration`. Without the
explicit `scanBasePackages = "org.dbs.poc.unblu"`, Spring would only scan that one package and
would miss all beans in the other four modules. The scan root must be the common ancestor package
of all modules.
