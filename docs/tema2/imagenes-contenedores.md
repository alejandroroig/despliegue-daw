# 🏗️ 2. Imágenes de contenedores

!!!info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/imagenes-contenedores.pptx){target="_blank" rel="noopener"}

---

En la sesión anterior construiste tu primera imagen con dos instrucciones y te pedimos expresamente que no te preguntaras qué eran. Funcionó porque partías de una imagen que ya lo traía todo hecho: PostgreSQL estaba instalado, configurado y listo, y tú solo dejaste caer un fichero dentro.

Empaquetar Escaparate es otra historia. Aquí no hay nadie que haya preparado la imagen por ti: hay que traer Java, compilar el proyecto, quedarse con el artefacto y dejar el resultado listo para arrancar. Hoy abrimos la receta, y vas a descubrir que escribirla es fácil y escribirla **bien** tiene bastante más miga: la diferencia entre una imagen ingenua y una imagen decente son cientos de megabytes, varios minutos en cada construcción y unos cuantos agujeros de seguridad.

---

## 🧾 El Dockerfile es la receta

Un **Dockerfile** es un fichero de texto con las instrucciones para construir una imagen, en orden. Cada instrucción produce **una capa** de las que viste la sesión pasada, apilada sobre la anterior.

| Instrucción | Qué hace | Lo que conviene saber |
|---|---|---|
| `FROM` | Fija la imagen base de la que partes | Siempre con etiqueta concreta, nunca `latest` |
| `WORKDIR` | Establece el directorio de trabajo | Lo crea si no existe; mejor que ir encadenando rutas |
| `COPY` | Copia ficheros desde tu proyecto a la imagen | Prefiérela a `ADD`, que además descarga URL y descomprime: más magia de la que quieres |
| `RUN` | Ejecuta un comando **durante la construcción** | Cada `RUN` es una capa; lo que borres en una capa posterior sigue ocupando en la anterior |
| `ENV` | Define variables de entorno dentro de la imagen | Para valores por defecto inocuos, **nunca** para credenciales |
| `ARG` | Variable disponible solo mientras se construye | Tampoco sirve para secretos: queda en el historial |
| `USER` | Cambia el usuario para todo lo que venga después | Si no la pones, tu aplicación corre como administrador |
| `EXPOSE` | Documenta qué puerto usa la aplicación | **No publica nada**: es informativa |
| `CMD` | Comando por defecto al arrancar el contenedor | Se puede sustituir entero al ejecutar |
| `ENTRYPOINT` | Proceso que se ejecuta siempre | Lo que pases al ejecutar se le añade como argumentos |
| `LABEL` | Metadatos en forma de clave y valor | Útil para dejar autoría, versión y enlace al repositorio |

Con eso, un primer intento de empaquetar Escaparate podría ser este:

```dockerfile
FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
COPY . .
RUN mvn package -DskipTests
CMD ["java", "-jar", "target/escaparate.war"]
```

Funciona. Parte de una imagen que trae Maven y el JDK de Java 21, copia el proyecto entero, lo compila y deja dicho que al arrancar se ejecute el artefacto resultante. Si lo construyes y lo ejecutas, Escaparate responde.

Y sin embargo tiene **cuatro problemas graves**, uno por sección de las que vienen.

---

## 📤 El contexto de construcción

Cuando lanzas la construcción, el último argumento del comando no significa «aquí»: significa **«empaqueta esta carpeta entera y mándasela al motor de Docker»**. Eso es el *contexto de construcción*, y es lo único que las instrucciones `COPY` pueden ver.

```bash
docker build -t escaparate:1.0.0 .
```

Ese punto final está diciendo «el contexto es el directorio actual». Si tu proyecto arrastra la carpeta de compilaciones anteriores, el historial de Git, dependencias descargadas y un fichero de configuración con contraseñas, **todo eso se empaqueta y se envía**, aunque el Dockerfile no lo use. Las consecuencias son tres:

- **Lentitud.** Cada construcción empieza copiando cientos de megabytes que no hacen falta.
- **Filtraciones.** Un `COPY . .` mete dentro de la imagen todo lo que haya, incluidos ficheros de credenciales que creías que no estaban ahí. Y una vez publicada la imagen, ese fichero está publicado.
- **Caché rota.** Como verás en un momento, si el contexto cambia, la caché deja de servir. Y el historial de Git cambia con cada commit.

La solución es un fichero `.dockerignore` en la raíz del proyecto, con la misma lógica que el `.gitignore` de la sesión 2:

```text
.git
target/
*.md
.env
```

Se lee así: fuera el historial de versiones, fuera los resultados de compilaciones anteriores hechas en tu máquina —el proyecto se compila **dentro** de la imagen, no fuera—, fuera la documentación que no se despliega y fuera cualquier fichero de variables locales.

!!! warning "Los dos ficheros no son el mismo"
    `.gitignore` decide qué no se versiona; `.dockerignore` decide qué no se empaqueta. Se parecen y comparten muchas líneas, pero hay cosas que quieres versionar y no meter en la imagen (la documentación) y cosas que quieres meter en la imagen y no versionar (rara vez, pero pasa). Escríbelos por separado.

---

## 🧊 Capas y caché: por qué el orden lo es todo

Al construir, Docker guarda cada capa y, en la siguiente construcción, reutiliza las que no hayan cambiado. La regla es implacable: **cuando una capa se invalida, todas las que vienen detrás se reconstruyen**, hayan cambiado o no.

Y una capa `COPY` se invalida en cuanto cambia **el contenido** de lo que copia. Vuelve al primer intento:

```dockerfile
COPY . .
RUN mvn package -DskipTests
```

Cambias una coma en un fichero de código, reconstruyes, y como el contexto ha cambiado se invalida el `COPY`; como se invalida el `COPY`, se invalida el `RUN`; y como se invalida el `RUN`, Maven vuelve a **descargar desde cero todas las dependencias del proyecto**. Cuatro minutos, cada vez, por una coma.

El arreglo consiste en separar lo que cambia poco de lo que cambia mucho, y ponerlo en ese orden:

```dockerfile
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package
```

Primero se copia **solo** el fichero que declara las dependencias, que cambia una vez al mes. Después se descargan esas dependencias, y esa capa queda en caché. Solo entonces se copia el código fuente, que cambia cada cinco minutos, y se compila. Ahora, cuando toques una coma, se invalidan las dos últimas capas y las dependencias siguen ahí: la construcción baja de minutos a segundos.

!!! tip "La regla, en una frase"
    Ordena las instrucciones de **menos volátil a más volátil**. Lo que casi nunca cambia, arriba; lo que cambia en cada commit, abajo. Es el mismo criterio que hace que no vuelvas a poner los cimientos cada vez que cambias un cuadro de sitio.

!!! warning "Lo que borras en un `RUN` posterior no adelgaza la imagen"
    Si una capa instala herramientas de compilación y otra las desinstala, la primera capa sigue existiendo con todo su peso: la segunda solo añade una marca de borrado encima. Por eso las limpiezas van encadenadas dentro del mismo `RUN`… o, mejor todavía, se resuelven con lo que viene ahora.

---

## 🪆 Construcción multietapa

Aquí está el segundo problema del primer intento, y es el gordo. Para **compilar** Escaparate hacen falta Maven, el JDK completo y todas las dependencias descargadas. Para **ejecutarlo** solo hace falta un entorno de ejecución de Java y el artefacto. Esa imagen ingenua lleva a producción el compilador, el código fuente y el repositorio de dependencias: cientos de megabytes de cosas que no solo sobran, sino que **son superficie de ataque**.

La construcción multietapa resuelve esto declarando varias imágenes en el mismo Dockerfile y quedándose solo con la última:

```dockerfile
# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S escaparate && adduser -S escaparate -G escaparate
WORKDIR /app
COPY --from=build /app/target/escaparate.war app.war
USER escaparate
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.war"]
```

Línea a línea, lo que importa:

- `FROM … AS build` **nombra** la primera etapa. Todo lo que ocurra en ella —Maven, el JDK, el código fuente, las dependencias— vive únicamente durante la construcción.
- El segundo `FROM` **empieza de cero**: descarta todo lo anterior y arranca sobre una imagen que solo trae el entorno de ejecución de Java, sin compilador.
- `COPY --from=build` es la costura entre ambas: rescata de la primera etapa **exclusivamente el artefacto** y lo mete en la imagen final.
- El `RUN` con `addgroup`/`adduser` y el `USER` posterior son el asunto de la sección siguiente.
- `ENTRYPOINT` fija el proceso que se ejecutará siempre al arrancar el contenedor.

El resultado: la imagen final no contiene ni Maven, ni el compilador, ni tu código fuente. Solo un entorno de ejecución y un artefacto. La diferencia de tamaño entre las dos versiones es de un orden de magnitud, y la vas a medir tú mismo en la actividad —conviene que el número lo veas en tu pantalla y no en un apunte—.

---

## 🎯 Etapas que no acaban en imagen

Una etapa intermedia no tiene por qué existir solo para alimentar a la final: puede servir para **extraer ficheros** de la construcción sin llevártelos a producción.

El caso típico es justo el tuyo. Al compilar Escaparate se generan informes de pruebas y documentación del código. Eso no debe ir dentro de la imagen de la aplicación —no lo necesita para funcionar y no quieres publicarlo con ella—, pero sí quieres tenerlo en disco para poder servirlo aparte. Es la misma tensión que viste en la sesión 2 con la documentación del código: un artefacto que se produce al construir, que no se versiona y que alguien tiene que publicar en algún sitio.

Guarda la idea, porque en la sesión 6 vas a servir esos informes desde Nginx, en una ruta distinta de la aplicación.

!!! info "Para saber más: cómo se extraen sin generar imagen"
    Docker permite construir solo hasta una etapa concreta y volcar su contenido en una carpeta de tu disco en lugar de guardarlo como imagen:

    ```dockerfile
    FROM scratch AS informes
    COPY --from=build /app/target/site ./
    ```

    ```bash
    docker build --target informes --output type=local,dest=./informes .
    ```

    `scratch` es una imagen literalmente vacía, que aquí no sirve para ejecutar nada sino solo para contener ficheros; `--target` dice hasta qué etapa construir; y `--output type=local` escribe el resultado en disco. Es la forma limpia de sacar artefactos de una construcción, y la verás en pipelines reales. No lo vas a necesitar en la actividad de hoy.

---

## 🛡️ El usuario y la base: los dos descuidos por defecto

**Por defecto, todo lo que corre dentro de un contenedor corre como administrador.** Es cómodo mientras experimentas y es una mala idea en cuanto la cosa sale de tu máquina: si alguien consigue ejecutar código a través de un fallo de tu aplicación, lo hace con todos los permisos dentro del contenedor, y desde ahí las posibilidades de saltar al anfitrión son mucho mayores que si fuera un usuario cualquiera.

El arreglo son dos líneas: crear un usuario sin privilegios y declararlo con `USER` antes de que arranque la aplicación. A partir de ahí, el proceso no puede escribir donde no debe ni instalar nada. Y si tu aplicación necesita escribir en algún sitio, tendrás que dárselo explícitamente, que es exactamente lo que quieres: obligarte a saber qué escribe y dónde.

La otra decisión es **de qué base partes**, y va del mismo asunto: cada paquete instalado en la imagen es código que no has escrito, que no usas y que puede tener fallos conocidos.

| Base | Tamaño | A cambio |
|---|---|---|
| Distribución completa (Debian, Ubuntu) | Cientos de MB | Todo funciona, todas las herramientas están, muchísimo paquete que no usas |
| Variante mínima (`-slim`, `alpine`) | Decenas de MB | Menos superficie y descargas más rápidas; alguna incompatibilidad ocasional con bibliotecas nativas |
| Sin distribución (`distroless`, `scratch`) | Mínimo | Superficie mínima, pero **no hay shell**: no puedes entrar a mirar cuando algo falla |

Para este módulo, las variantes mínimas son el punto dulce: bastante más ligeras y todavía depurables, y son las que vas a usar. Pero apunta el peaje de la última fila, porque es una permuta real: cuanto más segura y pequeña es la imagen, más ciego te quedas el día que hay que diagnosticar algo dentro. Que exista esa tercera fila, y por qué hay empresas que la eligen, es suficiente por ahora.

---

## 🔍 Vulnerabilidades: quién te las mete

Existen herramientas que analizan una imagen y listan las vulnerabilidades conocidas de todo lo que hay dentro: base, bibliotecas del sistema y dependencias de tu proyecto. Cada una tiene su identificador público, su gravedad y, casi siempre, la versión en la que se corrigió.

Lo llamativo la primera vez que lo ejecutas es que **la inmensa mayoría de los hallazgos no son culpa de tu código**: vienen de la imagen base y de las bibliotecas que arrastra. De ahí salen las dos consecuencias prácticas del apartado anterior: partir de una base mínima reduce la lista drásticamente, y **una imagen no se hace más segura con el tiempo, sino menos**, porque cada semana se descubren fallos en software que ya estaba dentro. Reconstruir periódicamente sobre una base actualizada no es mantenimiento opcional: es la única forma de que la imagen siga siendo aceptable dentro de seis meses.

Hoy solo lo vas a ver funcionar. El análisis en serio, con corrección de hallazgos, llega en la sesión 8 junto al resto de la seguridad del despliegue.

---

## 🏷️ Etiquetar y publicar

Ya sabes que una imagen se nombra `usuario/nombre:etiqueta`. Lo que toca decidir ahora es **qué pones en la etiqueta**, y la respuesta viene de la sesión 2: el mismo versionado semántico que usas para marcar el código.

La práctica habitual es publicar la misma imagen con varias etiquetas a la vez: una **inmutable**, que identifica esa construcción concreta y no se reutiliza jamás (`1.2.0`), y una o varias **móviles**, que van saltando a la última construcción de una serie (`1.2`, `1`, o `latest`).

!!! danger "En producción se despliega la inmutable"
    Si tu servidor arranca la etiqueta móvil, dos despliegues idénticos pueden acabar ejecutando código distinto y no habrá forma humana de saber qué versión estaba corriendo cuando ocurrió el incidente. Las etiquetas móviles son una comodidad para desarrollar; el despliegue reproducible referencia una versión exacta. Cuando quieras la máxima precisión, existe además el **digest**: la huella criptográfica del contenido de la imagen, que identifica un contenido exacto aunque alguien reescriba la etiqueta.

Publicar consiste en iniciar sesión en el registro, etiquetar la imagen con tu usuario y subirla. A partir de ese momento, cualquier máquina con acceso puede ejecutarla **sin tener tu código, ni Maven, ni Java**. Ese es el momento en que tu trabajo deja de vivir en tu ordenador: es, literalmente, el punto número dos de la lista de la primera sesión, resuelto.

---

## 🎯 Qué debes saber hacer al salir de esta sesión

- Escribir un `Dockerfile` que compile un proyecto Java y deje la aplicación lista para arrancar.
- Dejar fuera del contexto de construcción lo que no debe viajar, con un `.dockerignore`.
- Ordenar las instrucciones para que la caché sirva de algo, y explicar qué se reconstruye y qué no.
- Escribir una construcción multietapa y justificar la diferencia de tamaño con la versión ingenua.
- Hacer que la aplicación no corra como administrador dentro del contenedor.
- Publicar la imagen con una etiqueta inmutable y una móvil, y saber cuál se despliega.

Lo que basta con reconocer: la extracción de artefactos con `--target`, las bases sin distribución, el escaneo de vulnerabilidades —que se trabaja en la sesión 8— y el detalle del historial de capas.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - Un `Dockerfile` es la receta de la imagen y cada instrucción produce una capa. `EXPOSE` no publica nada, `ADD` hace más magia de la que quieres y `ARG`/`ENV` no sirven para secretos: quedan en el historial.
    - El contexto de construcción es la carpeta entera que se envía al motor, no solo lo que copias. Un `.dockerignore` evita que viajen el historial de Git, las compilaciones locales y los ficheros con credenciales.
    - La caché funciona por capas y en cascada: cuando una se invalida, todas las siguientes se reconstruyen. Ordena las instrucciones de menos volátil a más volátil, y copia el fichero de dependencias antes que el código.
    - Lo que borras en una capa posterior no reduce el tamaño: la capa anterior sigue ahí con todo su peso.
    - La construcción multietapa compila en una imagen con todas las herramientas y copia **solo el artefacto** a una imagen final mínima. Ni compilador, ni código fuente, ni dependencias en producción.
    - Una etapa intermedia también puede servir para extraer ficheros al disco sin generar imagen, usando `--target` y salida local: así se sacan los informes y la documentación de la construcción.
    - Por defecto el proceso corre como administrador dentro del contenedor. Crear un usuario sin privilegios y declararlo con `USER` cuesta dos líneas.
    - Cada paquete de la imagen base es superficie de ataque. Las variantes mínimas reducen mucho la lista de vulnerabilidades; las imágenes sin distribución la reducen más, a costa de quedarte sin shell para depurar.
    - La mayoría de vulnerabilidades vienen de la base, no de tu código, y una imagen se degrada con el tiempo: hay que reconstruirla periódicamente sobre una base actualizada.
    - Publica cada construcción con una etiqueta inmutable, y despliega siempre esa. Las etiquetas móviles valen para desarrollar; para producción, versión exacta o digest.

---

Con esto ya tienes las piezas para la **Actividad 2.2**. Vas a empaquetar Escaparate dos veces: primero de la forma ingenua y después con construcción multietapa, midiendo los tiempos y los tamaños de las dos para que el argumento sea tuyo y no mío. Por el camino tocarás una coma en el código y reconstruirás, que es la forma más rápida de entender para qué sirve la caché.

Después le quitarás los privilegios a la aplicación y publicarás la imagen en el registro con sus dos etiquetas. Al terminar, Escaparate será un paquete que cualquiera puede descargar y arrancar sin tener instalado ni Java ni Maven ni tu código.