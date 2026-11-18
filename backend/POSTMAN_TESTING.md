# 🚀 Guide Complet de Test des APIs ShopMind (Postman & Bruno)

Ce document détaille toutes les étapes pour tester l'intégralité des endpoints du backend **ShopMind** avant d'intégrer le module de paiement.

---

## 📌 1. Configuration Globale dans Postman / Bruno

Créez un environnement ou des variables de collection avec les valeurs suivantes :

| Variable | Valeur par défaut | Description |
| :--- | :--- | :--- |
| `baseUrl` | `http://localhost:8080` | URL de base du backend |
| `token` | *(Généré après login)* | Token JWT pour les routes protégées |
| `orderId` | *(UUID après checkout)* | ID UUID de la commande créée |
| `productId` | `1` | ID du produit testé |
| `categoryId` | `1` | ID de la catégorie testée |

> [!TIP]
> **Script Postman automatique :** Dans la requête `POST /api/auth/login`, onglet **Scripts > Post-response**, ajoutez ce script pour sauvegarder automatiquement le token :
> ```javascript
> const data = pm.response.json();
> if (data.token) {
>     pm.environment.set("token", data.token);
>     console.log("Token JWT sauvegardé :", data.token);
> }
> ```

---

## 🔐 2. Module Authentification (`/api/auth`)

### 2.1 Inscription (`POST /api/auth/register`)
- **URL :** `{{baseUrl}}/api/auth/register`
- **Méthode :** `POST`
- **Headers :** `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "fullName": "Anas Jarjini",
    "email": "anas@example.com",
    "password": "Password123!"
  }
  ```
- **Réponse attendue (201 Created) :**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "anas@example.com",
    "fullName": "Anas Jarjini",
    "role": "USER"
  }
  ```
  *(Un cookie HTTP-only `refresh_token` est automatiquement déposé dans la réponse)*

---

### 2.2 Connexion (`POST /api/auth/login`)
- **URL :** `{{baseUrl}}/api/auth/login`
- **Méthode :** `POST`
- **Headers :** `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "email": "anas@example.com",
    "password": "Password123!"
  }
  ```
- **Réponse attendue (200 OK) :**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "anas@example.com",
    "fullName": "Anas Jarjini",
    "role": "USER"
  }
  ```

---

### 2.3 Utilisateur Courant (`GET /api/auth/me`)
- **URL :** `{{baseUrl}}/api/auth/me`
- **Méthode :** `GET`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue (200 OK) :**
  ```json
  {
    "id": "c50a88b8-20b9-4723-b981-9e5fa721c736",
    "fullName": "Anas Jarjini",
    "email": "anas@example.com",
    "role": "USER"
  }
  ```

---

### 2.4 Rafraîchir le Token (`POST /api/auth/refresh`)
- **URL :** `{{baseUrl}}/api/auth/refresh`
- **Méthode :** `POST`
- **Headers :** Aucun Bearer nécessaire *(le cookie `refresh_token` est envoyé automatiquement par Postman/navigateur)*
- **Réponse attendue (200 OK) :**
  ```json
  {
    "token": "NOUVEAU_TOKEN_JWT...",
    "email": "anas@example.com",
    "fullName": "Anas Jarjini",
    "role": "USER"
  }
  ```

---

### 2.5 Déconnexion (`POST /api/auth/logout`)
- **URL :** `{{baseUrl}}/api/auth/logout`
- **Méthode :** `POST`
- **Réponse attendue :** `204 No Content` *(le cookie est supprimé et le refresh token est révoqué)*

---

### 2.6 Déconnexion de toutes les sessions (`POST /api/auth/logout-all`)
- **URL :** `{{baseUrl}}/api/auth/logout-all`
- **Méthode :** `POST`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue :** `204 No Content` *(tous les refresh tokens de l'utilisateur sont invalidés)*

---

## 👤 3. Profil Utilisateur (`/api/users`)

### 3.1 Consulter son Profil (`GET /api/users/profile`)
- **URL :** `{{baseUrl}}/api/users/profile`
- **Méthode :** `GET`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue (200 OK) :**
  ```json
  {
    "id": "c50a88b8-20b9-4723-b981-9e5fa721c736",
    "fullName": "Anas Jarjini",
    "email": "anas@example.com",
    "phone": null,
    "city": null,
    "address": null,
    "role": "USER"
  }
  ```

---

### 3.2 Modifier son Profil (`PUT /api/users/profile`)
- **URL :** `{{baseUrl}}/api/users/profile`
- **Méthode :** `PUT`
- **Headers :**
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "fullName": "Anas Jarjini",
    "phone": "+212 600-000000",
    "city": "Casablanca",
    "address": "123 Boulevard Mohammed V"
  }
  ```
- **Réponse attendue (200 OK) :** Renvoie l'objet profil mis à jour.

---

### 3.3 Modifier son Mot de Passe (`PUT /api/users/password`)
- **URL :** `{{baseUrl}}/api/users/password`
- **Méthode :** `PUT`
- **Headers :**
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "currentPassword": "Password123!",
    "newPassword": "NewPassword123@"
  }
  ```
- **Réponse attendue (200 OK) :**
  ```json
  {
    "message": "Mot de passe modifié"
  }
  ```

---

## 🏷️ 4. Catégories (`/api/categories`)

### 4.1 Lister toutes les catégories (`GET /api/categories`)
- **URL :** `{{baseUrl}}/api/categories`
- **Méthode :** `GET` *(Public)*
- **Réponse attendue (200 OK) :**
  ```json
  [
    {
      "id": 1,
      "name": "Électronique",
      "description": "Smartphones, ordinateurs et accessoires"
    }
  ]
  ```

---

### 4.2 Créer une catégorie [ADMIN] (`POST /api/categories`)
- **URL :** `{{baseUrl}}/api/categories`
- **Méthode :** `POST`
- **Headers :**
  - `Authorization: Bearer {{token}}` *(Compte ayant le rôle ADMIN)*
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "name": "Smartphones",
    "description": "Téléphones intelligents et tablettes"
  }
  ```
- **Réponse attendue :** `201 Created`

---

## 📦 5. Produits (`/api/products`)

### 5.1 Créer un produit [ADMIN] (`POST /api/products`)
- **URL :** `{{baseUrl}}/api/products`
- **Méthode :** `POST`
- **Headers :**
  - `Authorization: Bearer {{token}}` *(Rôle ADMIN)*
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "name": "iPhone 15 Pro",
    "description": "Smartphone Apple 128GB Titane naturel",
    "brand": "Apple",
    "price": 1199.99,
    "stock": 25,
    "imageUrl": "https://example.com/iphone15.jpg",
    "active": true,
    "categoryId": 1
  }
  ```
- **Réponse attendue :** `201 Created` *(Notez l'`id` du produit retourné)*

---

### 5.2 Lister tous les produits (`GET /api/products`)
- **URL :** `{{baseUrl}}/api/products`
- **Méthode :** `GET` *(Public)*

---

### 5.3 Rechercher un produit par nom (`GET /api/products/search`)
- **URL :** `{{baseUrl}}/api/products/search?name=iPhone`
- **Méthode :** `GET` *(Public)*

---

### 5.4 Consulter un produit par ID (`GET /api/products/{id}`)
- **URL :** `{{baseUrl}}/api/products/1`
- **Méthode :** `GET` *(Public)*

---

## 🛒 6. Panier d'Achat (`/api/cart`)

### 6.1 Ajouter un produit au panier (`POST /api/cart/items`)
- **URL :** `{{baseUrl}}/api/cart/items`
- **Méthode :** `POST`
- **Headers :**
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "productId": 1,
    "quantity": 2
  }
  ```
- **Réponse attendue (201 Created) :**
  ```json
  {
    "items": [
      {
        "productId": 1,
        "name": "iPhone 15 Pro",
        "imageUrl": "https://example.com/iphone15.jpg",
        "unitPrice": 1199.99,
        "quantity": 2,
        "subtotal": 2399.98
      }
    ],
    "total": 2399.98
  }
  ```

---

### 6.2 Consulter son panier (`GET /api/cart`)
- **URL :** `{{baseUrl}}/api/cart`
- **Méthode :** `GET`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue (200 OK) :** Retourne la liste des articles et le montant total calculé.

---

### 6.3 Modifier la quantité d'un article (`PUT /api/cart/items/{productId}`)
- **URL :** `{{baseUrl}}/api/cart/items/1`
- **Méthode :** `PUT`
- **Headers :**
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "quantity": 3
  }
  ```
  *(Mettre `quantity: 0` supprime directement l'article du panier)*
- **Réponse attendue (200 OK) :** Panier recalculé avec la nouvelle quantité.

---

### 6.4 Supprimer un article du panier (`DELETE /api/cart/items/{productId}`)
- **URL :** `{{baseUrl}}/api/cart/items/1`
- **Méthode :** `DELETE`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue (200 OK) :** Panier mis à jour.

---

### 6.5 Vider complètement le panier (`DELETE /api/cart`)
- **URL :** `{{baseUrl}}/api/cart`
- **Méthode :** `DELETE`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue :** `204 No Content`

---

## 📋 7. Commandes (`/api/orders`)

### 7.1 Passer la commande / Checkout (`POST /api/orders`)
> [!IMPORTANT]
> Assurez-vous d'avoir au moins 1 article dans votre panier avant de faire le checkout !

- **URL :** `{{baseUrl}}/api/orders`
- **Méthode :** `POST`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue (201 Created) :**
  ```json
  {
    "id": "e4b6c31a-6d45-4e3a-921d-7bb51e06fa99",
    "status": "PENDING",
    "cancelReason": null,
    "total": 2399.98,
    "createdAt": "2026-11-18T02:00:00Z",
    "items": [
      {
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "quantity": 2,
        "unitPrice": 1199.99
      }
    ]
  }
  ```
  *(Le stock du produit a été décrémenté et le panier a été automatiquement vidé)*.
  **Copiez l'ID UUID généré dans votre variable `{{orderId}}` !**

---

### 7.2 Lister ses commandes (`GET /api/orders`)
- **URL :** `{{baseUrl}}/api/orders?page=0&size=10`
- **Méthode :** `GET`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue (200 OK) :** Page paginée contenant la liste des commandes du client connecté.

---

### 7.3 Consulter le détail d'une commande (`GET /api/orders/{id}`)
- **URL :** `{{baseUrl}}/api/orders/{{orderId}}`
- **Méthode :** `GET`
- **Headers :** `Authorization: Bearer {{token}}`
- **Réponse attendue (200 OK) :** Détail complet de la commande avec ses lignes d'articles.

---

### 7.4 Annuler une commande en attente (`POST /api/orders/{id}/cancel`)
- **URL :** `{{baseUrl}}/api/orders/{{orderId}}/cancel`
- **Méthode :** `POST`
- **Headers :**
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "reason": "CHANGED_MIND"
  }
  ```
  *(Raisons valides pour le client : `CHANGED_MIND`, `PAYMENT_FAILED`)*
- **Réponse attendue (200 OK) :**
  ```json
  {
    "id": "{{orderId}}",
    "status": "CANCELLED",
    "cancelReason": "CHANGED_MIND",
    "total": 2399.98,
    "createdAt": "...",
    "items": [...]
  }
  ```
  *(Le stock du produit a été réintégré automatiquement)*.

---

### 7.5 Mettre à jour le statut [ADMIN] (`PUT /api/orders/{id}/status`)
- **URL :** `{{baseUrl}}/api/orders/{{orderId}}/status`
- **Méthode :** `PUT`
- **Headers :**
  - `Authorization: Bearer {{token}}` *(Compte ADMIN)*
  - `Content-Type: application/json`
- **Body JSON (Exemple expédition) :**
  ```json
  {
    "status": "SHIPPED",
    "reason": null
  }
  ```
- **Body JSON (Exemple remboursement) :**
  ```json
  {
    "status": "REFUNDED",
    "reason": "DEFECTIVE"
  }
  ```
- **Réponse attendue (200 OK) :** Statut de la commande mis à jour.

---

## 💳 8. Paiements (`/api/payments`)

### 8.1 Initier un Paiement (`POST /api/payments`)
- **URL :** `{{baseUrl}}/api/payments`
- **Méthode :** `POST`
- **Headers :**
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "orderId": "{{orderId}}"
  }
  ```
- **Réponse attendue (201 Created) :**
  ```json
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "orderId": "{{orderId}}",
    "provider": "MOCK",
    "providerRef": "mock_9f8e7d6c-...",
    "amount": 2399.98,
    "status": "INITIATED",
    "checkoutUrl": null
  }
  ```
  *(Le montant est automatiquement extrait de la commande existante pour empêcher toute fraude client).*

---

### 8.2 Simuler la Confirmation du Paiement (`POST /api/payments/{id}/mock-confirm`)
- **URL :** `{{baseUrl}}/api/payments/{{paymentId}}/mock-confirm?success=true`
- **Méthode :** `POST`
- **Headers :** `Authorization: Bearer {{token}}`
- **Paramètre URL :** `success=true` *(ou `false` pour simuler un échec)*
- **Réponse attendue (200 OK) :**
  ```json
  {
    "id": "{{paymentId}}",
    "orderId": "{{orderId}}",
    "provider": "MOCK",
    "providerRef": "mock_...",
    "amount": 2399.98,
    "status": "SUCCEEDED",
    "checkoutUrl": null
  }
  ```
  *(La commande liée passe automatiquement au statut `PAID` !)*

---

### 8.3 Webhook Fournisseur de Paiement (`POST /api/payments/webhook`)
- **URL :** `{{baseUrl}}/api/payments/webhook`
- **Méthode :** `POST` *(Public, sécurisé par le secret du webhook)*
- **Headers :**
  - `X-Webhook-Secret: shopmind-webhook-secret-dev`
  - `Content-Type: application/json`
- **Body JSON :**
  ```json
  {
    "providerRef": "mock_...",
    "status": "SUCCEEDED"
  }
  ```
- **Réponse attendue :** `200 OK`

---

## 🛡️ 9. Scénarios d'Erreurs à Vérifier

| Cas de Test | Route | Action | Statut Attendu |
| :--- | :--- | :--- | :--- |
| Mot de passe faible | `POST /api/auth/register` | Mot de passe sans majuscule/spécial | **`400 Bad Request`** avec message détaillé |
| Email déjà utilisé | `POST /api/auth/register` | Même email que l'inscription précédente | **`409 Conflict`** |
| Mauvais mot de passe | `POST /api/auth/login` | Mauvais mot de passe | **`401 Unauthorized`** |
| Accès sans token | `GET /api/cart` | Aucun header `Authorization` | **`403 Forbidden` / `401 Unauthorized`** |
| Stock insuffisant | `POST /api/cart/items` | Quantité demandée > Stock disponible | **`409 Conflict`** |
| Checkout panier vide | `POST /api/orders` | Checkout alors que le panier est vide | **`400 Bad Request`** ("Cart is empty") |
| Annulation commande non PENDING | `POST /api/orders/{id}/cancel` | Annuler une commande déjà `SHIPPED` | **`409 Conflict`** |
| Payer commande déjà payée | `POST /api/payments` | Initier paiement sur commande `PAID` | **`409 Conflict`** |
| Webhook mauvais secret | `POST /api/payments/webhook` | Header `X-Webhook-Secret` invalide | **`401 Unauthorized`** |
