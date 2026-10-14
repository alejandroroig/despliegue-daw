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

## Paso 4: Saca los valores locales del YAML (laboratorio)

El `compose.yaml` se versionará. Los valores locales no deben quedar escritos directamente en él.

!!! warning "Configuración externa no equivale a gestión de secretos"
    En el recorrido principal utilizamos únicamente credenciales ficticias de laboratorio. Sacar una contraseña del YAML y guardarla en `.env` evita versionarla, pero continúa llegando al contenedor como variable de entorno.

    Para contraseñas reales, la recomendación es utilizar un mecanismo de secretos. Al terminar encontrarás una **ampliación opcional con Compose Secrets para PostgreSQL y Escaparate**. No sustituye las cuatro evidencias obligatorias.

!!! info "Qué hace `.env`"
    Compose utiliza sus valores para interpolar `${VARIABLE}` en el YAML. No entrega automáticamente todas sus variables al contenedor: en este ejemplo las entregamos mediante `environment`.

!!! warning "Conserva las credenciales de la base de datos existente"
    Al externalizar los valores, utiliza los mismos que ya inicializaron PostgreSQL. Cambiar `POSTGRES_PASSWORD` no cambia la contraseña de una base de datos guardada en un volumen existente. La rotación requiere actuar sobre PostgreSQL; borrar el volumen solo es aceptable cuando se quiere descartar expresamente los datos del laboratorio.

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


---

## 🔐 Ampliación opcional: configuración y contraseñas por separado

### Objetivo y alcance

Mantén la práctica principal terminada. Ahora prepara una variante en la que **ni `bd` ni `app` reciban la contraseña mediante el bloque `environment` del Compose**.

Esta ampliación utiliza la misma imagen publicada de Escaparate, siempre que conserve su configuración Spring Boot estándar de `spring.datasource`. No requiere modificar Java ni reconstruir la imagen. Si tu imagen difiere de la aplicación de clase, comprueba primero su configuración.

El mecanismo tiene dos partes:

- **Compose** entrega un archivo a cada servicio autorizado.
- **Cada programa** sabe cómo leerlo: PostgreSQL mediante `POSTGRES_PASSWORD_FILE`; Spring Boot mediante `configtree`.

No añadas `DB_PASSWORD_FILE`: Escaparate no tiene por qué interpretar esa variable. El sufijo `_FILE` no es una capacidad automática de Docker.

!!! info "Complejidad acotada"
    Se proporciona el YAML completo para que puedas centrarte en el recorrido del secreto. La variante tiene su propio directorio, proyecto, volumen y puerto. No combines este fichero con el Compose principal mediante varios argumentos `-f`.

### A. Prepara los archivos

Desde la raíz del repositorio crea esta estructura:

```text
practicas/compose/ampliacion-secrets/
├── compose.yaml
├── .env.example
├── .env                    # local, no se versiona
└── secrets/
    ├── bd_clave.example.txt
    └── bd_clave.txt         # local, no se versiona
```

En `.env.example` escribe, sustituyendo `<usuario>` por tu usuario real de GHCR:

```dotenv
IMAGEN_BD=ghcr.io/<usuario>/escaparate-db:1.0.0
IMAGEN_APP=ghcr.io/<usuario>/escaparate:sesion-04
BD_USUARIO=escaparate
BD_NOMBRE=escaparate
APP_PUERTO=8081
```

Copia `.env.example` a `.env`. Aquí **ninguno contiene `BD_CLAVE`**.

En `secrets/bd_clave.example.txt` escribe una sola línea:

```text
clave-ficticia-solo-laboratorio-23
```

Copia el ejemplo a `secrets/bd_clave.txt`. Usa texto plano UTF-8 sin BOM, sin comillas y sin escribir `BD_CLAVE=`. Para esta prueba conserva la clave ficticia propuesta: es distinta de la contraseña por defecto de Escaparate y ayuda a detectar una configuración que no esté leyendo el secreto.

En el `.gitignore` de la raíz añade, si no existen reglas equivalentes:

```gitignore
/practicas/compose/ampliacion-secrets/.env
/practicas/compose/ampliacion-secrets/secrets/bd_clave.txt
```

Los archivos de ejemplo sí se versionan. Comprueba desde el directorio de la ampliación:

```bash
git check-ignore -v .env secrets/bd_clave.txt
git ls-files -- .env secrets/bd_clave.txt
```

El primer comando debe mostrar las reglas; el segundo no debe listar ninguno de los dos archivos. `.gitignore` no deja de versionar un archivo que ya estuviera registrado.

### B. Declara los dos consumidores del secreto

Guarda este `compose.yaml` completo en `ampliacion-secrets/`:

```yaml
services:
  bd:
    image: ${IMAGEN_BD:?define IMAGEN_BD en .env}
    environment:
      POSTGRES_USER: ${BD_USUARIO:?define BD_USUARIO en .env}
      POSTGRES_DB: ${BD_NOMBRE:?define BD_NOMBRE en .env}
      POSTGRES_PASSWORD_FILE: /run/secrets/bd_clave
    secrets:
      - bd_clave
    volumes:
      - datos-bd:/var/lib/postgresql
    healthcheck:
      test:
        - CMD-SHELL
        - pg_isready -h 127.0.0.1 -U "$${POSTGRES_USER}" -d "$${POSTGRES_DB}"
      interval: 2s
      timeout: 2s
      retries: 30
      start_period: 10s

  app:
    image: ${IMAGEN_APP:?define IMAGEN_APP en .env}
    ports:
      - "127.0.0.1:${APP_PUERTO:-8081}:8080"
    environment:
      DB_HOST: bd
      DB_PORT: "5432"
      DB_NAME: ${BD_NOMBRE:?define BD_NOMBRE en .env}
      DB_USER: ${BD_USUARIO:?define BD_USUARIO en .env}
      SPRING_CONFIG_IMPORT: "configtree:/run/secrets/"
    secrets:
      - source: bd_clave
        target: spring.datasource.password
    depends_on:
      bd:
        condition: service_healthy

volumes:
  datos-bd:

secrets:
  bd_clave:
    file: ./secrets/bd_clave.txt
```

Lee el recorrido:

```text
Archivo local: secrets/bd_clave.txt
    ├── bd  → /run/secrets/bd_clave
    │         PostgreSQL lo lee mediante POSTGRES_PASSWORD_FILE
    └── app → /run/secrets/spring.datasource.password
              Spring Boot lo importa mediante configtree
```

`source` identifica el secreto declarado; `target` cambia el nombre del archivo que recibe `app`. Spring Boot transforma ese nombre en la propiedad `spring.datasource.password`, que configura la conexión a PostgreSQL.

`SPRING_CONFIG_IMPORT` y `POSTGRES_PASSWORD_FILE` contienen instrucciones y rutas, no la contraseña. No añadas `POSTGRES_PASSWORD`, `DB_PASSWORD` ni `SPRING_DATASOURCE_PASSWORD` a `environment`. Una propiedad de contraseña suministrada por otra fuente de mayor prioridad podría anular la importada.

El prefijo `configtree:` termina con `/`. No usamos `optional:` porque queremos que la ausencia del directorio de configuración provoque un error. Esto no garantiza por sí solo que exista cada archivo: también comprobarás su lectura.

!!! note "Qué protege Compose Secrets local"
    Autoriza servicios concretos y entrega la información como archivos montados. No cifra por sí solo el archivo del anfitrión ni protege frente a un administrador con acceso al motor Docker. Mantén los archivos locales fuera de Git y controla el acceso a tu equipo.

    Tampoco garantiza que el programa consumidor nunca copie el valor a su entorno interno; el entrypoint oficial de PostgreSQL puede hacerlo. Lo que evitamos aquí es suministrar el valor directamente en la configuración de entorno de Compose.

### C. Valida y arranca la variante

Los comandos siguientes se ejecutan desde `practicas/compose/ampliacion-secrets/`, con Bash o Zsh (en Windows, utiliza WSL o un terminal equivalente).

```bash
docker compose -p escaparate-secrets config --quiet
docker compose -p escaparate-secrets up -d
docker compose -p escaparate-secrets ps
```

El nombre de proyecto `escaparate-secrets` separa sus recursos de la práctica principal. Utiliza **ese mismo `-p` en todos los comandos de esta ampliación**. El puerto por defecto es `8081` y solo se publica en el propio equipo.

Espera hasta dos minutos y haz una comprobación final que falle si Escaparate no está preparado:

```bash
for i in {1..60}; do
    if curl -fsS http://localhost:8081/api/salud/listo; then
        break
    fi
    sleep 2
done
curl -fsS http://localhost:8081/api/salud/listo
curl -fsS http://localhost:8081/
```

Si cambias `APP_PUERTO`, adapta también las URL.

Comprueba que el usuario normal de `app` puede leer el archivo, sin imprimir su contenido:

```bash
docker compose -p escaparate-secrets exec -T app \
  sh -c 'test -r /run/secrets/spring.datasource.password && test -s /run/secrets/spring.datasource.password && echo "Secreto accesible y no vacío"'
```

No ejecutes esta comprobación como `root`: queremos comprobar los permisos del usuario que ejecuta la aplicación.

!!! warning "Si falla, diagnostica antes de cambiar"
    Consulta `docker compose -p escaparate-secrets logs app` y `logs bd`.

    - **Archivo inexistente:** revisa la ubicación y el nombre de `secrets/bd_clave.txt`.
    - **Permiso denegado:** comprueba los permisos del archivo montado y el usuario de `app`. En Compose local con secretos de tipo archivo no debes confiar en `uid`, `gid` o `mode` del YAML para corregirlos: se utilizan bind mounts. El comportamiento debe comprobarse en Docker Desktop o Docker Engine según el equipo.
    - **Error de autenticación:** comprueba que el volumen se inicializó con la misma clave y que no hay otra fuente que sobrescriba la contraseña de Spring.
    - **Esquema ausente:** confirma que utilizas la imagen `escaparate-db` de clase, que contiene sus scripts de inicialización.

    No cambies la aplicación a `root` para resolver un problema de lectura.

### D. Comprueba persistencia y límites

Crea un producto de prueba y ejecuta:

```bash
docker compose -p escaparate-secrets down
docker compose -p escaparate-secrets up -d
```

Espera a readiness y comprueba que el producto permanece.

Cambiar `bd_clave.txt` **no rota automáticamente** la contraseña guardada en PostgreSQL. Para repetir la prueba desde cero con otra clave ficticia, puedes eliminar únicamente los recursos de esta variante:

```bash
# ATENCIÓN: elimina los datos de esta ampliación.
docker compose -p escaparate-secrets down -v
docker compose -p escaparate-secrets up -d
```

Hazlo solo si aceptas perder sus productos de prueba. No es un procedimiento de rotación para una base de datos real.

### E. Documentación y validación de la ampliación

Conserva las cuatro evidencias del recorrido principal. Si haces la ampliación, añade a la misma entrega un apartado breve con:

- recorrido del secreto hasta ambos servicios;
- resultado de la comprobación de lectura y de readiness, sin mostrar la contraseña;
- explicación de por qué `.env` no es un almacén de secretos;
- limitaciones de los archivos locales y de la rotación de contraseñas.

Documenta también en el README cómo preparar los dos archivos locales desde sus ejemplos y cómo arrancar esta variante con `-p escaparate-secrets`.

Versiona el nuevo `compose.yaml`, `.env.example`, `secrets/bd_clave.example.txt`, las reglas de exclusión y la documentación. No versiones los archivos locales. Puedes incorporarlo a la misma PR si todavía está abierta; si ya integraste la práctica principal, utiliza una nueva rama y PR.

Si entregas esta ampliación, añade estos pasos al job `validar-compose`, después de la validación principal:

```yaml
      - name: Preparar ejemplos de la ampliación
        working-directory: practicas/compose/ampliacion-secrets
        run: |
          cp .env.example .env
          cp secrets/bd_clave.example.txt secrets/bd_clave.txt

      - name: Validar Compose con secretos
        working-directory: practicas/compose/ampliacion-secrets
        run: docker compose -p escaparate-secrets config --quiet
```

Este job usa exclusivamente datos ficticios. Comprueba la interpretación del YAML; **no demuestra** que los contenedores arranquen, que el usuario de `app` pueda leer el archivo ni que la aplicación autentique contra PostgreSQL. Esas comprobaciones corresponden a la prueba de ejecución anterior.

### Referencias

- [Docker: secretos en Compose](https://docs.docker.com/compose/how-tos/use-secrets/).
- [Docker: secretos por servicio y limitaciones de permisos](https://docs.docker.com/reference/compose-file/services/#secrets).
- [Spring Boot: configuración mediante árboles de archivos](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files.configtrees).
- [Imagen oficial de PostgreSQL](https://hub.docker.com/_/postgres).
