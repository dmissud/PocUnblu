# 🛠️ Configuration et Entités Cœurs (Teams, Areas, Bots)

Ce document décrit les services techniques qui permettent de manipuler les briques de base de la configuration Unblu.

## 🧱 Services : `UnbluService` et `UnbluBotService`

Ces deux services sont principalement utilisés pour l'initialisation du système et le routing.

### Endpoints Unblu sous-jacents

- `GET /v4/accounts/getCurrentAccount` : Vérification de la connexion.
- `POST /v4/teams/search` : Liste des équipes d'agents.
- `POST /v4/namedAreas/search` : Liste des zones configurées.
- `POST /v4/persons/createOrUpdateBot` : Enregistrement technique d'un Bot.
- `POST /v4/bots/create` : Activation fonctionnelle d'un Bot.

---

## 🏃 Scénarios d'Usage

### 1. Initialisation & Audit (`UnbluService`)
Utile pour s'assurer que l'adaptateur parle correctement à Unblu.
- **Cas Nominal** : Récupération du compte courant (`getCurrentAccount`).
- **Cas d'Erreur (403)** : Indique souvent que la clé API n'a pas les droits de lecture sur le compte.

### 2. Récupération des Équipes et Zones (`UnbluService`)
Utilisé pour alimenter nos menus de sélection ou pour nos règles de gestion.
- **Entrée** : Aucune (récupération de tout le catalogue).
- **Transformation** : Mapping des objets `TeamData` ou `NamedAreaData` vers nos propres modèles de domaine `TeamInfo` et `NamedAreaInfo`.

### 3. Gestion Idempotente des Bots (`UnbluBotService`)
Un scénario crucial : s'assurer qu'un Bot existe sans le recréer à chaque fois (méthode `createOrGetBot`).
- **Cas Nominal (Le bot existe déjà)** : 
  1. On cherche par `sourceId` (ex: "bot-sav-france").
  2. L'API renvoie 200 OK avec les données existantes.
  3. On retourne l'ID existant.
- **Cas Nominal (Nouveau Bot)** : 
  1. La recherche par `sourceId` renvoie un 404.
  2. On procède à la création de la `Person` (le profil).
  3. On procède à la création du `Bot` (l'entité logique de dialogue).
- **Paramètres Techniques** : Notez que nous configurons les bots en mode `CUSTOM` avec une URL de webhook factice si nous n'avons pas besoin de callback actif (voir `createBot`).

---

## ⚠️ Conseils de Tech Lead

- **Idempotence** : Toujours utiliser `createOrGetBot` plutôt que `createBot` en direct pour éviter de polluer l'instance Unblu avec des doublons.
- **V4 API** : Nous utilisons exclusivement la V4 de l'API Unblu (`EWebApiVersion.V4`). Faites attention lors de l'utilisation du SDK à ne pas mélanger avec des modèles de versions antérieures.
- **Délai (Outbound Timeout)** : Lors de la création d'un bot, nous fixons un timeout de 5 secondes (`5000L`). C'est le temps que le serveur attendra une réponse du bot avant de couper la communication.
