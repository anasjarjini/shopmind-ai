# Étape 1 : Build de l'application Angular
FROM node:22-alpine AS build
WORKDIR /app

# Copie des fichiers de dépendances
COPY package*.json ./
RUN npm ci

# Copie du code source et compilation de production
COPY . .
RUN npm run build -- --configuration production

# Étape 2 : Serveur Nginx léger
FROM nginx:alpine

# Configuration personnalisée de Nginx (gestion du routage SPA)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copie des fichiers compilés d'Angular dans le répertoire Nginx
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
