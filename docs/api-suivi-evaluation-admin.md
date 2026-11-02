# API — Suivi, évaluation, tableau de bord et administration

Tous les endpoints exigent un jeton (`Authorization: Bearer <token>`). Les erreurs suivent le format
habituel `{ status, error, message, fieldErrors }`.

## 1. Suivi d'une demande : `OUVERTE → AIDE_ACCEPTEE → TERMINEE`

| Étape | Endpoint | Qui |
|---|---|---|
| OUVERTE → AIDE_ACCEPTEE | `POST /api/offers/{offerId}/accept` (existant) | auteur |
| AIDE_ACCEPTEE → TERMINEE | `POST /api/requests/{id}/complete` | auteur |

`complete` renvoie la demande (`HelpRequestResponse`, qui contient maintenant `completedAt`).

| Cas | Code |
|---|---|
| Pas l'auteur | 403 |
| Demande encore `OUVERTE` | 409 « …qu'après l'acceptation d'une aide » |
| Déjà `TERMINEE` | 409 « Cette demande est déjà terminée » |
| Inconnue ou masquée | 404 |

Une demande terminée ne reçoit plus de propositions et n'est plus modifiable (règles existantes).

## 2. Évaluation après une séance terminée

Participants = l'auteur + l'aidant dont la proposition a été **acceptée**. Chacun évalue l'autre, une seule fois.

`POST /api/requests/{id}/ratings` → 201

```json
{ "score": 5, "comment": "Explications très claires" }
```

- `score` : obligatoire, entier de 1 à 5 ; `comment` : facultatif, 500 caractères max.
- 403 si non participant (y compris aidant refusé et admin), 409 si la séance n'est pas `TERMINEE`,
  409 si déjà évaluée, 404 si inconnue ou masquée.

`GET /api/requests/{id}/ratings` (participants uniquement) :

```json
{ "canRate": true, "ratings": [ { "id": 1, "requestId": 7, "score": 5, "comment": "…",
  "createdAt": "…", "rater": { "id": 3, "fullName": "…" }, "rated": { "id": 4, "fullName": "…" } } ] }
```

`canRate` indique au frontend s'il faut afficher le formulaire d'évaluation.

## 3. Tableau de bord personnel

`GET /api/dashboard` (rôle STUDENT ; 403 pour un admin)

```json
{
  "stats": { "publishedRequests": 2, "openRequests": 1, "offersMade": 3, "offersAccepted": 1,
             "completedSessions": 1, "ratingsReceived": 1, "averageRating": 4.0 },
  "myRequests": [ { "id", "title", "subject", "level", "status", "hidden", "createdAt",
                    "completedAt", "offersCount", "pendingOffersCount" } ],
  "myOffers":   [ { "id", "message", "status", "createdAt", "requestId", "requestTitle",
                    "requestStatus", "subject", "requestAuthor" } ],
  "sessions":   [ { "requestId", "title", "subject", "completedAt", "role": "AUTEUR|AIDANT",
                    "partner", "ratedByMe" } ]
}
```

- `myRequests` inclut les demandes masquées par un admin (`hidden: true`) pour que l'auteur le sache.
- `myOffers` et `sessions` ignorent les demandes masquées.
- `averageRating` vaut `null` tant qu'aucune évaluation n'a été reçue (arrondi à 0,1).
- `sessions[].ratedByMe = false` → afficher un bouton « Évaluer ».

## 4. Signalement et espace administrateur

### Étudiant : signaler une publication

`POST /api/requests/{id}/reports` (rôle STUDENT) → 201

```json
{ "reason": "Contenu hors sujet" }
```

`reason` : 5 à 500 caractères. 403 sur sa propre demande, 409 si déjà signalée par lui, 404 si inconnue ou masquée.

### Administrateur (`/api/admin/**`, rôle ADMIN)

| Endpoint | Effet |
|---|---|
| `GET /api/admin/reports` | Publications ayant au moins un signalement `EN_ATTENTE`, regroupées par demande |
| `GET /api/admin/reports?pendingOnly=false` | Toutes les publications déjà signalées (historique) |
| `POST /api/admin/requests/{id}/hide` | Masque la publication ; signalements en attente → `TRAITE` |
| `POST /api/admin/requests/{id}/restore` | Rend visible une publication masquée |
| `POST /api/admin/requests/{id}/dismiss` | Signalements en attente → `REJETE` ; la publication reste visible |

Chaque élément renvoyé :

```json
{ "requestId": 7, "title": "…", "description": "…", "status": "OUVERTE", "hidden": false,
  "createdAt": "…", "subject": { "id": 1, "name": "…" }, "author": { "id": 3, "fullName": "…" },
  "pendingReports": 2,
  "reports": [ { "id": 1, "requestId": 7, "reason": "…", "status": "EN_ATTENTE",
                 "createdAt": "…", "resolvedAt": null, "reporter": { "id": 5, "fullName": "…" } } ] }
```

Erreurs : 404 si la demande n'existe pas, 409 si déjà masquée (`hide`), pas masquée (`restore`)
ou sans signalement en attente (`dismiss`). Un étudiant reçoit 403 sur tout `/api/admin/**`.

Une publication masquée disparaît de la liste, du détail et de toutes les actions des étudiants
(champ `hidden` déjà respecté par l'API existante).

## Modèle de données ajouté

- `help_requests.completed_at` (nullable)
- `ratings` : `score`, `comment`, `request_id`, `rater_id`, `rated_id`, unique (`request_id`, `rater_id`)
- `reports` : `reason`, `status`, `created_at`, `resolved_at`, `request_id`, `reporter_id`,
  unique (`reporter_id`, `request_id`)

Les tables et la colonne sont créées automatiquement (`spring.jpa.hibernate.ddl-auto=update`).
