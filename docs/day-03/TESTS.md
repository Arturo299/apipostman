# Día 3 — Tests: qué probar, cuándo y cómo

Complemento del [README del día 3](README.md). Hoy hemos hecho que **unas requests produzcan datos que consumen otras** (token, ids). Eso es justo lo que más se rompe, así que aquí lo probamos.

La sintaxis de los tests (`pm.test`, `pm.expect`, Chai) se verá a fondo el [día 4](../day-04/README.md). Hoy basta con esto:

```javascript
pm.test("Nombre del requisito", () => {
    pm.response.to.have.status(200);              // si una aserción falla, el test sale en rojo
    pm.expect(pm.response.json().name).to.equal("Margarita");
});
```

**Material:** [pizzeria-api-tests-dia3.postman_collection.json](pizzeria-api-tests-dia3.postman_collection.json) contiene todos los ejemplos de esta guía. Impórtala, selecciona el entorno *Pizzería - DEV* ([../day-02/environments/](../day-02/environments/)) y ejecútala con el Runner, o desde la terminal:

```bash
npx newman run docs/day-03/pizzeria-api-tests-dia3.postman_collection.json \
  -e docs/day-02/environments/pizzeria-dev.postman_environment.json
```

Resultado esperado: 25 requests, 68 aserciones y 0 fallos. Con `-n 3` (tres iteraciones seguidas) también pasa: la suite se puede **repetir**.

---

## 1. Dónde vive cada cosa: Arrange, Act, Assert

Todo test sigue el mismo esquema, y en Postman cada parte tiene su sitio:

| Fase | Dónde | Qué se hace |
|---|---|---|
| **Arrange** (preparar) | *Scripts → Pre-request* | Generar nombres únicos, conseguir un token, crear los datos que necesita la request |
| **Act** (actuar) | La request | Enviar el `POST`, `GET`, `PUT` o `DELETE` |
| **Assert** (comprobar) | *Scripts → Post-response* | `pm.test(...)` sobre status, cabeceras y body. Guardar lo que necesiten otras requests |

**Orden de ejecución** de los scripts para cada request:

```
Pre-request:   colección → carpeta → request
               ── se envía la request ──
Post-response: colección → carpeta → request
```

Por eso lo común (login automático, comprobaciones globales) va en la **colección**, y lo específico de un caso va en la **request**.

---

## 2. Mapa: qué test, cuándo y dónde

| Tipo de test | Cuándo hacerlo | Dónde | En la colección |
|---|---|---|---|
| **Smoke global** | Siempre, desde el primer día. Es la red mínima de cualquier colección, también de la generada desde OpenAPI | Post-response de la **colección** | Scripts de la colección |
| **Guardas del entorno** | En cuanto exista más de un entorno, y siempre que haya uno de PROD | Pre-request de la **colección** | Scripts de la colección |
| **Contrato** | Antes de importar o regenerar la colección desde `/v3/api-docs`, y en cada versión nueva de la API | Request al contrato | *1. Contrato* |
| **Request que produce datos** (login, crear) | Siempre que un script haga `pm.environment.set`: **antes de guardar, comprueba** | Post-response de esa request | *2. Auth → Login*, *3 → Crear ingrediente* |
| **Request que consume datos** | Cuando una request usa `{{token}}`, `{{ingredientId}}`… | Pre-request (Arrange) y post-response | *3. Ingredientes* |
| **Ciclo de vida (CRUD)** | Por cada recurso que se pueda crear y borrar | Una request por operación | *3. Ingredientes* |
| **Regla de negocio / flujo E2E** | Cuando el resultado de una operación depende de otra (precio ← coste) | Carpeta que se ejecuta entera y en orden | *4. Flujo* |
| **Seguridad** | Por cada operación protegida: al menos un caso sin token | Requests con *No Auth* | *5. Seguridad* |

---

## 3. Ejemplos, de lo general a lo particular

### 3.1 Smoke global (post-response de la colección)

**Cuándo:** desde el minuto uno. Son dos comprobaciones baratas que se ejecutan en **todas** las requests, incluida cualquiera que añadas o importes después.

```javascript
pm.test("[global] El servidor no devuelve un 5xx", () => {
    pm.expect(pm.response.code, pm.response.text()).to.be.below(500);
});

if (pm.response.code !== 204) {
    pm.test("[global] La respuesta es JSON", () => {
        pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json");
    });
}
```

- Un 4xx **no** se marca como fallo aquí: a veces es justo lo esperado (404 tras borrar, 401 sin token). Eso lo decide cada request.
- El segundo argumento de `pm.expect(valor, mensaje)` hace que, si falla, el informe muestre el body del error.
- El prefijo `[global]` distingue estos tests en los informes.

### 3.2 Guardas del entorno (pre-request de la colección)

**Cuándo:** en cuanto tengas varios entornos. Evitan dos accidentes clásicos: ejecutar sin entorno y escribir en producción.

```javascript
if (!pm.variables.get("baseUrl")) {
    throw new Error("No hay baseUrl: selecciona el entorno «Pizzería - DEV»");
}

const entorno = pm.environment.name || "";
if (/PROD/i.test(entorno) && pm.request.method !== "GET") {
    console.warn(`⛔ ${pm.info.requestName}: ${pm.request.method} omitido en ${entorno}`);
    pm.execution.skipRequest();
}
```

### 3.3 Login: comprobar antes de guardar

**Cuándo:** en cualquier request cuyo script guarde algo en el entorno. Si el login falla y el script guarda igualmente, el entorno se llena de `undefined` y **todas** las requests siguientes fallan con un 401 que no explica nada.

```javascript
pm.test("Login correcto (200)", () => {
    pm.response.to.have.status(200);
});

if (pm.response.code === 200) {
    const body = pm.response.json();

    pm.test("Devuelve accessToken, refreshToken, tokenType y expiresIn", () => {
        pm.expect(body).to.include.all.keys("accessToken", "refreshToken", "tokenType", "expiresIn");
        pm.expect(body.accessToken).to.be.a("string").and.not.be.empty;
        pm.expect(body.tokenType).to.equal("Bearer");
        pm.expect(body.expiresIn).to.be.a("number").and.above(0);
    });

    pm.environment.set("token", body.accessToken);
    pm.environment.set("refreshToken", body.refreshToken);
    pm.environment.set("tokenExpiresAt", Date.now() + body.expiresIn * 1000);

    // Test del propio script: ¿se ha escrito en el entorno lo que esperábamos?
    pm.test("El token queda guardado en el entorno", () => {
        pm.expect(pm.environment.get("token")).to.equal(body.accessToken);
    });
} else {
    pm.environment.unset("token");             // nada de tokens viejos o basura
}

pm.test("La respuesta no devuelve la contraseña", () => {
    pm.expect(pm.response.text()).to.not.include(pm.variables.get("password"));
});
```

Y justo después, **Me** comprueba que el token guardado sirve de verdad:

```javascript
pm.test("El token pertenece al usuario del entorno", () => {
    pm.expect(pm.response.json().username).to.equal(pm.environment.get("username"));
});
```

### 3.4 Crear: status, Location, eco del body y captura del id

**Cuándo:** en todo `POST`. Es la request que produce el id del que dependen las demás.

```javascript
pm.test("201 Created", () => {
    pm.response.to.have.status(201);
});

if (pm.response.code === 201) {
    const creado = pm.response.json();
    // Lo que enviamos, con las variables ya sustituidas
    const enviado = JSON.parse(pm.variables.replaceIn(pm.request.body.raw));

    pm.test("Devuelve lo que se envió, con un id nuevo", () => {
        pm.expect(creado.id).to.be.a("number");
        pm.expect(creado.name).to.equal(enviado.name);
        pm.expect(creado.cost).to.equal(enviado.cost);
    });

    pm.test("La cabecera Location apunta al recurso creado", () => {
        pm.expect(pm.response.headers.get("Location")).to.match(new RegExp(`/api/ingredients/${creado.id}$`));
    });

    pm.environment.set("ingredientId", creado.id);
}
```

Variante capturando por cabecera (*4. Flujo → Crear ingrediente (captura por Location)*):

```javascript
const id = pm.response.headers.get("Location").split("/").pop();
pm.environment.set("ingredientId", id);

pm.test("El id de Location coincide con el del body", () => {
    pm.expect(Number(id)).to.equal(pm.response.json().id);   // "23" (texto) frente a 23 (número)
});
```

### 3.5 Leer, modificar, borrar y el 404 final

**Cuándo:** por cada recurso con CRUD. Comprueban que cada operación hace lo que dice **y nada más**: que el `PUT` no cambia el id, que el `DELETE` no devuelve body y que, tras borrar, el recurso ya no existe.

```javascript
// Obtener: las variables se guardan como TEXTO, convierte antes de comparar
pm.expect(ingrediente.id).to.equal(Number(pm.environment.get("ingredientId")));

// Modificar
pm.expect(ingrediente.cost).to.equal(1.25);

// Borrar
pm.test("204 No Content y sin body", () => {
    pm.response.to.have.status(204);
    pm.expect(pm.response.text()).to.be.empty;
});

// Obtener tras borrar
pm.test("El mensaje dice qué id no existe", () => {
    pm.response.to.have.status(404);
    pm.expect(pm.response.json().message).to.include(pm.environment.get("deletedIngredientId"));
});
```

> **Trampa habitual:** `pm.expect(ingrediente.id).to.equal(pm.environment.get("ingredientId"))` falla con `expected 16 to equal '16'`. Es el aviso del apartado 1.3 del README convertido en error.

### 3.6 Flujo de negocio: el coste del ingrediente cambia el precio

**Cuándo:** cuando una regla relaciona dos recursos. Es la práctica 4 del README convertida en tests:

```
Crear ingrediente (1,00 €)  → guarda ingredientId
Crear pizza con él          → guarda pizzaId y precioInicial
PUT ingrediente a 1,50 €
GET pizza                   → price == precioInicial + 0,50 × 1,20
DELETE ingrediente          → 409 (lo usa la pizza)
DELETE pizza, DELETE ingrediente → 204 (limpieza)
```

```javascript
pm.test("El precio sube 0,50 × 1,20 = 0,60 €", () => {
    const inicial = Number(pm.collectionVariables.get("precioInicial"));
    pm.expect(pm.response.json().price).to.be.closeTo(inicial + 0.60, 0.001);
});
```

`closeTo` y no `equal`: con decimales en JavaScript, `0.1 + 0.2 !== 0.3`.

### 3.7 Seguridad

**Cuándo:** por cada operación protegida, al menos una prueba **sin token** (*Auth = No Auth*). Y si hay logout, que el token deje de valer.

```javascript
pm.test("401 Unauthorized", () => pm.response.to.have.status(401));

pm.test("Indica el esquema de autenticación esperado", () => {
    pm.expect(pm.response.headers.get("WWW-Authenticate")).to.equal("Bearer");
});
```

### 3.8 Contrato OpenAPI

**Cuándo:** antes de importar o regenerar la colección desde `/v3/api-docs` y con cada versión de la API. Detecta que alguien ha renombrado una ruta o ha quitado la seguridad de una operación **antes** de que fallen 20 requests.

```javascript
const contrato = pm.response.json();

pm.test("Publica todas las rutas que usa la colección", () => {
    pm.expect(contrato.paths).to.include.all.keys(
        "/api/auth/login", "/api/ingredients", "/api/ingredients/{id}", "/api/pizzas", "/api/pizzas/{id}");
});

pm.test("Las operaciones de escritura exigen token", () => {
    const op = contrato.paths["/api/ingredients"].post;
    pm.expect((op.security || []).flatMap(Object.keys)).to.include("bearerAuth");
});

pm.test("IngredientRequest exige name, cost y vegetarian", () => {
    pm.expect(contrato.components.schemas.IngredientRequest.required)
        .to.have.members(["name", "cost", "vegetarian"]);
});
```

---

## 4. ¿Y si ejecuto un DELETE antes que el POST?

Es **el** problema de las colecciones encadenadas. Si ejecutas *Borrar ingrediente* suelto, `{{ingredientId}}` está vacío (o apunta a algo que ya no existe) y la request sale contra `/api/ingredients/` → 401, 404 o 405, con un error que no dice cuál es el problema real.

Hay cuatro soluciones, y cada una tiene su momento:

| Estrategia | Cómo | Cuándo usarla |
|---|---|---|
| **A. Orden fijo** | Ejecutar siempre la carpeta completa con el Runner | Nunca como única defensa: no protege a quien pulsa *Send* en una request suelta |
| **B. Requisito + skip** (*fail fast*) | El pre-request comprueba la variable; si falta, marca un test en rojo con un mensaje claro y **no envía** la request | Flujos E2E, donde la dependencia entre pasos **es** lo que se está probando |
| **C. Fixture propio** (autosuficiente) | El pre-request **crea** lo que necesita con `pm.sendRequest` si no existe | Tests de una operación concreta: el `DELETE` prueba el borrado, no el alta |
| **D. Login automático** | El pre-request de la colección pide un token si falta o va a caducar | Siempre. Es el caso particular de C para el token |

La colección usa **C + D** en la carpeta *3. Ingredientes* y **B** en la carpeta *4. Flujo*.

### 4.1 Estrategia B: requisito + skip

```javascript
// Pre-request de «Obtener pizza (precio recalculado)»
if (!pm.environment.get("pizzaId")) {
    pm.test("Requisito: existe {{pizzaId}}", () => {
        pm.expect.fail("Ejecuta antes «Crear pizza con el ingrediente»");
    });
    pm.execution.skipRequest();          // no se envía: un fallo claro en vez de varios confusos
}
```

### 4.2 Estrategia C: la request prepara sus propios datos

Pre-request de *Obtener*, *Modificar* y *Borrar ingrediente*:

```javascript
// La guarda de PROD de la colección omite la request, pero NO los pm.sendRequest
if (/PROD/i.test(pm.environment.name || "")) {
    pm.execution.skipRequest();
    return;                                                 // ni fixture ni request
}

const baseUrl = pm.variables.get("baseUrl");
const id = pm.environment.get("ingredientId");

function crearIngrediente() {
    const nombre = `Test ${Date.now()}`;
    pm.sendRequest({
        url: `${baseUrl}/api/ingredients`,
        method: "POST",
        header: { "Content-Type": "application/json", "Authorization": `Bearer ${pm.environment.get("token")}` },
        body: { mode: "raw", raw: JSON.stringify({ name: nombre, cost: 0.75, vegetarian: true }) }
    }, (err, res) => {
        pm.test("[arrange] Ingrediente de prueba creado", () => {
            pm.expect(err).to.be.null;
            pm.expect(res.code).to.equal(201);
        });
        pm.environment.set("ingredientId", res.json().id);
        pm.collectionVariables.set("ingredientName", nombre);
    });
}

if (!id) {
    crearIngrediente();                                     // no hay id
} else {
    pm.sendRequest(`${baseUrl}/api/ingredients/${id}`, (err, res) => {
        if (res.code === 404) crearIngrediente();           // hay id, pero ya no existe
        else pm.collectionVariables.set("ingredientName", res.json().name);
    });
}
```

Fíjate en el segundo caso: en la aplicación de Postman el entorno **persiste entre ejecuciones**, así que un `ingredientId` de ayer puede apuntar a algo borrado. No basta con mirar si la variable existe: hay que comprobar que el recurso existe.

El post-response del `DELETE` deja el entorno limpio:

```javascript
if (pm.response.code === 204) {
    pm.environment.set("deletedIngredientId", pm.environment.get("ingredientId"));
    pm.environment.unset("ingredientId");                   // que nadie reutilice un id muerto
}
```

### 4.3 Demostración

En Postman: vacía `token` e `ingredientId` en el entorno y pulsa **Send** directamente en *Borrar ingrediente*. En la consola (`Ctrl+Alt+C`) verás tres peticiones: login automático, alta del ingrediente de prueba y el `DELETE`.

Con Newman, `--folder` admite también el **nombre de una request**, y cada ejecución parte del entorno del fichero (sin token ni ids):

```bash
C=docs/day-03/pizzeria-api-tests-dia3.postman_collection.json
E=docs/day-02/environments/pizzeria-dev.postman_environment.json

npx newman run $C -e $E --folder "Borrar ingrediente"
npx newman run $C -e $E --folder "Obtener ingrediente borrado (404)"
npx newman run $C -e $E --folder "Obtener pizza (precio recalculado)"
```

Salida real:

```
→ Borrar ingrediente
  POST http://localhost:8080/api/auth/login [200 OK]          ← D: login automático
  POST http://localhost:8080/api/ingredients [201 Created]    ← C: el fixture
  ✓  [arrange] Ingrediente de prueba creado
  DELETE http://localhost:8080/api/ingredients/19 [204 No Content]
  ✓  [global] El servidor no devuelve un 5xx
  ✓  204 No Content y sin body

→ Obtener pizza (precio recalculado)
  POST http://localhost:8080/api/auth/login [200 OK]
  1. Requisito: existe {{pizzaId}}                            ← B: falla claro y no envía el GET
```

### 4.4 Trampas que hemos encontrado montando esto

| Trampa | Síntoma | Solución |
|---|---|---|
| `pm.variables.set(...)` en un pre-request | La variable **local** no dura una request: dura **toda la ejecución** del Runner o de Newman y tapa la del entorno. Al poner un token inválido en una request, en la siguiente iteración todas daban 401 | Para algo que solo afecta a una request, modifica la propia request: `pm.request.headers.upsert({ key: "Authorization", value: ... })` |
| Guarda de PROD solo en la colección | `skipRequest()` omite la request, pero los `pm.sendRequest` de los fixtures **se envían igual**: en PROD se creaban y borraban ingredientes de prueba | Cada script que escribe con `pm.sendRequest` comprueba también el entorno |
| `await pm.sendRequest(...)` | Funciona en la aplicación de Postman, pero Newman 6 da `SyntaxError` | Usa la forma con *callback*: funciona en los dos |
| El fixture duplicado en varias requests | Cambiar el body del alta obliga a tocar 3 scripts | En un equipo, publícalo en el **Package Library** y úsalo con `pm.require(...)`. En ficheros, mantenlo en un único sitio y genera la colección |
| Guardar sin comprobar | Un login fallido deja `token = undefined` y todo da 401 | Primero `pm.test`, después `pm.environment.set`, y solo si el status es el esperado |

> Los callbacks de `pm.sendRequest` terminan **antes** de que empiece el siguiente script. Por eso el login automático de la colección ya ha guardado el token cuando el pre-request de la request crea el fixture.

---

## 5. Buenas prácticas

**Independencia y repetibilidad**
- Cada request de la carpeta de operaciones debe funcionar **sola** y **en cualquier orden** (estrategia C).
- Los flujos E2E van en su propia carpeta, con dependencias explícitas (estrategia B).
- Nombres únicos con `Date.now()`: así la suite se puede repetir sin chocar con el 409.
- **Deja los datos como estaban:** lo que crea un test, lo borra el propio test o su flujo. Prueba siempre con `-n 2`: si la segunda vuelta falla, hay datos o variables que se arrastran.
- Al borrar un recurso, haz `unset` de su id.

**Qué comprobar**
- Primero el status y después el body. Y **antes de guardar** una variable, comprueba la respuesta.
- No te quedes en el *happy path*: por cada escritura, al menos un caso de error (sin token, id inexistente, recurso en uso).
- Comprueba la **lógica** (precio = coste × 1,20) y no solo la forma de la respuesta.
- No uses ids fijos de los datos semilla para lo que creas tú. Para lectura sí puedes (`ingredientIds: [1, 2, 3]`), porque `data.sql` es estable.
- Decimales con `closeTo`. Ids del entorno con `Number(...)`.

**Cómo escribirlos**
- Nombra el test como un requisito: *"Devuelve 404 si el ingrediente no existe"*, no *"test 3"*.
- Un `pm.test` por idea. Si falla, el nombre ya dice qué se ha roto.
- Pasa un mensaje en `pm.expect(valor, "contexto")` cuando compruebes elementos de un array o respuestas de error.
- Prefijos `[global]` y `[arrange]` para distinguir en el informe qué es del caso y qué es infraestructura.
- Lo que se repite en todas las requests, en la colección. Lo de un recurso, en la carpeta. Lo del caso, en la request.

**Entornos y secretos**
- Guarda contra PROD en el pre-request de la colección: nada de escrituras.
- `password` como *secret*, sin *initial value*, o en el Vault. Comprueba que la respuesta no la devuelve.
- Los tests no deben imprimir tokens ni contraseñas con `console.log`.

**Antipatrones**

| ❌ Evitar | ✅ Mejor |
|---|---|
| Copiar a mano el id del `POST` al `DELETE` | Capturarlo en el post-response del `POST` |
| Un único test gigante con 15 `expect` | Varios `pm.test`, uno por requisito |
| `if (status === 200) { pm.test(...) }` sin `else` | Testear siempre el status. Si no es el esperado, el test debe salir en rojo |
| Tiempos estrictos en local (`below(50)`) | Umbral holgado y en una variable por entorno (día 4) |
| Tests que solo pasan en un orden concreto sin decirlo | Estrategia B: el requisito explícito |
| Datos de prueba que se quedan en la base de datos | Limpieza al final del flujo |

---

## 6. Práctica

1. Importa la colección y ejecútala entera con el Runner. Repite con 3 iteraciones.
2. Vacía `token` e `ingredientId` en tu entorno y pulsa **Send** solo en *Modificar ingrediente*. Abre la consola y explica las peticiones que ves.
3. Pon un `ingredientId` que no exista (`999999`) y vuelve a enviar *Borrar ingrediente*. ¿Qué rama del pre-request se ejecuta?
4. Ejecuta suelta *Obtener pizza (precio recalculado)*. ¿Por qué esta carpeta usa la estrategia B y no la C?
5. Cambia la contraseña del entorno por una errónea y ejecuta la colección. ¿Cuántos tests fallan y cuál es el primero que lo explica?
6. Duplica tu entorno DEV, llámalo *Pizzería - PROD (prueba)* (sigue apuntando a localhost) y lanza la carpeta *3. Ingredientes*. ¿Qué requests se omiten? Quita el `if` de PROD del pre-request de *Borrar ingrediente* y repite: ¿qué llega ahora al servidor?
7. Añade a tu colección del README los tests de 3.3 (login) y 3.4 (crear) a las requests que ya tenías.
8. **Reto:** convierte la carpeta *4. Flujo* a la estrategia C: que *Obtener pizza (precio recalculado)* funcione suelta creando su ingrediente y su pizza, y que se limpie al terminar.

---

## Recursos

- [Escribir tests (post-response scripts)](https://learning.postman.com/docs/tests-and-scripts/write-scripts/test-scripts/)
- [Scripts pre-request](https://learning.postman.com/docs/tests-and-scripts/write-scripts/pre-request-scripts/)
- [Orden de ejecución de los scripts](https://learning.postman.com/docs/tests-and-scripts/write-scripts/intro-to-scripts/)
- [Referencia del objeto `pm` (`pm.sendRequest`, `pm.execution`)](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/overview/)
- [Ámbitos de variables (incluidas las locales)](https://learning.postman.com/docs/use/send-requests/variables/variables/)
- [Newman: opciones de `newman run` (`--folder`, `-n`)](https://github.com/postmanlabs/newman#command-line-options)
