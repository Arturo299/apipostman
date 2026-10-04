# Pizzería API

API REST de prácticas para el curso **Desarrollo de APIs con Postman**. Gestiona un catálogo de ingredientes y pizzas. El precio de cada pizza se calcula en cada petición:

```
ingredientsCost = suma del coste de sus ingredientes
price           = ingredientsCost × (1 + 0,20)      ← 20 % de beneficio, redondeado a céntimos
```

**Stack:** Java 17 · Spring Boot 4.1 · Spring Data JPA · H2 en memoria · Bean Validation · springdoc-openapi.

## Arranque

```bash
./mvnw spring-boot:run                                        # DEV     → http://localhost:8080
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging     # STAGING → http://localhost:8081
```

En Windows, usa `mvnw.cmd` en lugar de `./mvnw`. También puedes empaquetar y lanzar el jar:

```bash
./mvnw package
java -jar target/pizzeria-api-0.0.1-SNAPSHOT.jar                                 # DEV
java -jar target/pizzeria-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=staging
```

La base de datos se crea en memoria y se rellena con [data.sql](src/main/resources/data.sql) (15 ingredientes y 7 pizzas) en cada arranque.

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI | http://localhost:8080/v3/api-docs · http://localhost:8080/v3/api-docs.yaml |
| Consola H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzeria`, usuario `sa`, sin contraseña) |
| Health | http://localhost:8080/actuator/health |

## Endpoints

| Método | Ruta | Auth | Respuestas |
|---|---|---|---|
| `POST` | `/api/auth/login` | — | 200, 400, 401 |
| `POST` | `/api/auth/refresh` | — | 200, 400, 401 |
| `GET` | `/api/auth/me` | Bearer | 200, 401 |
| `POST` | `/api/auth/logout` | Bearer | 204, 401 |
| `GET` | `/api/ingredients?vegetarian=` | — | 200 |
| `GET` | `/api/ingredients/{id}` | — | 200, 400, 404 |
| `POST` | `/api/ingredients` | Bearer | 201 + `Location`, 400, 401, 409 |
| `PUT` | `/api/ingredients/{id}` | Bearer | 200, 400, 401, 404, 409 |
| `DELETE` | `/api/ingredients/{id}` | Bearer | 204, 401, 404, 409 (si lo usa alguna pizza) |
| `GET` | `/api/pizzas?name=&vegetarian=&maxPrice=` | — | 200 |
| `GET` | `/api/pizzas/{id}` | — | 200, 400, 404 |
| `POST` | `/api/pizzas` | Bearer | 201 + `Location`, 400, 401, 409, 422 (ingrediente inexistente) |
| `PUT` | `/api/pizzas/{id}` | Bearer | 200, 400, 401, 404, 409, 422 |
| `DELETE` | `/api/pizzas/{id}` | Bearer | 204, 401, 404 |

### Ejemplos

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "admin", "password": "admin123" }
```

```json
{ "accessToken": "…", "refreshToken": "…", "tokenType": "Bearer", "expiresIn": 300 }
```

```http
POST /api/pizzas
Authorization: Bearer …
Content-Type: application/json

{ "name": "Diavola", "description": "Picante", "ingredientIds": [1, 2, 3, 7, 9] }
```

```json
{
  "id": 8, "name": "Diavola", "description": "Picante", "vegetarian": false,
  "ingredients": [ { "id": 1, "name": "Masa", "cost": 1.50, "vegetarian": true }, "…" ],
  "ingredientsCost": 6.20, "profitMargin": 0.20, "price": 7.44
}
```

Todos los errores comparten el mismo formato:

```json
{
  "timestamp": "2026-10-05T14:00:00Z", "status": 400, "error": "Bad Request",
  "message": "La petición contiene datos no válidos", "path": "/api/ingredients",
  "errors": [ { "field": "cost", "message": "debe ser mayor que o igual a 0.01" } ]
}
```

## Configuración por entorno

| Propiedad | DEV | STAGING |
|---|---|---|
| `server.port` | 8080 | 8081 |
| Usuarios (`pizzeria.auth.users.*`) | `admin/admin123`, `alumno/postman` | `admin/staging-secret`, `alumno/staging-postman` |
| `pizzeria.auth.access-token-ttl` | 5m | 60s |
| `pizzeria.auth.refresh-token-ttl` | 1h | 1h |
| `pizzeria.pricing.profit-margin` | 0.20 | 0.20 |

> La autenticación es **didáctica**: usa tokens opacos guardados en memoria, que se pierden al reiniciar. No es un mecanismo para producción.

## Estructura

```
src/main/java/com/curso/pizzeria
├── auth/          AuthService (tokens), AuthInterceptor (Bearer), WebConfig
├── dto/           Records de entrada y salida
├── exception/     Excepciones de negocio → 401, 404, 409, 422
├── model/         Entidades JPA Ingredient y Pizza
├── repository/    Spring Data JPA
├── service/       IngredientService, PizzaService, PriceCalculator
└── web/           Controladores REST, GlobalExceptionHandler, OpenApiConfig
```

## Tests

```bash
./mvnw test
```
