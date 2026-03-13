# Unblu Frontend - Interface de Test

Interface web Angular pour tester les services Unblu.

## Fonctionnalités

### 1. Conversation Directe
- Sélectionner un **Client (VIRTUAL)**
- Sélectionner un **Agent (USER_DB)**
- Créer une conversation directe entre eux
- Afficher l'ID et l'URL de la conversation

### 2. Conversation avec Équipe
- Sélectionner un **Client (VIRTUAL)**
- Sélectionner une **Équipe**
- Créer une conversation routée vers l'équipe
- Afficher l'ID, URL et détails de la conversation

## Développement

### Prérequis
- Node.js 18+ et npm
- Angular CLI 19+

### Installation
```bash
cd unblu-frontend
npm install
```

### Développement local (avec proxy vers API)
```bash
npm start
# Ouvre http://localhost:4200
# Les appels API sont proxifiés vers http://localhost:8081
```

### Build pour production
```bash
npm run build
# Les fichiers sont générés dans ../unblu-configuration/src/main/resources/static/browser/
```

Ou depuis la racine du projet :
```bash
./build-frontend.sh
```

## Structure

```
unblu-frontend/
├── src/
│   ├── app/
│   │   ├── models/          # Modèles TypeScript
│   │   │   ├── person.model.ts
│   │   │   ├── team.model.ts
│   │   │   └── conversation.model.ts
│   │   ├── services/        # Services API
│   │   │   └── api.service.ts
│   │   ├── app.ts          # Composant principal
│   │   ├── app.html        # Template HTML
│   │   └── app.css         # Styles
│   └── index.html
└── angular.json
```

## Intégration avec Spring Boot

Le build Angular génère les fichiers dans :
```
unblu-configuration/src/main/resources/static/browser/
```

Spring Boot sert ces fichiers statiques grâce à :
```properties
spring.web.resources.static-locations=classpath:/static/browser/
```

## Endpoints API utilisés

- `GET /api/v1/persons/clients` - Liste des clients VIRTUAL
- `GET /api/v1/persons/agents` - Liste des agents USER_DB
- `GET /api/v1/teams` - Liste des équipes
- `POST /api/v1/conversations/direct` - Créer conversation directe
- `POST /api/v1/conversations/team` - Créer conversation équipe

## Accès

Une fois l'application Spring Boot démarrée :

- **Interface principale** : http://localhost:8081/
- **Swagger UI** : http://localhost:8081/swagger
- **API** : http://localhost:8081/api/v1/

## Technologies

- Angular 19 (Standalone Components)
- TypeScript
- RxJS
- CSS moderne
