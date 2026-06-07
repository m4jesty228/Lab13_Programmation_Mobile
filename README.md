# Lab — MapApplication : Géolocalisation Android avec OpenStreetMap, PHP et MySQL (Java)

**Auteur :** DOSSAH Yao Landry  
**Filière :** Génie CyberDéfense et Systèmes de Télécommunications Embarquées (GCDSTE)  
**Établissement :** ENSA Marrakech

---

## Contexte pédagogique

Ce laboratoire couvre une chaîne complète de géolocalisation mobile : une application Android capte les coordonnées GPS en temps réel via `LocationManager`, les transmet via Volley (HTTP POST) à un backend PHP hébergé sur XAMPP, qui les persiste dans une base MySQL. Une deuxième activité affiche toutes les positions enregistrées sous forme de marqueurs sur une carte OpenStreetMap via la bibliothèque OSMDroid. L'ensemble fonctionne en réseau local entre l'émulateur et le PC hôte.

---

## Environnement

| Composant | Détail |
|-----------|--------|
| IDE Android | Android Studio |
| Langage Android | Java |
| Min SDK | API 24 |
| Compile SDK | 36 |
| Backend | PHP 8+ (XAMPP) |
| Base de données | MySQL — base `map_project` |
| Réseau HTTP | Volley 1.2.1 |
| Affichage carte | OSMDroid 6.1.18 (OpenStreetMap) |
| Identifiant appareil | `ANDROID_ID` (Settings.Secure) |
| Adresse serveur | `10.0.2.2` (localhost émulateur) |

---

## Architecture globale

```
┌─────────────────────────────────┐
│         Android (émulateur)     │
│                                 │
│  MainActivity                   │
│  └── GPS (LocationManager)  ────┼──► POST lat/lon/date/imei
│                                 │         │
│  GoogleMapActivity              │    createPosition.php
│  └── OSMDroid MapView       ◄───┼──── getPosition.php
│       └── Markers (JSON)        │         │
└─────────────────────────────────┘         │
                                            ▼
                                 ┌─────────────────────┐
                                 │   XAMPP (10.0.2.2)  │
                                 │   map_project/       │
                                 │   MySQL: positions   │
                                 └─────────────────────┘
```

---

## Architecture du projet Android

```
MapApplication/
├── app/src/main/
│   ├── java/com/example/mapapplication/
│   │   ├── MainActivity.java         ← GPS + Volley POST
│   │   └── GoogleMapActivity.java    ← OSMDroid + marqueurs
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml         ← bouton "Afficher La Map"
│   │   │   └── activity_google_map.xml   ← MapView OSMDroid
│   │   ├── values/
│   │   │   └── strings.xml               ← messages provider/localisation
│   │   └── xml/
│   │       └── network_security_config.xml ← cleartext HTTP autorisé pour 10.0.2.2
│   └── AndroidManifest.xml
├── build.gradle
└── gradle/libs.versions.toml
```

---

## Architecture du backend PHP

```
htdocs/map_project/
├── db.php                ← connexion PDO centralisée
├── createPosition.php    ← endpoint POST : reçoit Android, insère en base
└── getPosition.php       ← endpoint GET/POST : retourne toutes les positions en JSON
```

---

## Flux de données complet

```
[Émulateur Android]
      |
      | GPS fix (latitude, longitude, altitude, accuracy)
      ↓
onLocationChanged(location)
      |
      ├── Toast : nouvelle position détectée
      └── addPosition(lat, lon)
              |
              | Volley StringRequest POST → 10.0.2.2
              ↓
[createPosition.php]
      |
      ├── Validation paramètres POST (latitude, longitude, date, imei)
      ├── INSERT INTO positions VALUES (...)
      └── {"success": true, "message": "Position enregistrée"}

[Bouton "Afficher La Map"]
      |
      ↓
GoogleMapActivity → loadPositions()
      |
      | Volley JsonObjectRequest POST → 10.0.2.2
      ↓
[getPosition.php]
      |
      └── SELECT * FROM positions ORDER BY date DESC
              |
              ↓
      {"success": true, "positions": [...]}
              |
              ↓
      Pour chaque position → new Marker(map) → map.getOverlays().add(marker)
      map.invalidate()
```

---

## Partie 1 — Base de données MySQL

La base `map_project` est créée dans phpMyAdmin. La table `positions` stocke chaque coordonnée envoyée par l'émulateur :

```sql
CREATE TABLE `positions` (
  `id`        int(11)      NOT NULL AUTO_INCREMENT,
  `latitude`  double       NOT NULL,
  `longitude` double       NOT NULL,
  `date`      datetime     NOT NULL,
  `imei`      varchar(50)  NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

**Résultat observé :** après le premier fix GPS simulé via *Extended Controls → Location* de l'émulateur, une ligne apparaît dans phpMyAdmin avec les coordonnées exactes, la date au format `YYYY-MM-DD HH:mm:ss` et l'`ANDROID_ID` de l'émulateur.

<img width="959" height="479" alt="2" src="https://github.com/user-attachments/assets/b47d78ac-bcf2-4d33-91ee-febc90302131" />

---

## Partie 2 — Backend PHP

### Connexion centralisée (`db.php`)

La connexion PDO est centralisée dans `db.php` avec `ERRMODE_EXCEPTION` activé — sans cette option, les erreurs SQL passent silencieusement. Les deux endpoints l'incluent via `require_once`.

### `createPosition.php`

Vérifie que la méthode est POST, valide les quatre paramètres (`latitude`, `longitude`, `date`, `imei`), et insère en base via une requête préparée avec paramètres nommés (`:latitude`, `:longitude`, etc.) pour prévenir toute injection SQL. Retourne :

```json
{"success": true, "message": "Position enregistrée"}
```

### `getPosition.php`

Exécute `SELECT * FROM positions ORDER BY date DESC` et sérialise le résultat. La réponse JSON est directement consommable par Volley côté Android.

**Résultat observé :** accès direct depuis le navigateur sur `localhost/map_project/getPosition.php` après une insertion :

<img width="864" height="108" alt="3" src="https://github.com/user-attachments/assets/23d90661-dc63-49cd-bd35-6c90fe4f70e0" />

---

## Partie 3 — Android : MainActivity (GPS + Volley)

### Permissions et sécurité réseau

Quatre permissions sont déclarées dans le manifest : `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `READ_PHONE_STATE`, `INTERNET`. Le trafic HTTP cleartext vers `10.0.2.2` est autorisé via `network_security_config.xml` — Android 9+ bloque le HTTP par défaut, cette configuration est indispensable pour joindre XAMPP depuis l'émulateur.

### Localisation GPS

`LocationManager` est configuré avec `GPS_PROVIDER`, un intervalle de 60 secondes et une distance minimale de 150 mètres. Ces paramètres sont adaptés à un usage réel ; pour les tests en émulateur, la position est injectée manuellement via *Extended Controls → Location*.

### Identifiant appareil

`Settings.Secure.ANDROID_ID` est utilisé à la place de l'IMEI — stable par installation, ne nécessite pas de permission supplémentaire, et compatible avec toutes les versions Android récentes qui restreignent `TelephonyManager.getDeviceId()`.

### Envoi Volley

`addPosition()` construit un `StringRequest` POST avec quatre paramètres formatés, dont la date en `yyyy-MM-dd HH:mm:ss` via `SimpleDateFormat`.

**Résultat observé :** à chaque fix GPS simulé, un Toast affiche les coordonnées et la position est insérée en base, confirmée dans phpMyAdmin.

---

## Partie 4 — Android : GoogleMapActivity (OSMDroid + marqueurs)

OSMDroid doit être initialisé via `Configuration.getInstance().load()` **avant** `setContentView()` — sans cette étape, la carte ne se charge pas correctement. La source de tuiles `MAPNIK` correspond au style standard OpenStreetMap.

`loadPositions()` envoie un `JsonObjectRequest` POST vers `getPosition.php`. La réponse JSON est parsée : chaque objet du tableau `positions` donne lieu à un `Marker` positionné sur `GeoPoint(latitude, longitude)`, avec le titre et la date en snippet. `map.invalidate()` force le rendu après ajout de tous les marqueurs.

Le cycle de vie de la carte est correctement géré : `map.onResume()`, `map.onPause()` et `map.onDetach()` sont appelés dans les méthodes correspondantes de l'activité.

**Résultat observé :** au lancement de `GoogleMapActivity`, la carte OpenStreetMap se charge et les marqueurs correspondant aux positions enregistrées apparaissent aux coordonnées exactes.

<img width="959" height="484" alt="1" src="https://github.com/user-attachments/assets/46268fa3-546b-45c9-84d0-c5985fa1ac98" />

---

## Erreurs rencontrées et solutions

| Erreur | Cause | Solution |
|--------|-------|----------|
| `isMinifyEnabled` — GradleScriptException | Kotlin DSL utilisé dans un projet Groovy | Remplacer par `minifyEnabled false` (syntaxe Groovy) |
| `Cannot convert Dependency: activity` | Clé `libs.activity` absente du toml | Renommer en `activity-ktx` dans le toml, utiliser `libs.activity.ktx` |
| AAR metadata — compileSdk trop bas | `androidx.activity:1.13.0` requiert compileSdk ≥ 36 | Passer `compileSdk` et `targetSdk` à 36 |
| Carte blanche / pas de tuiles | `Configuration.getInstance().load()` absent ou après `setContentView` | Appeler la configuration OSMDroid avant `setContentView()` |
| Volley — connexion refusée | Trafic HTTP bloqué par Android 9+ | Ajouter `network_security_config.xml` avec `cleartextTrafficPermitted="true"` pour `10.0.2.2` |
| Aucune insertion en base | Paramètre POST mal nommé ou PHP non rechargé | Tester `createPosition.php` directement via le navigateur avant Android |

---

## Points clés retenus

- `10.0.2.2` est l'adresse spéciale qui pointe vers `localhost` de la machine hôte depuis l'émulateur Android — une IP Wi-Fi classique ne fonctionnerait pas ici.
- `network_security_config.xml` est la bonne pratique pour autoriser le HTTP sur une adresse précise, plutôt que `android:usesCleartextTraffic="true"` qui autorise tout le trafic HTTP sans restriction.
- OSMDroid nécessite une initialisation explicite avant toute utilisation — c'est une différence notable avec Google Maps SDK qui s'initialise via le manifest.
- `ANDROID_ID` est l'identifiant appareil recommandé depuis Android 10+ ; l'IMEI est progressivement verrouillé par les politiques de confidentialité Google.
- Les requêtes préparées PDO avec paramètres nommés (`:param`) sont indispensables dès qu'une valeur externe entre dans une requête SQL.
- Le cycle de vie OSMDroid (`onResume`, `onPause`, `onDetach`) doit être géré explicitement pour éviter les fuites mémoire et les comportements incorrects lors de la navigation.

---

*Lab réalisé dans le cadre du cours Développement Mobile — ENSA Marrakech, Filière GCDSTE*
