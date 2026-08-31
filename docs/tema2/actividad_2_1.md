# 🧪 Actividad 2.1: Ejecutar, inspeccionar y publicar

## Contexto

El equipo con el que trabajas ha decidido que, a partir de ahora, todo lo que se despliegue viajará empaquetado en imágenes. Antes de empaquetar la aplicación, te toca lo que le toca a todo el mundo: **manejar con soltura contenedores de otros**, saber inspeccionarlos y no dejar el equipo lleno de restos.

Como primer encargo relacionado con Escaparate vas a preparar algo pequeño pero útil: una imagen de PostgreSQL capaz de crear por sí sola la base de datos de pruebas del proyecto.

No recibirás una nueva versión de Escaparate para esta actividad. Utilizarás los scripts de base de datos que ya tienes en el repositorio desde la actividad 1.2.

## Qué vas a practicar

- **Ejecutar** contenedores publicando puertos y pasando configuración desde fuera.
- **Diagnosticar** utilizando logs y una shell dentro del contenedor.
- **Comprobar** qué sobrevive y qué desaparece en distintos estados del ciclo de vida.
- **Construir** una imagen sencilla a partir de otra.
- **Publicar** una imagen propia en un registro.

## Requisitos previos

- Docker funcionando en tu equipo. Comprueba que el motor responde:

```bash
docker version
```

Si la parte del servidor da error de permisos, avisa antes de seguir: tu usuario tiene que pertenecer al grupo que puede dar órdenes al demonio de Docker.

- El repositorio `daw-despliegue` creado en la actividad 1.2, clonado en tu equipo.
- Dentro de `escaparate/db/` deben estar los scripts de inicialización de la base de datos:

```text
escaparate/
└── db/
    ├── 01-schema.sql
    └── 02-data.sql
```

- **El PAT** `DAW - Curso` **creado en la actividad 1.2 disponible**. Si elegiste la alternativa de mínimo privilegio, crearás en el paso 9 la credencial específica para GHCR.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main
git pull --ff-only
git switch -c sesion-03
```

!!! info "Documenta la actividad en el repositorio"
    Crea al comenzar la sesión:

    ```text
    entregas/
    └── tema2/
        └── actividad-2.1/
            ├── actividad-2.1.md
            └── img/
    ```

    Documenta en `actividad-2.1.md` las respuestas, reflexiones y resultados que se pidan durante la actividad. Guarda las capturas en `img/` e insértalas en el Markdown mediante rutas relativas.

    Los ficheros técnicos de despliegue no se duplican dentro de `entregas/`: permanecerán en `practicas/`, en la ubicación indicada en cada paso.

!!! info "Reparto de tiempo orientativo"
    - Pasos 1 a 4: unos 35 minutos. 
    - Pasos 5 y 6: unos 20 min. 
    - Pasos 7 a 10: unos 35 min.

---

## Paso 1: Arranca algo y publica su puerto

Pon en marcha, en segundo plano, un contenedor a partir de la imagen `nginx:1.30.4-alpine`, de forma que puedas verlo desde el navegador de tu equipo en el puerto **8080**. Dale un nombre reconocible en lugar de dejar que Docker invente uno.

Después comprueba desde el terminal qué contenedores tienes en marcha.

**Comprueba:** el navegador muestra la página de bienvenida del servidor en `http://localhost:8080`.

**Captura:** listado de contenedores en ejecución, con el nombre y el puerto visibles.

!!! tip "Fíjate en la etiqueta"
    No hemos escrito `nginx` a secas: hemos fijado versión y variante. A partir de ahora debes acostumbrarte a saber qué imagen concreta estás ejecutando.

---

## Paso 2: Lee los logs

Recarga la página del navegador tres o cuatro veces y pide después al contenedor que muestre su salida. Cada recarga debe aparecer en ella.

Ahora solicita una dirección que no exista, por ejemplo `http://localhost:8080/no-existe`, y vuelve a mirar los logs.

**Comprueba:** distingues las peticiones correctas de la que no lo era por su código de estado.

**Captura:** salida del contenedor con ambos tipos de petición.

---

## Paso 3: Entra dentro

Abre una shell **dentro** del contenedor y responde con hechos a dos preguntas:

- ¿Qué sistema operativo dice tener el contenedor?
- ¿Qué hay en el directorio desde el que Nginx sirve sus ficheros?

Ya que estás dentro, **modifica la página de bienvenida** para que muestre un texto tuyo. Sal y recarga el navegador.

**Comprueba:** el navegador muestra tu texto.

**Captura:** comandos ejecutados dentro del contenedor y página modificada.

---

## Paso 4: El experimento que hay que entender

Haz esta secuencia en orden y **anota después de cada paso si tu texto sigue apareciendo o no**:

1. Detén el contenedor y vuelve a arrancarlo.
2. Elimina el contenedor y crea uno nuevo, exactamente igual, a partir de la misma imagen.

**Captura:** navegador después de cada uno de los dos pasos.

!!! question "Reflexiona"
    Tu texto ha sobrevivido a una de las operaciones y no a la otra. **¿Dónde estaba escrito exactamente ese fichero y por qué la imagen de la que arrancan ambos contenedores no lo contiene?** Si en lugar de una frase fuera la base de datos de una tienda, ¿qué acabas de aprender?

---

## Paso 5: Dos contenedores, una imagen

Arranca un **segundo** contenedor de la misma imagen, con otro nombre y publicado en el puerto 8081. Modifica su página de bienvenida con un texto distinto.

**Comprueba:** `http://localhost:8080` y `http://localhost:8081` muestran textos diferentes.

**Captura:** las dos páginas y el listado de contenedores.

Documenta en `actividad-2.1.md`: si ahora eliminaras el primer contenedor, ¿qué le pasaría al segundo y por qué?

---

## Paso 6: Recoge

Antes de limpiar, apunta cuánto espacio están ocupando en tu equipo las imágenes y los contenedores. Después detén y elimina los dos contenedores, elimina la imagen descargada y vuelve a medir.

**Comprueba:** no queda ningún contenedor de la prueba, ni siquiera detenido.

**Captura:** medida de espacio antes y después.

!!! danger "La norma de la limpieza"
    Los equipos del aula los utilizan otros grupos. Toda práctica termina sin contenedores tuyos en ejecución y sin imágenes que ya no necesites. Este hábito también será importante en la nube, donde dejar recursos funcionando puede tener coste.

---

## Paso 7: Una base de datos que se inicializa sola

Hasta ahora has ejecutado imágenes creadas por otros. Ahora vas a construir una muy sencilla.

El objetivo es obtener una imagen de PostgreSQL que, al arrancar por primera vez con un almacenamiento vacío, cree las tablas de Escaparate y cargue los productos de ejemplo **sin ejecutar manualmente los scripts SQL**.

Los scripts ya están en:

```text
escaparate/db/
├── 01-schema.sql
└── 02-data.sql
```

La imagen oficial de PostgreSQL dispone de un mecanismo de inicialización: durante el primer arranque ejecuta los scripts que encuentra en un directorio concreto. **Busca en la documentación oficial de la imagen cuál es ese directorio** y anota de dónde has obtenido el dato.

Crea ahora:

```text
practicas/
└── docker/
    └── db/
        └── Dockerfile
```

El `Dockerfile` debe tener únicamente lo necesario para:

1. partir de `postgres:18-alpine`;
2. copiar los dos scripts de `escaparate/db/` al directorio de inicialización.

!!! info "Atención al contexto de construcción"
    El `Dockerfile` está en `practicas/docker/db/`, pero los ficheros que necesita están en `escaparate/db/`. Al construir la imagen tendrás que elegir un **contexto de construcción** desde el que Docker pueda acceder a esos scripts. Los caminos utilizados por `COPY` se interpretan respecto a ese contexto, no respecto a la ubicación del `Dockerfile`.

Construye la imagen local con la etiqueta:

```text
escaparate-db:1.0.0
```

**Comprueba:** la imagen aparece en el listado local con esa etiqueta exacta.

**Captura:** `Dockerfile`, comando utilizado para construir y salida de la construcción.

!!! info "Inicializar no es migrar"
    Aquí personalizamos una imagen de PostgreSQL para aprender cómo se construyen imágenes y cómo funciona su mecanismo de inicialización. En un proyecto real, la evolución del esquema suele gestionarse mediante migraciones versionadas, por ejemplo con Flyway o Liquibase. Hoy solo necesitas distinguir ambos problemas.

---

## Paso 8: Arráncala con las credenciales desde fuera

Pon en marcha un contenedor de tu imagen indicándole **al arrancar**:

- usuario;
- contraseña;
- nombre de la base de datos.

Publica PostgreSQL en el puerto **5433** de tu equipo.

Conéctate después desde dentro del propio contenedor utilizando `psql` y comprueba:

- que existe la tabla `productos`;
- que `SELECT count(*) FROM productos;` devuelve **8** registros.

Ahora elimina ese contenedor y arranca otro **de la misma imagen** con un usuario y una contraseña diferentes.

**Comprueba:** el esquema corresponde a Escaparate, la consulta devuelve **8 productos** y la misma imagen funciona con credenciales distintas.

**Captura:** listado de tablas, cuenta de productos y segundo arranque con las nuevas credenciales.

!!! tip "Por qué 5433 y no 5432"
    Podría existir ya un PostgreSQL instalado en la máquina escuchando en 5432. Publicarlo en otro puerto evita ese conflicto y te obliga a distinguir el puerto del anfitrión del puerto interno del contenedor.

!!! question "Reflexiona"
    La misma imagen ha funcionado con dos usuarios y dos contraseñas distintas. **¿Dónde estaban esas credenciales si no estaban dentro de la imagen?** ¿Qué tendría de malo escribirlas en el `Dockerfile` para no tener que proporcionarlas al arrancar?

---

## Paso 9: Publícala

Hasta ahora las imágenes solo han existido en tu equipo. Para que otra persona pueda descargarlas necesitas un registro de contenedores. Utilizaremos GitHub Container Registry (`ghcr.io`).

### 9.1 Prepara la credencial para GHCR

En la Actividad 1.2 aprendiste qué es un Personal Access Token y configuraste la autenticación de GitHub por HTTPS.

Si seguiste la **opción simplificada del curso**, tu PAT classic `DAW - Curso` ya contiene:

```text
repo
write:packages
```

Por tanto, **no necesitas crear otro token**. El permiso `write:packages`, que hasta ahora no habías utilizado, es el que permitirá publicar la imagen en GitHub Container Registry.

??? info "Si elegiste la alternativa de mínimo privilegio"
    Si en la Actividad 1.2 utilizaste un PAT *fine-grained* limitado a `daw-despliegue`, no lo reutilices para GHCR.

    GitHub Packages requiere actualmente un **Personal Access Token (classic)** para la autenticación manual. Crea ahora uno independiente:

    ```text
    GitHub
    → Settings
    → Developer settings
    → Personal access tokens
    → Tokens (classic)
    → Generate new token (classic)
    ```

    Utiliza:

    ```text
    Nombre: DAW - GHCR
    Caducidad: hasta el final del curso o la indicada por el profesor
    Scope: write:packages
    ```

    No necesita `delete:packages` ni acceso general a tus repositorios. Si al seleccionar `write:packages` GitHub marca también `repo` automáticamente, puedes abrir directamente:

    ```text
    https://github.com/settings/tokens/new?scopes=write:packages
    ```

    para crear el token con el ámbito de paquetes sin añadir acceso general a repositorios. Guarda el token en tu gestor de contraseñas y no lo incluyas en el repositorio ni en capturas.

### 9.2 Inicia sesión en GHCR

Git y Docker son clientes diferentes. Haber utilizado un PAT con `git push` no significa que Docker esté autenticado en `ghcr.io`.

En Linux, inicia sesión con:

```bash
docker login ghcr.io -u <tu-usuario>
```

Cuando Docker solicite la contraseña:

- si seguiste la opción simplificada, utiliza el mismo PAT `DAW - Curso`;
- si elegiste mínimo privilegio, utiliza el PAT `DAW - GHCR` creado para paquetes.

No utilices la contraseña normal de GitHub.

Si todo ha ido bien, Docker mostrará:

```text
Login Succeeded
```

!!! info "Dos servicios, dos sesiones"
    Git se autentica contra `github.com` y Docker contra `ghcr.io`. `docker login` y `docker logout` afectan a la sesión de Docker con el registro y **no cierran ni cambian la autenticación que utiliza Git para `pull` o `push`**.

!!! question "Reflexiona"
    La opción simplificada reutiliza un PAT classic con `repo` y `write:packages`. ¿Qué riesgo adicional supone que una sola credencial tenga ambas capacidades? ¿Cómo reduce ese riesgo la alternativa de separar el acceso al repositorio y al registro de contenedores?

### 9.3 Etiqueta y publica la imagen

Etiqueta tu imagen con el nombre completo del registro:

```text
ghcr.io/<tu-usuario>/escaparate-db:1.0.0
```
y publícala.

Después entra en GitHub, localiza el paquete recién creado y configúralo como **público**.

!!! info "Por qué el paquete es público"
    El repositorio `daw-despliegue` seguirá siendo privado durante el curso. El paquete de GHCR se hace público únicamente para que pueda verificarse desde un equipo externo sin utilizar credenciales del alumno. En un proyecto real podría mantenerse privado y limitar su acceso a usuarios o sistemas autorizados.

### 9.4 Comprueba que lo publicado funciona

Primero cierra la sesión de **Docker** con el registro:

```bash
docker logout ghcr.io
```

Esto no afecta a la autenticación de Git con `github.com`; podrás seguir utilizando `git pull` y `git push` normalmente.

Elimina después los contenedores de prueba que todavía utilicen la imagen, si los hubiera, y borra sus referencias locales:

```bash
docker image rm ghcr.io/<tu-usuario>/escaparate-db:1.0.0
docker image rm escaparate-db:1.0.0
```

Vuelve a descargarla **sin iniciar sesión**:

```bash
docker pull ghcr.io/<tu-usuario>/escaparate-db:1.0.0
```

La comprobación debe demostrar que utilizas realmente la copia publicada y que una persona ajena a tu cuenta puede obtenerla sin autenticarse.

**Comprueba:**

- el paquete aparece asociado a tu cuenta de GitHub;
- puede descargarse sin iniciar sesión una vez configurado como público;
- la imagen descargada arranca correctamente;
- los scripts crean las tablas y los productos igual que antes.

**Captura:** página del paquete publicado y arranque después de volver a descargarlo.

---

## Paso 10: Tu chuleta y cierre de la sesión

Crea:

```text
entregas/
└── tema2/
    └── actividad-2.1/
        └── docker-chuleta.md
```

Incluye **ocho comandos** de los utilizados hoy, elegidos por ti. Para cada uno escribe:

- el comando, con las opciones necesarias;
- una línea explicando qué hace;
- una línea indicando qué problema resuelve o cuándo lo utilizarías.

No copies una tabla de teoría. La utilidad está en seleccionar los comandos que realmente has necesitado y explicarlos con tus palabras.

Antes de cerrar la sesión, revisa `actividad-2.1.md` y comprueba que contiene todas las respuestas, reflexiones y capturas solicitadas.

Después sigue el flujo que ya utilizaste en la actividad 1.2:

1. registra los cambios de la sesión;
2. publica `sesion-03`;
3. abre una Pull Request hacia `main`;
4. revisa los cambios.

Antes de fusionar la Pull Request, haz una captura de la PR abierta y del fichero `docker-chuleta.md` renderizado. Guarda ambas en `entregas/tema2/actividad-2.1/img/`, enlázalas desde `actividad-2.1.md` y registra y publica esos últimos cambios.

Comprueba que la Pull Request se actualiza con el nuevo commit. Cuando toda la documentación esté incluida, fusiónala mediante **Create a merge commit** y actualiza tu `main` local.

**Comprueba:**

- la PR aparece fusionada;
- `entregas/tema2/actividad-2.1/` está en `main`;
- `practicas/docker/db/Dockerfile` está en `main`;
- el grafo permite identificar la rama `sesion-03` y su fusión.

---

## Si te sobra tiempo

Arranca un contenedor de tu imagen, **añade manualmente un producto nuevo** a la tabla y comprueba que existe. Elimina después ese contenedor y crea otro igual.

El producto añadido ya no debería estar, pero los productos de ejemplo volverán a aparecer.

Con ese resultado, responde: si PostgreSQL guardara sus ficheros en un almacenamiento que sobreviviera al contenedor, **¿volverían a ejecutarse los scripts de inicialización en cada arranque?** Razona qué tendría que comprobar la imagen antes de ejecutarlos.

---

## Verificación

Para dar por válida la práctica se podrá ejecutar, sustituyendo `<usuario>` por el correspondiente:

```bash
docker logout ghcr.io 2>/dev/null || true
docker rm -f verifica 2>/dev/null || true
docker rmi ghcr.io/<usuario>/escaparate-db:1.0.0 2>/dev/null || true

docker pull ghcr.io/<usuario>/escaparate-db:1.0.0

docker run -d --name verifica -p 5434:5432 \
  -e POSTGRES_USER=profesor \
  -e POSTGRES_PASSWORD=otra-distinta \
  -e POSTGRES_DB=escaparate \
  ghcr.io/<usuario>/escaparate-db:1.0.0

sleep 10

docker exec verifica psql -U profesor -d escaparate -c "\dt"
docker exec verifica psql -U profesor -d escaparate -c "SELECT count(*) FROM productos;"

docker rm -f verifica
```

Y debe observarse:

- La imagen se descarga sin necesidad de iniciar sesión.
- Arranca con unas credenciales distintas de las utilizadas por el alumno.
- Existe la tabla `productos` y la consulta devuelve **8** registros.
- En el repositorio está `practicas/docker/db/Dockerfile`.
- En el repositorio está `entregas/tema2/actividad-2.1/actividad-2.1.md`.
- En el repositorio está `entregas/tema2/actividad-2.1/docker-chuleta.md`.
- Las capturas están en `entregas/tema2/actividad-2.1/img/` y se enlazan mediante rutas relativas.
- Ningún token ni contraseña utilizada durante la práctica ha entrado en el repositorio.
- La entrega ha llegado a `main` mediante la Pull Request de `sesion-03`.

---

## Qué se entrega

- [ ] `entregas/tema2/actividad-2.1/actividad-2.1.md` con respuestas, reflexiones y resultados.
- [ ] `entregas/tema2/actividad-2.1/img/` con las capturas enlazadas mediante rutas relativas.
- [ ] `entregas/tema2/actividad-2.1/docker-chuleta.md`.
- [ ] Ciclo de vida documentado: arranque, puerto publicado, logs y shell.
- [ ] Experimento del paso 4 con explicación de la persistencia dentro del contenedor.
- [ ] Dos contenedores independientes creados desde una misma imagen.
- [ ] Medida de espacio antes y después de limpiar.
- [ ] `practicas/docker/db/Dockerfile`.
- [ ] Imagen `escaparate-db:1.0.0` funcionando con dos juegos de credenciales y **8 productos**.
- [ ] Imagen publicada como paquete público en `ghcr.io`.
- [ ] Pull Request `sesion-03 → main` fusionada.

!!! info "Dónde queda la entrega"
    La evidencia de la actividad queda versionada en el repositorio privado. El paquete de GHCR es público únicamente para permitir su comprobación sin credenciales.

---

## ✅ Cierre

Al terminar ya sabes ejecutar una imagen que te dan, publicar sus puertos, pasarle configuración desde fuera, leer sus logs, entrar en un contenedor y distinguir entre detenerlo, eliminarlo y volver a crearlo. También has comprobado que modificar un contenedor no modifica la imagen de la que nació.

Además, has construido y publicado una imagen sencilla relacionada con Escaparate. Los scripts de base de datos que ya estaban en tu repositorio se han convertido en una pieza que puede inicializar PostgreSQL automáticamente, mientras que las credenciales siguen estando fuera de la imagen y se deciden al arrancarla.

En la próxima sesión harás lo mismo con la aplicación de verdad. Esta vez ya no bastará con copiar dos ficheros: Escaparate tendrá que compilarse con Java y Maven, y tendrás que decidir qué debe formar parte de la imagen final y qué debería quedarse fuera.
