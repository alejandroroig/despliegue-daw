# 🧪 Actividad 2.2: Empaquetar Escaparate

## Contexto

La base de datos de pruebas ya puede viajar como una imagen. Ahora llega el encargo principal: **empaquetar Escaparate** para que cualquier equipo con Docker pueda ejecutarlo sin tener instalado Java ni Maven.

Trabajarás sobre la misma aplicación que incorporaste al repositorio en la actividad 1.2:

```text
daw-despliegue/
└── escaparate/
```

No se entrega otra distribución para esta sesión.

Vas a construir la imagen dos veces. La primera será deliberadamente sencilla; la segunda estará pensada para reducir trabajo repetido, tamaño y privilegios. Después compararás ambas con datos reales.

## Qué vas a practicar

- **Escribir** un `Dockerfile` para una aplicación Java que necesita compilarse.
- **Entender** el contexto de construcción y el efecto de `COPY`.
- **Medir** cómo afecta el orden de las instrucciones a la caché de construcción.
- **Reducir** el tamaño de una imagen mediante una construcción multietapa.
- **Ejecutar** el proceso con un usuario sin privilegios.
- **Publicar** una imagen y distinguir una etiqueta fija de una etiqueta móvil.

## Requisitos previos

- Actividad 2.1 terminada y la imagen `ghcr.io/<usuario>/escaparate-db:1.0.0` publicada.
- El proyecto recibido en la actividad 1.2 dentro de `escaparate/`.
- La credencial que utilizaste en la actividad 2.1 para autenticar Docker en `ghcr.io`. En el itinerario simplificado será el PAT `DAW - Curso` creado en la actividad 1.2; si elegiste la alternativa de mínimo privilegio, será el PAT classic específico de GHCR.
- Docker funcionando y espacio libre suficiente.
- Tu rama de esta sesión:

```bash
git switch main
git pull --ff-only
git switch -c sesion-04
```

!!! info "Reparto de tiempo orientativo"
    - Pasos 1 a 3: unos 35 minutos. 
    - Pasos 4 y 5: unos 30 minutos.
    - Pasos 6 a 8: unos 25 minutos. 
    
    Mientras una construcción avanza, aprovecha para documentar los resultados de la actividad.

!!! info "Documenta la actividad en el repositorio"
    Crea al comenzar la sesión:

    ```text
    entregas/
    └── tema2/
        └── actividad-2.2/
            ├── actividad-2.2.md
            └── img/
    ```

    Documenta en `actividad-2.2.md` las respuestas, mediciones, reflexiones y resultados que se pidan durante la actividad. Guarda las capturas en `img/` e insértalas en el Markdown mediante rutas relativas.

    Los ficheros técnicos de despliegue no se duplican dentro de `entregas/`: permanecerán en `practicas/` o en `escaparate/`, según se indique.

---

## Paso 1: Reconoce el terreno

Antes de escribir un `Dockerfile`, inspecciona `escaparate/` y responde:

- ¿Con qué herramienta se compila?
- ¿Qué fichero declara las dependencias?
- ¿Qué artefacto se genera y en qué carpeta?
- ¿Qué versión de Java necesita el proyecto?
- ¿Qué variables utiliza Escaparate para localizar PostgreSQL?
- ¿Qué puerto escucha la aplicación?
- ¿Qué diferencia hay entre `/api/salud/vivo` y `/api/salud/listo`?

Busca las respuestas en el propio proyecto: `pom.xml`, `README.md` y configuración de Spring.

!!! info "Arquitectura que vas a empaquetar"
    Esta distribución de Escaparate integra el frontend y la API en la misma aplicación Spring Boot. Al arrancar el contenedor tendrás **un único servicio de aplicación** escuchando en el puerto 8080. PostgreSQL seguirá siendo otro contenedor independiente.

---

## Paso 2: La versión ingenua

Crea:

```text
practicas/
└── docker/
    └── app/
        └── Dockerfile.ingenuo
```

Escribe una primera versión sencilla que:

1. parta de `maven:3.9.16-eclipse-temurin-21-alpine`;
2. establezca un directorio de trabajo;
3. copie el proyecto completo;
4. lo compile;
5. indique qué debe ejecutar el contenedor al arrancar.

Utiliza `escaparate/` como **contexto de construcción**, aunque el `Dockerfile` esté almacenado en `practicas/docker/app/`.

Construye la imagen como:

```text
escaparate:ingenua
```

Anota:

- duración total de la construcción;
- **Content Size** de la imagen.

!!! info "Content Size y Disk usage no son lo mismo"
    Docker puede mostrar varias cifras relacionadas con el tamaño de una imagen:

    - **Content Size** representa el tamaño lógico del contenido de la imagen, es decir, el conjunto de capas que forman esa imagen. Es la medida que utilizaremos para comparar la versión ingenua y la optimizada.
    - **Disk usage** refleja el espacio que Docker está utilizando localmente para almacenar imágenes, capas y otros datos asociados. Puede verse afectado por capas compartidas con otras imágenes y por la forma en que Docker gestiona su almacenamiento local, por lo que no es una medida tan adecuada para comparar dos imágenes concretas.

    En esta actividad, cuando se pida el **tamaño de una imagen**, anota siempre su **Content Size** y utiliza la misma métrica en todas las comparaciones.

    Si trabajas desde terminal, puedes consultar el tamaño de la imagen con:

    ```bash
    docker image ls escaparate:ingenua
    ```

    La columna `SIZE` es la medida que utilizaremos para la comparación.

!!! tip "Mide siempre de la misma forma"
    En Linux puedes anteponer `time` al comando de construcción. Utiliza el mismo procedimiento en las cuatro mediciones para que la comparación tenga sentido.

### Ponla en marcha

Crea una red Docker llamada:

```text
escaparate-red
```

Arranca en ella:

1. tu imagen de PostgreSQL de la actividad 2.1;
2. `escaparate:ingenua`.

Configura la aplicación mediante las variables que identificaste en el paso 1. No escribas las credenciales dentro del `Dockerfile`.

**Comprueba:**

```text
http://localhost:8080/
```

muestra el catálogo y:

```text
http://localhost:8080/api/salud/listo
```

responde correctamente.

**Captura:** catálogo funcionando, duración de la construcción y listado de imágenes donde se vea el `SIZE` de `escaparate:ingenua`.

!!! tip "Si Escaparate no encuentra PostgreSQL"
    Mira primero los logs de la aplicación. Comprueba después el valor de `DB_HOST` y recuerda que dos contenedores solo pueden resolverse por nombre cuando comparten una red adecuada.

---

## Paso 3: Toca el código

Haz un cambio insignificante en un fichero de código fuente: por ejemplo, añade un comentario en el fichero `EscaparateApplication.java`.

Vuelve a construir `escaparate:ingenua`.

Anota:

- duración de la segunda construcción;
- qué pasos se han reutilizado;
- si Maven ha tenido que resolver otra vez las dependencias.

**Captura:** salida de la construcción donde se vean los pasos reconstruidos y los reutilizados.

---

## Paso 4: La versión optimizada

Crea ahora:

```text
practicas/
└── docker/
    └── app/
        ├── Dockerfile.ingenuo
        └── Dockerfile
```

La versión nueva debe mejorar tres aspectos.

### 4.1 Contexto limpio

Crea también:

```text
escaparate/.dockerignore
```

El fichero debe excluir del contexto, como mínimo:

- `.git` como regla preventiva, aunque en esta estructura el repositorio real está fuera del contexto `escaparate/`;
- `target/`;
- configuración local de IDE;
- `.env` y variantes locales;
- `uploads/`.

!!! note "Por qué `.dockerignore` está dentro de `escaparate/`"
    En esta práctica el contexto de construcción es `escaparate/`. Docker busca las reglas de exclusión en la raíz del contexto, por eso este fichero está junto al código aunque los `Dockerfile` de la práctica estén en `practicas/docker/app/`.

### 4.2 Capas reutilizables

Organiza la etapa de construcción para que la información necesaria para resolver dependencias se copie **antes** que el código fuente.

El objetivo es que modificar una clase Java no obligue a repetir una descarga de dependencias que no ha cambiado.

### 4.3 Construcción multietapa

Utiliza:

- una primera etapa basada en `maven:3.9.16-eclipse-temurin-21-alpine` con las herramientas necesarias para compilar;
- una etapa final basada en `eclipse-temurin:21.0.12_8-jre-alpine-3.24` que contenga únicamente Java de ejecución y el artefacto generado.

Construye esta versión como:

```text
escaparate:optimizada
```

Comprueba que funciona exactamente igual que la ingenua.

**Captura:** `Dockerfile`, `.dockerignore` y listado de imágenes mostrando el `SIZE` de `escaparate:ingenua` y `escaparate:optimizada`.

---

## Paso 5: Toca de nuevo el código

Haz otro cambio insignificante en el código y reconstruye:

```text
escaparate:optimizada
```

Anota la duración.

**Captura:** salida de construcción donde se vea qué capas se han reutilizado.

Los cambios de código realizados en los pasos 3 y 5 solo servían para provocar invalidaciones de caché. Antes de continuar, **deshaz esos cambios temporales** y comprueba que no quedan modificaciones accidentales en el código de Escaparate.

---

## Paso 6: Compara con datos

Completa:

| | Construcción inicial | Tras modificar código | Content Size final |
|---|---|---|---|
| Imagen ingenua | | | |
| Imagen optimizada | | | |

Utiliza para ambas filas la misma medida de tamaño. No mezcles `Content Size` con `Disk usage`.

Responde brevemente:

1. ¿Qué trabajo ha podido reutilizar la versión optimizada después de modificar código?
2. ¿Qué contiene la imagen ingenua que no necesita el contenedor final? Nombra al menos tres elementos.
3. De lo que sobra en la imagen ingenua, ¿qué es solo peso innecesario y qué podría aumentar además la superficie de ataque?

No se evalúa que tus tiempos coincidan con los de otra persona. Se evalúa que expliques **tus propias mediciones**.

---

## Paso 7: Quítale privilegios

Modifica la imagen final para que Escaparate no se ejecute como `root`.

Ten en cuenta dos cosas:

- el proceso Java debe arrancar con un usuario sin privilegios;
- Escaparate utiliza almacenamiento de imágenes en filesystem, por lo que la imagen final debe definir una ruta mediante `APP_STORAGE_PATH` que sea escribible por ese usuario.

Vuelve a construir la imagen y comprueba desde dentro del contenedor qué usuario está ejecutando el proceso y que la ruta de almacenamiento es escribible.

**Comprueba:**

- la identidad no es `root`;
- `APP_STORAGE_PATH` tiene un valor definido y la ruta es escribible por ese usuario;
- el catálogo sigue funcionando;
- `/api/salud/listo` sigue respondiendo.

**Captura:** identidad del usuario y Escaparate funcionando.

---

## Paso 8: Publica y cierra la sesión

Si cerraste la sesión de GHCR al terminar la actividad 2.1, vuelve a autenticar **Docker** utilizando la misma credencial que usaste entonces. No necesitas crear otro token.

En el itinerario simplificado será el PAT `DAW - Curso` creado en la actividad 1.2. Si elegiste la alternativa de mínimo privilegio, utiliza el PAT classic específico de GHCR con permiso `write:packages`.

```bash
docker login ghcr.io -u <tu-usuario>
```

Cuando Docker solicite la contraseña, pega el PAT correspondiente, no tu contraseña de GitHub.

!!! info "Git y GHCR siguen siendo autenticaciones distintas"
    Este inicio de sesión afecta a Docker contra `ghcr.io`. No cambia la autenticación que utiliza Git para trabajar con el repositorio privado en `github.com`.

Publica la imagen final en GitHub Container Registry con dos etiquetas:

```text
ghcr.io/<usuario>/escaparate:sesion-04
ghcr.io/<usuario>/escaparate:latest
```

En esta actividad:

- `sesion-04` identifica el resultado concreto que acabas de validar;
- `latest` es una etiqueta móvil que podría apuntar a otra imagen en el futuro.

Configura el paquete como **público**.

!!! info "Repositorio privado, imagen pública"
    El repositorio `daw-despliegue` seguirá siendo privado durante el curso. La imagen de GHCR se hace pública para que pueda verificarse desde un equipo externo sin utilizar credenciales del alumno. En un proyecto real podría mantenerse privada y limitar el acceso a usuarios o sistemas autorizados.

!!! note "Todavía no son releases de Escaparate"
    Estas etiquetas identifican el resultado de tu práctica de empaquetado. Más adelante trabajarás con versiones reales de la aplicación y entonces aparecerán etiquetas de release como `v1.0.0` y `v2.0.0`.

### 8.1 Comprueba la descarga anónima

Cierra la sesión del registro:

```bash
docker logout ghcr.io
```

Elimina las referencias locales publicadas y vuelve a descargar la etiqueta fija:

```bash
docker image rm ghcr.io/<usuario>/escaparate:sesion-04
docker image rm ghcr.io/<usuario>/escaparate:latest
docker pull ghcr.io/<usuario>/escaparate:sesion-04
```

Si alguna referencia no puede eliminarse porque un contenedor la está utilizando, elimina primero ese contenedor.

**Comprueba:** el paquete aparece en GitHub con ambas etiquetas y `sesion-04` puede descargarse sin autenticación.

### 8.2 Cierra la rama

Antes de cerrar la sesión, revisa `actividad-2.2.md` y comprueba que contiene las respuestas, la tabla de mediciones, las reflexiones y las capturas solicitadas.

Después:

1. registra `Dockerfile.ingenuo`, `Dockerfile`, `.dockerignore` y la documentación de la actividad;
2. publica `sesion-04`;
3. abre una Pull Request hacia `main`;
4. revisa los cambios.

Antes de fusionar la Pull Request, haz una captura de la PR abierta, del paquete de GHCR con ambas etiquetas y del Markdown de la actividad renderizado. Guarda las capturas en `entregas/tema2/actividad-2.2/img/`, enlázalas desde `actividad-2.2.md` y registra y publica esos últimos cambios.

Comprueba que la Pull Request se actualiza con el nuevo commit. Cuando toda la documentación esté incluida, fusiónala mediante **Create a merge commit** y actualiza tu `main` local.

**Comprueba:**

- la PR aparece fusionada;
- `entregas/tema2/actividad-2.2/` está en `main`;
- `practicas/docker/app/Dockerfile.ingenuo`, `practicas/docker/app/Dockerfile` y `escaparate/.dockerignore` están en `main`;
- el grafo permite identificar la rama `sesion-04` y su fusión.

!!! question "Reflexiona"
    Si hoy descargas `latest` y mañana alguien vuelve a publicar una imagen distinta con esa misma etiqueta, ¿qué puede ocurrir la siguiente vez que ejecutes "la misma" referencia? ¿Cuál de las dos etiquetas utilizarías en un procedimiento que deba ser reproducible?

---

## Si te sobra tiempo

### Prueba la imagen de un compañero

Intercambia el nombre de la imagen con otra persona. Descarga su etiqueta `sesion-04` y ejecútala contra tu propia base de datos.

Si el catálogo aparece, estás ejecutando una aplicación Java compilada por otra persona sin tener que utilizar su entorno de desarrollo.

### Mira las capas

Utiliza el historial de la imagen para identificar las capas de mayor tamaño en la versión ingenua y en la optimizada.

---

## Verificación

Para dar por válida la práctica se podrá comprobar, sustituyendo `<usuario>`:

```bash
docker logout ghcr.io 2>/dev/null || true
docker rm -f verifica bd 2>/dev/null || true
docker network create escaparate-red 2>/dev/null || true

docker pull ghcr.io/<usuario>/escaparate-db:1.0.0
docker pull ghcr.io/<usuario>/escaparate:sesion-04

docker image ls ghcr.io/<usuario>/escaparate:sesion-04

docker run -d --name bd \
  --network escaparate-red \
  -e POSTGRES_USER=profesor \
  -e POSTGRES_PASSWORD=otra-distinta \
  -e POSTGRES_DB=escaparate \
  ghcr.io/<usuario>/escaparate-db:1.0.0

sleep 10

docker run -d --name verifica \
  --network escaparate-red \
  -p 8080:8080 \
  -e DB_HOST=bd \
  -e DB_PORT=5432 \
  -e DB_NAME=escaparate \
  -e DB_USER=profesor \
  -e DB_PASSWORD=otra-distinta \
  ghcr.io/<usuario>/escaparate:sesion-04

sleep 15

curl -fsS http://localhost:8080/api/salud/vivo
curl -fsS http://localhost:8080/api/salud/listo
docker exec verifica id
docker exec verifica sh -c 'printf "%s\n" "$APP_STORAGE_PATH"; test -w "$APP_STORAGE_PATH" && echo "storage writable"'

docker rm -f verifica bd
```

Y debe observarse:

- Las imágenes se descargan sin iniciar sesión.
- Escaparate arranca y sirve tanto el frontend como la API desde el mismo contenedor.
- `/api/salud/vivo` responde mientras el proceso está activo.
- `/api/salud/listo` confirma que PostgreSQL está disponible.
- El proceso no se ejecuta como `root`.
- `APP_STORAGE_PATH` está definido y es escribible por el usuario del contenedor.
- En el repositorio están `practicas/docker/app/Dockerfile.ingenuo`, `practicas/docker/app/Dockerfile` y `escaparate/.dockerignore`.
- En el repositorio están `entregas/tema2/actividad-2.2/actividad-2.2.md` y su carpeta `img/`.
- La entrega ha llegado a `main` mediante la Pull Request de `sesion-04`.

---

## Qué se entrega

- [ ] `entregas/tema2/actividad-2.2/actividad-2.2.md` con respuestas, mediciones y reflexiones.
- [ ] `entregas/tema2/actividad-2.2/img/` con las capturas enlazadas mediante rutas relativas.
- [ ] `practicas/docker/app/Dockerfile.ingenuo`.
- [ ] `practicas/docker/app/Dockerfile`.
- [ ] `escaparate/.dockerignore`.
- [ ] Tabla con las cuatro mediciones y el **Content Size** de ambas imágenes.
- [ ] Respuestas razonadas del paso 6.
- [ ] Comprobación de ejecución con usuario sin privilegios y almacenamiento escribible.
- [ ] Imagen pública en `ghcr.io` con `sesion-04` y `latest`.
- [ ] Pull Request `sesion-04 → main` fusionada.

!!! info "Dónde queda la entrega"
    La evidencia de la actividad queda versionada en el repositorio privado. El paquete de GHCR es público únicamente para permitir su comprobación sin credenciales.

---

## ✅ Cierre

Escaparate ya puede viajar como una imagen. Cualquier equipo con Docker puede descargarla y ejecutar la misma aplicación sin instalar Java, Maven ni las herramientas que utilizaste para compilarla.

También has comprobado que "meter la aplicación en Docker" no consiste solo en conseguir que arranque. El contexto que envías, el orden de las capas, las herramientas que dejas dentro y el usuario con el que ejecutas el proceso afectan al tiempo de construcción, al tamaño y a la seguridad de la imagen.

Sin embargo, poner en marcha el sistema completo todavía exige recordar varios comandos: crear una red, iniciar PostgreSQL con su configuración y después arrancar Escaparate con la suya. En la próxima sesión escribirás ese procedimiento en un fichero versionado para que el conjunto completo pueda levantarse de forma reproducible con un único comando.
