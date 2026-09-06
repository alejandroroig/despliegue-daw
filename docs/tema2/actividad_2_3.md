# 🧪 Actividad 2.3: El conjunto entero con un comando

## Contexto

Hasta ahora poner en marcha Escaparate exige recordar varias órdenes: crear una red, arrancar PostgreSQL con su configuración, esperar a que esté preparado y después iniciar la aplicación.

Tu encargo es convertir ese procedimiento en **infraestructura declarada en un fichero** y versionarla junto al resto del proyecto.

Escaparate mantiene la arquitectura utilizada en las sesiones anteriores:

```text
Navegador
    │
    ▼
Escaparate (frontend + API)
    │
    ▼
PostgreSQL
```

El despliegue tendrá únicamente **dos servicios**, `app` y `bd`. Al finalizar, solo la aplicación publicará un puerto hacia el equipo anfitrión.

## Qué vas a practicar

- **Describir** un despliegue completo con Docker Compose.
- **Resolver** servicios por su nombre dentro de la red de Compose.
- **Comprobar** qué ocurre con los datos al detener, eliminar o recrear contenedores.
- **Distinguir** comunicación interna y puertos publicados hacia el anfitrión.
- **Separar** configuración versionable de valores locales.
- **Esperar** a que una dependencia esté realmente preparada.
- **Diagnosticar** un conjunto mediante estado y logs.
- **Documentar** un procedimiento reproducible.

## Requisitos previos

Debes tener publicadas las imágenes de las actividades anteriores:

```text
ghcr.io/<usuario>/escaparate-db:1.0.0
ghcr.io/<usuario>/escaparate:sesion-04
```

Las dos imágenes son públicas, por lo que Compose podrá descargarlas sin iniciar sesión en GHCR.

El repositorio `daw-despliegue` continúa siendo privado. Las operaciones Git utilizan la autenticación configurada anteriormente.

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

y la carpeta de entrega:

```text
entregas/
└── tema2/
    └── actividad-2.3/
        ├── actividad-2.3.md
        └── img/
```

Los ficheros técnicos permanecerán en `practicas/compose/`. En `entregas/` solo guardarás documentación y evidencias.

---

## Paso 1: Levanta Escaparate con Compose

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

!!! info "Ruta de persistencia en PostgreSQL 18"
    Esta actividad utiliza PostgreSQL 18. Para las prácticas del módulo el volumen se monta en:

    ```text
    /var/lib/postgresql
    ```

    No reutilices automáticamente ejemplos de versiones anteriores que utilicen `/var/lib/postgresql/data`.

Este fichero contiene dos decisiones que iremos corrigiendo:

- un error deliberado en la conexión de `app` con PostgreSQL;
- un puerto de PostgreSQL publicado temporalmente hacia el anfitrión.

Levanta el conjunto:

```bash
cd practicas/compose
docker compose up -d
```

No empieces modificando el YAML al azar. Diagnostica primero:

```bash
docker compose ps
docker compose logs app
docker compose logs bd
```

La aplicación debe dejar una pista sobre el nombre de host al que está intentando conectarse.

Compara ese nombre con los servicios declarados en `compose.yaml`, corrige el problema y vuelve a levantar el conjunto.

**Comprueba:**

```text
http://localhost:8080/
```

muestra el catálogo y:

```text
http://localhost:8080/api/salud/listo
```

responde correctamente.

**Captura 1:** línea relevante de los logs antes de corregir el error.

**Captura 2:** `docker compose ps` y Escaparate funcionando después de la corrección.

!!! question "Reflexiona"
    Una vez corregido, la aplicación puede utilizar `bd` como nombre de host. ¿Quién proporciona ese nombre y por qué no necesitas conocer la dirección IP del contenedor?

---

## Paso 2: Comprueba qué significa persistir

Con el conjunto funcionando, añade desde Escaparate un producto cuyo nombre incluya tu apellido.

Antes de cada operación escribe en `actividad-2.3.md` si esperas que ese producto continúe existiendo.

### Caso A: detener y volver a arrancar

```bash
docker compose stop
docker compose start
```

Comprueba el producto.

### Caso B: desmontar y volver a crear los contenedores

```bash
docker compose down
docker compose up -d
```

Espera a que Escaparate esté disponible y comprueba de nuevo el producto.

### Caso C: eliminar también el volumen

```bash
docker compose down -v
docker compose up -d
```

Vuelve a comprobar el catálogo.

Completa:

| Operación | ¿Contenedores conservados? | ¿Volumen conservado? | ¿Producto conservado? |
|---|---|---|---|
| `stop` + `start` | | | |
| `down` + `up` | | | |
| `down -v` + `up` | | | |

!!! question "Reflexiona"
    Si los contenedores se eliminan con `down` pero el producto reaparece, ¿dónde estaba almacenado realmente?

    ¿Qué cambia al utilizar `down -v`?

    Si se tratara de una base de datos real, ¿qué mecanismo adicional necesitarías para recuperar los datos después de perder también el volumen?

---

## Paso 3: Publica solo lo necesario

En el punto de partida PostgreSQL publica:

```text
5433:5432
```

pero la única aplicación que necesita hablar con la base de datos es `app`, que ya comparte con ella la red de Compose.

Elimina la publicación del puerto de `bd` y vuelve a aplicar el despliegue:

```bash
docker compose up -d
```

Comprueba:

```bash
docker compose ps
curl -fsS http://localhost:8080/api/salud/listo
```

Al finalizar debe cumplirse:

```text
Desde el anfitrión:
app:8080     accesible
bd:5432      no publicado

Dentro de la red de Compose:
app → bd:5432
funciona
```

No necesitas crear una prueba adicional para demostrar la comunicación interna: si `/api/salud/listo` responde mientras `bd` no publica ningún puerto, la aplicación está llegando a PostgreSQL a través de la red interna.

!!! question "Reflexiona"
    ¿Qué diferencia hay entre que un servicio sea accesible **dentro de una red Docker** y que tenga un puerto **publicado hacia el anfitrión**?

---

## Paso 4: Saca los valores locales del YAML

`compose.yaml` va a entrar en Git. Las credenciales locales no.

Crea:

```text
practicas/
└── compose/
    ├── compose.yaml
    ├── .env
    └── .env.example
```

Utiliza variables de Compose para sacar del YAML, como mínimo:

- usuario de PostgreSQL;
- contraseña;
- nombre de la base de datos.

Puedes usar claves como:

```text
BD_USUARIO
BD_CLAVE
BD_NOMBRE
```

### `.env`

Contiene los valores que utilizas en tu equipo y **no se versiona**.

### `.env.example`

Contiene las mismas claves con valores ficticios o sustituibles y **sí se versiona**.

Comprueba que `.env` está ignorado:

```bash
git check-ignore -v practicas/compose/.env
```

Comprueba también que Compose puede interpretar la configuración:

```bash
docker compose config
```

!!! warning "Revisa la salida antes de capturarla"
    `docker compose config` puede mostrar valores ya interpolados. No publiques ni captures una salida que contenga una contraseña real.

Levanta de nuevo el conjunto y comprueba que continúa funcionando.

**Captura 3:** fragmento relevante de `compose.yaml`, `.env.example` y resultado de `git check-ignore` que demuestre que `.env` no entrará en Git.

---

## Paso 5: Contenedor iniciado no significa servicio preparado

Hasta ahora `depends_on` establece un orden de arranque, pero no garantiza que PostgreSQL esté preparado para aceptar conexiones.

Vas a provocar esa diferencia de forma controlada.

### 5.1. Introduce un retraso reproducible

Desmonta el conjunto eliminando el volumen:

```bash
docker compose down -v
```

Crea:

```text
practicas/compose/00-delay.sql
```

con:

```sql
SELECT pg_sleep(20);
```

Este fichero solo existe para esta práctica. Introduce una pausa durante la primera inicialización de un volumen vacío.

Monta **solo ese fichero** en `bd`:

```text
/docker-entrypoint-initdb.d/00-delay.sql
```

y hazlo en modo solo lectura:

```text
:ro
```

!!! warning "No montes el directorio completo"
    La imagen `escaparate-db:1.0.0` ya contiene `01-schema.sql` y `02-data.sql`.

    Si montaras una carpeta local completa sobre `/docker-entrypoint-initdb.d/`, ocultarías esos ficheros. Monta únicamente `00-delay.sql`.

Levanta el conjunto:

```bash
docker compose up -d
```

Observa inmediatamente:

```bash
docker compose ps
docker compose logs app
docker compose logs bd
```

Comprueba qué ocurre cuando `app` intenta comenzar mientras PostgreSQL todavía está inicializándose.

### 5.2. Espera a una condición real

Añade al servicio `bd` un `healthcheck` basado en `pg_isready`.

La comprobación debe verificar que PostgreSQL acepta **conexiones TCP**, no únicamente que el proceso existe.

Después cambia `depends_on` para que `app` espere a:

```text
service_healthy
```

No necesitas esperar un número fijo de segundos para comprobar Escaparate. Utiliza:

```bash
for i in {1..30}; do
    if curl -fsS http://localhost:8080/api/salud/listo; then
        break
    fi
    sleep 2
done
```

El bucle consulta una condición real cada dos segundos y termina en cuanto la aplicación está preparada.

### 5.3. Repite un arranque limpio

Comprueba la solución desde cero una vez:

```bash
docker compose down -v
docker compose up -d
```

Observa:

```bash
docker compose ps
```

y utiliza el bucle anterior hasta que Escaparate esté preparado.

**Comprueba:**

- `bd` pasa a estado `healthy`;
- `app` comienza después de que se cumpla esa condición;
- `/api/salud/listo` termina respondiendo;
- el catálogo funciona.

**Captura 4:** estado final de `docker compose ps`, mostrando `bd` saludable, junto con la comprobación satisfactoria de readiness.

!!! question "Reflexiona"
    "El contenedor se ha iniciado" y "el servicio está preparado" no significan lo mismo.

    ¿Qué comprueba `pg_isready`?

    ¿Qué diferencia conceptual hay entre una comprobación de **liveness** y una de **readiness**?

---

## Paso 6: Completa el procedimiento del repositorio

En el `README.md` principal dejaste pendiente el apartado:

```text
Puesta en marcha
```

Complétalo para que una persona que acaba de clonar el repositorio pueda levantar Escaparate sin preguntarte nada.

Debe explicar:

- qué necesita tener instalado;
- dónde está `compose.yaml`;
- cómo crear `.env` a partir de `.env.example`;
- qué valores debe completar;
- cómo arrancar;
- URL del catálogo;
- endpoint de readiness;
- cómo detener sin borrar datos;
- cómo desmontar eliminando también los volúmenes;
- qué consecuencia tiene esta última operación.

!!! tip "La prueba del compañero"
    Un procedimiento es reproducible cuando otra persona puede ejecutarlo sin preguntarte qué comando falta ni qué querías decir.

---

## Paso 7: Amplía la comprobación automática

El repositorio ya contiene:

```text
.github/
└── workflows/
    └── validar.yml
```

Sustituye su contenido por:

```yaml
name: Validar repositorio

on:
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  estructura:
    name: Comprobar repositorio
    runs-on: ubuntu-latest

    steps:
      - name: Descargar repositorio
        uses: actions/checkout@v4

      - name: Comprobar estructura básica
        run: |
          test -f escaparate/pom.xml
          test -f escaparate/mvnw
          test -f practicas/README.md

      - name: Comprobar ficheros que no deben versionarse
        shell: bash
        run: |
          prohibidos="$(
            git ls-files \
              | grep -E '(^|/)\.env($|\.)|(^|/)target/|\.class$' \
              | grep -vE '(^|/)\.env\.example$' \
              || true
          )"

          if [ -n "$prohibidos" ]; then
            echo "Se han encontrado ficheros que no deberían estar versionados:"
            echo "$prohibidos"
            exit 1
          fi

  construir-imagen:
    name: Construir imagen
    needs: estructura
    runs-on: ubuntu-latest

    steps:
      - name: Descargar repositorio
        uses: actions/checkout@v4

      - name: Construir la imagen de Escaparate
        run: |
          docker build \
            -f practicas/docker/app/Dockerfile \
            -t escaparate:validacion \
            escaparate/

  validar-compose:
    name: Validar Docker Compose
    needs: estructura
    runs-on: ubuntu-latest

    steps:
      - name: Descargar repositorio
        uses: actions/checkout@v4

      - name: Preparar valores de ejemplo
        working-directory: practicas/compose
        run: cp .env.example .env

      - name: Validar la configuración
        working-directory: practicas/compose
        run: docker compose config > /dev/null
```

No necesitas estudiar todavía la sintaxis de GitHub Actions.

La nueva comprobación añade una garantía concreta:

```text
checkout limpio
→ .env.example
→ docker compose config
→ configuración reproducible
```

No levanta los servicios ni necesita tus credenciales locales.

---

## Paso 8: Cierra la rama

Revisa `actividad-2.3.md` y comprueba que contiene:

- diagnóstico del primer fallo;
- tabla de persistencia;
- reflexiones pedidas;
- explicación de la exposición de puertos;
- las cuatro capturas solicitadas.

Después:

1. registra los cambios;
2. publica `sesion-05`;
3. abre una Pull Request hacia `main`;
4. espera a que terminen las comprobaciones.

La PR debe mostrar:

```text
Comprobar repositorio    ✓
Construir imagen         ✓
Validar Docker Compose   ✓
```

Si `Validar Docker Compose` falla, revisa el detalle. La comprobación parte de un checkout limpio y crea `.env` desde `.env.example`, por lo que un fallo suele indicar que falta configuración versionable o que `compose.yaml` no puede interpretarse fuera de tu equipo.

Cuando todo esté correcto, fusiona mediante **Create a merge commit** y actualiza tu `main` local.

---

## Verificación

La práctica podrá comprobarse desde un clon limpio:

```bash
git clone https://github.com/<usuario>/daw-despliegue.git verifica
cd verifica/practicas/compose

cp .env.example .env
```

Después de completar los valores:

```bash
docker compose up -d
docker compose ps

curl -fsS http://localhost:8080/
curl -fsS http://localhost:8080/api/salud/vivo
curl -fsS http://localhost:8080/api/salud/listo
```

PostgreSQL no debe publicar ningún puerto hacia el anfitrión.

También se probará un arranque completamente nuevo:

```bash
docker compose down -v
docker compose up -d

for i in {1..30}; do
    if curl -fsS http://localhost:8080/api/salud/listo; then
        exit 0
    fi
    sleep 2
done

echo "Escaparate no ha alcanzado el estado ready"
exit 1
```

Debe observarse:

- los dos servicios están en ejecución;
- `bd` aparece como `healthy`;
- `app` espera a que PostgreSQL esté preparado;
- el catálogo integrado responde en el puerto 8080;
- `/api/salud/vivo` y `/api/salud/listo` responden;
- PostgreSQL no publica puerto hacia el anfitrión;
- `compose.yaml` no contiene credenciales reales;
- `.env` no está registrado en Git;
- `.env.example` sí está versionado;
- `Validar Docker Compose` termina correctamente;
- la sección `Puesta en marcha` permite reproducir el despliegue;
- los cambios han llegado a `main` mediante la PR de `sesion-05`.

---

## Qué se entrega

- [ ] `practicas/compose/compose.yaml` final con `app` y `bd`.
- [ ] Tabla del experimento `stop`, `down` y `down -v`.
- [ ] Diagnóstico del fallo deliberado de conexión.
- [ ] PostgreSQL sin puerto publicado hacia el anfitrión.
- [ ] `.env.example` versionado y `.env` correctamente ignorado.
- [ ] `practicas/compose/00-delay.sql`.
- [ ] `healthcheck` de PostgreSQL y dependencia con `service_healthy`.
- [ ] Un arranque limpio comprobado mediante readiness.
- [ ] Sección `Puesta en marcha` completa en `README.md`.
- [ ] Workflow con `Validar Docker Compose`.
- [ ] `entregas/tema2/actividad-2.3/actividad-2.3.md`.
- [ ] `entregas/tema2/actividad-2.3/img/` con las cuatro capturas.
- [ ] Pull Request `sesion-05 → main` fusionada.

---

## ✅ Cierre

Con esta sesión ya no necesitas recordar una secuencia de órdenes para poner en marcha Escaparate. La aplicación, PostgreSQL, su red, la persistencia, la configuración y el orden de arranque están descritos en un fichero versionado.

También has separado tres ideas que serán importantes durante el resto del módulo:

```text
contenedor iniciado
≠ servicio preparado

servicio accesible en la red interna
≠ puerto publicado hacia fuera

contenedor
≠ dato persistente
```

A partir de ahora puedes entregar el repositorio a otra persona y pedirle que levante el sistema siguiendo la sección `Puesta en marcha`.

En el siguiente bloque empezarás a estudiar qué ocurre por delante de la aplicación: cómo un servidor web sirve contenido, responde a distintos nombres y, más adelante, se coloca como punto de entrada antes de Escaparate.
