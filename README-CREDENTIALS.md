# Configuration des Credentials Unblu

## Solution implémentée : Variables d'environnement

Les credentials ne sont plus stockés dans `application.yaml`. Utilisez l'une des méthodes suivantes :

### Option 1 : Fichier .env local (Développement)

1. Créez un fichier `.env` à la racine du projet :
```bash
cp .env.example .env
```

2. Éditez `.env` avec vos vraies credentials :
```properties
UNBLU_API_BASE_URL=https://services8.unblu.com/app/rest/v4
UNBLU_API_USERNAME=hb-admin
UNBLU_API_PASSWORD=*REMOVED*
```

3. Ajoutez la dépendance (si pas déjà présente) :
```xml
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>
```

4. Lancez l'application normalement

### Option 2 : Variables d'environnement système

#### Linux/Mac :
```bash
export UNBLU_API_USERNAME=hb-admin
export UNBLU_API_PASSWORD=*REMOVED*
mvn spring-boot:run
```

#### Windows (CMD) :
```cmd
set UNBLU_API_USERNAME=hb-admin
set UNBLU_API_PASSWORD=*REMOVED*
mvn spring-boot:run
```

#### Windows (PowerShell) :
```powershell
$env:UNBLU_API_USERNAME="hb-admin"
$env:UNBLU_API_PASSWORD="*REMOVED*"
mvn spring-boot:run
```

### Option 3 : Configuration IntelliJ IDEA

1. Run → Edit Configurations
2. Sélectionnez votre configuration Spring Boot
3. Dans "Environment variables", ajoutez :
```
UNBLU_API_USERNAME=hb-admin;UNBLU_API_PASSWORD=*REMOVED*
```

### Option 4 : Profils Spring (application-local.yaml)

1. Créez `src/main/resources/application-local.yaml` :
```yaml
unblu:
  api:
    username: hb-admin
    password: *REMOVED*
```

2. Ajoutez `application-local.yaml` au `.gitignore`

3. Lancez avec le profil local :
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Option 5 : Déploiement Production (Kubernetes/Docker)

#### Docker :
```bash
docker run -e UNBLU_API_USERNAME=xxx -e UNBLU_API_PASSWORD=xxx your-image
```

#### Kubernetes Secret :
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: unblu-credentials
type: Opaque
stringData:
  username: hb-admin
  password: *REMOVED*
```

## Sécurité

⚠️ **Important** :
- Ne commitez JAMAIS les fichiers `.env` ou `application-local.yaml`
- Utilisez des secrets managers en production (AWS Secrets Manager, HashiCorp Vault, etc.)
- Le fichier `.gitignore` est configuré pour ignorer `.env`
