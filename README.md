# CampusConnect

Plateforme d'entraide entre étudiants : un étudiant publie une demande d'aide (matière, niveau, disponibilités),
d'autres étudiants proposent leur aide, l'auteur choisit un aidant, la séance est suivie jusqu'à sa clôture,
puis les deux participants s'évaluent. Un administrateur modère les publications signalées.

Projet universitaire — TEK-UP, Génie Logiciel.

| | |
|---|---|
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Ionicons |
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, OAuth2 Resource Server / JWT) |
| Base de données | PostgreSQL |
| Tests | JUnit 5 + Spring Boot Test : 92 tests d'intégration HTTP sur une vraie base PostgreSQL |

---

## Sommaire

1. [Fonctionnalités](#1-fonctionnalités)
2. [Architecture](#2-architecture)
3. [Installation](#3-installation)
4. [Démarrage](#4-démarrage)
5. [Comptes et données de départ](#5-comptes-et-données-de-départ)
6. [API REST](#6-api-rest)
7. [Règles de gestion](#7-règles-de-gestion)
8. [Tests](#8-tests)
9. [Structure du dépôt](#9-structure-du-dépôt)
10. [Dépannage](#10-dépannage)
11. [Limites connues](#11-limites-connues)

---

## 1. Fonctionnalités

**Étudiant**
- Inscription, connexion, déconnexion ; profil (nom, spécialité, niveau, compétences).
- Publication, consultation et modification de ses demandes d'aide.
- Liste paginée des demandes avec filtres par matière, niveau et statut.
- Proposition d'aide sur la demande d'un autre étudiant (avec un message).
- Pour l'auteur : acceptation ou refus des propositions reçues.
- Suivi de la demande : **OUVERTE → AIDE ACCEPTÉE → TERMINÉE**, avec frise d'état.
- Évaluation (note de 1 à 5 + commentaire) entre l'auteur et l'aidant après la séance.
- Tableau de bord personnel : demandes publiées, aides proposées, séances réalisées, note moyenne.
- Signalement d'une publication inappropriée.

**Administrateur**
- Espace de modération : publications signalées, motifs, masquage / restauration d'une publication,
  rejet des signalements.

**Interface**
- Logo et icônes Ionicons, mode clair / sombre (préférence mémorisée), affichage adapté au mobile.
- Messages d'erreur clairs, champs obligatoires signalés, pages 404 et erreur dédiées.

---

## 2. Architecture

```
Navigateur ──► Next.js (port 3000)
                 ├─ pages React (App Router)
                 ├─ /api/auth/login | logout | session  → pose / lit un cookie httpOnly « cc_token »
                 └─ /api/...  (relais)  → ajoute « Authorization: Bearer <jeton> »
                                               │
                                               ▼
                                   Spring Boot (port 8270)  ──►  PostgreSQL (port 5434)
                                   Controller → Service → Repository (JPA)
```

- Le jeton JWT (HS256) n'est **jamais** lisible en JavaScript : il reste dans un cookie `httpOnly`, `SameSite=Lax`,
  géré par les routes API de Next.js qui le transmettent au backend.
- `src/proxy.ts` (middleware Next.js) redirige vers `/login` les pages protégées quand il n'y a pas de session
  et réserve `/admin` aux administrateurs.
- Le backend est sans état (`SessionCreationPolicy.STATELESS`) ; les rôles sont dans le jeton (`STUDENT`, `ADMIN`).
- Les opérations concurrentes sensibles (accepter une proposition, clôturer une séance) verrouillent la demande
  en base (verrou pessimiste) pour éviter deux aides acceptées sur la même demande.

Une description détaillée (frontend et backend) est dans [`docs/CampusConnect_Documentation.pdf`](docs/CampusConnect_Documentation.pdf).

---

## 3. Installation

### Pré-requis

| Outil | Version |
|---|---|
| JDK | 21 ou plus |
| Maven | fourni (`backend/mvnw.cmd`), ou une installation locale |
| Node.js | 20.9 ou plus (22 recommandé) |
| PostgreSQL | 14 ou plus |

### 3.1 Base de données

```sql
CREATE USER campusconnect_user WITH PASSWORD 'votre_mot_de_passe';
CREATE DATABASE campusconnect OWNER campusconnect_user ENCODING 'UTF8';
```

Les tables sont créées automatiquement au premier démarrage (`spring.jpa.hibernate.ddl-auto=update`).

### 3.2 Configuration du backend

Copier `backend/.env.example` en `backend/.env` (ce fichier n'est **pas** versionné) puis renseigner :

| Variable | Rôle | Exemple |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | adresse de PostgreSQL | `localhost`, `5434`, `campusconnect` |
| `DB_USERNAME`, `DB_PASSWORD` | compte PostgreSQL | `campusconnect_user`, … |
| `JWT_SECRET` | clé de signature des jetons, **32 caractères minimum** | chaîne aléatoire |
| `JWT_EXPIRATION_MINUTES` | durée de validité d'une session | `120` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | compte administrateur créé au démarrage (laisser vide pour ne pas en créer) | `admin@campusconnect.local` |
| `JAVA_HOME`, `MAVEN_CMD` | *facultatif* : JDK et Maven à utiliser par `start.bat` | |
| `BACKEND_PORT`, `FRONTEND_PORT` | *facultatif* : ports (défaut 8270 et 3000) | |

Le mot de passe de la base et la clé JWT n'ont **aucune valeur par défaut** : sans eux, le backend refuse de démarrer.

### 3.3 Dépendances du frontend

```bash
cd frontend
npm install
```

---

## 4. Démarrage

### Windows : un seul clic

```bat
start.bat     :: lit backend\.env, lance le backend puis le frontend dans deux fenêtres
stop.bat      :: arrête les deux
```

L'application est alors sur **http://localhost:3000** (API sur http://localhost:8270).

Dans IntelliJ IDEA, les configurations **Start CampusConnect** et **Stop CampusConnect** (dossier `.run/`)
apparaissent directement dans la liste des configurations d'exécution.

### Manuellement (tous systèmes)

```bash
# Terminal 1 — backend (les variables de backend/.env doivent être exportées)
cd backend
./mvnw spring-boot:run          # Windows : mvnw.cmd spring-boot:run

# Terminal 2 — frontend
cd frontend
BACKEND_URL=http://localhost:8270 npm run dev     # Windows PowerShell : $env:BACKEND_URL="http://localhost:8270"; npm run dev
```

Version de production du frontend : `npm run build` puis `npm start`.

Vérification rapide : `GET http://localhost:8270/api/health` doit répondre `{"status":"ok", ...}`.

---

## 5. Comptes et données de départ

Au démarrage, le backend crée automatiquement :
- les **10 matières** de départ (Mathématiques, Algorithmique, Programmation Java, Bases de données,
  Développement web, Réseaux, Systèmes d'exploitation, Génie logiciel, Intelligence artificielle, Anglais technique) ;
- le **compte administrateur** défini par `ADMIN_EMAIL` / `ADMIN_PASSWORD`, s'il n'existe pas encore.

Les comptes étudiants se créent depuis la page **Créer un compte**. Pour tester le parcours complet,
créez deux étudiants (un auteur, un aidant) dans deux navigateurs différents ou une fenêtre privée.

Un scénario de test pas à pas couvrant toutes les fonctionnalités est fourni :
[`docs/CampusConnect_Scenario_de_test.pdf`](docs/CampusConnect_Scenario_de_test.pdf).

---

## 6. API REST

Toutes les routes sauf `/api/auth/**` et `/api/health` exigent l'en-tête `Authorization: Bearer <jeton>`.
Les erreurs ont toujours le même format :

```json
{ "status": 409, "error": "Conflict", "message": "Vous avez déjà proposé votre aide pour cette demande", "fieldErrors": {} }
```

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/api/health` | public | état du service |
| POST | `/api/auth/register` | public | inscription (201) |
| POST | `/api/auth/login` | public | connexion → jeton JWT |
| GET / PUT | `/api/users/me` | connecté | lire / modifier son profil |
| GET | `/api/subjects` | connecté | liste des matières |
| POST | `/api/requests` | étudiant | publier une demande (201) |
| GET | `/api/requests?subjectId=&level=&status=&page=&size=` | connecté | liste paginée et filtrée (50 par page au maximum) |
| GET | `/api/requests/{id}` | connecté | détail d'une demande |
| PUT | `/api/requests/{id}` | auteur | modifier une demande ouverte |
| POST | `/api/requests/{id}/offers` | étudiant | proposer son aide (201) |
| GET | `/api/requests/{id}/offers` | auteur | propositions reçues |
| POST | `/api/offers/{id}/accept` | auteur | accepter une proposition |
| POST | `/api/offers/{id}/reject` | auteur | refuser une proposition |
| POST | `/api/requests/{id}/complete` | auteur | clôturer la séance |
| POST | `/api/requests/{id}/ratings` | participant | évaluer l'autre participant (201) |
| GET | `/api/requests/{id}/ratings` | participant | évaluations de la séance |
| GET | `/api/dashboard` | connecté | tableau de bord personnel |
| POST | `/api/requests/{id}/reports` | étudiant | signaler une publication (201) |
| GET | `/api/admin/reports` | admin | publications signalées |
| POST | `/api/admin/requests/{id}/hide` | admin | masquer une publication |
| POST | `/api/admin/requests/{id}/restore` | admin | rétablir une publication |
| POST | `/api/admin/requests/{id}/dismiss` | admin | rejeter les signalements |

Détails (corps, codes d'erreur) du suivi, de l'évaluation, du tableau de bord et de l'administration :
[`docs/api-suivi-evaluation-admin.md`](docs/api-suivi-evaluation-admin.md).

---

## 7. Règles de gestion

| Règle | Comportement |
|---|---|
| Inscription | email unique (sans tenir compte de la casse), mot de passe de 8 à 64 caractères, haché avec BCrypt |
| Propositions | uniquement sur une demande **OUVERTE**, jamais sur sa propre demande, une seule par aidant et par demande |
| Acceptation | réservée à l'auteur ; la demande passe en **AIDE_ACCEPTEE** et les autres propositions en attente sont refusées |
| Clôture | réservée à l'auteur, uniquement depuis **AIDE_ACCEPTEE** → **TERMINEE** |
| Modification | l'auteur peut modifier sa demande tant qu'elle est **OUVERTE** |
| Évaluation | réservée aux deux participants, après la clôture, une seule fois chacun, note de 1 à 5 |
| Signalement | pas sur sa propre demande, une seule fois par utilisateur ; une publication masquée disparaît des listes |
| Accès | les routes `/api/admin/**` et la page `/admin` sont réservées au rôle `ADMIN` |

---

## 8. Tests

```bash
cd backend
./mvnw test          # Windows : mvnw.cmd test   (les variables DB_* et JWT_SECRET doivent être définies)
```

Les 92 tests démarrent l'application sur un port aléatoire et l'appellent en HTTP sur une vraie base
PostgreSQL (la base configurée dans `.env`). Chaque test crée ses propres données (emails uniques)
et les supprime ensuite. Ils couvrent l'inscription, la connexion, les demandes, les filtres, les propositions,
l'acceptation, la clôture, l'évaluation, le tableau de bord et la modération, y compris les cas d'erreur
(401, 403, 404, 409, validation 400).

Contrôles du frontend :

```bash
cd frontend
npm run lint
npm run build
```

---

## 9. Structure du dépôt

```
campusconnect/
├── backend/                     API Spring Boot
│   ├── src/main/java/com/campusconnect/backend/
│   │   ├── controller/          routes REST
│   │   ├── service/             règles de gestion
│   │   ├── repository/          accès aux données (Spring Data JPA)
│   │   ├── entity/              modèle (User, Subject, HelpRequest, HelpOffer, Rating, Report…)
│   │   ├── dto/                 objets d'entrée / sortie de l'API
│   │   ├── security/            JWT, configuration Spring Security
│   │   ├── exception/           erreurs métier et format d'erreur commun
│   │   └── config/              données de départ (matières, administrateur)
│   ├── src/test/java/…          92 tests d'intégration
│   └── .env.example             modèle de configuration
├── frontend/                    application Next.js
│   └── src/
│       ├── app/                 pages : login, register, requests, dashboard, profile, admin + routes API (relais)
│       ├── components/          composants d'interface (en-tête, badges, frise d'état, thème…)
│       ├── lib/                 types, appels API, session
│       └── proxy.ts             protection des pages selon la session et le rôle
├── docs/                        documentation, scénario de test, détail de l'API
├── .run/                        configurations IntelliJ (Start / Stop CampusConnect)
├── start.bat / stop.bat         démarrage et arrêt sous Windows
└── README.md
```

---

## 10. Dépannage

| Problème | Solution |
|---|---|
| `Fichier backend\.env introuvable` | copier `backend/.env.example` en `backend/.env` |
| Le backend s'arrête au démarrage avec une erreur sur `DB_PASSWORD` ou `JWT_SECRET` | ces variables sont obligatoires ; la clé JWT doit faire au moins 32 caractères |
| `Connection refused` vers PostgreSQL | vérifier que PostgreSQL tourne et que `DB_PORT` correspond (5434 par défaut ici, 5432 dans une installation standard) |
| « Authentification requise » sur toutes les pages, ou port déjà utilisé | un autre service occupe le port 8080 (Jenkins, Tomcat) : le backend utilise **8270** ; vérifier `BACKEND_PORT` et `BACKEND_URL` |
| `Module not found: Can't resolve 'ionicons/icons'` | lancer `npm install` dans `frontend/` (`start.bat` le fait à chaque démarrage) |
| « Interpreter not found » dans IntelliJ | utiliser les configurations du dossier `.run/` (type npm), pas une configuration Shell Script |

---

## 11. Limites connues

- Pas de messagerie entre étudiants ni de notifications : le contact passe par le message de la proposition.
- Pas de récupération de mot de passe par email.
- Le cookie de session n'est marqué `Secure` qu'en production (`NODE_ENV=production`) : un déploiement réel doit passer par HTTPS.
- Le schéma est géré par `ddl-auto=update` : un outil de migration (Flyway, Liquibase) serait préférable en production.
