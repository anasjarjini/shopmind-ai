# ShopMind AI 🛍️🤖

**ShopMind AI** est une plateforme e-commerce moderne et scalable, spécialisée en produits électroniques, intégrant un microservice d'intelligence artificielle pour la recommandation personnalisée de produits en temps réel.

---

## 🛠️ Stack Technique

| Domaine | Technologies |
| :--- | :--- |
| **Frontend** | Angular 22, TypeScript, Tailwind CSS v4, Nginx (Alpine) |
| **Backend API** | Java 21, Spring Boot 3, Spring Security 6 (JWT + Refresh Cookies), Spring Data JPA, Hibernate |
| **AI Service** | Python 3.12, FastAPI, Scikit-learn (TF-IDF Vectorizer & Cosine Similarity), Uvicorn |
| **Base de Données** | PostgreSQL 16 Alpine |
| **Cache & Sessions** | Redis 7 Alpine |
| **Conteneurisation** | Docker & Docker Compose (Multi-stage builds) |

---

## 🌐 Services & Ports

| Service | Port Hôte | Port Conteneur | URL / Accès | Description |
| :--- | :---: | :---: | :--- | :--- |
| **Frontend** | `3000` | `80` | [http://localhost:3000](http://localhost:3000) | Application Web Angular servie par Nginx |
| **Backend API** | `8080` | `8080` | [http://localhost:8080](http://localhost:8080) | API REST Spring Boot principale |
| **AI Service** | `5000` | `5000` | [http://localhost:5000](http://localhost:5000) | Microservice IA FastAPI (Recommandations) |
| **PostgreSQL** | `5433` | `5432` | `localhost:5433` | Base de données relationnelle principale |
| **Redis** | `6379` | `6379` | `localhost:6379` | Cache applicatif |

---

## 📁 Structure du Projet

```text
shopmind-ai/
├── frontend/                     # Application Angular 22 + Tailwind CSS
│   ├── src/                      # Composants, services et styles
│   ├── Dockerfile                # Build multi-stage (Node 22 -> Nginx Alpine)
│   ├── nginx.conf                # Configuration Nginx SPA + Reverse Proxy API
│   └── package.json
├── backend/                      # API Spring Boot 3 (Java 21)
│   ├── src/main/java/com/shopmind/backend/
│   │   ├── client/               # Client HTTP vers le microservice IA
│   │   ├── controller/           # Endpoints REST (Auth, User, Product, Cart, Order, Payment)
│   │   ├── dto/                  # Data Transfer Objects
│   │   ├── entity/               # Entités JPA (User, Product, Order, Payment, etc.)
│   │   ├── repository/           # Spring Data JPA Repositories
│   │   ├── security/             # JWT, UserDetailsService, SecurityConfig
│   │   └── service/              # Logique métier transactionnelle
│   ├── POSTMAN_TESTING.md        # Guide complet de test des APIs
│   ├── shopmind_postman_collection.json # Collection Postman prête à l'emploi
│   └── Dockerfile
├── ai-service/                   # Microservice IA (FastAPI)
│   ├── recommendation/           # Moteur TF-IDF & extracteur de caractéristiques
│   ├── repositories/             # Requêtes directes PostgreSQL
│   ├── main.py                   # Endpoints FastAPI
│   └── Dockerfile
└── docker-compose.yml            # Orchestration de tous les conteneurs
```

---

## ✨ Fonctionnalités Principales

### 1. Sécurité & Authentification
- Authentification JWT sans état (*stateless*) avec Access Token (15 min) et Refresh Token sécurisé en cookie `httpOnly` (14 jours).
- Invalidation dynamique des tokens lors de la déconnexion (`/logout` et `/logout-all`).
- Gestion des profils utilisateurs et contrôle d'accès par rôles (`ROLE_USER`, `ROLE_ADMIN`).

### 2. Catalogue Produits & Import Amazon
- Gestion des catégories et des fiches produits complètes (nom, prix, marque, stock, note Amazon, avis).
- Galeries d'images multiples pour chaque produit.
- Endpoint d'importation en masse (`POST /api/products/import`) prenant en charge les jeux de données JSON Amazon (déduplication ASIN, validation stricte).

### 3. Moteur de Recommandation IA (Machine Learning)
- Microservice Python dédié calculant la similarité textuelle avec **TF-IDF** et la **similarité cosinus** (*Cosine Similarity*).
- Pondération intelligente des attributs (titre, marque, catégorie, description).
- Filtre par catégorie optionnel (`sameCategory=true`) et tri par score de similarité décroissant.
- Endpoint : `GET /api/products/{id}/recommendations?limit=5&sameCategory=true`.

### 4. Panier d'Achat & Gestion des Commandes
- Panier dynamique persistant par utilisateur.
- Workflow complet de commande : `PENDING` ➔ `PAID` ➔ `SHIPPED` ➔ `DELIVERED` (ou `CANCELLED`).
- Décrémentation automatique des stocks et restitution lors d'une annulation.

### 5. Système de Paiement Sécurisé
- Initiation de paiement avec validation du montant côté serveur (`POST /api/payments`).
- Simulation de passerelle de paiement (Mock Provider) pour les tests.
- Support des webhooks sécurisés par signature (`POST /api/payments/webhook`) synchronisant l'état des commandes.

---

## 🚀 Démarrage Rapide avec Docker

### Lancer tous les services en une commande :

```bash
cd ~/Pictures/shopmind-ai
docker compose up --build -d
```

### Vérifier l'état des conteneurs :

```bash
docker compose ps
```

---

## 💻 Démarrage en Mode Développement

Si vous souhaitez exécuter les services séparément sur votre machine :

### 1. Démarrer l'infrastructure (Postgres, Redis, IA)
```bash
docker compose up -d postgres redis ai-service
```

### 2. Lancer le Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```

### 3. Lancer le Frontend (Angular)
```bash
cd frontend
npm install
npm start
# L'application est accessible sur http://localhost:4200
```

---

## 🧪 Tests & Documentation API

Pour tester l'intégralité des endpoints :
- 📖 Consultez le [Guide de Test Postman](backend/POSTMAN_TESTING.md).
- 📥 Importez la collection Postman prête à l'emploi : [shopmind_postman_collection.json](backend/shopmind_postman_collection.json).
