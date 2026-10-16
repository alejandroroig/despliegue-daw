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
- Separar configuración no sensible en `.env` y contraseñas mediante Compose Secrets.
- Entregar el mismo secreto a PostgreSQL y a Spring Boot como archivo.
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

## Paso 4: Separa configuración y secretos

El despliegue final utilizará `.env` para configuración no sensible y **Compose Secrets para la contraseña**, tanto en `bd` como en `app`. Esta parte es obligatoria y guiada.

### 4.1. Prepara los archivos locales

Desde `practicas/compose/`, prepara:

```text
compose.yaml
.env.example
.env
secrets/
├── bd_clave.example.txt
└── bd_clave.txt
```

En `.env.example` escribe, sustituyendo `<usuario>` por tu usuario real de GHCR:

```dotenv
IMAGEN_BD=ghcr.io/<usuario>/escaparate-db:1.0.0
IMAGEN_APP=ghcr.io/<usuario>/escaparate:sesion-04
BD_USUARIO=escaparate
BD_NOMBRE=escaparate
```

Copia ese archivo a `.env`:

```bash
cp .env.example .env
mkdir -p secrets
```

En `secrets/bd_clave.example.txt` escribe una sola línea:

```text
clave-ficticia-solo-laboratorio-23
```

Cópialo al archivo local:

```bash
cp secrets/bd_clave.example.txt secrets/bd_clave.txt
```

Usa texto plano UTF-8 sin BOM, sin comillas y sin escribir `BD_CLAVE=`. En esta práctica todos utilizaremos ese valor ficticio. No reutilices contraseñas personales.

Añade al `.gitignore` de la raíz del repositorio:

```gitignore
/practicas/compose/.env
/practicas/compose/secrets/bd_clave.txt
```

Los archivos de ejemplo sí se versionan. Comprueba desde `practicas/compose/`:

```bash
git check-ignore -v .env secrets/bd_clave.txt
git ls-files -- .env secrets/bd_clave.txt
```

El primer comando debe mostrar las reglas de exclusión; el segundo no debe listar ninguno de los dos archivos. Si ya estaban registrados, `.gitignore` no basta: retíralos del índice con `git rm --cached -- .env` o `git rm --cached -- secrets/bd_clave.txt`, según corresponda. Se conservarán en el equipo.

### 4.2. Sustituye el Compose por esta configuración

Reemplaza el contenido de `compose.yaml` por el siguiente. Conserva los dos servicios, el volumen y la publicación exclusiva de `app`. Todavía utiliza la dependencia básica: en el paso 5 comprobarás por qué hay que mejorarla.

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

  app:
    image: ${IMAGEN_APP:?define IMAGEN_APP en .env}
    ports:
      - "8080:8080"
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
      - bd

volumes:
  datos-bd:

secrets:
  bd_clave:
    file: ./secrets/bd_clave.txt
```

### 4.3. Comprende el recorrido del secreto

```text
secrets/bd_clave.txt en el anfitrión
    ├── bd  → /run/secrets/bd_clave
    │         PostgreSQL: POSTGRES_PASSWORD_FILE
    └── app → /run/secrets/spring.datasource.password
              Spring Boot: configtree
```

- La sección superior `secrets` declara el archivo de origen.
- El bloque `secrets` de cada servicio autoriza su acceso.
- `target` cambia el nombre del archivo que recibe `app`.
- Spring Boot importa ese archivo como la propiedad `spring.datasource.password`.
- `POSTGRES_PASSWORD_FILE` y `SPRING_CONFIG_IMPORT` contienen rutas e instrucciones, no la contraseña.

No añadas `POSTGRES_PASSWORD`, `DB_PASSWORD` ni `SPRING_DATASOURCE_PASSWORD` al bloque `environment`. Tampoco inventes `DB_PASSWORD_FILE`: el sufijo `_FILE` solo funciona cuando la imagen o aplicación lo implementa.

!!! info "Configuración de Escaparate"
    La aplicación de clase utiliza Spring Boot y su configuración estándar `spring.datasource`. `configtree` permite suministrar la contraseña sin cambiar Java ni reconstruir la imagen. Conserva exactamente el nombre `spring.datasource.password` y la barra final de `configtree:/run/secrets/`.

!!! note "Qué hace `.env`"
    Compose utiliza sus valores para interpolar `${VARIABLE}` en el YAML. No entrega automáticamente todas sus variables al contenedor. En este ejemplo configuramos explícitamente cada servicio con `environment`.

### 4.4. Inicializa el laboratorio con la nueva contraseña

El volumen actual se creó con la contraseña del paso 1. Cambiar un archivo o una variable **no cambia la contraseña almacenada en PostgreSQL**.

En esta práctica vamos a reinicializar expresamente la base de datos de laboratorio. Los productos creados hasta ahora se perderán; ya deben estar documentados los resultados del paso 2.

```bash
docker compose config --quiet
# ATENCIÓN: elimina los datos del laboratorio.
docker compose down -v
docker compose up -d
```

Este borrado no es un procedimiento de rotación de contraseñas para una base de datos real.

Espera a que responda readiness, con Bash o Zsh (en Windows, WSL o un terminal equivalente):

```bash
for i in {1..60}; do
    if curl -fsS http://localhost:8080/api/salud/listo; then
        break
    fi
    sleep 2
done
curl -fsS http://localhost:8080/api/salud/listo
```

Si `app` terminó durante la inicialización de PostgreSQL, comprueba los logs y, cuando la base de datos acepte conexiones, ejecuta `docker compose up -d app` y repite la comprobación. En el paso 5 resolverás esta dependencia de arranque.

Comprueba la lectura del secreto con el usuario normal de la aplicación, sin mostrar su contenido:

```bash
docker compose exec -T app \
  sh -c 'test -r /run/secrets/spring.datasource.password && test -s /run/secrets/spring.datasource.password && echo "Secreto accesible y no vacío"'
```

No ejecutes la comprobación como `root`. La clave ficticia elegida es distinta de la contraseña predeterminada de Escaparate: readiness correcto tras inicializar la base de datos con esa clave ayuda a verificar que se está aplicando la configuración nueva.

!!! tip "Si la comprobación no pasa"
    - **Archivo inexistente:** revisa la ruta y el nombre de `secrets/bd_clave.txt`.
    - **Permiso denegado:** comprueba los permisos del archivo y el usuario de `app` con el profesor. No cambies la aplicación a `root`. Compose local usa un montaje del archivo y no permite resolver siempre sus permisos con `uid`, `gid` o `mode` en el YAML.
    - **Error de autenticación:** comprueba que realizaste la reinicialización indicada y que no conservas otra variable de contraseña.
    - **Esquema ausente:** utiliza la imagen `escaparate-db` de clase, que incorpora los scripts de inicialización.

!!! note "Alcance de la protección"
    Compose Secrets local entrega archivos a servicios autorizados; no cifra por sí solo el archivo del anfitrión. Este permanece fuera de Git y requiere controlar el acceso al equipo. El programa consumidor puede tratar internamente el valor de otras formas; por ejemplo, el entrypoint de PostgreSQL puede copiarlo a su entorno. Aquí evitamos suministrar la contraseña directamente en el entorno declarado por Compose.

**Evidencia 3:** fragmentos de `compose.yaml`, `.env.example`, resultado de `git check-ignore` para ambos archivos locales y mensaje «Secreto accesible y no vacío». No muestres el contenido del secreto.

!!! question "Reflexiona"
    ¿Qué diferencia hay entre interpolar un valor de `.env` y montar un secreto? ¿Qué programa lee el archivo en cada servicio?

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

Añade este bloque dentro de `bd`, al mismo nivel que `environment`, `secrets` y `volumes`:

```yaml
    healthcheck:
      test:
        - CMD-SHELL
        - pg_isready -h 127.0.0.1 -U "$${POSTGRES_USER}" -d "$${POSTGRES_DB}"
      interval: 2s
      timeout: 2s
      retries: 30
      start_period: 10s
```

Sustituye el `depends_on` de `app` por:

```yaml
    depends_on:
      bd:
        condition: service_healthy
```

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
- cómo crear `secrets/bd_clave.txt` desde el ejemplo ficticio;
- qué archivos locales deben quedar fuera de Git;
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
3. copiar `secrets/bd_clave.example.txt` a `secrets/bd_clave.txt`;
4. comprobar que el archivo de secreto existe y no está vacío;
5. ejecutar `docker compose config --quiet`;
6. fallar si alguna comprobación falla.

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
        run: |
          cp .env.example .env
          cp secrets/bd_clave.example.txt secrets/bd_clave.txt
          test -s secrets/bd_clave.txt

      - name: Validar la configuración
        working-directory: practicas/compose
        run: docker compose config --quiet
```

!!! info "Qué demuestra este job"
    Parte de un checkout limpio. Si puede preparar los archivos locales desde los ejemplos e interpretar el Compose, la configuración básica necesaria para reproducir el despliegue está versionada.

    Este job **no levanta los servicios**, no comprueba los permisos de lectura dentro del contenedor ni demuestra la autenticación. La lectura del secreto y readiness se verifican durante la práctica. Solo se utilizan credenciales ficticias en CI.

---

## Paso 8: Documenta e integra

Revisa `actividad-2.3.md`. Debe contener:

- diagnóstico del fallo inicial;
- tabla de persistencia;
- reflexión sobre puertos;
- explicación del recorrido del secreto y su diferencia respecto a `.env`;
- reflexión sobre healthcheck, liveness y readiness;
- cuatro evidencias;
- incidencias relevantes.

Registra:

- `compose.yaml`;
- `.env.example`;
- `secrets/bd_clave.example.txt` con el valor ficticio y las reglas de `.gitignore`;
- README actualizado;
- workflow actualizado;
- documentación y capturas.

!!! warning "No debe quedar"
    Antes del commit final comprueba que:

    - `.env` y `secrets/bd_clave.txt` no están versionados;
    - ambos servicios reciben el secreto mediante `secrets`;
    - el Compose no declara valores en `POSTGRES_PASSWORD`, `DB_PASSWORD` o `SPRING_DATASOURCE_PASSWORD`;
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
- [ ] `.env.example` versionado, sin contraseña, y `.env` ignorado;
- [ ] ejemplo ficticio del secreto versionado y `secrets/bd_clave.txt` ignorado;
- [ ] secreto autorizado para `bd` y `app`, con `_FILE` y `configtree` respectivamente;
- [ ] lectura del secreto comprobada con el usuario normal de `app`;
- [ ] `healthcheck` de PostgreSQL y dependencia `service_healthy`;
- [ ] **sin** `00-delay.sql` ni espera artificial en el despliegue final;
- [ ] sección `Puesta en marcha` completa;
- [ ] workflow con `Validar Docker Compose`;
- [ ] `actividad-2.3.md` con tabla, reflexiones y cuatro evidencias;
- [ ] Pull Request `sesion-05 → main` fusionada.

!!! info "Dónde queda la entrega"
    La infraestructura y la documentación quedan versionadas en el repositorio. `.env` y `secrets/bd_clave.txt` permanecen únicamente en cada equipo.

??? info "Cómo se comprobará"
    La práctica podrá reproducirse desde un clon limpio.

    Tras copiar `.env.example` a `.env`, completar las referencias de imagen y copiar `secrets/bd_clave.example.txt` a `secrets/bd_clave.txt`, se podrá ejecutar desde `practicas/compose/`:

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
    - `.env` y el archivo local del secreto fuera de Git;
    - contraseña suministrada como archivo a ambos servicios;
    - `compose.yaml` válido con los dos archivos de ejemplo.

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

## Referencias de la configuración de secretos

- [Docker: secretos en Compose](https://docs.docker.com/compose/how-tos/use-secrets/).
- [Spring Boot: árboles de configuración](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files.configtrees).
- [Imagen oficial de PostgreSQL](https://hub.docker.com/_/postgres).
