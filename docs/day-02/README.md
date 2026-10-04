# Día 2 — Colecciones, variables y entornos

**Martes 6 de octubre · 15:30–19:00**

| Bloque | Duración | Contenido |
|---|---|---|
| M2 (2ª parte) | 1h10 | Colecciones y carpetas, variables de colección frente a globales, pre-request scripts básicos, guardar y reutilizar |
| Descanso | 10 min | |
| M3 (1ª parte) | 2h10 | Qué es un entorno, crear dev/staging/prod, scope y jerarquía de variables, `{{baseUrl}}`, `{{token}}`, `{{userId}}`, cambio de entorno |

**Objetivo del día:** pasar de requests sueltas a una **colección organizada y parametrizada** que funcione contra dos servidores distintos sin tocar ninguna URL.

---

## 1. Colecciones

Una **colección** agrupa requests relacionadas. Es la unidad que se comparte, se versiona, se documenta y se ejecuta con el *Runner* y Newman. Puede contener **carpetas**, y tanto la colección como cada carpeta tienen su propia configuración:

| Nivel | Puede definir |
|---|---|
| Colección | Authorization, Scripts (pre-request y post-response), Variables, Documentación |
| Carpeta | Authorization, Scripts, Documentación |
| Request | Todo lo anterior y además la petición en sí |

**Herencia:** si una request tiene *Auth Type = Inherit auth from parent*, usa la autorización de su carpeta. Si la carpeta tampoco la define, usa la de la colección. Así se configura el token **una sola vez**.

### Estructura propuesta para el curso

```
🍕 Pizzería API
├── 🔐 Auth
│   ├── Login
│   ├── Refresh
│   └── Me
├── 🧀 Ingredientes
│   ├── Listar ingredientes
│   ├── Listar vegetarianos
│   ├── Obtener ingrediente
│   ├── Crear ingrediente
│   ├── Modificar ingrediente
│   └── Borrar ingrediente
└── 🍕 Pizzas
    ├── Listar pizzas
    ├── Buscar por nombre
    ├── Obtener pizza
    ├── Crear pizza
    ├── Modificar pizza
    └── Borrar pizza
```

**Buenas prácticas desde el primer día:**
- Nombres que describan la intención ("Crear ingrediente"), no la URL.
- Una request = un caso. Si quieres probar el 404, crea "Obtener pizza inexistente (404)" en lugar de editar la existente.
- Guarda siempre con `Ctrl+S`. Una request sin guardar solo vive en la pestaña. Si te sirve como plantilla, usa *Duplicate*.
- Rellena la pestaña de documentación de cada request. En el día 6 la publicaremos.

## 2. Variables: la base de todo lo demás

Una variable se escribe `{{nombre}}` en la URL, en las cabeceras, en el body o en la autorización. Postman la sustituye justo antes de enviar.

### 2.1 Ámbitos (*scopes*)

De más amplio a más concreto:

```
Global  →  Collection  →  Environment  →  Data (Runner/Newman)  →  Local
 (más amplio)                                                   (más concreto)
```

**Si dos ámbitos definen la misma variable, gana el más concreto.** Si `baseUrl` existe como global y en el entorno activo, se usa la del entorno.

| Ámbito | Vive en | Úsala para | API en scripts |
|---|---|---|---|
| Global | El workspace | Casi nada. Es fácil que se convierta en un cajón desastre | `pm.globals` |
| Collection | La colección (viaja con ella al exportar) | Constantes de la API que no cambian entre entornos (`apiPath = /api`) | `pm.collectionVariables` |
| Environment | El entorno seleccionado | Lo que cambia entre dev, staging y prod: URL, credenciales, tokens | `pm.environment` |
| Data | El fichero CSV/JSON de una ejecución | Datos de prueba por iteración (día 6) | `pm.iterationData` |
| Local | Solo la ejecución actual | Valores temporales dentro de un script | `pm.variables.set` |

`pm.variables.get("x")` lee la variable **resolviendo la jerarquía**, igual que `{{x}}`. Es la forma recomendada de leer.

### 2.2 Valor inicial y valor actual

Cada variable de entorno o de colección tiene dos valores:

- **Initial value (valor compartido):** se sincroniza con la nube de Postman y se exporta. Lo ven tus compañeros.
- **Current value (valor local):** solo está en tu máquina. Es el que se usa al enviar.

> Nunca pongas una contraseña real en el *initial value*. En el día 3 veremos las variables *secret* y el *Vault*. Las versiones recientes de Postman llaman a estos campos *Shared value* y *Local value*, pero el concepto es el mismo.

### 2.3 Variables dinámicas

Postman incluye generadores que empiezan por `$`:

| Variable | Ejemplo |
|---|---|
| `{{$guid}}` | `3f2504e0-4f89-11d3-9a0c-0305e82c3301` |
| `{{$timestamp}}` | `1791180000` |
| `{{$randomInt}}` | `0`–`1000` |
| `{{$randomFirstName}}`, `{{$randomPrice}}`… | Datos falsos verosímiles |

Por ejemplo, crea ingredientes sin chocar con el 409 por nombre duplicado:

```json
{ "name": "Ingrediente {{$timestamp}}", "cost": 0.75, "vegetarian": true }
```

## 3. Pre-request scripts básicos

Un **pre-request script** es JavaScript que se ejecuta **antes** de enviar la request, dentro del *sandbox* de Postman y con el objeto `pm`. En la pestaña **Scripts** aparecen dos editores: *Pre-request* y *Post-response*. En versiones antiguas de Postman, el segundo se llamaba *Tests*.

Orden de ejecución cuando hay scripts en varios niveles:

```
Pre-request colección → Pre-request carpeta → Pre-request request
                      → ENVÍO →
Post-response colección → Post-response carpeta → Post-response request
```

Ejemplos útiles:

```javascript
// Generar un nombre único y guardarlo para usarlo en el body como {{ingredientName}}
const nombre = "Ingrediente " + Date.now();
pm.collectionVariables.set("ingredientName", nombre);
console.log("Creando:", nombre);
```

```javascript
// Añadir una cabecera de trazabilidad a todas las peticiones (script de colección)
pm.request.headers.upsert({ key: "X-Request-Id", value: pm.variables.replaceIn("{{$guid}}") });
```

```javascript
// Avisar si no hay entorno seleccionado
if (!pm.environment.name) {
    console.warn("⚠️ No hay ningún entorno seleccionado");
}
```

`console.log` escribe en la **Postman Console**. Úsala sin miedo.

## 4. Entornos (*Environments*)

### 4.1 Qué es y por qué es crítico

Un **entorno** es un conjunto de variables con nombre: `Pizzería - DEV`, `Pizzería - STAGING`, `Pizzería - PROD`. Al cambiar el entorno en el selector, **las mismas requests** apuntan a otro servidor y usan otras credenciales.

Sin entornos acaban pasando cosas como estas:
- URLs copiadas y pegadas en 40 requests, y una se queda apuntando a producción.
- Contraseñas de producción dentro de la colección que se comparte con el equipo.
- "En mi máquina funciona" porque cada persona tiene su URL escrita a mano.

Es la misma idea que el factor III de la metodología *Twelve-Factor App*: **la configuración se separa del código**. Aquí, del código de las requests.

### 4.2 Dos servidores reales para practicar

La Pizzería API tiene un perfil `staging` que arranca en otro puerto y con otras credenciales. Puedes tener los dos a la vez:

```bash
# Terminal 1 — DEV (puerto 8080)
mvnw.cmd spring-boot:run

# Terminal 2 — STAGING (puerto 8081, contraseñas distintas, tokens de 60 s)
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=staging
```

Si ya has empaquetado el proyecto (`mvnw.cmd package`), también puedes arrancar el jar:

```bash
java -jar target/pizzeria-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=staging
```

### 4.3 Crear los entornos

| Variable | DEV | STAGING | PROD |
|---|---|---|---|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8081` | `https://api.pizzeria.example` (ficticio) |
| `username` | `admin` | `admin` | — |
| `password` *(secret)* | `admin123` | `staging-secret` | — |
| `token` | *(se rellena con el login)* | *(se rellena con el login)* | |
| `ingredientId` | | | |
| `pizzaId` | | | |

Tienes los ficheros listos para importar en [environments/](environments/). Los usaremos para practicar la importación (`Import → Files`).

En la colección, sustituye `http://localhost:8080` por `{{baseUrl}}` en todas las requests:

```
{{baseUrl}}/api/pizzas
{{baseUrl}}/api/pizzas/{{pizzaId}}
{{baseUrl}}/api/ingredients/{{ingredientId}}
```

Y en *Collection → Authorization*, elige **Bearer Token** con el valor `{{token}}`. Todas las requests en *Inherit auth from parent* lo usarán.

> El temario usa `{{userId}}` como ejemplo genérico de "id que se captura de una respuesta y se reutiliza". En nuestra API los equivalentes son `{{ingredientId}}` y `{{pizzaId}}`.

### 4.4 Cambio de entorno en tiempo de ejecución

- Con el **selector** (arriba a la derecha) para el trabajo manual.
- Con el **Collection Runner**, que pide el entorno antes de ejecutar.
- Con **Newman**: `newman run coleccion.json -e staging.postman_environment.json`.
- Pasa el ratón por encima de `{{baseUrl}}` para ver qué valor se usará y de qué ámbito viene. Una variable sin resolver aparece en **rojo**.

**Protege PROD:** usa colores o un emoji en el nombre (`🔴 Pizzería - PROD`) y, si lo necesitas, un pre-request de colección que bloquee las operaciones destructivas:

```javascript
if (pm.environment.name?.includes("PROD") && pm.request.method !== "GET") {
    throw new Error("Operación de escritura bloqueada en PROD");
}
```

---

## 5. Práctica

1. Crea la colección **Pizzería API** con las carpetas *Auth*, *Ingredientes* y *Pizzas*, y guarda en ella las requests del día 1.
2. Crea la variable de colección `apiPath = /api` y úsala: `{{baseUrl}}{{apiPath}}/pizzas`.
3. Importa los entornos de [environments/](environments/), o créalos a mano siguiendo la tabla del apartado 4.3.
4. Sustituye todas las URLs por `{{baseUrl}}` y comprueba que `GET Listar pizzas` funciona en DEV.
5. Arranca STAGING en el puerto 8081 y cambia de entorno. Lanza la misma request: ¿funciona sin tocar nada?
6. Haz login en STAGING con `admin/admin123`. ¿Qué código devuelve? Corrige el problema usando `{{username}}` y `{{password}}` en el body del login:
   ```json
   { "username": "{{username}}", "password": "{{password}}" }
   ```
7. Configura la autorización Bearer `{{token}}` en la colección y pon *Inherit auth from parent* en todas las requests. Pega el token a mano en el *current value* del entorno y crea un ingrediente.
8. Añade el pre-request que genera `ingredientName` y úsalo en el body de "Crear ingrediente". Lánzalo tres veces seguidas: no debe aparecer ningún 409.
9. Define `baseUrl` también como variable global con otro valor. ¿Cuál gana? Bórrala después.
10. **Reto:** crea el entorno PROD con el script de protección y comprueba que un `DELETE` se bloquea antes de enviarse.

> **Problema que dejamos para mañana:** copiar el token a mano cada 5 minutos (cada 60 s en staging) es inaceptable. En el día 3 lo automatizamos con `pm.environment.set`.

---

## Recursos

- [Colecciones en Postman](https://learning.postman.com/docs/use/use-collections/overview/)
- [Fundamentos de las requests](https://learning.postman.com/docs/use/send-requests/create-requests/request-basics/)
- [Parámetros de la request](https://learning.postman.com/docs/use/send-requests/create-requests/parameters/)
- [Cabeceras de la request](https://learning.postman.com/docs/use/send-requests/create-requests/headers/)
- [Introducción a las variables](https://learning.postman.com/docs/use/send-requests/variables/variables-intro/)
- [Guardar y usar variables (ámbitos y prioridad)](https://learning.postman.com/docs/use/send-requests/variables/variables/)
- [Gestionar entornos](https://learning.postman.com/docs/use/send-requests/variables/managing-environments/)
- [Introducción a los scripts](https://learning.postman.com/docs/tests-and-scripts/write-scripts/intro-to-scripts/)
- [Pre-request scripts](https://learning.postman.com/docs/tests-and-scripts/write-scripts/pre-request-scripts/)
- [Referencia del objeto `pm`](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/overview/)
- [The Twelve-Factor App — III. Configuración](https://12factor.net/es/config)
- [Spring Boot — Perfiles y configuración externa](https://docs.spring.io/spring-boot/reference/features/profiles.html)
