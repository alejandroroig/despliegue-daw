# 🧪 Actividad 2.3: El conjunto entero con un comando

## Contexto

Hasta ahora poner en marcha Escaparate exige recordar varias órdenes: crear una red, arrancar PostgreSQL, pasar configuración, esperar a que esté preparado y después iniciar la aplicación.

Tu objetivo es convertir ese procedimiento en **infraestructura declarada en un fichero versionado**.

El despliegue de esta sesión tendrá solo dos servicios:

```text
navegador → app → bd
```

- `app`: Escaparate, con frontend + API y Tomcat embebido;
- `bd`: PostgreSQL.

Al finalizar, solo `app` publicará un puerto hacia el anfitrión.

!!! abstract "Cómo vas a trabajar"
    **Declarar → diagnosticar → persistir → aislar → configurar → esperar → documentar**

    No se trata únicamente de conseguir que `docker compose up -d` funcione. Debes entender **qué declara cada parte del YAML y qué problema resuelve**.

---

## Qué vas a practicar

- Describir varios servicios mediante Docker Compose.
- Utilizar nombres de servicio dentro de la red.
- Distinguir red interna y puertos publicados.
- Comprobar el ciclo de vida de un volumen.
- Externalizar valores mediante `.env`.
- Esperar a una condición real con `healthcheck`.
- Diagnosticar mediante `ps`, `logs` y `config`.
- Documentar un despliegue que otra persona pueda reproducir.

---

## Requisitos previos

Debes tener publicadas:

```text
ghcr.io/<usuario>/escaparate-db:1.0.0
ghcr.io/<usuario>/escaparate:sesion-04
```

Prepara tu rama:

```bash
git switch main
git pull --ff-only
git switch -c sesion-05
```

Crea:

```text
practicas/
└── compose/
```

y:

```text
entregas/
└── tema2/
    └── actividad-2.3/
        ├── actividad-2.3.md
        └── img/
```

!!! info "Evidencias"
    Solo se piden **cuatro evidencias**. Los ficheros técnicos permanecen en `practicas/compose/`; en `entregas/` documentarás decisiones, resultados y reflexiones.

---

## Paso 1: Declara y diagnostica el conjunto

Crea:

```text
practicas/compose/compose.yaml
```

con este punto de partida:

```yaml
services:
  bd:
    image: ghcr.io/<usuario>/escaparate-db:1.0.0
    environment:
      POSTGRES_USER: escaparate
      POSTGRES_PASSWORD: escaparate
      POSTGRES_DB: escaparate
    ports:
      - "5433:5432"
    volumes:
      - datos-bd:/var/lib/postgresql

  app:
    image: ghcr.io/<usuario>/escaparate:sesion-04
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: escaparate
      DB_USER: escaparate
      DB_PASSWORD: escaparate
    depends_on:
      - bd

volumes:
  datos-bd:
```

!!! info "PostgreSQL 18"
    Para esta versión utilizamos el volumen en:

    ```text
    /var/lib/postgresql
    ```

    No copies automáticamente ejemplos antiguos que monten `/var/lib/postgresql/data`.

El fichero contiene dos decisiones que corregirás durante la práctica:

- un **error deliberado** en la conexión de `app`;
- PostgreSQL publica temporalmente un puerto hacia el anfitrión.

Levanta el conjunto:

```bash
cd practicas/compose
docker compose up -d
```

Antes de cambiar nada:

```bash
docker compose ps
docker compose logs app
docker compose logs bd
```

Busca en los logs de `app` el host al que intenta conectarse y compáralo con los nombres de servicio del YAML.

Corrige el problema y vuelve a aplicar:

```bash
docker compose up -d
```

Comprueba:

```text
http://localhost:8080/
http://localhost:8080/api/salud/listo
```

**Evidencia 1:** fragmento de logs donde se vea la pista del fallo inicial.

**Evidencia 2:** `docker compose ps` y Escaparate funcionando después de la corrección.

!!! question "Reflexiona"
    Una vez corregido, `app` puede utilizar `bd` como host. ¿Quién proporciona ese nombre y por qué no necesitas conocer la IP del contenedor?

---

## Paso 2: Comprueba qué persiste

Con el conjunto funcionando, añade desde Escaparate un producto cuyo nombre incluya tu apellido.

Antes de cada operación, anota qué esperas que ocurra con ese producto.

### 2.1. Detener y volver a arrancar

```bash
docker compose stop
docker compose start
```

Comprueba el catálogo.

### 2.2. Eliminar y recrear contenedores

```bash
docker compose down
docker compose up -d
```

Espera a que Escaparate vuelva a estar disponible y comprueba el producto.

### 2.3. Eliminar también el volumen

```bash
docker compose down -v
docker compose up -d
```

Comprueba de nuevo el catálogo.

Completa:

| Operación | ¿Contenedores conservados? | ¿Volumen conservado? | ¿Producto conservado? |
|---|---|---|---|
| `stop` + `start` | | | |
| `down` + `up` | | | |
| `down -v` + `up` | | | |

!!! question "Reflexiona"
    Si `down` elimina los contenedores pero el producto reaparece, ¿dónde estaban realmente los datos?

    ¿Qué cambia con `down -v`?

    Si también perdieras el volumen de una base de datos real, ¿qué mecanismo necesitarías para recuperar la información?

---

## Paso 3: Publica solo la aplicación

En el YAML inicial PostgreSQL tenía:

```yaml
ports:
  - "5433:5432"
```

Pero `app` ya puede comunicarse con `bd` dentro de la red de Compose.

Elimina la publicación de PostgreSQL y aplica:

```bash
docker compose up -d
```

Comprueba:

```bash
docker compose ps
curl -fsS http://localhost:8080/api/salud/listo
```

El estado final debe corresponder a:

```text
DESDE EL ANFITRIÓN

localhost:8080 → app
PostgreSQL      no publicado


DENTRO DE COMPOSE

app → bd:5432
```

!!! question "Reflexiona"
    ¿Qué diferencia hay entre que un servicio sea accesible **dentro de la red de Compose** y que tenga un puerto **publicado hacia el anfitrión**?

---

## Paso 4: Saca los valores locales del YAML

El `compose.yaml` se versionará. Los valores locales no deben quedar escritos directamente en él.

Crea:

```text
practicas/
└── compose/
    ├── compose.yaml
    ├── .env
    └── .env.example
```

Saca del YAML, como mínimo:

```text
BD_USUARIO
BD_CLAVE
BD_NOMBRE
```

Utiliza interpolación:

```yaml
POSTGRES_USER: ${BD_USUARIO}
POSTGRES_PASSWORD: ${BD_CLAVE}
POSTGRES_DB: ${BD_NOMBRE}
```

y reutiliza esos valores para configurar la conexión de `app`.

### 4.1. `.env`

Contiene tus valores locales y **no se versiona**.

### 4.2. `.env.example`

Contiene las mismas claves con valores ficticios y **sí se versiona**.

Por ejemplo:

```text
BD_USUARIO=usuario
BD_CLAVE=cambia-esta-clave
BD_NOMBRE=escaparate
```

Comprueba:

```bash
git check-ignore -v .env
docker compose config
```

!!! warning "Revisa antes de capturar"
    `docker compose config` puede mostrar los valores ya interpolados. No incluyas una contraseña real en una captura.

Levanta de nuevo el conjunto y verifica que sigue funcionando.

**Evidencia 3:** fragmento relevante de `compose.yaml`, `.env.example` y `git check-ignore` demostrando que `.env` queda fuera de Git.

---

## Paso 5: Demuestra que arrancado no significa preparado

Hasta ahora `depends_on` establece una dependencia de arranque, pero no espera necesariamente a que PostgreSQL haya terminado su inicialización.

Vas a hacer visible esa diferencia.

### 5.1. Introduce temporalmente un retraso

Empieza con un volumen vacío:

```bash
docker compose down -v
```

Crea temporalmente:

```text
practicas/compose/00-delay.sql
```

con:

```sql
SELECT pg_sleep(20);
```

Monta **solo ese fichero** en `bd` como:

```text
/docker-entrypoint-initdb.d/00-delay.sql
```

y en modo:

```text
:ro
```

!!! warning "No montes el directorio completo"
    La imagen de base de datos ya contiene `01-schema.sql` y `02-data.sql`. Si montaras un directorio completo sobre `/docker-entrypoint-initdb.d/`, ocultarías esos ficheros.

Levanta:

```bash
docker compose up -d
```

Observa inmediatamente:

```bash
docker compose ps
docker compose logs app
docker compose logs bd
```

Comprueba qué ocurre mientras PostgreSQL continúa inicializándose.

### 5.2. Espera a PostgreSQL de verdad

Añade a `bd` un `healthcheck` basado en:

```text
pg_isready
```

La comprobación debe realizar una conexión TCP contra:

```text
127.0.0.1
```

y utilizar las variables `POSTGRES_USER` y `POSTGRES_DB` **dentro del contenedor**.

Después cambia la dependencia de `app` para esperar:

```text
service_healthy
```

!!! info "Recuerda el doble `$`"
    En el healthcheck necesitarás expresiones como:

    ```text
    $${POSTGRES_USER}
    ```

    para que Compose no las sustituya antes de crear el contenedor.

Repite desde cero:

```bash
docker compose down -v
docker compose up -d
```

Observa:

```bash
docker compose ps
```

y espera a Escaparate mediante una condición real:

```bash
for i in {1..30}; do
    if curl -fsS http://localhost:8080/api/salud/listo; then
        break
    fi
    sleep 2
done
```

Comprueba:

- `bd` alcanza `healthy`;
- `app` comienza después de cumplirse esa condición;
- `/api/salud/listo` termina respondiendo.

**Evidencia 4:** `docker compose ps` mostrando `bd` saludable y la comprobación satisfactoria de readiness.

!!! question "Reflexiona"
    ¿Qué comprueba `pg_isready`?

    ¿Por qué «contenedor arrancado» y «servicio preparado» no significan lo mismo?

    ¿Qué diferencia conceptual hay entre **liveness** y **readiness**?

### 5.3. Retira el retraso artificial

`00-delay.sql` solo servía para hacer visible el problema.

Antes de continuar:

1. elimina su montaje de `compose.yaml`;
2. borra `00-delay.sql`;
3. conserva el `healthcheck` y `service_healthy`;
4. comprueba un arranque limpio final.

```bash
docker compose down -v
docker compose up -d
```

El despliegue definitivo **no debe introducir una espera artificial de 20 segundos**.

---

## Paso 6: Completa la puesta en marcha

En el README principal dejaste pendiente:

```text
## Puesta en marcha
```

Complétalo para que una persona que acaba de clonar el repositorio pueda levantar el sistema sin preguntarte nada.

Debe explicar:

- requisito de Docker;
- ubicación de `compose.yaml`;
- cómo crear `.env` a partir de `.env.example`;
- qué valores deben completarse;
- cómo arrancar;
- URL del catálogo;
- endpoint de readiness;
- cómo detener conservando los datos;
- cómo desmontar;
- qué ocurre si se utiliza `down -v`.

!!! tip "La prueba del compañero"
    Un procedimiento es reproducible cuando otra persona puede ejecutarlo sin preguntarte qué comando falta.

---

## Paso 7: Valida Compose automáticamente

Amplía `.github/workflows/validar.yml` para conservar las comprobaciones anteriores y añadir una tercera:

```text
Validar Docker Compose
```

El job debe:

1. hacer checkout;
2. copiar `.env.example` a `.env`;
3. ejecutar `docker compose config`;
4. fallar si la configuración no puede interpretarse.

Puedes utilizar:

```yaml
  validar-compose:
    name: Validar Docker Compose
    needs: estructura
    runs-on: ubuntu-latest

    steps:
      - name: Descargar repositorio
        uses: actions/checkout@v7

      - name: Preparar valores de ejemplo
        working-directory: practicas/compose
        run: cp .env.example .env

      - name: Validar la configuración
        working-directory: practicas/compose
        run: docker compose config > /dev/null
```

!!! info "Qué demuestra este job"
    Parte de un checkout limpio. Si puede crear `.env` desde el fichero de ejemplo e interpretar el Compose, la configuración básica necesaria para reproducir el despliegue está versionada.

    Este job **no levanta los servicios**.

---

## Paso 8: Documenta e integra

Revisa `actividad-2.3.md`. Debe contener:

- diagnóstico del fallo inicial;
- tabla de persistencia;
- reflexión sobre puertos;
- reflexión sobre healthcheck, liveness y readiness;
- cuatro evidencias;
- incidencias relevantes.

Registra:

- `compose.yaml`;
- `.env.example`;
- README actualizado;
- workflow actualizado;
- documentación y capturas.

!!! warning "No debe quedar"
    Antes del commit final comprueba que:

    - `.env` no está versionado;
    - `00-delay.sql` ya no existe;
    - PostgreSQL no publica ningún puerto;
    - el healthcheck y `service_healthy` permanecen en el Compose definitivo.

Publica `sesion-05` y abre:

```text
sesion-05 → main
```

La PR debe mostrar:

```text
Comprobar repositorio    ✓
Construir imagen         ✓
Validar Docker Compose   ✓
```

Cuando todo esté correcto, fusiona mediante **Create a merge commit** y actualiza:

```bash
git switch main
git pull --ff-only
```

---

## Qué se entrega

Antes de terminar, comprueba:

- [ ] `practicas/compose/compose.yaml` final con `app` y `bd`;
- [ ] PostgreSQL sin puerto publicado;
- [ ] volumen de base de datos declarado;
- [ ] `.env.example` versionado y `.env` ignorado;
- [ ] `healthcheck` de PostgreSQL y dependencia `service_healthy`;
- [ ] **sin** `00-delay.sql` ni espera artificial en el despliegue final;
- [ ] sección `Puesta en marcha` completa;
- [ ] workflow con `Validar Docker Compose`;
- [ ] `actividad-2.3.md` con tabla, reflexiones y cuatro evidencias;
- [ ] Pull Request `sesion-05 → main` fusionada.

!!! info "Dónde queda la entrega"
    La infraestructura y la documentación quedan versionadas en el repositorio. `.env` permanece únicamente en cada equipo.

??? info "Cómo se comprobará"
    La práctica podrá reproducirse desde un clon limpio.

    Tras copiar `.env.example` a `.env` y completar los valores, se podrá ejecutar:

    ```bash
    docker compose up -d

    for i in {1..30}; do
        if curl -fsS http://localhost:8080/api/salud/listo; then
            break
        fi
        sleep 2
    done

    docker compose ps
    curl -fsS http://localhost:8080/
    curl -fsS http://localhost:8080/api/salud/vivo
    curl -fsS http://localhost:8080/api/salud/listo
    ```

    Debe observarse:

    - `bd` saludable;
    - `app` preparado;
    - catálogo accesible por `8080`;
    - PostgreSQL sin puerto publicado;
    - `.env` fuera de Git;
    - `compose.yaml` válido desde `.env.example`.

---

## ✅ Cierre

Escaparate ya puede levantarse como un **conjunto declarado y versionado**: servicios, red, persistencia, configuración y dependencia de arranque están descritos en el repositorio.

Durante la práctica has separado tres ideas importantes:

```text
contenedor arrancado
≠ servicio preparado

red interna
≠ puerto publicado

contenedor
≠ dato persistente
```

En el siguiente tema añadirás Nginx delante de `app`. El sistema seguirá ejecutando la misma aplicación, pero aparecerá una nueva capa de publicación como punto de entrada.
