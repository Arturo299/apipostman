# Día 6 — Automatización con Newman, CI/CD y buenas prácticas

**Miércoles 14 de octubre · 15:30–19:00**

| Bloque | Duración | Contenido |
|---|---|---|
| M6 (2ª parte) | 1h50 | Data-driven testing con CSV y JSON, Newman (instalación, opciones, reporters), integración con GitHub Actions y Azure DevOps, Monitors |
| Descanso | 10 min | |
| M7 | 1h30 | Estructura de colecciones profesionales, scripts globales de colección, refresh automático de tokens, documentación publicada, introducción a Postman Flows |

**Objetivo del día:** que la suite de tests se ejecute **sola**, en cada cambio de código, y que la colección esté lista para entregársela a otro equipo.

**Material de esta carpeta:**

| Fichero | Contenido |
|---|---|
| [pizzeria-api-tests.postman_collection.json](pizzeria-api-tests.postman_collection.json) | Suite completa: auth, ingredientes, pizzas, flujo de negocio, errores y data-driven (unas 110 aserciones) |
| [data/ingredientes.csv](data/ingredientes.csv) | Data file CSV: ingredientes válidos e inválidos con el código esperado |
| [data/pizzas.json](data/pizzas.json) | Data file JSON: combinaciones de ingredientes con el precio esperado |
| [ci/github-actions-api-tests.yml](ci/github-actions-api-tests.yml) | Pipeline de GitHub Actions |
| [ci/azure-pipelines.yml](ci/azure-pipelines.yml) | Pipeline de Azure DevOps |

---

## 1. Data-driven testing

La misma request se ejecuta **una vez por cada fila** de un fichero de datos. Cada columna (CSV) o propiedad (JSON) se convierte en una variable del ámbito *data*.

### 1.1 CSV

[data/ingredientes.csv](data/ingredientes.csv):

```csv
name,cost,vegetarian,expectedStatus
Rúcula,0.50,true,201
Anchoas,1.90,false,201
Jalapeños,0.70,true,201
Gratis,0,true,400
Carísimo,150,true,400
,1.00,true,400
Masa,1.00,true,409
```

Body de la request *Crear ingrediente desde datos*:

```json
{
  "name": "{{name}}",
  "cost": {{cost}},
  "vegetarian": {{vegetarian}}
}
```

Fíjate en que `{{cost}}` y `{{vegetarian}}` van **sin comillas**, para que lleguen como número y como booleano.

Test:

```javascript
const esperado = Number(pm.iterationData.get("expectedStatus"));
pm.test(`"${pm.iterationData.get("name")}" → ${esperado}`, () => {
    pm.response.to.have.status(esperado);
});
```

**Incluir la respuesta esperada como columna** (`expectedStatus`) permite mezclar casos válidos e inválidos en el mismo fichero. Es la forma más barata de multiplicar la cobertura.

### 1.2 JSON

[data/pizzas.json](data/pizzas.json) es un array de objetos. A diferencia del CSV, admite **tipos y estructuras**: números, booleanos y arrays.

```json
[
  { "name": "Data Margarita", "ingredientIds": [1, 2, 3, 4],    "vegetarian": true,  "expectedPrice": 5.16 },
  { "name": "Data Diavola",   "ingredientIds": [1, 2, 3, 7, 9], "vegetarian": false, "expectedPrice": 7.44 }
]
```

Si un valor es un array o un objeto, hay que serializarlo en el pre-request antes de meterlo en el body:

```javascript
pm.variables.set("ingredientIdsJson", JSON.stringify(pm.iterationData.get("ingredientIds")));
```

```json
{ "name": "{{name}} {{$timestamp}}", "ingredientIds": {{ingredientIdsJson}} }
```

### 1.3 Variables de iteración

| Expresión | Valor |
|---|---|
| `{{name}}` en la request | El valor de la columna en la iteración actual |
| `pm.iterationData.get("name")` | Lo mismo, desde un script |
| `pm.iterationData.has("expectedStatus")` | ¿Hay data file con esa columna? |
| `pm.info.iteration` | Número de iteración (empieza en 0) |
| `pm.info.iterationCount` | Total de iteraciones |

> **Ejecuta solo la carpeta que usa los datos.** Con un data file, el Runner repite *toda* la selección una vez por cada fila. En la colección de referencia, las carpetas *Data-driven* se saltan solas (`pm.execution.skipRequest()`) si no hay data file, y cada fila creada se **borra** en la request siguiente para que la ejecución sea repetible.

En el Runner: selecciona la carpeta → *Select File* → *Preview* para comprobar cómo se han interpretado las columnas.

## 2. Newman

### 2.1 Instalación

```bash
node -v                                        # Node.js 18 o superior
npm install -g newman newman-reporter-htmlextra
newman -v
```

### 2.2 Ejecución básica

```bash
cd docs
newman run day-06/pizzeria-api-tests.postman_collection.json \
       -e day-02/environments/pizzeria-dev.postman_environment.json
```

En PowerShell, cambia `\` por `` ` `` para partir la línea, o escríbelo todo en una sola.

### 2.3 Opciones más útiles

| Opción | Uso |
|---|---|
| `-e fichero.json` | Entorno |
| `-g globals.json` | Globales |
| `-d datos.csv` | Data file |
| `-n 5` | Número de iteraciones |
| `--folder "05 Errores"` | Ejecutar solo una carpeta (se puede repetir) |
| `--env-var "password=$SECRETO"` | Sobrescribir una variable de entorno (ideal para secretos en CI) |
| `--delay-request 100` | Pausa entre requests en ms |
| `--timeout-request 5000` | Tiempo máximo por request |
| `--bail` | Parar al primer fallo |
| `--insecure` | Aceptar certificados autofirmados (solo en entornos internos) |
| `--export-environment salida.json` | Guardar el entorno resultante (con los tokens y los ids capturados) |
| `-r cli,htmlextra,junit` | Reporters |

**Código de salida:** Newman termina con `0` si todo pasa y con `1` si falla alguna aserción. Eso es lo que hace que un pipeline se ponga en **rojo**.

Data-driven desde la línea de comandos:

```bash
newman run day-06/pizzeria-api-tests.postman_collection.json \
       -e day-02/environments/pizzeria-dev.postman_environment.json \
       --folder "06 Data-driven ingredientes" -d day-06/data/ingredientes.csv

newman run day-06/pizzeria-api-tests.postman_collection.json \
       -e day-02/environments/pizzeria-dev.postman_environment.json \
       --folder "07 Data-driven pizzas" -d day-06/data/pizzas.json
```

### 2.4 Reporters

| Reporter | Para qué |
|---|---|
| `cli` | Salida en consola (por defecto) |
| `json` | Resultado completo en JSON para procesarlo después |
| `junit` | XML JUnit: lo entienden Jenkins, GitLab, Azure DevOps, GitHub (con acciones) y SonarQube |
| `htmlextra` | Informe HTML navegable con requests, respuestas y tests (paquete aparte) |

```bash
newman run day-06/pizzeria-api-tests.postman_collection.json \
       -e day-02/environments/pizzeria-dev.postman_environment.json \
       -r cli,htmlextra,junit \
       --reporter-htmlextra-export reports/informe.html \
       --reporter-junit-export reports/junit.xml
```

## 3. Integración continua

El patrón es siempre el mismo, sea cual sea la herramienta:

```
1. Checkout del código
2. Compilar y arrancar la API (o desplegarla en un entorno de pruebas)
3. Esperar a que responda el health check
4. Instalar Newman
5. Ejecutar la colección con el entorno de CI y los secretos inyectados con --env-var
6. Publicar los informes (JUnit para el pipeline, HTML como artefacto)
```

- **GitHub Actions:** [ci/github-actions-api-tests.yml](ci/github-actions-api-tests.yml). Cópialo a `.github/workflows/` y crea el secreto `PIZZERIA_PASSWORD` en *Settings → Secrets and variables → Actions*.
- **Azure DevOps:** [ci/azure-pipelines.yml](ci/azure-pipelines.yml). Define `PIZZERIA_PASSWORD` como variable secreta. `PublishTestResults@2` muestra cada test en la pestaña *Tests* del pipeline.

> En los ficheros de entorno del curso, la contraseña de DEV está incluida para simplificar. En un proyecto real, el fichero versionado lleva el valor vacío y el pipeline lo inyecta con `--env-var`.

**Para el framework propio del cliente:** solo cambia el paso 2, es decir, cómo se arranca su servicio. Los pasos 3 a 6 son idénticos.

## 4. Monitors de Postman

Un **Monitor** ejecuta una colección en la nube de Postman de forma programada (cada 5 minutos, cada hora, cada día…) y avisa por correo, Slack o Teams si algo falla.

- Se crea desde `Colección → ⋯ → Monitor collection`, eligiendo entorno, frecuencia y región.
- Se ejecuta **desde los servidores de Postman**, así que **no llega a `localhost` ni a redes internas** si no se configura una IP estática o un acceso permitido. Para APIs internas, la alternativa es Newman o Postman CLI en un *cron* o en un pipeline programado.
- Úsalo para *smoke tests* ligeros y de solo lectura (`GET` de salud, login, un listado), no para la suite completa: los planes tienen un límite de llamadas mensuales.
- Monitor + suite en CI = **detectar** que algo se ha roto en producción + **impedir** que se rompa en cada cambio.

---

## 5. Buenas prácticas y casos reales

### 5.1 Estructura de colecciones profesionales

```
🍕 Pizzería API - Tests
├── 01 Auth                       ← lo que otras carpetas necesitan va primero
├── 02 Ingredientes               ← una carpeta por recurso: lecturas y casos sencillos
├── 03 Pizzas                     ← tests comunes del recurso a nivel de carpeta
├── 04 Flujo de negocio           ← escenarios de extremo a extremo que se limpian solos
├── 05 Errores                    ← un caso por código de error del contrato
├── 06 Data-driven ingredientes   ← se ejecutan con su data file
└── 07 Data-driven pizzas
```

| Aspecto | Convención |
|---|---|
| Carpetas | Prefijo numérico (`01`, `02`…) para fijar el orden de ejecución |
| Requests | Verbo + objeto, en lenguaje de negocio: "Crear pizza con trufa", "404 pizza inexistente" |
| Variables | `camelCase`. Prefijo por flujo (`flowPizzaId`, `dataIngredientId`) para que no se pisen entre carpetas |
| Tests | Redactados como requisitos. Prefijo `[global]` o `[pizzas]` si vienen de un nivel superior |
| Independencia | Cada carpeta debe poder ejecutarse sola (`--folder`): el login automático lo hace posible |
| Datos | Crear lo necesario y borrarlo al terminar. No depender de ids "mágicos" salvo los datos semilla documentados |
| Una colección por API | Las colecciones generadas desde OpenAPI (exploración) van separadas de la suite de tests (regresión) |

### 5.2 Scripts globales a nivel de colección

La colección de referencia concentra en el nivel de colección todo lo transversal:

- **Pre-request:** autenticación automática (apartado 5.3).
- **Post-response:** tiempo máximo (`{{maxResponseTime}}`), `Content-Type` JSON y formato estándar de los errores 4xx.
- **Variables de colección:** los JSON Schema de ingrediente y pizza (`ingredientSchema`, `pizzaSchema`), reutilizados en varias requests con `JSON.parse(pm.collectionVariables.get("pizzaSchema"))`.

Así, añadir una request nueva a la colección le da **gratis** autenticación y una docena de comprobaciones.

### 5.3 Manejo de tokens caducados con refresh automático

El perfil `staging` emite access tokens de **60 segundos**, para que podamos ver el problema en directo. La estrategia completa, en el pre-request de la colección:

```
¿Hay token y le quedan más de 5 s?  ── sí ──▶ no hacer nada
          │ no
          ▼
¿Hay refresh token?  ── sí ──▶ POST /api/auth/refresh ── 200 ──▶ guardar los tokens nuevos
          │ no                                          └─ error ─▶ login
          ▼
POST /api/auth/login con username/password ──▶ guardar los tokens
```

```javascript
const MARGEN_MS = 5000;
const token = pm.environment.get("token");
const expiresAt = Number(pm.environment.get("tokenExpiresAt") || 0);
if (token && Date.now() < expiresAt - MARGEN_MS) {
    return;
}

const baseUrl = pm.variables.get("baseUrl");
const guardar = (res) => {
    const body = res.json();
    pm.environment.set("token", body.accessToken);
    pm.environment.set("refreshToken", body.refreshToken);
    pm.environment.set("tokenExpiresAt", Date.now() + body.expiresIn * 1000);
};
const peticion = (path, payload) => ({
    url: baseUrl + path,
    method: "POST",
    header: { "Content-Type": "application/json" },
    body: { mode: "raw", raw: JSON.stringify(payload) }
});
const login = () => pm.sendRequest(
    peticion("/api/auth/login", { username: pm.variables.get("username"), password: pm.variables.get("password") }),
    (err, res) => { if (!err && res.code === 200) guardar(res); });

const refreshToken = pm.environment.get("refreshToken");
if (!refreshToken) {
    login();
} else {
    pm.sendRequest(peticion("/api/auth/refresh", { refreshToken }), (err, res) => {
        if (!err && res.code === 200) guardar(res); else login();
    });
}
```

Claves del diseño:
- **Renovar antes de que caduque** (margen de 5 s) es mejor que esperar al 401: evita reintentos.
- El refresh token de la Pizzería API es **de un solo uso**: tras usarlo, hay que guardar el nuevo. Es la práctica recomendada (*refresh token rotation*).
- Si el refresh falla, por ejemplo porque el servidor se ha reiniciado y ha perdido los tokens en memoria, se hace **login** como último recurso.
- Las requests de `/api/auth/login` y `/api/auth/refresh` se excluyen del script para evitar bucles.

### 5.4 Documentación de APIs desde Postman

- Cada colección, carpeta y request tiene una pestaña de **documentación en Markdown**. Describe ahí el propósito, los parámetros y los casos especiales.
- **Guarda ejemplos de respuesta** (*Save Response → Save as example*) para el caso feliz y para los errores principales. Aparecen en la documentación y sirven de base para un *mock server*.
- **Publish:** `Colección → View documentation → Publish` genera una web pública con la documentación, los ejemplos y los fragmentos de código en varios lenguajes. Para uso interno basta con la vista de documentación del workspace del equipo.
- Si la API ya publica un contrato OpenAPI (Swagger UI), la documentación de Postman lo **complementa** con flujos y ejemplos reales. No lo sustituye.

### 5.5 Postman Flows (introducción)

**Flows** es un editor visual para encadenar requests y lógica arrastrando bloques (*Send Request*, *If*, *Evaluate*, *Repeat*, *Display*…), sin escribir scripts. Úsalo para:

- Prototipar un flujo de negocio y enseñárselo a perfiles no técnicos.
- Orquestar varias APIs: login → consultar → transformar → enviar a otra API.
- Desplegar el flujo como un pequeño servicio en la nube de Postman.

**Ejercicio bonus:** crea un Flow que haga login, liste las pizzas, filtre las de menos de 7 € y muestre sus nombres en un bloque *Display*.

---

## 6. Práctica

1. Ejecuta la carpeta *06 Data-driven ingredientes* en el Runner con `data/ingredientes.csv`. Añade dos filas nuevas: un nombre de 61 caracteres (¿qué código esperas?) y un ingrediente válido.
2. Ejecuta *07 Data-driven pizzas* con `data/pizzas.json`. Añade una pizza y calcula a mano su `expectedPrice`.
3. Instala Newman y ejecuta la suite completa contra DEV y contra STAGING. ¿Qué cambia en la línea de comandos?
4. Genera el informe `htmlextra` y el `junit.xml`. Abre el HTML y localiza una request con su respuesta completa.
5. Rompe la API a propósito (cambia `pizzeria.pricing.profit-margin` a `0.25` en `application.properties` y reinicia). Ejecuta Newman: ¿cuántos tests fallan y cuál es el código de salida (`echo $?` o `$LASTEXITCODE`)?
6. **Refresh en directo:** arranca STAGING y ejecuta la suite con `--delay-request 3000`. En la consola deben aparecer mensajes `♻️ Token renovado con refresh token`.
7. Sube el proyecto a un repositorio de GitHub, añade el workflow y el secreto, y comprueba que el pipeline queda en verde. Descarga el artefacto con los informes.
8. Documenta la carpeta *04 Flujo de negocio* y guarda ejemplos de respuesta para "Crear pizza con trufa" y "422 ingrediente inexistente".
9. **Cierre:** con la checklist siguiente, revisa una colección real de vuestro proyecto.

### Checklist de una colección lista para producción

- [ ] Ninguna URL escrita a mano: todo pasa por `{{baseUrl}}`.
- [ ] Ningún secreto en los *initial values* ni en los ficheros versionados.
- [ ] Autenticación heredada desde la colección, con login o refresh automático.
- [ ] Tests globales a nivel de colección (tiempo, formato y errores).
- [ ] Cada request de escritura tiene al menos un caso de error asociado.
- [ ] Los flujos crean sus datos y los borran al terminar.
- [ ] Se ejecuta en verde con `newman run` desde una máquina limpia.
- [ ] Integrada en el pipeline con informe JUnit.
- [ ] Documentada, con ejemplos de respuesta.

---

## Recursos

**Data-driven y Runner**
- [Ejecutar colecciones con data files](https://learning.postman.com/docs/tests-and-scripts/running-collections/test-data/working-with-data-files/)
- [Introducción al Collection Runner](https://learning.postman.com/docs/tests-and-scripts/running-collections/intro-to-collection-runs/)

**Newman y CLI**
- [Newman — repositorio oficial y documentación](https://github.com/postmanlabs/newman)
- [Newman en el Learning Center](https://learning.postman.com/docs/reference/newman-cli/command-line-integration-with-newman)
- [Opciones de Newman](https://learning.postman.com/docs/reference/newman-cli/newman-options)
- [newman-reporter-htmlextra](https://github.com/DannyDainton/newman-reporter-htmlextra)
- [Postman CLI](https://learning.postman.com/docs/postman-cli/postman-cli-overview/)

**CI/CD**
- [Documentación de GitHub Actions](https://docs.github.com/es/actions)
- [Documentación de Azure Pipelines](https://learn.microsoft.com/es-es/azure/devops/pipelines/?view=azure-devops)
- [Node.js — descargas](https://nodejs.org/es/download)

**Monitors, documentación y Flows**
- [Introducción a los Monitors](https://learning.postman.com/docs/monitoring-your-api/intro-monitors/)
- [Documentar APIs en Postman](https://learning.postman.com/docs/publishing-your-api/api-documentation-overview/)
- [Postman Flows](https://learning.postman.com/flows/overview)

**Seguridad de tokens**
- [RFC 6749 — OAuth 2.0 (refresh tokens, sección 1.5)](https://www.rfc-editor.org/rfc/rfc6749.html)
- [OAuth 2.0 — oauth.net](https://oauth.net/2/)
