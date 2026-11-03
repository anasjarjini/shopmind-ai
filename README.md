# ShopMind AI 🛍️🤖

Plateforme e-commerce spécialisée en électronique avec système de recommandation IA.

## Stack Technique

| Service | Technologie |
|---|---|
| Frontend | React + Vite + TypeScript + Tailwind CSS + Zustand |
| Backend | Spring Boot 3 + Java 17 + Spring Security (JWT) |
| Base de données | PostgreSQL |
| Cache | Redis |
| AI Service | Python + FastAPI + Scikit-learn |
| DevOps | Docker + Docker Compose |

## Structure du projet

```
shopmind-ai/
├── frontend/        # React + Vite + TypeScript
├── backend/         # Spring Boot (auth, product, order, recommendation)
├── ai-service/      # Python + FastAPI
└── docker-compose.yml
```

## Lancer le projet

```bash
docker-compose up --build
```

## Services & Ports

| Service | Port |
|---|---|
| Frontend | 3000 |
| Backend API | 8080 |
| AI Service | 8000 |
| PostgreSQL | 5432 |
| Redis | 6379 |
