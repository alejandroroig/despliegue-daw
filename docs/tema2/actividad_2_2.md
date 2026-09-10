# 🧪 Actividad 2.2: Empaquetar Escaparate

## Contexto

La base de datos de pruebas ya puede viajar como una imagen. Ahora llega el encargo principal: **empaquetar Escaparate** para que cualquier equipo con Docker pueda ejecutarlo sin tener Java ni Maven instalados.

Construirás dos versiones:

- una primera imagen deliberadamente sencilla;
- una segunda preparada para reducir trabajo repetido, tamaño y privilegios.

Después compararás ambas con datos reales.

!!! abstract "Cómo vas a trabajar"
    **Inspeccionar → construir → medir → modificar → optimizar → comparar → publicar**

    No se trata de conseguir únicamente que Escaparate arranque. El objetivo es identificar **qué problema resuelve cada mejora del Dockerfile**.

---

## Qué vas a practicar

- Escribir un Dockerfile para una aplicación Java que necesita compilarse.
- Distinguir Dockerfile y contexto de construcción.
- Observar el efecto de la caché.
- Reducir el contenido final mediante una construcción multietapa.
- Ejecutar la aplicación con un usuario sin privilegios.
- Medir tiempo de construcción y tamaño de imagen.
- Publicar una imagen con una referencia estable y otra móvil.
- Comprobar mediante GitHub Actions que el Dockerfile puede construirse desde un entorno limpio.

---

## Requisitos previos

Necesitas:

- la Actividad 2.1 terminada;
- `ghcr.io/<usuario>/escaparate-db:1.0.0` publicada;
- el proyecto dentro de `escaparate/`;
- Docker funcionando;
- la credencial utilizada para GHCR.

Prepara la rama:

```bash
git switch main
git pull --ff-only
git switch -c sesion-04
```

Crea también:

```text
entregas/
└── tema2/
    └── actividad-2.2/
        ├── actividad-2.2.md
        └── img/
```

!!! info "Evidencias"
    Documenta solo las respuestas, mediciones, incidencias y **cuatro evidencias** solicitadas. Los Dockerfile reales permanecerán en `practicas/docker/app/`.

---

## Paso 1: Reconoce la aplicación que vas a empaquetar

Antes de escribir el Dockerfile, busca en `escaparate/` las respuestas a estas preguntas:

1. ¿Con qué herramienta se compila?
2. ¿Qué fichero declara las dependencias?
3. ¿Qué artefacto se genera y en qué directorio?
4. ¿Qué versión de Java necesita?
5. ¿Qué variables utiliza para conectarse a PostgreSQL?
6. ¿En qué puerto escucha?

Consulta `pom.xml`, el README y la configuración de Spring.

!!! info "Arquitectura que vas a empaquetar"
    El frontend y la API forman parte de la misma aplicación Spring Boot. El WAR es ejecutable y arranca su **Tomcat embebido** en el puerto 8080.

    La imagen de Escaparate será, conceptualmente:

    ```text
    JRE
      ↓
    WAR ejecutable
      ↓
    Spring Boot + Tomcat embebido
    ```

    PostgreSQL seguirá siendo un contenedor independiente.

---

## Paso 2: Construye una primera versión sencilla

Crea:

```text
practicas/
└── docker/
    └── app/
        └── Dockerfile.ingenuo
```

Escribe una versión que:

1. parta de `maven:3.9.16-eclipse-temurin-21-alpine`;
2. establezca un directorio de trabajo;
3. copie el proyecto completo;
4. compile con Maven;
5. arranque el WAR generado.

Utiliza:

```text
Dockerfile
→ practicas/docker/app/Dockerfile.ingenuo

contexto
→ escaparate/

imagen
→ escaparate:ingenua
```

Mide la construcción con el mismo procedimiento que utilizarás después:

```bash
time docker build \
  -f practicas/docker/app/Dockerfile.ingenuo \
  -t escaparate:ingenua \
  escaparate/
```

Anota:

- duración total;
- `SIZE` mostrado por:

```bash
docker image ls escaparate:ingenua
```

!!! tip "Compara siempre con la misma métrica"
    No utilices `docker system df` para comparar las dos imágenes. En esta actividad tomaremos como referencia la columna `SIZE` de `docker image ls`.

### Comprueba que funciona

Crea una red:

```bash
docker network create escaparate-red
```

Arranca en esa red:

1. tu PostgreSQL de la Actividad 2.1;
2. `escaparate:ingenua`.

Utiliza `--network escaparate-red` en ambos contenedores y pasa a Escaparate las variables de conexión que identificaste en el Paso 1.

!!! info "Primero PostgreSQL"
    Revisa los logs de la base de datos y espera a que termine su inicialización antes de arrancar Escaparate.

Comprueba:

```text
http://localhost:8080/
http://localhost:8080/api/salud/listo
```

**Evidencia 1:** Escaparate funcionando y listado de imágenes donde se vea el `SIZE` de `escaparate:ingenua`. Anota el tiempo de construcción en `actividad-2.2.md`.

---

## Paso 3: Observa el problema de caché

Haz un cambio insignificante en una clase Java —por ejemplo, un comentario— y reconstruye **la misma imagen ingenua** con el mismo comando.

Anota:

- duración;
- qué pasos aparecen como reutilizados;
- qué parte ha tenido que repetirse.

!!! question "Interpreta"
    ¿Por qué cambiar una sola clase obliga a repetir tanto trabajo si el Dockerfile contiene `COPY . .` antes de compilar?

No necesitas una captura independiente de este paso si has documentado claramente el resultado.

---

## Paso 4: Construye la versión optimizada

Crea ahora:

```text
practicas/
└── docker/
    └── app/
        ├── Dockerfile.ingenuo
        └── Dockerfile
```

La segunda versión debe mejorar cuatro aspectos.

### 4.1. Reduce el contexto

Crea:

```text
escaparate/.dockerignore
```

Excluye, como mínimo:

- `target/`;
- configuración local de IDE;
- `.env` y variantes locales;
- `uploads/`.

Puedes añadir otras reglas justificadas.

!!! note "Dónde debe estar `.dockerignore`"
    Como el contexto es `escaparate/`, el fichero debe estar en la raíz de esa carpeta, aunque el Dockerfile esté en `practicas/docker/app/`.

### 4.2. Aprovecha mejor la caché

En la etapa de construcción:

1. copia primero `pom.xml`;
2. prepara las dependencias;
3. copia después `src/`;
4. compila la aplicación.

El objetivo es que un cambio únicamente en código no obligue a repetir todos los pasos relacionados con dependencias.

### 4.3. Separa construcción y ejecución

Utiliza dos etapas:

```text
BUILD
maven:3.9.16-eclipse-temurin-21-alpine

RUNTIME
eclipse-temurin:21.0.12_8-jre-alpine-3.24
```

La etapa final debe contener únicamente lo necesario para ejecutar el WAR.

### 4.4. Ejecuta con menos privilegios

En la etapa final:

- crea un usuario sin privilegios;
- prepara una ruta escribible para las imágenes subidas;
- configura `APP_STORAGE_PATH`;
- ejecuta Escaparate con ese usuario.

Construye:

```bash
time docker build \
  -f practicas/docker/app/Dockerfile \
  -t escaparate:optimizada \
  escaparate/
```

Comprueba que funciona con la misma base de datos y configuración que la imagen ingenua.

Comprueba también:

```bash
docker exec <contenedor> id
```

y:

```bash
docker exec <contenedor> sh -c \
  'printf "%s\n" "$APP_STORAGE_PATH"; test -w "$APP_STORAGE_PATH" && echo "writable"'
```

**Evidencia 2:** comparación de `SIZE` entre `escaparate:ingenua` y `escaparate:optimizada`, junto con la comprobación de que la versión optimizada no se ejecuta como `root`.

---

## Paso 5: Comprueba la mejora de caché

Haz otro cambio mínimo en una clase Java y reconstruye:

```text
escaparate:optimizada
```

con exactamente el mismo comando utilizado en el Paso 4.

Anota:

- duración;
- qué pasos se reutilizan;
- qué pasos deben repetirse.

**Evidencia 3:** fragmento de la salida de construcción donde se vea qué pasos se han reutilizado tras modificar únicamente el código.

Después **deshaz los cambios temporales** de los pasos 3 y 5. Comprueba con Git que no queda ninguna modificación accidental en el código de Escaparate.

---

## Paso 6: Compara con datos

Completa:

| | Construcción inicial | Tras modificar código | `SIZE` final |
|---|---:|---:|---:|
| **Ingenua** | | | |
| **Optimizada** | | | |

Responde:

1. ¿Qué trabajo reutilizó la imagen optimizada que la ingenua tuvo que repetir?
2. ¿Qué contiene la imagen ingenua que la imagen final no necesita? Indica al menos tres elementos.
3. ¿Qué mejora explica principalmente la reducción del tiempo de reconstrucción?
4. ¿Qué mejora explica principalmente la reducción de tamaño?
5. ¿Qué decisiones mejoran seguridad aunque no reduzcan necesariamente el tiempo de build?

!!! info "Tus datos son los que importan"
    No se evalúa que los tiempos coincidan con los de otro equipo. Se evalúa que interpretes correctamente **tus propias mediciones**.

---

## Paso 7: Publica la imagen validada

Autentica Docker en GHCR si es necesario:

```bash
docker login ghcr.io -u <tu-usuario>
```

Publica la imagen optimizada con dos referencias:

```text
ghcr.io/<usuario>/escaparate:sesion-04
ghcr.io/<usuario>/escaparate:latest
```

En esta actividad:

- `sesion-04` identifica de forma estable **por convención** el resultado validado de esta sesión;
- `latest` es una referencia móvil que podría cambiar más adelante.

Configura el paquete como público.

!!! note "Todavía no es una release de Escaparate"
    `sesion-04` identifica el resultado de esta práctica. Más adelante utilizarás etiquetas de release reales como `v1.0.0`.

!!! question "Reflexiona"
    Si hoy descargas `latest` y mañana esa etiqueta apunta a otra imagen, ¿qué podría ocurrir al repetir el despliegue? ¿Cuál de las dos referencias utilizarías para describir esta práctica de forma reproducible?

---

## Paso 8: Añade una comprobación automática de construcción

El workflow creado en el Tema 1 ya comprueba la estructura del repositorio. Ahora añadirá una segunda comprobación: **construir la imagen optimizada desde un entorno limpio**.

Sustituye `.github/workflows/validar.yml` por:

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
        uses: actions/checkout@v7

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
        uses: actions/checkout@v7

      - name: Construir la imagen de Escaparate
        run: |
          docker build \
            -f practicas/docker/app/Dockerfile \
            -t escaparate:validacion \
            escaparate/
```

No necesitas estudiar todavía todo el YAML. La idea es importante:

```text
Pull Request
      ↓
comprobar estructura
      ↓
construir Dockerfile
      ↓
solo integrar si funciona
```

El workflow **no publica** la imagen; únicamente comprueba que el Dockerfile versionado puede construirse en una máquina limpia.

---

## Paso 9: Documenta e integra

Revisa `actividad-2.2.md`. Debe contener:

- mediciones de las dos imágenes;
- respuestas del Paso 6;
- reflexión sobre `latest`;
- cuatro evidencias;
- incidencias relevantes y cómo las resolviste.

!!! tip "No documentes lo que ya está versionado"
    No copies el contenido completo de los Dockerfile en el informe. Los ficheros reales ya están en `practicas/docker/app/`.

Registra:

- `Dockerfile.ingenuo`;
- `Dockerfile`;
- `escaparate/.dockerignore`;
- workflow actualizado;
- documentación de la actividad.

Publica `sesion-04` y abre:

```text
sesion-04 → main
```

La Pull Request debe terminar con:

```text
Comprobar repositorio  ✓
Construir imagen       ✓
```

Si falla la construcción automática, abre el detalle y compara el error con el build local antes de corregirlo.

**Evidencia 4:** Pull Request con ambas comprobaciones correctas y paquete de GHCR mostrando `sesion-04` y `latest`. Utiliza dos capturas si una sola no resulta legible.

Fusiona mediante **Create a merge commit** y actualiza:

```bash
git switch main
git pull --ff-only
```

---

## Qué se entrega

Antes de terminar, comprueba:

- [ ] `actividad-2.2.md` con mediciones, respuestas, reflexión y cuatro evidencias;
- [ ] `practicas/docker/app/Dockerfile.ingenuo`;
- [ ] `practicas/docker/app/Dockerfile`;
- [ ] `escaparate/.dockerignore`;
- [ ] imagen pública con `sesion-04` y `latest`;
- [ ] workflow capaz de construir la imagen en la Pull Request;
- [ ] Pull Request `sesion-04 → main` fusionada.

!!! info "Dónde queda la entrega"
    Los ficheros y evidencias quedan en el repositorio privado. La imagen de GHCR se mantiene pública para facilitar su comprobación.

??? info "Cómo se comprobará"
    El profesor podrá descargar las imágenes publicadas y arrancar Escaparate con una PostgreSQL configurada con valores diferentes.

    En lugar de utilizar tiempos fijos, se esperará a que cada servicio esté realmente disponible.

    ```bash
    docker rm -f verifica bd 2>/dev/null || true
    docker network create escaparate-red 2>/dev/null || true

    docker pull ghcr.io/<usuario>/escaparate-db:1.0.0
    docker pull ghcr.io/<usuario>/escaparate:sesion-04

    docker run -d --name bd \
      --network escaparate-red \
      -e POSTGRES_USER=profesor \
      -e POSTGRES_PASSWORD=prueba-verificacion \
      -e POSTGRES_DB=escaparate \
      ghcr.io/<usuario>/escaparate-db:1.0.0

    until docker exec bd \
      pg_isready -U profesor -d escaparate >/dev/null 2>&1; do
      sleep 1
    done

    docker run -d --name verifica \
      --network escaparate-red \
      -p 8080:8080 \
      -e DB_HOST=bd \
      -e DB_PORT=5432 \
      -e DB_NAME=escaparate \
      -e DB_USER=profesor \
      -e DB_PASSWORD=prueba-verificacion \
      ghcr.io/<usuario>/escaparate:sesion-04

    until curl -fsS http://localhost:8080/api/salud/listo >/dev/null; do
      sleep 2
    done

    curl -fsS http://localhost:8080/api/salud/vivo
    curl -fsS http://localhost:8080/api/salud/listo
    docker exec verifica id
    docker exec verifica sh -c \
      'printf "%s\n" "$APP_STORAGE_PATH"; test -w "$APP_STORAGE_PATH" && echo "storage writable"'

    docker rm -f verifica bd
    ```

---

## ✅ Cierre

Escaparate ya puede viajar como una imagen que contiene lo necesario para ejecutarse, pero no las herramientas utilizadas para compilarla.

También has comprobado que **optimizar una imagen no significa una única cosa**: el contexto, la caché, la construcción multietapa y los privilegios resuelven problemas diferentes.

En la próxima sesión dejarás de arrancar manualmente cada contenedor y describirás el sistema completo mediante Docker Compose.
