# Tests independientes en Postman mediante fixtures

## 1. El principio: los tests deben ser independientes

Un test no debe depender de que otro se haya ejecutado antes ni del estado que este haya dejado. Cada test debe poder ejecutarse:

- de forma aislada,
- en cualquier orden,
- tantas veces como se quiera, con el mismo resultado.

## 2. El problema en Postman: el orden del Collection Runner

El Collection Runner (y también el Postman CLI y Newman) ejecuta las peticiones **de forma secuencial**, en el orden en que aparecen en la colección: carpetas y peticiones de arriba abajo, tal como se ven en el sidebar.

Ese orden solo cambia si:

- se reordenan o desmarcan peticiones en la pantalla del Runner antes de lanzarlo,
- se ejecuta una carpeta concreta en lugar de toda la colección,
- se altera el flujo desde un script con `pm.execution.setNextRequest()` (antes `postman.setNextRequest()`).

### Consecuencia

Muchas colecciones se construyen como una cadena:

```
POST   /productos        → guarda {{id}}
GET    /productos/{{id}}
PUT    /productos/{{id}}
DELETE /productos/{{id}}
```

Si se ejecuta el `DELETE` sin que el `POST` previo haya creado el recurso:

- Lo habitual es recibir un **404**.
- Algunas APIs tratan el DELETE como idempotente y devuelven **204** aunque el recurso no exista.
- Si el id viene de una variable (`{{id}}`) que fija el POST, puede pasar algo peor:
  - la variable no existe y se envía el literal `{{id}}`, o
  - la variable conserva un valor de una ejecución anterior y se borra **otro recurso**, o se recibe un 404 por un motivo distinto.

Una colección encadenada de esta forma es, por diseño, un conjunto de **tests dependientes del orden**.

## 3. La solución: fixtures en el pre-request

La idea es aplicar el patrón **Arrange – Act – Assert** dentro de cada petición:

| Fase    | Dónde en Postman       | Qué hace                                   |
|---------|------------------------|--------------------------------------------|
| Arrange | Pre-request script     | La fixture crea el estado que necesita el test |
| Act     | La propia petición     | Se ejecuta el endpoint que se quiere probar |
| Assert  | Post-response script   | `pm.test` / `pm.expect` sobre la respuesta  |
| Teardown| Post-response script   | Se elimina lo que creó la fixture           |

Así, cada petición prepara su propio estado y deja de depender del orden de la colección.

### 3.1. Cómo reutilizar las fixtures: `pm.require`

Postman permite importar código en los scripts mediante `pm.require`:

- **Package Library del equipo**: scripts compartidos y mantenidos en un lugar central.

  ```js
  const fixtures = pm.require('@tu-equipo/fixtures');
  ```

- **Paquetes públicos de npm y JSR**:

  ```js
  const lodash = pm.require('npm:lodash@4.17.21');
  const pkg    = pm.require('jsr:@scope/paquete@1.0.0');
  ```

- **Paquetes npm privados**: disponibles en los planes Solo, Team y Enterprise, si un Admin ha configurado el acceso.

### 3.2. Importante: el sandbox de Postman no es Node

Los scripts se ejecutan en un sandbox seguro, por lo que:

- No hay `fs`, `net`, `child_process` ni nada que dependa de esos módulos. Un paquete que los use no funcionará.
- No hay `fetch` documentado. Las peticiones HTTP se hacen con **`pm.sendRequest`**, que admite `await`.
- Las fixtures no acceden a la base de datos: **crean los datos llamando a la propia API**.

### 3.3. Dónde se puede ejecutar

Según la documentación, las colecciones que importan paquetes externos funcionan en:

- Collection Runner
- Monitors
- Postman Flows
- Postman CLI (ejecuciones de colecciones y monitors)

Newman no aparece en esa lista; para CI conviene usar el **Postman CLI**.

## 4. Ejemplos

### 4.1. Fixture mínima (paquete en el Package Library)

La fixture recibe las variables como parámetros y hace la petición:

```js
// Paquete: @tu-equipo/fixtures
async function crearProducto(baseUrl, token, datos) {
  const res = await pm.sendRequest({
    url: `${baseUrl}/productos`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: { mode: 'raw', raw: JSON.stringify(datos) }
  });
  return res.json();
}

module.exports = { crearProducto };
```

### 4.2. Uso en el pre-request del DELETE

```js
// Pre-request de DELETE /productos/{{id}}
const { crearProducto } = pm.require('@tu-equipo/fixtures');

const producto = await crearProducto(
  pm.environment.get('baseUrl'),
  pm.environment.get('token'),
  { nombre: 'fixture', precio: 10 }
);

pm.variables.set('id', producto.id);
```

`pm.variables.set` deja el id en el ámbito local de esa petición, así que no se cruza con otros tests.

```js
// Post-response de DELETE /productos/{{id}}
pm.test('El producto se elimina', () => {
  pm.response.to.have.status(204);
});
```

### 4.3. Fixture robusta: datos únicos y control de errores

```js
// Paquete: @tu-equipo/fixtures
async function crearProducto(baseUrl, token, datos = {}) {
  const payload = {
    nombre: `fixture-${Date.now()}-${Math.floor(Math.random() * 1e6)}`,
    precio: 10,
    ...datos
  };

  const res = await pm.sendRequest({
    url: `${baseUrl}/productos`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: { mode: 'raw', raw: JSON.stringify(payload) }
  });

  // Deja claro en el informe que lo que ha fallado es la precondición
  pm.test('fixture: crear producto', () => {
    pm.expect(res.code, 'La fixture no pudo crear el producto').to.equal(201);
  });

  if (res.code !== 201) {
    return null;
  }
  return res.json();
}

async function eliminarProducto(baseUrl, token, id) {
  return pm.sendRequest({
    url: `${baseUrl}/productos/${id}`,
    method: 'DELETE',
    header: { Authorization: `Bearer ${token}` }
  });
}

module.exports = { crearProducto, eliminarProducto };
```

Pre-request con salida controlada si la fixture falla:

```js
const { crearProducto } = pm.require('@tu-equipo/fixtures');

const producto = await crearProducto(
  pm.environment.get('baseUrl'),
  pm.environment.get('token')
);

if (!producto) {
  // No tiene sentido ejecutar el test si no se ha podido preparar el estado
  pm.execution.skipRequest();
} else {
  pm.variables.set('id', producto.id);
}
```

### 4.4. Teardown en un GET o un PUT

El DELETE se limpia solo, pero un GET o un PUT que usan la fixture dejan datos creados. La limpieza se hace en el post-response:

```js
// Post-response de GET /productos/{{id}}
pm.test('Devuelve el producto', () => {
  pm.response.to.have.status(200);
  pm.expect(pm.response.json().id).to.equal(pm.variables.get('id'));
});

// Teardown
const { eliminarProducto } = pm.require('@tu-equipo/fixtures');
await eliminarProducto(
  pm.environment.get('baseUrl'),
  pm.environment.get('token'),
  pm.variables.get('id')
);
```

## 5. Consideraciones

- **Limpieza (teardown).** Sin ella, los tests dejan basura que puede afectar a otros tests y a ejecuciones posteriores.
- **Fallo de la precondición frente a fallo del test.** Si el POST falla, el DELETE fallará también y el informe culpará al DELETE. La fixture debe comprobar el código de respuesta y dejarlo claro (`pm.test` con nombre propio o `pm.execution.skipRequest()`).
- **Datos únicos.** Si la API tiene restricciones de unicidad (nombre, email…), la fixture debe generar valores aleatorios (`Date.now()`, `{{$randomUUID}}`…) para no chocar entre ejecuciones ni con restos de ejecuciones anteriores.
- **Dependencia del ecosistema Postman.** El Package Library obliga a trabajar con cuenta y equipo en Postman, y en CI a usar el Postman CLI en lugar de Newman.
- **Tiempo.** Cada test hace una o dos peticiones extra. Normalmente no importa, pero en colecciones grandes se nota.

## 6. Referencias

- [Import packages from external registries in Postman](https://learning.postman.com/docs/tests-and-scripts/write-scripts/packages/external-package-registries/) — importación de paquetes npm y JSR con `pm.require`, paquetes privados y herramientas compatibles.
- [Add internal scripts to the Package Library in Postman](https://learning.postman.com/docs/tests-and-scripts/write-scripts/packages/package-library/) — Package Library del equipo e importación con `pm.require('@team-domain/package-name')`.
- [Import packages into your scripts (`pm.require`)](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-require/) — referencia de `pm.require` y objetos globales disponibles en el sandbox.
- [Use scripts to send requests in Postman (`pm.sendRequest`)](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-send-request/) — envío de peticiones desde scripts, con callback o con `await`.
- [Use scripts in collection runs (`pm.execution`)](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-execution/) — `setNextRequest`, `skipRequest` y `runRequest`.
