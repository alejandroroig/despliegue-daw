# 🧪 Actividad 2.1: Ejecutar, inspeccionar y publicar

## Contexto

El equipo con el que trabajas ha decidido que, a partir de ahora, todo lo que se despliegue viajará empaquetado en imágenes. Antes de empaquetar la aplicación, te toca lo que le toca a todo el mundo: **manejar con soltura contenedores de otros, saber inspeccionarlos y no dejar el equipo lleno de restos**.

Como primer encargo relacionado con Escaparate vas a preparar algo pequeño pero útil: una imagen de PostgreSQL capaz de crear por sí sola la base de datos de pruebas del proyecto.

No recibirás una nueva versión de Escaparate para esta actividad. Utilizarás los scripts de base de datos que ya tienes en el repositorio desde la actividad 1.2.

## Qué vas a practicar

- **Ejecutar** contenedores publicando puertos y pasando configuración desde fuera.
- **Diagnosticar** utilizando logs y una shell dentro del contenedor.
- **Distinguir** imagen y contenedor observando qué cambia al detener, eliminar o recrear.
- **Construir** una imagen sencilla a partir de otra.
- **Publicar** una imagen propia en un registro y comprobar que puede descargarse desde fuera de tu equipo.

## Requisitos previos

- Docker funcionando en tu equipo. Comprueba que el motor responde:

```bash
docker version
```

Si la parte del servidor da error de permisos, avisa antes de seguir: tu usuario tiene que pertenecer al grupo que puede dar órdenes al demonio de Docker.

- El repositorio `daw-despliegue` creado en la actividad 1.2, clonado en tu equipo.
- Dentro de `escaparate/db/` deben estar:

```text
escaparate/
└── db/
    ├── 01-schema.sql
    └── 02-data.sql
```

- El PAT `DAW - Curso` disponible.
- Tu rama de esta sesión:

```bash
git switch main
git pull --ff-only
git switch -c sesion-03
```

!!! info "Documenta la actividad en el repositorio"
    Crea al comenzar:

    ```text
    entregas/
    └── tema2/
        └── actividad-2.1/
            ├── actividad-2.1.md
            └── img/
    ```

    Documenta únicamente las respuestas, reflexiones y resultados que se pidan. Guarda las **cuatro evidencias** solicitadas en `img/` e insértalas mediante rutas relativas.

---

## Paso 1: Ejecuta tu primer contenedor

Pon en marcha, en segundo plano, un contenedor a partir de `nginx:1.30.4-alpine`, publicado en el puerto **8080** y con un nombre reconocible.

Después comprueba qué contenedores tienes en marcha.

**Comprueba:** `http://localhost:8080` muestra la página de bienvenida de Nginx.

!!! tip "Fíjate en la etiqueta"
    No hemos escrito `nginx` a secas: hemos fijado versión y variante.

---

## Paso 2: Obsérvalo desde fuera y desde dentro

Recarga la página tres o cuatro veces y consulta los logs del contenedor.

Solicita después:

```text
http://localhost:8080/no-existe
```

y vuelve a mirar los logs.

**Comprueba:** distingues peticiones correctas y fallidas por su código de estado.

Abre una shell **dentro** del contenedor y averigua:

- qué sistema operativo utiliza;
- qué hay en el directorio desde el que Nginx sirve sus ficheros.

Modifica la página de bienvenida para que muestre un texto tuyo. Sal y recarga el navegador.

**Comprueba:** el navegador muestra tu texto.

**Evidencia 1:** una captura donde se vea el contenedor en ejecución y logs con peticiones correctas y una fallida.

---

## Paso 3: Imagen y contenedor no son lo mismo

### 3.1. Detén y vuelve a arrancar

Detén el primer contenedor, vuelve a arrancarlo y abre otra vez `http://localhost:8080`.

**Comprueba:** tu modificación sigue apareciendo.

### 3.2. Crea otro contenedor desde la misma imagen

Sin eliminar el primero, arranca un **segundo** contenedor desde `nginx:1.30.4-alpine`, con otro nombre y publicado en el puerto **8081**.

Abre:

```text
http://localhost:8080
http://localhost:8081
```

**Comprueba:**

- el primero muestra tu página modificada;
- el segundo muestra la página original de la imagen.

Modifica ahora la página del segundo contenedor con un texto distinto.

**Evidencia 2:** una captura donde se vea que dos contenedores creados desde la misma imagen muestran contenido diferente.

### 3.3. Elimina y recrea

Elimina el primer contenedor y vuelve a crearlo con el mismo nombre, el mismo puerto y **la misma imagen**.

**Comprueba:** la modificación del primer contenedor ya no existe.

En `actividad-2.1.md` responde:

1. ¿Por qué detener y volver a arrancar conserva la modificación?
2. ¿Por qué dos contenedores de la misma imagen pueden tener contenido diferente?
3. ¿Por qué eliminar y recrear hace desaparecer la modificación?
4. Si ese fichero fuera la base de datos de una tienda, ¿qué problema tendrías?

!!! question "La idea que debes conservar"
    ¿Dónde estaba realmente escrito tu cambio: en la imagen o en el contenedor?

---

## Paso 4: Limpia antes de construir

Detén y elimina los contenedores de Nginx.

Comprueba:

```bash
docker ps -a
```

No debe quedar ninguno de esta parte de la actividad.

??? info "Si te sobra tiempo: cuánto ocupa Docker"
    Ejecuta `docker system df` antes y después de limpiar y observa qué espacio ocupaban los recursos eliminados.

---

## Paso 5: Construye tu primera imagen

El objetivo es obtener una imagen de PostgreSQL que cree automáticamente las tablas de Escaparate y cargue los productos de ejemplo.

Los scripts están en:

```text
escaparate/db/
├── 01-schema.sql
└── 02-data.sql
```

La imagen oficial de PostgreSQL ejecuta durante la inicialización los scripts colocados en:

```text
/docker-entrypoint-initdb.d/
```

Crea:

```text
practicas/
└── docker/
    └── db/
        └── Dockerfile
```

El `Dockerfile` debe:

1. partir de `postgres:18-alpine`;
2. copiar los dos scripts a `/docker-entrypoint-initdb.d/`.

!!! info "Atención al contexto de construcción"
    El `Dockerfile` está en `practicas/docker/db/`, pero los scripts están en `escaparate/db/`.

    Al construir debes elegir un **contexto de construcción** desde el que Docker pueda acceder a esos ficheros. Los caminos de `COPY` se interpretan respecto al contexto, no respecto a la ubicación del `Dockerfile`.

Construye la imagen con la etiqueta:

```text
escaparate-db:1.0.0
```

**Comprueba:** la imagen aparece en el listado local con esa etiqueta.

!!! info "Inicializar no es migrar"
    Aquí personalizamos una imagen para aprender construcción e inicialización. En un proyecto real, la evolución del esquema suele gestionarse mediante migraciones versionadas como Flyway o Liquibase.

---

## Paso 6: Configúrala desde fuera

Arranca un contenedor de tu imagen indicándole al arrancar:

- usuario;
- contraseña;
- nombre de la base de datos.

Publica PostgreSQL en el puerto **5433**.

Conéctate desde dentro con `psql` y comprueba:

- que existe la tabla `productos`;
- que `SELECT count(*) FROM productos;` devuelve **8** registros.

Elimina ese contenedor y arranca otro **de la misma imagen** con un usuario y una contraseña diferentes.

**Comprueba:** el esquema sigue siendo el de Escaparate y la consulta devuelve **8 productos**.

**Evidencia 3:** captura del listado de tablas o de la consulta con resultado `8`, y del segundo arranque con credenciales distintas. No muestres contraseñas reales.

!!! question "Reflexiona"
    La misma imagen ha funcionado con dos juegos de credenciales distintos. **¿Dónde estaban esas credenciales si no estaban dentro de la imagen?** ¿Qué tendría de malo escribirlas en el `Dockerfile`?

---

## Paso 7: Publícala en GHCR

### 7.1. Prepara la credencial

Si seguiste la opción simplificada del curso, tu PAT classic `DAW - Curso` ya contiene:

```text
repo
write:packages
```

No necesitas crear otro token.

??? info "Si elegiste mínimo privilegio"
    Utiliza un PAT classic independiente para GHCR con `write:packages`.

### 7.2. Inicia sesión

```bash
docker login ghcr.io -u <tu-usuario>
```

Cuando Docker solicite la contraseña, utiliza el PAT correspondiente. No uses la contraseña normal de GitHub.

### 7.3. Etiqueta y publica

Etiqueta tu imagen como:

```text
ghcr.io/<tu-usuario>/escaparate-db:1.0.0
```

y publícala.

Después localiza el paquete en GitHub y configúralo como **público**.

### 7.4. Demuestra que no dependes de la copia local

Cierra la sesión del registro:

```bash
docker logout ghcr.io
```

Elimina los contenedores de prueba y las referencias locales:

```bash
docker image rm ghcr.io/<tu-usuario>/escaparate-db:1.0.0
docker image rm escaparate-db:1.0.0
```

Vuelve a descargarla **sin iniciar sesión**:

```bash
docker pull ghcr.io/<tu-usuario>/escaparate-db:1.0.0
```

Arráncala de nuevo y comprueba que crea correctamente las tablas y los **8 productos**.

**Evidencia 4:** página del paquete publicado y prueba de descarga/ejecución después de eliminar las referencias locales.

---

## Paso 8: Documenta y cierra

Crea:

```text
entregas/
└── tema2/
    └── actividad-2.1/
        └── docker-chuleta.md
```

Elige **cinco comandos** de los utilizados hoy. Para cada uno escribe:

- el comando;
- una línea explicando qué hace;
- una línea indicando cuándo lo utilizarías.

Revisa `actividad-2.1.md` y comprueba que contiene:

- las respuestas del Paso 3;
- la reflexión del Paso 6;
- las cuatro evidencias;
- cualquier incidencia relevante y cómo la resolviste.

Después:

1. registra los cambios;
2. publica `sesion-03`;
3. abre una Pull Request hacia `main`;
4. espera a que terminen las comprobaciones;
5. revisa los cambios;
6. fusiona mediante **Create a merge commit**;
7. actualiza tu `main` local.

**Comprueba:**

- la PR aparece fusionada;
- `entregas/tema2/actividad-2.1/` está en `main`;
- `practicas/docker/db/Dockerfile` está en `main`;
- el grafo muestra la rama `sesion-03` y su fusión.

---

## Verificación

Para dar por válida la práctica se podrá ejecutar:

```bash
docker logout ghcr.io 2>/dev/null || true
docker rm -f verifica 2>/dev/null || true
docker rmi ghcr.io/<usuario>/escaparate-db:1.0.0 2>/dev/null || true

docker pull ghcr.io/<usuario>/escaparate-db:1.0.0

docker run -d --name verifica -p 5434:5432   -e POSTGRES_USER=profesor   -e POSTGRES_PASSWORD=otra-distinta   -e POSTGRES_DB=escaparate   ghcr.io/<usuario>/escaparate-db:1.0.0

sleep 10

docker exec verifica psql -U profesor -d escaparate -c "\dt"
docker exec verifica psql -U profesor -d escaparate -c "SELECT count(*) FROM productos;"

docker rm -f verifica
```

Debe observarse:

- la imagen se descarga sin iniciar sesión;
- arranca con credenciales distintas;
- existe la tabla `productos`;
- la consulta devuelve **8** registros;
- `practicas/docker/db/Dockerfile` está versionado;
- `actividad-2.1.md` y `docker-chuleta.md` están en el repositorio;
- las cuatro evidencias están enlazadas mediante rutas relativas;
- ningún token ni contraseña real ha entrado en el repositorio;
- la entrega ha llegado a `main` mediante la PR de `sesion-03`.

---

## Qué se entrega

- [ ] `actividad-2.1.md` con respuestas, reflexiones y resultados.
- [ ] Carpeta `img/` con las cuatro evidencias.
- [ ] `docker-chuleta.md` con cinco comandos elegidos y explicados.
- [ ] Experimento que demuestra la diferencia entre detener, crear otra instancia y eliminar/recrear.
- [ ] `practicas/docker/db/Dockerfile`.
- [ ] Imagen `escaparate-db:1.0.0` funcionando con dos juegos de credenciales y **8 productos**.
- [ ] Imagen publicada como paquete público en `ghcr.io`.
- [ ] Pull Request `sesion-03 → main` fusionada.

!!! info "Dónde queda la entrega"
    La evidencia queda versionada en el repositorio privado. El paquete de GHCR es público únicamente para permitir su comprobación sin credenciales.

---

## ✅ Cierre

Al terminar ya sabes ejecutar una imagen, publicar puertos, pasar configuración desde fuera, leer logs, entrar en un contenedor y distinguir entre detenerlo, crear otra instancia, eliminarlo y recrearlo.

También has comprobado directamente que **modificar un contenedor no modifica la imagen de la que nació** y que dos contenedores creados desde la misma imagen pueden evolucionar de forma independiente.

Además, has construido y publicado una imagen sencilla relacionada con Escaparate. Los scripts de base de datos se han convertido en una pieza capaz de inicializar PostgreSQL automáticamente, mientras que las credenciales siguen estando fuera de la imagen.

En la próxima sesión harás lo mismo con la aplicación completa. Esta vez ya no bastará con copiar dos ficheros: Escaparate tendrá que compilarse con Java y Maven, y tendrás que decidir qué debe formar parte de la imagen final y qué debería quedarse fuera.
