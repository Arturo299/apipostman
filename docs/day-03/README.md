# Día 3 — Entornos dinámicos e importación de colecciones y contratos

**Miércoles 7 de octubre · 15:30–19:00**

| Bloque | Duración | Contenido |
|---|---|---|
| M3 (2ª parte) | 50 min | Inyección dinámica con `pm.environment.set`, variables secretas, exportar e importar entornos, entornos en equipo |
| M4 (1ª parte) | 1h20 | Importar colecciones desde fichero y desde URL |
| Descanso | 10 min | |
| M4 (cont.) | 1h10 | Importar desde OpenAPI/Swagger, generación automática de requests desde el contrato |

**Objetivo del día:** que el token y los ids se gestionen solos, y **no escribir a mano ninguna request que ya esté descrita en un contrato**.

---

## 1. Inyección dinámica de variables desde scripts

El patrón de casi todas las colecciones profesionales es este: **una request produce un dato y las siguientes lo consumen**.

### 1.1 Guardar el token tras el login

En la request **Login**, pestaña *Scripts → Post-response*:

```javascript
const body = pm.response.json();

pm.environment.set("token", body.accessToken);
pm.environment.set("refreshToken", body.refreshToken);
// Guardamos cuándo caduca para refrescarlo automáticamente el día 6
pm.environment.set("tokenExpiresAt", Date.now() + body.expiresIn * 1000);

console.log(`Token guardado, caduca en ${body.expiresIn} s`);
```

Desde ese momento, todas las requests que heredan *Bearer `{{token}}`* funcionan sin copiar nada.

### 1.2 Capturar el id de lo que acabas de crear

En **Crear ingrediente** (*Post-response*):

```javascript
if (pm.response.code === 201) {
    const ingrediente = pm.response.json();
    pm.environment.set("ingredientId", ingrediente.id);
}
```

También se puede sacar de la cabecera `Location`, lo que resulta útil cuando la API responde a un `POST` con un body vacío:

```javascript
const location = pm.response.headers.get("Location");     // http://localhost:8080/api/ingredients/16
pm.environment.set("ingredientId", location.split("/").pop());
```

### 1.3 Métodos que conviene conocer

| Método | Qué hace |
|---|---|
| `pm.environment.set(k, v)` / `.get(k)` / `.unset(k)` | Escribe, lee o borra en el entorno activo |
| `pm.collectionVariables.set(k, v)` | Lo mismo en la colección |
| `pm.globals.set(k, v)` | Lo mismo en las globales |
| `pm.variables.get(k)` | Lee resolviendo la jerarquía |
| `pm.variables.set(k, v)` | Variable local, solo durante la ejecución actual |
| `pm.environment.has(k)` | ¿Existe? |
| `pm.variables.replaceIn("Hola {{username}}")` | Sustituye las variables dentro de un texto |

> `pm.environment.set` falla si no hay ningún entorno seleccionado. Si una colección tiene que funcionar también sin entorno, usa `pm.collectionVariables`.

> Los valores se guardan como **cadenas**. Un `pm.environment.set("ingredientId", 16)` se lee después como `"16"`. Tenlo en cuenta al comparar en los tests.

## 2. Variables secretas (*sensitive values*)

| Mecanismo | Qué aporta |
|---|---|
| **Tipo `secret`** en una variable de entorno o de colección | Enmascara el valor en pantalla (`••••`). Evita que se vea al compartir pantalla, **pero no lo cifra** y se puede sincronizar |
| **Current value** (local) | No se sincroniza ni se comparte. Es el lugar de las credenciales personales |
| **Postman Vault** | Almacén cifrado **local**. Se referencia como `{{vault:password}}` y nunca sale de tu máquina. Se puede enlazar con gestores externos (Azure Key Vault, AWS Secrets Manager, HashiCorp Vault) en los planes de empresa |
| **Secret Scanner** | Postman avisa si detecta tokens reales en elementos públicos |

**Regla práctica:** el *initial value* de un secreto debe estar vacío o ser un marcador (`<pon-aquí-tu-password>`). Cada persona rellena su *current value* o usa el Vault.

## 3. Exportar e importar entornos

- **Exportar:** `Environments → ⋯ → Export` genera `Pizzería - DEV.postman_environment.json`.
- **Importar:** `Import → Files` (o arrastrar el fichero). Acepta varios a la vez.
- **Antes de compartir un entorno exportado, ábrelo y revísalo.** Si contiene contraseñas o tokens, límpialos.
- Versiona en Git los entornos **sin secretos**. En CI, los secretos se inyectan desde el gestor de secretos del pipeline (día 6):
  ```bash
  newman run coleccion.json -e dev.postman_environment.json --env-var "password=$PIZZERIA_PASSWORD"
  ```

Formato del fichero, tal como lo verás en [../day-02/environments/](../day-02/environments/):

```json
{
  "name": "Pizzería - DEV",
  "values": [
    { "key": "baseUrl", "value": "http://localhost:8080", "type": "default", "enabled": true },
    { "key": "password", "value": "admin123", "type": "secret", "enabled": true }
  ],
  "_postman_variable_scope": "environment"
}
```

## 4. Entornos en equipo

- En un **Team workspace** los entornos son visibles para todos sus miembros, cada uno con sus *current values*.
- **Roles:** *Viewer* puede usar el entorno pero no modificar sus valores iniciales. *Editor* puede cambiarlos.
- Una convención útil: `Proyecto - ENTORNO` (`Pizzería - STAGING`) y un color o emoji para PROD.
- Un solo entorno "oficial" por servidor. Si alguien necesita hacer pruebas, que lo haga con sus *current values*, no duplicando el entorno.

---

## 5. Importaciones

`Import` (botón superior izquierdo) acepta ficheros, carpetas, URLs y texto pegado:

| Origen | Formatos | Resultado |
|---|---|---|
| Colección Postman | `.json` (v2.0 / v2.1) | Colección idéntica, con scripts y tests |
| Entorno / globales | `.json` | Entorno |
| OpenAPI 3.x / Swagger 2.0 | `.yaml`, `.json`, URL | Colección generada (y opcionalmente una spec en *APIs*) |
| cURL | Texto | Una request |
| HAR | `.har` | Una request por cada entrada (día 4) |
| Otros | RAML, GraphQL, WSDL, Insomnia… | Colección |

### 5.1 Importar una colección desde fichero

En [pizzeria-api.postman_collection.json](pizzeria-api.postman_collection.json) tienes la colección de referencia del curso, con las carpetas *Auth*, *Ingredientes* y *Pizzas*, Bearer heredado y los scripts de los apartados 1.1 y 1.2. Impórtala y compárala con la tuya.

### 5.2 Importar desde URL

Pega una URL en el cuadro de `Import`. Postman descarga el contenido y detecta el formato:

- Una colección publicada en un repositorio (por ejemplo, la URL *raw* de GitHub de un `.postman_collection.json`).
- Un enlace de colección pública de la [Postman API Network](https://www.postman.com/explore).
- **El contrato de nuestra API:** `http://localhost:8080/v3/api-docs`

### 5.3 Importar desde OpenAPI / Swagger

**OpenAPI** (antes Swagger) es el estándar para describir una API REST en YAML o JSON: rutas, parámetros, cuerpos, respuestas, esquemas y seguridad. La Pizzería API lo genera automáticamente con *springdoc-openapi* a partir de las anotaciones de los controladores:

- JSON: http://localhost:8080/v3/api-docs
- YAML: http://localhost:8080/v3/api-docs.yaml
- Interfaz visual: http://localhost:8080/swagger-ui.html

Fragmento del contrato:

```yaml
openapi: 3.1.0
info:
  title: Pizzería API
  version: 1.0.0
paths:
  /api/pizzas/{id}:
    get:
      tags: [Pizzas]
      summary: Obtiene una pizza con el desglose de su precio
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: integer, format: int64 }
components:
  securitySchemes:
    bearerAuth: { type: http, scheme: bearer }
```

**Opciones de importación** (*View import settings*):

| Opción | Recomendación |
|---|---|
| *Import as* | **Postman Collection** para trabajar ya. *OpenAPI spec + collection* si vais a mantener el contrato dentro de Postman |
| *Folder organization* | **Tags**: agrupa por `Pizzas`, `Ingredientes` y `Autenticación` |
| *Parameter generation* | **Example** si el contrato trae ejemplos. **Schema** genera marcadores como `<long>` |
| *Enable optional parameters* | Desactivado: así no aparecen todos los filtros marcados |

### 5.4 Generación automática de requests desde el contrato

Lo que **sí** obtienes:
- Una request por operación, con método, ruta, *path params* (`:id`), *query params* y *body* de ejemplo.
- La variable de colección `{{baseUrl}}` creada a partir de `servers`.
- La autorización configurada si el contrato declara `securitySchemes`.
- La documentación de cada request tomada de `summary` y `description`.

Lo que **no** obtienes, y es tu trabajo:
- Scripts (login automático, captura de ids) y **tests**.
- Datos coherentes: un body de ejemplo con `"cost": 0` provocará un 400.
- El orden lógico de un flujo (login → crear → consultar → borrar).

**Code-first frente a contract-first:** en nuestra API, el contrato se genera a partir del código (*code-first*). En *contract-first*, el equipo diseña antes el YAML y el código se implementa contra él. En ambos casos, cuando el contrato cambia, la colección generada se regenera o se sincroniza. Las requests "a mano" con tests se mantienen en una colección aparte.

> **Pregunta para el cliente:** ¿su framework propio publica un contrato OpenAPI? Si no lo hace, Postman puede *generar* uno a partir de una colección, y una opción realista es documentar el contrato a mano en YAML.

---

## 6. Práctica

1. Añade el script de login (1.1) y comprueba en el entorno que `token` y `refreshToken` se rellenan solos.
2. Añade la captura de `ingredientId` (1.2) en "Crear ingrediente" y usa `{{ingredientId}}` en "Obtener", "Modificar" y "Borrar ingrediente". Ejecuta el ciclo completo: crear → obtener → modificar → borrar → obtener (debe dar 404).
3. Repite la captura usando la cabecera `Location` en lugar del body.
4. Haz lo mismo con las pizzas: crea una pizza con `{{ingredientId}}` entre sus ingredientes y guarda `pizzaId`. Después modifica el coste del ingrediente y vuelve a pedir la pizza: **¿ha cambiado su precio?**
5. Marca `password` como *secret* y vacía su *initial value*. Después muévela al Vault y usa `{{vault:pizzeria-password}}` en el body del login.
6. Exporta tu entorno DEV, abre el `.json` y comprueba qué valores contiene.
7. Importa [pizzeria-api.postman_collection.json](pizzeria-api.postman_collection.json) y compárala con tu colección.
8. Importa desde la URL `http://localhost:8080/v3/api-docs` con la organización por *Tags*. Revisa lo que se ha generado: variables, auth y bodies de ejemplo.
9. Descarga `http://localhost:8080/v3/api-docs.yaml`, guárdalo como `pizzeria-openapi.yaml` e impórtalo como fichero con *Parameter generation = Schema*. ¿Qué diferencias ves frente a *Example*?
10. **Reto:** de la colección generada, ¿qué requests fallan al ejecutarlas tal cual? Arréglalas y explica el motivo de cada fallo.

---

## Recursos

**Scripts y variables**
- [Guardar y usar variables (ámbitos y prioridad)](https://learning.postman.com/docs/use/send-requests/variables/variables/)
- [Gestionar entornos](https://learning.postman.com/docs/use/send-requests/variables/managing-environments/)
- [Referencia del objeto `pm`](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/overview/)
- [Secretos en Postman Vault](https://learning.postman.com/docs/use/postman-vault/postman-vault-secrets/)

**Importar y exportar**
- [Visión general de importar y exportar](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-and-exporting-overview/)
- [Importar datos en Postman](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/)
- [Exportar datos desde Postman](https://learning.postman.com/docs/getting-started/importing-and-exporting/exporting-data/)
- [Importar una especificación de API](https://learning.postman.com/docs/design-apis/specifications/import-a-specification/)
- [Postman API Network (colecciones públicas)](https://www.postman.com/explore)

**OpenAPI**
- [Especificación OpenAPI (última versión)](https://spec.openapis.org/oas/latest.html)
- [Swagger — Introducción a OpenAPI 3](https://swagger.io/docs/specification/v3_0/about/)
- [springdoc-openapi (generación del contrato en Spring Boot)](https://springdoc.org/)
