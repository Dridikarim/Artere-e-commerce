# Shop : Catalogue de produits & Panier

API REST backend (Spring Boot 3, Java 21) répondant aux deux user stories du test :

1. **Catalogue** : catégories imbriquées à l'infini, produits, liens entre les deux.
2. **Panier** : créer un panier, ajouter / modifier / supprimer des produits, consulter le total.

## Stack

- Java 21, Spring Boot 3.3
- Spring Web, Spring Data JPA, Bean Validation
- H2 (base en mémoire, aucune installation requise)
- Lombok
- JUnit 5, Mockito, AssertJ, MockMvc

## Lancer le projet

```bash
mvn spring-boot:run     # démarre sur http://localhost:8080
mvn test                # lance tous les tests
```

Console H2 : http://localhost:8080/h2-console (JDBC URL : `jdbc:h2:mem:shopdb`, utilisateur `sa`, mot de passe vide).

## Architecture

```
src/main/java/com/example/shop/
├── entity/        Modèle JPA : Category, Product, Cart, CartItem
├── repository/    Accès aux données (Spring Data JPA)
├── dto/           Objets de requête avec validation
├── service/       Logique métier (transactions, règles de gestion)
├── controller/    API REST (aucune logique métier)
└── exception/     Exceptions métier + GlobalExceptionHandler (404 / 400)
```

Flux d'une requête : `controller → service → repository → base`.

### Modèle de données

```
Category (id, name, description, parent_id)
   ├── 1..* Category    (sous-catégories, via parent_id)
   └── *..* Product     (table de jointure category_product)

Product  (id, name, price, stock)

Cart     (id)  ── 1..* ── CartItem (id, product_id, quantity)
```

## Endpoints

### Catégories

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/categories` | Arbre complet depuis les catégories racines |
| GET | `/api/categories/{id}` | Une catégorie avec ses sous-catégories et produits |
| POST | `/api/categories` | Créer (`parentId` optionnel pour créer une sous-catégorie) |
| PUT | `/api/categories/{id}` | Modifier le nom et la description |
| DELETE | `/api/categories/{id}` | Supprimer (sous-catégories supprimées, produits conservés) |

### Produits

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/products` | Lister les produits |
| GET | `/api/products/{id}` | Détail d'un produit |
| POST | `/api/products` | Créer |
| PUT | `/api/products/{id}` | Modifier |
| DELETE | `/api/products/{id}` | Supprimer (refusé s'il est dans un panier) |

### Liens catalogue

| Méthode | URL | Description |
|---|---|---|
| POST | `/api/categories/{categoryId}/products/{productId}` | Lier un produit à une catégorie |
| DELETE | `/api/categories/{categoryId}/products/{productId}` | Délier |
| POST | `/api/categories/{parentId}/children/{childId}` | Faire d'une catégorie la sous-catégorie d'une autre |
| DELETE | `/api/categories/{parentId}/children/{childId}` | Délier (la sous-catégorie redevient racine) |

### Panier

| Méthode | URL | Description |
|---|---|---|
| POST | `/api/carts` | Créer un panier |
| GET | `/api/carts/{id}` | Lignes du panier et champ `total` |
| POST | `/api/carts/{id}/items` | Ajouter un produit (`{"productId":1,"quantity":2}`) |
| PUT | `/api/carts/{id}/items/{productId}` | Modifier la quantité (`{"quantity":3}`) |
| DELETE | `/api/carts/{id}/items/{productId}` | Retirer un produit |

## Exemple de scénario

```bash
# 1. Catalogue
curl -X POST localhost:8080/api/categories -H "Content-Type: application/json" \
     -d '{"name":"Informatique","description":"High-tech"}'
curl -X POST localhost:8080/api/categories -H "Content-Type: application/json" \
     -d '{"name":"Ordinateurs","parentId":1}'
curl -X POST localhost:8080/api/products -H "Content-Type: application/json" \
     -d '{"name":"Laptop","price":999.90,"stock":5}'
curl -X POST localhost:8080/api/categories/2/products/1
curl localhost:8080/api/categories

# 2. Panier
curl -X POST localhost:8080/api/carts
curl -X POST localhost:8080/api/carts/1/items -H "Content-Type: application/json" \
     -d '{"productId":1,"quantity":2}'
curl -X PUT  localhost:8080/api/carts/1/items/1 -H "Content-Type: application/json" \
     -d '{"quantity":3}'
curl localhost:8080/api/carts/1          # total = 2999.70
```

## Gestion des erreurs

Les erreurs renvoient un JSON homogène :

```json
{ "status": 404, "error": "Not Found", "message": "Catégorie introuvable: 42" }
```

| Cas | Code |
|---|---|
| Ressource inconnue | 404 |
| Données invalides (nom vide, prix négatif, quantité < 1…) | 400 |
| Règle métier violée (cycle, stock insuffisant…) | 400 |

## Choix de conception

- **Arbre de catégories** : une auto-référence `parent` suffit pour une profondeur illimitée. Le lien d'une sous-catégorie est refusé s'il créerait un **cycle** (une catégorie ne peut pas devenir l'ancêtre d'elle-même).
- **Produit multi-catégories** : relation many-to-many, plus souple que le schéma du sujet, qui reste respecté.
- **Suppression d'une catégorie** : ses sous-catégories sont supprimées en cascade, les produits sont conservés (seuls les liens disparaissent).
- **Suppression d'un produit** : refusée s'il figure dans un panier, pour éviter des paniers incohérents.
- **Stock** : vérifié à l'ajout et à la modification d'une ligne de panier, mais **non décrémenté** (il n'y a pas de validation de commande dans le sujet).
- **Total du panier** : calculé à la volée (`prix × quantité`, en `BigDecimal`), jamais stocké, donc toujours cohérent.
- **Un même produit** ajouté deux fois dans un panier fusionne les quantités sur une seule ligne.

## Tests

| Niveau | Fichiers | Objectif |
|---|---|---|
| Unitaire (Mockito) | `CategoryServiceTest`, `ProductServiceTest`, `CartServiceTest`, `CartTest` | Règles métier isolées, sans Spring |
| Web (`@WebMvcTest`) | `ProductControllerTest` | Codes HTTP, validation, format d'erreur |
| Intégration (`@SpringBootTest`) | `ShopApiTest` | Parcours complets avec la base H2 |

## Pistes d'amélioration

Faute de temps (test limité à 2 heures) :

- DTO de réponse pour ne plus exposer directement les entités.
- Pagination et recherche sur les produits.
- Validation de commande avec décrémentation du stock et gestion de la concurrence (verrou optimiste).
- Authentification et un panier par client.
- Documentation OpenAPI / Swagger.
- Base PostgreSQL avec migrations Flyway.