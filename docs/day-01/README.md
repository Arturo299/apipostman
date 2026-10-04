# Día 1 — Fundamentos de APIs REST y primeros pasos con Postman

**Lunes 5 de octubre · 15:30–19:00**

| Bloque | Duración | Contenido |
|---|---|---|
| M1 | 2h | Qué es una API REST, anatomía de una request, autenticación y formatos de datos |
| Descanso | 10 min | |
| M2 (1ª parte) | 1h20 | Instalación, cuenta, workspaces e interfaz de Postman. Primeras peticiones a la Pizzería API |

**Objetivo del día:** nivelar los conocimientos de HTTP del grupo y lanzar las primeras peticiones con Postman.

---

## 1. Qué es una API REST

Una **API** (*Application Programming Interface*) es un contrato que define cómo un programa pide algo a otro. En las **APIs web**, ese contrato se apoya en **HTTP**: el cliente envía una *request* y el servidor contesta con una *response*.

**REST** (*Representational State Transfer*) es un estilo de arquitectura que Roy Fielding describió en su tesis doctoral en el año 2000. No es un estándar ni un protocolo, sino un conjunto de restricciones:

| Restricción | Qué significa en la práctica |
|---|---|
| Cliente-servidor | El cliente (Postman, un frontend, otro servicio) y el servidor evolucionan por separado |
| Sin estado (*stateless*) | Cada petición lleva todo lo necesario, por ejemplo el token. El servidor no guarda una "sesión de conversación" |
| Cacheable | Las respuestas indican si se pueden cachear (`Cache-Control`, `ETag`) |
| Interfaz uniforme | Recursos identificados por URL y manipulados con los verbos HTTP estándar |
| Sistema en capas | Puede haber proxies, gateways o balanceadores sin que el cliente lo note |

### 1.1 Recursos

Un **recurso** es cualquier "cosa" con identidad que expone la API. En nuestro proyecto hay dos:

```
/api/ingredients        → colección de ingredientes
/api/ingredients/3      → el ingrediente con id 3 (Mozzarella)
/api/pizzas             → colección de pizzas
/api/pizzas/1           → la pizza con id 1 (Margarita)
```

Convenciones habituales:

- Sustantivos en plural, nunca verbos: `/api/pizzas`, no `/api/getPizzas`.
- Jerarquía con `/` e identificadores en la ruta: `/api/pizzas/{id}`.
- Filtros, orden y paginación en la *query string*: `/api/pizzas?vegetarian=true&maxPrice=8`.

### 1.2 Verbos HTTP

| Método | Uso | ¿Seguro? | ¿Idempotente? | Ejemplo en la Pizzería |
|---|---|---|---|---|
| `GET` | Leer | Sí | Sí | `GET /api/pizzas` |
| `POST` | Crear o ejecutar una acción | No | No | `POST /api/ingredients` |
| `PUT` | Reemplazar el recurso completo | No | Sí | `PUT /api/ingredients/16` |
| `PATCH` | Modificar parcialmente | No | No necesariamente | (no implementado) |
| `DELETE` | Borrar | No | Sí | `DELETE /api/pizzas/8` |
| `HEAD` / `OPTIONS` | Metadatos y capacidades | Sí | Sí | |

- **Seguro:** no modifica el estado del servidor.
- **Idempotente:** repetir la petición N veces deja el servidor igual que hacerla una vez. Importa mucho cuando hay reintentos automáticos.

### 1.3 Códigos de estado (*status codes*)

| Familia | Significado | Los que verás en el curso |
|---|---|---|
| 1xx | Informativos | Poco frecuentes en APIs |
| 2xx | Éxito | **200** OK · **201** Created (con cabecera `Location`) · **204** No Content |
| 3xx | Redirecciones | 301, 302, 304 Not Modified |
| 4xx | Error del cliente | **400** Bad Request · **401** Unauthorized · 403 Forbidden · **404** Not Found · 405 Method Not Allowed · **409** Conflict · 415 Unsupported Media Type · **422** Unprocessable Content |
| 5xx | Error del servidor | 500 Internal Server Error · 502 Bad Gateway · 503 Service Unavailable |

La Pizzería API usa cada código con un criterio concreto. Eso es lo que probaremos en el módulo de testing:

| Situación | Código |
|---|---|
| JSON mal formado, campo obligatorio vacío o coste negativo | 400 |
| Falta el token o está caducado | 401 |
| El id no existe | 404 |
| Nombre duplicado, o borrar un ingrediente que usa alguna pizza | 409 |
| Crear una pizza con un `ingredientId` inexistente | 422 |

> **401 frente a 403:** 401 significa "no sé quién eres" (falta autenticación o no es válida). 403 significa "sé quién eres, pero no tienes permiso".

## 2. Anatomía de una request

```http
POST /api/ingredients?source=curso HTTP/1.1          ← método, ruta, query string, versión
Host: localhost:8080                                  ← cabeceras (headers)
Content-Type: application/json
Accept: application/json
Authorization: Bearer 3f0c9c1e-...

{                                                     ← cuerpo (body)
  "name": "Rúcula",
  "cost": 0.50,
  "vegetarian": true
}
```

Y la respuesta:

```http
HTTP/1.1 201 Created                                  ← código de estado
Location: http://localhost:8080/api/ingredients/16
Content-Type: application/json

{"id":16,"name":"Rúcula","cost":0.50,"vegetarian":true}
```

### 2.1 URL

```
http://localhost:8080/api/pizzas/1?verDesglose=true#seccion
└─┬─┘  └───┬───┘└─┬┘└─────┬────┘ └──────┬───────┘ └──┬──┘
esquema   host  puerto   ruta       query string   fragmento (no viaja al servidor)
```

- **Path params:** forman parte de la ruta (`/pizzas/{id}`). En Postman se escriben como `/pizzas/:id` y se rellenan en la pestaña *Params*.
- **Query params:** pares `clave=valor` tras el `?`, separados por `&`. Los caracteres especiales se codifican (*URL encoding*): un espacio se convierte en `%20`.

### 2.2 Cabeceras más importantes

| Cabecera | Sentido | Para qué sirve |
|---|---|---|
| `Content-Type` | Request y response | Formato del body que se envía (`application/json`) |
| `Accept` | Request | Formato que el cliente quiere recibir |
| `Authorization` | Request | Credenciales (`Bearer …`, `Basic …`) |
| `Location` | Response | URL del recurso recién creado (con 201) |
| `WWW-Authenticate` | Response | Esquema de autenticación esperado (con 401) |
| `Cache-Control`, `ETag` | Response | Caché |
| `X-Request-Id` (o similar) | Ambos | Trazabilidad entre servicios. No es estándar, pero es muy común |

### 2.3 Body

Solo lo llevan normalmente `POST`, `PUT` y `PATCH`. Su formato lo indica `Content-Type` (ver el apartado 4).

## 3. Autenticación

| Mecanismo | Cómo viaja | Comentario |
|---|---|---|
| **API Key** | Cabecera (`X-API-Key: abc123`) o query param (`?api_key=abc123`) | Sencillo. Identifica a la aplicación, no al usuario. Mejor en cabecera que en la URL, porque las URLs acaban en los logs |
| **Basic Auth** | `Authorization: Basic base64(usuario:contraseña)` | Base64 **no es cifrado**: solo es seguro con HTTPS |
| **Bearer Token** | `Authorization: Bearer <token>` | El token se obtiene antes (login u OAuth). Puede ser opaco o un JWT |
| **OAuth 2.0** | Un servidor de autorización emite *access tokens* (y *refresh tokens*) mediante distintos *grant types* | Estándar para delegar acceso: *client credentials* (máquina a máquina), *authorization code + PKCE* (usuarios) |

**En la Pizzería API:**

- Los `GET` son públicos.
- `POST`, `PUT` y `DELETE` exigen `Authorization: Bearer <accessToken>`.
- El token se obtiene con `POST /api/auth/login`, que recibe `{"username":"admin","password":"admin123"}`.
- El access token caduca a los 5 minutos (60 segundos en *staging*) y se renueva con `POST /api/auth/refresh`, igual que en OAuth 2.0.

Postman gestiona todos estos tipos desde la pestaña **Authorization** de una request, carpeta o colección.

## 4. Formatos de datos

| Content-Type | Ejemplo | Uso típico |
|---|---|---|
| `application/json` | `{"name":"Margarita","price":5.16}` | El estándar de las APIs actuales |
| `application/xml` | `<pizza><name>Margarita</name></pizza>` | Sistemas heredados, SOAP, banca y administración |
| `application/x-www-form-urlencoded` | `username=admin&password=admin123` | Formularios HTML clásicos y el *token endpoint* de OAuth 2.0 |
| `multipart/form-data` | Partes separadas por un *boundary* | Subida de ficheros junto a campos de texto |
| binario (`application/octet-stream`, `image/png`…) | Bytes | Descarga o subida de un fichero "tal cual" |

**JSON en 30 segundos:** objetos `{}`, arrays `[]`, cadenas entre comillas dobles, números, `true`/`false` y `null`. No admite comentarios ni comas finales. Fíjate en que `"cost": 0.50` es un número y `"cost": "0.50"` es una cadena. Los tests del día 4 distinguirán entre ambos.

---

## 5. Postman desde cero (1ª parte)

### 5.1 Instalación y cuenta

1. Descarga la aplicación de escritorio desde [postman.com/downloads](https://www.postman.com/downloads/).
2. Crea una cuenta gratuita, o entra con la de la empresa si existe un *team*.
3. Usa la versión de escritorio. La versión web no llega a `localhost` sin instalar el *Postman Agent*.

### 5.2 Workspaces

| Tipo | Visibilidad | Cuándo usarlo |
|---|---|---|
| **Personal** | Solo tú | Pruebas y aprendizaje |
| **Team** | Miembros del equipo | Colecciones y entornos compartidos del proyecto |
| **Partner / Public** | Externos / todo el mundo | APIs públicas y colaboración con clientes |

Para el curso, crea un workspace personal llamado **Curso Postman**.

### 5.3 Recorrido por la interfaz

- **Sidebar:** *Collections*, *Environments*, *History*, *APIs*, *Mock servers*, *Monitors* y *Flows*.
- **Constructor de requests:** método, URL y las pestañas *Params*, *Authorization*, *Headers*, *Body* y *Scripts*.
- **Panel de respuesta:** *Body* (Pretty / Raw / Preview), *Cookies*, *Headers*, *Test Results*, código de estado, tiempo y tamaño.
- **Selector de entorno** (arriba a la derecha) y **Globals**.
- **Console** (abajo a la izquierda): muestra la petición *real* que se ha enviado. Es la primera herramienta para depurar.

---

## 6. Práctica: arrancar la Pizzería API y lanzar las primeras peticiones

### 6.1 Arrancar el proyecto

```bash
cd pizzeria-api
mvnw.cmd spring-boot:run          # Windows
./mvnw spring-boot:run            # Linux / macOS
```

Cuando el log muestre `Started PizzeriaApiApplication`, abre http://localhost:8080/swagger-ui.html para ver los endpoints.

Modelo de datos: un **ingrediente** tiene `name`, `cost` (en euros) y `vegetarian`. Una **pizza** tiene `name`, `description` y una lista de ingredientes. Su precio no se guarda: se calcula en cada petición.

```
ingredientsCost = suma de cost de sus ingredientes
price           = ingredientsCost × 1,20     (redondeado a céntimos)
```

Por ejemplo, la Margarita lleva masa (1,50) + tomate (0,80) + mozzarella (1,70) + albahaca (0,30). Sus ingredientes cuestan 4,30 € y su precio es **5,16 €**.

### 6.2 Ejercicios

1. **GET de una colección.** Lanza `GET http://localhost:8080/api/pizzas`. ¿Cuántas pizzas hay? ¿Qué código de estado devuelve? ¿Cuánto tarda?
2. **Path param.** Escribe `GET http://localhost:8080/api/pizzas/:id` y da el valor `4` a `id` en la pestaña *Params*. Comprueba a mano que `price = ingredientsCost × 1,20`.
3. **Query params.** Busca las pizzas vegetarianas que cuesten 8 € o menos: `?vegetarian=true&maxPrice=8`. Activa y desactiva los parámetros con las casillas de *Params*.
4. **Errores.** Pide `/api/pizzas/999` y `/api/pizzas/abc`. Compara los códigos y el JSON de error.
5. **Sin permiso.** Intenta `POST /api/ingredients` con el body siguiente, sin token. ¿Qué código obtienes? Mira la cabecera `WWW-Authenticate` de la respuesta.
   ```json
   { "name": "Rúcula", "cost": 0.50, "vegetarian": true }
   ```
6. **Login y Bearer.** Lanza `POST /api/auth/login` con el body `{"username":"admin","password":"admin123"}` en *raw / JSON*. Copia el `accessToken`, pégalo en *Authorization → Bearer Token* y repite el ejercicio 5. Debes obtener **201** y una cabecera `Location`.
7. **Base de datos.** Abre http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzeria`, usuario `sa`, sin contraseña) y ejecuta `SELECT * FROM ingredients`. Comprueba que tu rúcula está en la tabla.
8. **Consola de Postman.** Abre la *Console* y revisa las cabeceras que Postman ha añadido por su cuenta (`User-Agent`, `Postman-Token`…).
9. **Bonus:** pulsa el icono `</>` (*Code*) de una request para generar el equivalente en cURL o en Java.

> Los datos viven en memoria: al reiniciar la API se vuelven a cargar los 15 ingredientes y las 7 pizzas iniciales.

---

## Recursos

**HTTP y REST**
- [MDN — Generalidades del protocolo HTTP](https://developer.mozilla.org/es/docs/Web/HTTP/Guides/Overview)
- [MDN — Mensajes HTTP](https://developer.mozilla.org/es/docs/Web/HTTP/Guides/Messages)
- [MDN — Métodos de petición HTTP](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Methods)
- [MDN — Códigos de estado de respuesta HTTP](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Status)
- [MDN — Cabeceras HTTP](https://developer.mozilla.org/es/docs/Web/HTTP/Reference/Headers)
- [MDN — Tipos MIME](https://developer.mozilla.org/es/docs/Web/HTTP/Guides/MIME_types)
- [RFC 9110 — HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html), la especificación oficial
- [Roy Fielding — Capítulo 5 de su tesis: REST](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)
- [restfulapi.net — Guía de diseño REST](https://restfulapi.net/)
- [JSON.org (en español)](https://www.json.org/json-es.html)

**Autenticación**
- [MDN — Autenticación HTTP](https://developer.mozilla.org/es/docs/Web/HTTP/Guides/Authentication)
- [RFC 7617 — Basic Authentication](https://www.rfc-editor.org/rfc/rfc7617.html)
- [RFC 6750 — Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750.html)
- [OAuth 2.0 — oauth.net](https://oauth.net/2/)
- [Introducción a JWT — jwt.io](https://www.jwt.io/introduction)

**Postman**
- [Descargar Postman](https://www.postman.com/downloads/)
- [Postman Learning Center — Primeros pasos](https://learning.postman.com/docs/getting-started/overview/)
- [Navegar por la interfaz de Postman](https://learning.postman.com/docs/getting-started/basics/navigating-postman/)
- [Enviar tu primera request (quick start)](https://learning.postman.com/docs/getting-started/quick-start/)
- [Workspaces](https://learning.postman.com/docs/collaborating-in-postman/using-workspaces/overview/)
- [Tipos de autorización en Postman](https://learning.postman.com/docs/use/send-requests/authorization/authorization-types/)
- [Generar fragmentos de código](https://learning.postman.com/docs/use/send-requests/create-requests/generate-code-snippets/)
