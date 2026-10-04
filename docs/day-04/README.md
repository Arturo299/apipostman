# Día 4 — HAR, exportación, Git y primeros tests

**Jueves 8 de octubre · 15:30–19:00**

| Bloque | Duración | Contenido |
|---|---|---|
| M4 (2ª parte) | 30 min | Importar desde HAR, exportar en v2.1, sincronización con Git (Postman API y CI), avance de Postman CLI y Newman |
| M5 (1ª parte) | 1h20 | `pm.test` y `pm.expect` (aserciones Chai). Tests de status, tiempo de respuesta y Content-Type |
| Descanso | 10 min | |
| M5 (cont.) | 1h30 | Existencia y tipo de los campos del body, valores concretos, el cálculo del precio |

**Objetivo del día:** cerrar el ciclo de vida de una colección (capturar → exportar → versionar) y empezar a **verificar automáticamente** lo que hasta ahora mirábamos a ojo.

---

## 1. Importar desde HAR (captura de tráfico)

Un **HAR** (*HTTP Archive*) es un JSON con todas las peticiones y respuestas que ha registrado un navegador. Sirve para:

- Reproducir en Postman lo que hace una aplicación web sin tener su documentación.
- Adjuntar a una incidencia exactamente lo que ocurrió.

**Cómo obtenerlo:**
1. Abre http://localhost:8080/swagger-ui.html en Chrome o Edge y pulsa `F12 → Network`.
2. Ejecuta varias operaciones desde Swagger UI (*Try it out*).
3. Clic derecho en la lista → **Save all as HAR** (o el icono de descarga ⬇).
4. En Postman: `Import → Files → fichero.har`. Se crea una request por cada entrada.

> ⚠️ **Un HAR contiene cookies, cabeceras `Authorization` y bodies completos.** No lo compartas sin revisarlo. Chrome ofrece exportarlo "sanitizado" sin datos sensibles.

**Alternativas para capturar tráfico:** el *Postman Proxy* integrado y la extensión *Postman Interceptor* graban las peticiones directamente en una colección o en el *History*.

## 2. Exportar colecciones: formato v2.1

`Colección → ⋯ → Export → Collection v2.1 (recommended)`.

```json
{
  "info": {
    "name": "Pizzería API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth":     { "type": "bearer", "bearer": [{ "key": "token", "value": "{{token}}" }] },
  "event":    [ /* scripts de colección: prerequest / test */ ],
  "variable": [ { "key": "apiPath", "value": "/api" } ],
  "item": [
    { "name": "Auth", "item": [ /* requests */ ] }
  ]
}
```

- **v2.1 frente a v2.0:** la única diferencia relevante es cómo se representa la URL (un objeto con `host`, `path` y `query` en lugar de una cadena). Usa siempre v2.1, que es la que esperan Newman y la mayoría de herramientas.
- La exportación **incluye** scripts, tests, variables de colección y documentación. **No incluye** entornos ni globales: se exportan aparte.

## 3. Sincronización con repositorios Git

Una colección es código de pruebas y se versiona junto al código de la API.

| Estrategia | Cómo funciona | Para quién |
|---|---|---|
| **Exportar a mano** | `Export` → `git commit` del `.json` | Equipos pequeños. Es simple pero depende de la disciplina de cada persona |
| **Integración nativa con Git** | Postman conecta el workspace con una carpeta del repositorio local y trabaja sobre ficheros, con ramas y *commits* desde tu cliente Git | La opción recomendada en las versiones actuales |
| **Postman API** | Descargar o actualizar colecciones por HTTP con una API key | CI, automatizaciones y backups |
| **Integración con GitHub/GitLab** | Copia periódica de la colección a un repositorio | Backup y auditoría |

**Postman API: descargar la colección "oficial" en CI.** Genera una API key en *Settings → API Keys* y lanza:

```http
GET https://api.getpostman.com/collections/{{collectionUid}}
X-API-Key: {{postmanApiKey}}
```

Newman puede ejecutar directamente desde esa URL, sin fichero intermedio:

```bash
newman run "https://api.getpostman.com/collections/$COLLECTION_UID?apikey=$POSTMAN_API_KEY"
```

## 4. Avance: Postman CLI y Newman

Son dos herramientas distintas para ejecutar colecciones desde la línea de comandos:

| | **Newman** | **Postman CLI** |
|---|---|---|
| Origen | Open source (npm), desde 2015 | Oficial de Postman, binario propio |
| Instalación | `npm install -g newman` | Instalador o script de postman.com |
| Ejecuta | Ficheros `.json` o URLs | Colecciones por id desde tu cuenta, o ficheros |
| Login | No hace falta | `postman login --with-api-key …` |
| Reporters | CLI, JSON, JUnit, HTML (plugins como *htmlextra*) | CLI, JSON, JUnit, HTML. Los resultados se ven también en Postman |
| Extra | Muy extendido. Se usa como librería Node | Gobernanza de APIs (*lint*), integración con el workspace |

```bash
newman run pizzeria-api.postman_collection.json -e pizzeria-dev.postman_environment.json
postman collection run 12345678-abcd-... -e 12345678-efgh-...
```

Los veremos en profundidad en los días 5 y 6.

---

## 5. Testing de APIs: `pm.test` y `pm.expect`

Los tests son JavaScript en *Scripts → Post-response*, y se ejecutan **después** de recibir la respuesta.

```javascript
pm.test("Nombre descriptivo del test", function () {
    // Una o varias aserciones. Si alguna falla (lanza un error), el test falla.
    pm.expect(1 + 1).to.equal(2);
});
```

- Cada `pm.test` aparece en la pestaña **Test Results** como PASS o FAIL.
- Si un test falla, **los demás se ejecutan igual**: cada `pm.test` es independiente.
- `pm.expect` usa la librería **Chai** con su estilo BDD (`expect`). Las palabras `to`, `be`, `been`, `is`, `that`, `which`, `and`, `has`, `have`, `with`, `at`, `of` y `same` no hacen nada: solo sirven para que la frase se lea bien.

### 5.1 Aserciones de Chai más usadas

| Aserción | Ejemplo |
|---|---|
| Igualdad estricta | `pm.expect(json.name).to.equal("Margarita")` |
| Igualdad profunda (objetos/arrays) | `pm.expect(json.errors).to.eql([])` |
| Tipo | `pm.expect(json.price).to.be.a("number")` |
| Propiedad | `pm.expect(json).to.have.property("price")` |
| Varias claves | `pm.expect(json).to.include.all.keys("id", "name", "price")` |
| Comparación | `pm.expect(json.price).to.be.above(0).and.below(20)` |
| Aproximación | `pm.expect(json.price).to.be.closeTo(5.16, 0.001)` |
| Contiene | `pm.expect(json.message).to.include("no existe")` |
| Pertenencia | `pm.expect(pm.response.code).to.be.oneOf([200, 201])` |
| Longitud | `pm.expect(json).to.have.lengthOf(7)` |
| Booleanos y null | `.to.be.true` · `.to.be.false` · `.to.be.null` · `.to.exist` |
| Negación | `pm.expect(json.description).to.not.be.empty` |

### 5.2 Aserciones sobre la respuesta: `pm.response.to`

```javascript
pm.response.to.have.status(200);
pm.response.to.have.status("OK");
pm.response.to.have.header("Content-Type");
pm.response.to.be.json;                  // body JSON válido
pm.response.to.be.ok;                    // cualquier 2xx
pm.response.to.be.clientError;           // cualquier 4xx
pm.response.to.have.jsonBody("price");   // existe la ruta en el JSON
```

### 5.3 Datos de la respuesta disponibles en los scripts

| Expresión | Valor |
|---|---|
| `pm.response.code` | `200` |
| `pm.response.status` | `"OK"` |
| `pm.response.responseTime` | Tiempo en ms |
| `pm.response.headers.get("Content-Type")` | `"application/json"` |
| `pm.response.json()` | Body parseado |
| `pm.response.text()` | Body como texto |
| `pm.request.method`, `pm.request.url.toString()` | Datos de la request |

---

## 6. Tests básicos

### 6.1 Status code, tiempo de respuesta y Content-Type

En **Obtener pizza** (`GET {{baseUrl}}/api/pizzas/1`):

```javascript
pm.test("Status 200", () => {
    pm.response.to.have.status(200);
});

pm.test("Responde en menos de 500 ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

pm.test("Devuelve JSON", () => {
    pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json");
});
```

> Los tests de tiempo son **orientativos** en local. En CI define un umbral realista (por ejemplo, el percentil 95 de las ejecuciones normales) y guárdalo en una variable (`{{maxResponseTime}}`) para ajustarlo por entorno.

### 6.2 Existencia y tipo de los campos

```javascript
const pizza = pm.response.json();

pm.test("Tiene los campos del contrato", () => {
    pm.expect(pizza).to.include.all.keys(
        "id", "name", "description", "vegetarian",
        "ingredients", "ingredientsCost", "profitMargin", "price");
});

pm.test("Los tipos son correctos", () => {
    pm.expect(pizza.id).to.be.a("number");
    pm.expect(pizza.name).to.be.a("string").and.not.be.empty;
    pm.expect(pizza.vegetarian).to.be.a("boolean");
    pm.expect(pizza.ingredients).to.be.an("array").that.is.not.empty;
    pm.expect(pizza.price).to.be.a("number");
});
```

### 6.3 Valores concretos

```javascript
pm.test("Es la Margarita", () => {
    pm.expect(pizza.name).to.equal("Margarita");
    pm.expect(pizza.vegetarian).to.be.true;
});

pm.test("Margen de beneficio del 20 %", () => {
    pm.expect(pizza.profitMargin).to.equal(0.2);
});
```

### 6.4 La regla de negocio: precio = ingredientes + 20 %

Este es el test que de verdad aporta valor, porque comprueba la **lógica** y no solo la forma de la respuesta:

```javascript
pm.test("ingredientsCost es la suma de los ingredientes", () => {
    const suma = pizza.ingredients.reduce((total, i) => total + i.cost, 0);
    pm.expect(pizza.ingredientsCost).to.be.closeTo(suma, 0.001);
});

pm.test("price = ingredientsCost × 1,20 (redondeado a céntimos)", () => {
    const esperado = Math.round(pizza.ingredientsCost * 1.2 * 100) / 100;
    pm.expect(pizza.price).to.be.closeTo(esperado, 0.001);
});
```

> ¿Por qué `closeTo` y no `equal`? En JavaScript, `0.1 + 0.2 === 0.30000000000000004`. Con importes decimales, compara siempre con una tolerancia.

---

## 7. Práctica

1. Importa en Postman la colección generada a partir de un HAR de Swagger UI. ¿Qué cabeceras "sobran" respecto a una request hecha a mano?
2. Exporta tu colección en v2.1, ábrela en un editor y localiza: la auth de colección, los scripts (`event`) y la variable `apiPath`.
3. **Opcional:** genera una API key de Postman y descarga tu colección con `GET https://api.getpostman.com/collections` (cabecera `X-API-Key`).
4. Añade a **Listar pizzas** los tests de status, tiempo y Content-Type.
5. Añade a **Obtener pizza** los tests de campos, tipos, valores y precio (apartado 6). Ejecútalos para los ids 1 a 7 cambiando `pizzaId`.
6. **Rompe los tests a propósito:** cambia `0.2` por `0.25` en el test del margen y mira cómo se presenta el fallo.
7. En **Listar ingredientes vegetarianos** (`?vegetarian=true`), comprueba que todos los elementos tienen `vegetarian === true`. *Pista:* `json.forEach(...)` o `json.every(...)`.
8. En **Crear ingrediente**, comprueba que devuelve 201, que la cabecera `Location` termina en el `id` del body, y que `cost` y `name` coinciden con lo enviado. *Pista:* `JSON.parse(pm.request.body.raw)`. Si el body usa variables, pásalo antes por `pm.variables.replaceIn(...)`.
9. **Reto:** modifica el coste de un ingrediente con `PUT` y comprueba, en la request siguiente, que el precio de una pizza que lo contiene ha cambiado en `Δcoste × 1,2`.

---

## Recursos

**Importación, exportación y Git**
- [Importar datos (incluye HAR y cURL)](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/)
- [Exportar datos](https://learning.postman.com/docs/getting-started/importing-and-exporting/exporting-data/)
- [Chrome DevTools — Referencia del panel Network (guardar como HAR)](https://developer.chrome.com/docs/devtools/network/reference)
- [Capturar tráfico HTTP con Postman](https://learning.postman.com/docs/use/capturing-request-data/capturing-http-requests/)
- [Formato de colección Postman (esquemas oficiales)](https://schema.postman.com/)
- [Integración nativa con Git](https://learning.postman.com/docs/use/native-git/overview/)
- [Introducción a la Postman API](https://learning.postman.com/docs/reference/postman-api/intro-api)
- [Postman CLI](https://learning.postman.com/docs/postman-cli/postman-cli-overview/)
- [Newman en GitHub](https://github.com/postmanlabs/newman)

**Testing**
- [Tests y scripts en Postman](https://learning.postman.com/docs/tests-and-scripts/tests-and-scripts/)
- [Escribir tests (post-response scripts)](https://learning.postman.com/docs/tests-and-scripts/write-scripts/test-scripts/)
- [Ejemplos de tests](https://learning.postman.com/docs/tests-and-scripts/write-scripts/test-examples/)
- [Referencia del objeto `pm`](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/overview/)
- [Chai — API BDD (`expect`)](https://www.chaijs.com/api/bdd/)
