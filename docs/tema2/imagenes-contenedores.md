# 🏗️ Imágenes de contenedores

!!! info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/imagenes-contenedores.pptx){target="_blank" rel="noopener"}

---

En la sesión anterior construiste una imagen muy sencilla: elegiste una base con `FROM` y añadiste contenido con `COPY`.

Empaquetar una aplicación que antes debe **compilarse** introduce nuevos problemas. Ahora no basta con conseguir que la imagen funcione: también interesa que pueda reconstruirse con rapidez, que no incluya material innecesario y que ejecute la aplicación con los privilegios mínimos necesarios.

!!! abstract "Mapa de la sesión"
    **Dockerfile → contexto → caché → multietapa → runtime → publicación**

    Partirás de una imagen funcional y la irás mejorando. Cada cambio resolverá un problema distinto: **qué enviamos al builder, qué podemos reutilizar, qué debe llegar a producción y con qué privilegios se ejecuta**.

---

## 🧾 1. El Dockerfile describe la construcción

Un **Dockerfile** es un fichero de texto que indica cómo construir una imagen y cómo debe arrancar después el contenedor.

En esta sesión utilizarás sobre todo estas instrucciones:

| Instrucción | Función |
|---|---|
| `FROM` | elige una imagen de partida |
| `WORKDIR` | fija el directorio de trabajo |
| `COPY` | incorpora ficheros desde el contexto |
| `RUN` | ejecuta órdenes durante la construcción |
| `USER` | selecciona el usuario que ejecutará el proceso |
| `EXPOSE` | documenta el puerto esperado |
| `ENTRYPOINT` / `CMD` | indican qué se ejecuta al arrancar |

!!! info "Construcción y ejecución no son lo mismo"
    `RUN` se ejecuta **mientras se construye la imagen**. `ENTRYPOINT` y `CMD` describen qué ocurrirá **cuando arranque un contenedor** a partir de ella.

Otras instrucciones, como `ENV`, `ARG` o `LABEL`, también son habituales, pero no necesitas dominarlas todas hoy.

Usaremos como ejemplo una aplicación Java con Maven y Spring Boot que genera un **WAR ejecutable** y escucha en el puerto 8080. Son decisiones de este ejemplo: otros proyectos pueden generar un JAR, necesitar un servidor externo o utilizar otro lenguaje.

Una primera versión podría ser:

```dockerfile
FROM maven:3.9.16-eclipse-temurin-21-alpine
WORKDIR /app

COPY . .
RUN mvn -B package -DskipTests

ENTRYPOINT ["java", "-jar", "target/<artefacto>.war"]
```

Esta imagen puede funcionar, pero conserva Maven, el JDK y el código fuente junto al artefacto. La construcción multietapa permitirá separar **las herramientas para compilar** de **lo necesario para ejecutar**.

!!! info "Por qué aquí aparece `-DskipTests`"
    En esta sesión aislamos el problema de **construcción de la imagen**. Las pruebas se incorporarán explícitamente al proceso cuando trabajes con integración continua.

---

## 📤 2. Dockerfile, contexto y `.dockerignore`

Cuando ejecutas:

```bash
docker build -t mi-app:prueba .
```

el último argumento (`.`) indica el **contexto de construcción**: los ficheros que Docker puede utilizar durante el build.

El Dockerfile puede encontrarse en otro lugar.

### 2.1. El Dockerfile no tiene que estar dentro del contexto

Por ejemplo:

```bash
docker build \
  -f docker/Dockerfile \
  -t aplicacion:optimizada \
  aplicacion/
```

![El Dockerfile está en docker y el contexto es aplicacion. Su .dockerignore excluye target, .env y uploads; COPY pom.xml utiliza aplicacion/pom.xml.](img/dockerfile-contexto-dockerignore.png)

*Figura 1. El Dockerfile aporta las instrucciones; el contexto aporta los ficheros disponibles y .dockerignore los filtra.*

Con ese contexto, `COPY pom.xml .` busca `aplicacion/pom.xml`. El destino `.` corresponde al directorio de trabajo de la etapa, fijado mediante `WORKDIR`. Así podemos mantener el Dockerfile en `docker/` y construir con el contenido de `aplicacion/`.

Que un fichero forme parte del contexto **no significa que entre automáticamente en la imagen**: las instrucciones `COPY` seleccionan qué se incorpora.

### 2.2. `.dockerignore`: qué no debe entrar

No todo lo que existe dentro del contexto debe enviarse al builder.

Un `.dockerignore` colocado en la raíz del contexto puede excluir, por ejemplo:

```dockerignore
target/
.idea/
.vscode/
*.iml

.env
.env.*

uploads/
```

Esto ayuda a:

- reducir el contexto;
- evitar invalidaciones de caché por ficheros irrelevantes;
- disminuir el riesgo de incorporar accidentalmente contenido local o sensible.

!!! warning "`.gitignore` y `.dockerignore` no son lo mismo"
    `.gitignore` evita incorporar determinados ficheros no rastreados a Git; no elimina lo que ya está versionado. `.dockerignore` excluye ficheros del **contexto de construcción**.

---

## 🧊 3. Caché: evitar trabajo repetido

Docker intenta reutilizar resultados de builds anteriores. Para aprovechar esa caché, interesa separar las partes que cambian poco de las que cambian constantemente.

Un Dockerfile como este:

```dockerfile
COPY . .
RUN mvn -B package -DskipTests
```

depende de todo el contexto. Un cambio mínimo en una clase Java puede obligar a repetir pasos costosos.

Una organización más útil es:

```dockerfile
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests
```

![Al modificar solo src, la versión ingenua repite COPY y Maven. La optimizada reutiliza la copia de pom.xml y la preparación de dependencias, y repite la copia de src y la compilación.](img/docker-cache-reconstruccion.png)

*Figura 2. Reutilización de pasos al reconstruir con una caché previa disponible.*

El ejemplo supone que solo cambia `src/` y que siguen disponibles los resultados anteriores. Si cambia `pom.xml` y no existe una entrada de caché coincidente, se repiten los cuatro pasos mostrados de la versión optimizada.

!!! tip "Regla práctica"
    Coloca antes los pasos que dependen de ficheros **menos volátiles** y después los que dependen de contenido que cambia con frecuencia.

`dependency:go-offline` ayuda a anticipar muchas descargas de Maven, aunque algún plugin o dependencia adicional todavía podría resolverse durante la compilación.

### 3.1. Medir correctamente

En esta sesión compararás dos métricas distintas:

1. **tiempo de construcción**;
2. **tamaño de la imagen**.

Anota también si las imágenes base ya estaban descargadas y si había caché de builds anteriores. Una primera construcción y una reconstrucción tras cambiar código pueden partir de condiciones distintas. Para observar su efecto, conserva la caché entre las reconstrucciones durante la comparación.

En Linux puedes medir:

```bash
time docker build ...
```

y consultar después:

```bash
docker image ls aplicacion:ingenua
docker image ls aplicacion:optimizada
```

La columna `SIZE` representa el tamaño acumulado de la imagen y sus capas padre y permite comparar ambas si utilizas la misma métrica en todos los casos.

!!! info "Tamaño de imagen y uso total de disco no responden a la misma pregunta"
    `docker image ls` permite comparar imágenes concretas. `docker system df` muestra cuánto espacio utiliza Docker en conjunto y puede verse afectado por cachés, capas compartidas y otros objetos.

!!! warning "Caché y tamaño final son problemas distintos"
    Ordenar bien las instrucciones puede acelerar reconstrucciones. Eso **no elimina automáticamente Maven, el JDK o el código fuente de la imagen final**.

---

## 🪆 4. Construcción multietapa: compilar no es ejecutar

La etapa de construcción utiliza **Maven + JDK** para generar el WAR. La etapa final parte de una base con **JRE** y recibe el artefacto ejecutable.

![La etapa build genera el WAR con Maven y JDK. COPY --from=build lleva el WAR a una nueva base JRE. El artefacto conserva las bibliotecas de la aplicación y arranca Spring Boot con Tomcat embebido.](img/docker-multietapa-build-runtime.png)

*Figura 3. Solo el artefacto pasa de build a la etapa final; las herramientas y materiales de construcción quedan atrás.*

En este ejemplo, el WAR de Spring Boot es ejecutable: `java -jar /app/app.war` inicia Spring Boot y su **Tomcat embebido** en el puerto 8080. No necesita un Tomcat externo. Normalizamos el nombre del artefacto a `app.war` y suponemos que `target/*.war` coincide con un único archivo. **No todos los WAR se pueden arrancar con `java -jar`**: depende de cómo se hayan empaquetado.

Un Dockerfile multietapa puede expresar ese proceso así:

```dockerfile
# Etapa 1: construcción
FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests \
    && cp target/*.war /tmp/app.war

# Etapa 2: ejecución
FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24
WORKDIR /app

COPY --from=build /tmp/app.war /app/app.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.war"]
```

El segundo `FROM` inicia una **nueva etapa**. La imagen final no hereda automáticamente todo lo que había en la etapa anterior: solo recibe aquello que copiamos explícitamente con `COPY --from=build`.

!!! info "Dos niveles de empaquetado"
    Conviene distinguir:

    ```text
    WAR
    → artefacto de la aplicación Java

    imagen
    → unidad de despliegue con JRE + WAR
    ```

!!! note "Las dependencias necesarias no desaparecen"
    Las bibliotecas Java requeridas para ejecutar la aplicación siguen dentro del artefacto. Lo que dejamos atrás son **herramientas y materiales de construcción** que producción no necesita.

!!! tip "No confundas las dos optimizaciones"
    La construcción multietapa reduce principalmente **qué termina dentro de la imagen final**. La mejora del tiempo de reconstrucción procede sobre todo de la **caché** estudiada en el apartado anterior.

??? info "Para saber más: una etapa puede producir otros resultados"
    Una construcción puede incluir etapas destinadas a generar informes u otros artefactos sin incorporarlos a la imagen final.

    Docker permite incluso construir una etapa concreta mediante `--target`. No necesitas utilizarlo en esta sesión.

---

## 🛡️ 5. Una imagen preparada para ejecutar

Reducir el tamaño no es la única mejora. La imagen final debe ejecutar la aplicación con los recursos y privilegios que realmente necesita.

### 5.1. Usuario sin privilegios

Si no se configura otro usuario, muchos contenedores terminan ejecutando su proceso como `root`.

Supongamos que nuestra aplicación guarda ficheros subidos y admite la variable `APP_STORAGE_PATH` para elegir su carpeta. El ejemplo utiliza las órdenes de creación de usuarios de Alpine. En la etapa final, prepara ambos **antes de `USER app`**:

```dockerfile
RUN addgroup -S app \
    && adduser -S app -G app \
    && mkdir -p /data/uploads \
    && chown -R app:app /data

ENV APP_STORAGE_PATH=/data/uploads

USER app
```

![Durante el build se crean el usuario app y /data/uploads con propietario app:app. ENV indica la ruta y USER selecciona el usuario. En ejecución, Java lee el WAR y escribe en esa carpeta.](img/docker-usuario-ruta-escribible.png)

*Figura 4. Preparación del usuario, la configuración de almacenamiento y los permisos de escritura.*

`ENV` define una variable de entorno. La aplicación de este ejemplo está programada para interpretar `APP_STORAGE_PATH`; Docker no añade esa capacidad. La variable no crea la carpeta ni concede permisos. `USER app` selecciona el usuario creado previamente. Puedes comprobar tanto la identidad del proceso con `id` como la posibilidad de escribir en `APP_STORAGE_PATH`.

!!! warning "Escritura y persistencia"
    Preparar `/data/uploads` no crea un volumen. Si los datos quedan en la capa escribible del contenedor, se pierden al eliminarlo. La persistencia necesita un montaje apropiado, que configuraremos al describir el despliegue.

### 5.2. Runtime ajustado

La etapa final debe incluir únicamente lo necesario para ejecutar y diagnosticar razonablemente la aplicación.

| Base | Ventaja | Limitación |
|---|---|---|
| Imagen completa | más herramientas disponibles | más software innecesario |
| Variante mínima (`alpine`, `slim`) | menor tamaño habitual | pueden faltar herramientas o bibliotecas |
| Distroless | superficie muy reducida | diagnóstico interactivo más difícil |

En este módulo utilizaremos variantes mínimas que todavía permitan inspeccionar el contenedor cuando sea necesario.

!!! info "Una imagen también envejece"
    Aunque tu código no cambie, la imagen base y sus paquetes pueden recibir correcciones de seguridad. Por eso una estrategia real de mantenimiento incluye reconstruir y actualizar las imágenes periódicamente.

### 5.3. Cada optimización resuelve un problema diferente

Una imagen preparada para desplegar combina varias decisiones:

| Mejora | Problema que resuelve |
|---|---|
| `.dockerignore` | contexto innecesario o sensible |
| ordenar capas | reconstrucciones costosas |
| multietapa | herramientas de build en producción |
| runtime mínimo | tamaño y superficie de software |
| usuario sin privilegios | permisos excesivos |
| rutas escribibles controladas | escritura segura durante la ejecución |

No todas las mejoras persiguen lo mismo. Conviene decir siempre si estamos intentando reducir **tiempo**, **tamaño**, **privilegios** o **superficie de ataque**.

---

## 🏷️ 6. Identificar y publicar la imagen

Podemos publicar la misma imagen con dos etiquetas: `1.0.0`, que conservaremos **por convención** para identificar una versión, y `latest`, que podremos mover a otro resultado más adelante. El repositorio remoto del ejemplo es `ghcr.io/<usuario>/mi-app`; sustituye `<usuario>` por el propietario real.

![1.0.0 y latest apuntan inicialmente a la imagen A. Después latest apunta a la imagen B y 1.0.0 conserva A por convención. Cada contenido tiene su digest.](img/docker-etiquetas-digest.png)

*Figura 5. Las etiquetas son nombres reasignables; el digest identifica el contenido exacto. Los digests de la figura están abreviados.*

!!! warning "Una etiqueta no es técnicamente inmutable"
    Ambas etiquetas pueden reasignarse. `latest` no se actualiza sola ni garantiza ser la versión más reciente: depende de qué imagen publiquemos bajo ese nombre. Para referirnos al contenido exacto utilizamos el digest, con la forma `repositorio@sha256:...`.

Para publicar:

```bash
docker login ghcr.io -u <usuario>

docker tag aplicacion:optimizada \
  ghcr.io/<usuario>/mi-app:1.0.0

docker push ghcr.io/<usuario>/mi-app:1.0.0
```

La misma imagen puede recibir además `latest` sin duplicar su contenido:

```bash
docker tag aplicacion:optimizada \
  ghcr.io/<usuario>/mi-app:latest

docker push ghcr.io/<usuario>/mi-app:latest
```

---

## 🎯 Qué debes saber hacer al salir de esta sesión

Al terminar deberías poder:

- distinguir el **Dockerfile** del contexto de construcción;
- utilizar `.dockerignore` para reducir el contexto;
- ordenar instrucciones para aprovechar mejor la caché;
- diferenciar una mejora de **tiempo de reconstrucción** de una reducción de **tamaño final**;
- construir una imagen multietapa;
- explicar qué atraviesa la frontera entre etapa de build y etapa de runtime;
- ejecutar la aplicación con un usuario sin privilegios y una ruta escribible preparada;
- comparar de forma consistente tiempo y tamaño;
- publicar una imagen con una referencia estable por convención y otra móvil.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - El Dockerfile describe la construcción; el **contexto** determina qué ficheros pueden utilizar sus `COPY`.
    - `.dockerignore` reduce lo que Docker recibe durante la construcción.
    - La caché funciona mejor cuando colocamos primero lo que cambia menos.
    - **Caché** y **multietapa** resuelven problemas distintos: una reduce trabajo repetido; la otra reduce lo que llega a producción.
    - La aplicación Java del ejemplo puede compilarse con Maven + JDK y ejecutarse después únicamente con **JRE + WAR**.
    - El segundo `FROM` marca una nueva etapa; solo pasa a ella lo que copiamos explícitamente.
    - Ejecutar como usuario sin privilegios requiere preparar las rutas que la aplicación necesita escribir.
    - Una imagen más pequeña no es automáticamente «mejor»: cada optimización debe responder a un objetivo concreto.
    - `latest` es una etiqueta móvil; un digest identifica contenido exacto.

---

## 🧪 Conexión con la actividad 2.2

En la actividad partirás de una imagen funcional de Escaparate y la transformarás en una imagen más adecuada para despliegue, comparando con datos reales qué mejora cada decisión.
