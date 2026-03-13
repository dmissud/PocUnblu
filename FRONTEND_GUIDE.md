# Guide - Interface Frontend Unblu

## 🎯 Vue d'ensemble

Une interface web Angular pour tester facilement vos services Unblu sans avoir besoin de Postman ou cURL.

## 🚀 Démarrage rapide

### Option 1 : Build automatique avec Maven (recommandé)
```bash
# Maven build le frontend Angular automatiquement
mvn clean install

# Lancer l'application
mvn spring-boot:run -pl unblu-configuration
```

### Option 2 : Build manuel du frontend
```bash
# Build manuel (optionnel si déjà fait par Maven)
./build-frontend.sh

# Lancer l'application
mvn spring-boot:run -pl unblu-configuration
```

### Accéder à l'interface
Ouvrir dans votre navigateur : **http://localhost:8081**

## 📋 Fonctionnalités

### Workflow 1 : Conversation Directe
1. Sélectionner un **client** (VIRTUAL) dans la liste déroulante
2. Sélectionner un **agent** (USER_DB) dans la liste déroulante
3. (Optionnel) Saisir un sujet pour la conversation
4. Cliquer sur **"Créer Conversation Directe"**
5. Le résultat affiche :
   - ID de la conversation
   - URL pour rejoindre la conversation

### Workflow 2 : Conversation avec Équipe
1. Sélectionner un **client** (VIRTUAL) dans la liste déroulante
2. Sélectionner une **équipe** dans la liste déroulante
3. (Optionnel) Saisir un sujet pour la conversation
4. Cliquer sur **"Créer Conversation Équipe"**
5. Le résultat affiche :
   - ID de la conversation
   - URL pour rejoindre la conversation
   - ID de l'équipe
   - ID de l'agent assigné (si routage effectué)

## 🛠️ Architecture

### Backend (Nouveaux Controllers REST)

#### PersonController
```
GET /rest/v1/persons/clients  → Liste des clients VIRTUAL
GET /rest/v1/persons/agents   → Liste des agents USER_DB
```

#### TeamController
```
GET /rest/v1/teams → Liste des équipes
```

#### ConversationController
```
POST /rest/v1/conversations/direct
Body: {
  "clientSourceId": "string",
  "agentSourceId": "string",
  "subject": "string"
}

POST /rest/v1/conversations/team
Body: {
  "clientId": "string",
  "subject": "string"
}
```

**Note:** Les endpoints REST utilisent `/rest/v1/` au lieu de `/api/v1/` car Camel intercepte les routes `/api/*`.

### Frontend (Angular 19)

```
unblu-frontend/
├── src/app/
│   ├── models/              # Modèles TypeScript
│   ├── services/            # Services API (HttpClient)
│   ├── app.ts              # Composant principal
│   ├── app.html            # Template
│   └── app.css             # Styles
└── angular.json
```

## 📦 Build et Déploiement

### Build automatique avec Maven (intégré)

Le build Angular est **automatiquement intégré** dans le cycle de vie Maven via le `frontend-maven-plugin` :

```bash
mvn clean install
```

Ce qui exécute automatiquement :
1. Installation de Node.js v20.11.0 et npm v10.2.4 (si nécessaire)
2. `npm install` (installation des dépendances)
3. `npm run build` (build de l'application Angular)
4. Empaquetage dans le JAR Spring Boot

Les fichiers buildés sont générés dans :
```
unblu-configuration/src/main/resources/static/browser/
```

### Build manuel (pour développement)
```bash
cd unblu-frontend
npm install
npm run build
```

Ou avec le script :
```bash
./build-frontend.sh
```

### Configuration Spring Boot
```properties
# application.properties
spring.web.resources.static-locations=classpath:/static/browser/
```

### Points d'accès
- **/** → Redirige vers `/index.html` (interface Angular)
- **/swagger** → Redirige vers `/swagger-ui.html`
- **/api/v1/** → API REST

## 🔧 Développement Frontend

### Mode développement avec hot-reload
```bash
cd unblu-frontend
npm start
```

Ouvre http://localhost:4200 avec proxy vers l'API sur :8081

### Modification du frontend
1. Modifier les fichiers dans `unblu-frontend/src/`
2. Rebuilder : `npm run build` ou `./build-frontend.sh`
3. Redémarrer Spring Boot

## 🎨 Personnalisation

### Ajouter de nouveaux endpoints
1. Créer/modifier le controller dans `unblu-exposition/src/main/java/.../rest/`
2. Ajouter la méthode dans `api.service.ts`
3. Utiliser la méthode dans `app.ts`
4. Mettre à jour le template `app.html`

### Modifier les styles
Éditer `unblu-frontend/src/app/app.css` ou `src/styles.css`

## 🐛 Troubleshooting

### Frontend ne charge pas
- Vérifier que le build a réussi : `ls unblu-configuration/src/main/resources/static/browser/`
- Vérifier la config dans `application.properties`
- Vérifier la console du navigateur (F12)

### Erreurs CORS
Les appels sont sur le même domaine (localhost:8081), pas de problème CORS

### API ne répond pas
- Vérifier que Spring Boot est démarré sur le port 8081
- Vérifier les logs : `logging.level.org.dbs.poc.unblu=DEBUG`
- Tester l'API avec Swagger : http://localhost:8081/swagger

## 📝 Notes

- L'interface charge automatiquement les listes au démarrage
- Les sélections persistent entre les deux workflows (client commun)
- Les boutons sont désactivés pendant les appels API
- Les erreurs sont affichées en rouge en haut de la page
- Les succès sont affichés en vert dans chaque card

## 🎓 Pour aller plus loin

### Ajouter des filtres de recherche
Modifier `SearchPersonsQuery` pour accepter des filtres et mettre à jour l'UI

### Ajouter l'historique des conversations
Créer un nouveau endpoint `/api/v1/conversations/history` et un composant Angular

### Ajouter l'authentification
Intégrer Spring Security et Angular Guards
