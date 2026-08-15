# 🧪 Actividad 2.1: Ejecutar, inspeccionar y publicar

!!! warning "Descarga la plantilla"
    📄 [Plantilla 2.1 — Ejecutar, inspeccionar y publicar](plantillas/Actividad_2_1_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

El equipo con el que trabajas ha decidido que, a partir de ahora, todo lo que se despliegue viajará empaquetado en imágenes. Antes de empaquetar nada tuyo, te toca lo que le toca a todo el mundo: **manejar con soltura contenedores de otros**, saber mirarlos por dentro y no dejar el disco lleno de restos.

Y como primer encargo real, algo pequeño y muy útil: el resto del equipo pierde media mañana cada vez que necesita una base de datos de Escaparate con datos de prueba. Vas a dejarles una imagen que lo haga sola.

## Qué vas a practicar

- **Ejecutar** contenedores publicando puertos y pasando configuración desde fuera.
- **Diagnosticar** con los logs y con una shell dentro del contenedor.
- **Comprobar** qué sobrevive y qué desaparece en cada estado del ciclo de vida.
- **Construir** y **publicar** una imagen propia en un registro.

## Requisitos previos

- Docker funcionando en tu equipo. Comprueba que el motor responde:

```bash
docker version
```

Si la parte del servidor da error de permisos, avisa antes de seguir: tu usuario tiene que pertenecer al grupo que puede dar órdenes al demonio.

- El repositorio `daw-despliegue` creado en la actividad 1.2, clonado en tu equipo.
- El fichero `init.sql` de Escaparate, que se te entrega con la actividad.
- **Sesión iniciada en el registro de contenedores de GitHub** (`ghcr.io`). Necesitas un token personal con permiso de escritura de paquetes: créalo al principio de la sesión y guárdalo donde guardas tus contraseñas. Es el registro que usarás el resto del curso, así que este trámite se hace una sola vez.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main && git pull
git switch -c sesion-03
```

!!! danger "Ese token no entra en el repositorio"
    Ni en un fichero, ni en una captura. Dale el permiso mínimo que necesita y ninguno más. En la sesión 13 verás que un pipeline no necesita que le des ningún token tuyo: la propia plataforma le presta uno temporal.

!!! info "Reparto de tiempo orientativo"
    Pasos 1 a 4, unos 35 minutos. Pasos 5 y 6, unos 20. Pasos 7 a 9, unos 35.

---

## Paso 1 — Arranca algo y publica su puerto

Pon en marcha, en segundo plano, un contenedor a partir de la imagen `nginx:1.27-alpine`, de forma que puedas verlo desde el navegador de tu equipo en el puerto **8080**. Dale un nombre reconocible en lugar de dejar que Docker le invente uno.

Después, comprueba desde el terminal qué contenedores tienes en marcha.

**Comprueba**: el navegador muestra la página de bienvenida del servidor en `http://localhost:8080`.
**Captura**: el listado de contenedores en ejecución, con el nombre y el puerto visibles.

!!! tip "Fíjate en la etiqueta"
    No hemos escrito `nginx` a secas: hemos fijado versión y variante. Es la norma del módulo desde hoy, y más adelante te tocará hacer lo mismo con la tuya.

---

## Paso 2 — Lee los logs

Recarga la página del navegador tres o cuatro veces y pide después al contenedor que te enseñe su salida. Cada recarga tiene que aparecer ahí.

Ahora pide una dirección que no exista en ese servidor, por ejemplo `http://localhost:8080/no-existe`, y vuelve a mirar la salida.

**Comprueba**: distingues en los logs las peticiones correctas de la que no lo era, por su código de estado.
**Captura**: la salida del contenedor con ambos tipos de petición.

---

## Paso 3 — Entra dentro

Abre una shell **dentro** del contenedor y contesta con hechos a dos preguntas: qué sistema operativo dice tener ahí dentro, y qué hay en el directorio desde el que el servidor sirve los ficheros.

Ya que estás dentro, **modifica la página de bienvenida** para que diga algo tuyo: tu nombre, la fecha, lo que quieras. Sal y recarga el navegador.

**Comprueba**: el navegador muestra tu texto.
**Captura**: los comandos ejecutados dentro y la página modificada en el navegador.

---

## Paso 4 — El experimento que hay que entender

Haz esta secuencia en orden y **anota después de cada paso si tu texto sigue apareciendo o no**:

1. Detén el contenedor y vuelve a arrancarlo.
2. Elimina el contenedor y crea uno nuevo, exactamente igual, a partir de la misma imagen.

**Captura**: el navegador después de cada uno de los dos pasos.

!!! question "Reflexiona"
    Tu texto ha sobrevivido a una de las dos operaciones y no a la otra. **¿Dónde estaba escrito exactamente ese fichero**, y por qué la imagen de la que arrancan los dos contenedores no lo tiene? Si en lugar de una frase en una página fuera la base de datos de una tienda, ¿qué acabas de aprender?

---

## Paso 5 — Dos contenedores, una imagen

Arranca un **segundo** contenedor de la misma imagen, con otro nombre y publicado en el puerto 8081. Modifica su página de bienvenida con un texto distinto.

**Comprueba**: `http://localhost:8080` y `http://localhost:8081` muestran textos diferentes.
**Captura**: las dos páginas y el listado de contenedores.

Responde en la plantilla: si ahora eliminaras el primer contenedor, ¿qué le pasaría al segundo y por qué?

---

## Paso 6 — Recoge

Antes de limpiar, apunta cuánto espacio están ocupando en tu equipo las imágenes y los contenedores. Después, detén y elimina los dos contenedores, elimina la imagen descargada y vuelve a medir.

**Comprueba**: no queda ningún contenedor, ni siquiera detenido.
**Captura**: la medida de espacio antes y después.

!!! danger "La norma de la limpieza"
    Los equipos del aula los usan otros grupos. Toda práctica termina sin contenedores tuyos corriendo y sin imágenes que no vayas a volver a usar. Este reflejo, además, te va a evitar un disgusto económico en el módulo de nube, donde lo que se queda encendido se factura.

---

## Paso 7 — Una imagen que se inicializa sola

Cambia de lado: hasta ahora has ejecutado imágenes de otros, y ahora vas a construir una.

El objetivo es una imagen de PostgreSQL que, al arrancar por primera vez, cree las tablas de Escaparate y meta los productos de ejemplo **sin que nadie ejecute nada a mano**. Y que no lleve dentro ni una sola contraseña.

La imagen oficial de PostgreSQL tiene un mecanismo de inicialización: si al arrancar por primera vez encuentra ficheros `.sql` en un directorio concreto, los ejecuta. **Busca en su página de Docker Hub cuál es ese directorio**, y anota de dónde has sacado el dato. Saber leer la documentación de una imagen ajena es parte del oficio, y hoy es la primera vez que lo haces.

Después crea una carpeta con el `init.sql` que se te ha entregado y un fichero `Dockerfile` de **dos instrucciones**: una que diga de qué imagen partes —`postgres:18-alpine`— y otra que copie el script a ese directorio. Constrúyela etiquetándola como `escaparate-db:1.0.0`.

**Comprueba**: la imagen aparece en el listado local con esa etiqueta exacta.
**Captura**: el `Dockerfile` y la salida de la construcción.

!!! info "Dos instrucciones y ya está"
    Hoy no vamos a explicar qué es un `Dockerfile`, cómo se ordenan sus instrucciones ni por qué unas son más caras que otras: eso es la sesión siguiente entera. Hoy solo interesa el efecto, que es enorme: acabas de convertir un fichero suelto en un paquete que cualquiera puede ejecutar sin saber nada de tu proyecto.

!!! info "Inicializar no es migrar"
    Aquí personalizamos una imagen de PostgreSQL porque queremos aprender cómo se construyen imágenes y cómo funciona su mecanismo de inicialización. En un proyecto real, la evolución del esquema de una base de datos suele gestionarse mediante migraciones versionadas —por ejemplo con herramientas como Flyway o Liquibase—, no construyendo una imagen nueva de PostgreSQL cada vez que cambia una tabla. No necesitas aprender esas herramientas hoy; basta con distinguir ambos problemas.

---

## Paso 8 — Arráncala con las credenciales desde fuera

Pon en marcha un contenedor de tu imagen indicándole **al arrancar** el usuario, la contraseña y el nombre de la base de datos, y publicándola en el puerto 5433 de tu equipo.

Conéctate a esa base de datos desde dentro del propio contenedor y comprueba que las tablas existen y que tienen filas.

Ahora elimina ese contenedor y arranca otro **de la misma imagen** con un usuario y una contraseña completamente distintos. Tiene que funcionar igual de bien.

**Comprueba**: el listado de tablas corresponde al esquema de Escaparate, las consultas devuelven productos, y la misma imagen sirve con dos credenciales distintas.
**Captura**: el listado de tablas, la cuenta de productos y el arranque con las credenciales nuevas.

!!! tip "Por qué el puerto 5433 y no el 5432"
    Por si algún día tienes un PostgreSQL instalado en la propia máquina escuchando en el puerto de siempre. Publicar en otro te evita un choque difícil de diagnosticar, y de paso te obliga a mirar bien el orden de los dos números.

!!! question "Reflexiona"
    La misma imagen ha funcionado con dos usuarios y dos contraseñas distintas. **¿Dónde estaban esas credenciales, si no estaban dentro de la imagen?** Y la consecuencia: ¿qué tendría de malo haberlas escrito en el `Dockerfile` para no tener que teclearlas cada vez?

---

## Paso 9 — Publícala

Etiqueta tu imagen para el registro de GitHub como `ghcr.io/<tu-usuario>/escaparate-db:1.0.0` y súbela. Configura el paquete como **público** en tu perfil.

Después **borra la copia local** y vuelve a descargarla desde el registro, para comprobar que lo que has publicado funciona de verdad.

**Comprueba**: el paquete aparece en tu perfil de GitHub, se descarga sin iniciar sesión y arranca en limpio.
**Captura**: la página del paquete publicado y el arranque tras volver a descargarla.

---

## Paso 10 — Tu chuleta

Crea en el repositorio el fichero `entregas/tema2/docker-chuleta.md` con **ocho comandos** de los que has usado hoy, elegidos por ti. Para cada uno:

- El comando, con las opciones que hayas necesitado.
- Una línea **escrita por ti** explicando qué hace.
- Una línea diciendo **qué problema te resuelve**, es decir, en qué situación lo querrías.

Nada de copiar la tabla del apunte: esa está para consultarla. La chuleta útil es la que escribes con tus palabras después de haber visto el efecto de cada comando, y elegir cuáles son los ocho importantes ya es parte del ejercicio.

Cierra la sesión abriendo la petición de fusión de `sesion-03` hacia la rama principal.

**Captura**: el fichero renderizado en el repositorio.

---

## Si te sobra tiempo

Arranca un contenedor de tu imagen, **añade a mano un producto nuevo** a la tabla y comprueba que está. Ahora elimina ese contenedor y arranca otro igual. El producto que añadiste ya no está, pero los de ejemplo sí.

Con eso en la cabeza, la pregunta de verdad: si esta base de datos guardara sus ficheros en un sitio que sobreviviera al contenedor, **¿volvería a ejecutarse el script de inicialización en cada arranque?** Piensa qué tendría que comprobar la imagen para decidirlo, y qué pasaría si se ejecutase dos veces sobre una base de datos que ya tiene productos.

---

## Verificación

Para dar por válida la práctica se ejecutará, sustituyendo `<usuario>` por el tuyo:

```bash
docker rmi ghcr.io/<usuario>/escaparate-db:1.0.0 2>/dev/null
docker run -d --name verifica -p 5434:5432 \
  -e POSTGRES_USER=profesor -e POSTGRES_PASSWORD=otra-distinta -e POSTGRES_DB=escaparate \
  ghcr.io/<usuario>/escaparate-db:1.0.0
sleep 10
docker exec verifica psql -U profesor -d escaparate -c "\dt"
docker exec verifica psql -U profesor -d escaparate -c "SELECT count(*) FROM productos;"
docker rm -f verifica
```

Y debe observarse:

- Que la imagen **se descarga sin necesidad de iniciar sesión** en el registro.
- Que arranca correctamente con unas credenciales que tú no has visto nunca.
- Que el listado de tablas corresponde al esquema de Escaparate y la cuenta de productos no es cero.
- Que en el repositorio está la chuleta con sus ocho comandos y la carpeta con el `Dockerfile`.
- Que la entrega llega a la rama principal **a través de una petición de fusión** desde `sesion-03`.

---

## Qué se entrega

- [ ] El ciclo de vida documentado: arranque con puerto publicado, logs, shell dentro.
- [ ] El experimento del paso 4, con el resultado de las dos operaciones y su reflexión.
- [ ] Los dos contenedores de la misma imagen y la explicación de su independencia.
- [ ] La medida de espacio antes y después de limpiar.
- [ ] El `Dockerfile` de dos instrucciones y la imagen construida.
- [ ] La imagen arrancando con dos credenciales distintas, con su reflexión.
- [ ] La imagen publicada en `ghcr.io` como paquete público, descargada en limpio.
- [ ] `entregas/tema2/docker-chuleta.md` con ocho comandos explicados con tus palabras.
- [ ] La petición de fusión de `sesion-03`, fusionada.

---

## ✅ Cierre

Al terminar sabes ejecutar cualquier imagen que te den, publicar su puerto, pasarle configuración desde fuera, leer sus logs, entrar por dentro y limpiar sin dejar restos. Y has visto con tus ojos dónde viven los datos que un contenedor escribe, que es la lección que más caro se paga cuando se aprende en producción.

También has construido y publicado tu primera imagen, aunque casi sin mirarla: dos instrucciones que han convertido un fichero suelto en algo que cualquiera del equipo puede arrancar sin saber nada de tu proyecto. Está en un registro público, con su versión, y funciona con credenciales que tú no controlas.

En la próxima sesión abrimos ese `Dockerfile` y empaquetamos la aplicación de verdad, que es bastante más difícil que copiar un fichero: hay que traer Java, compilar el proyecto y quedarse solo con el resultado. Descubrirás que el orden de las instrucciones decide cuánto tarda cada construcción, y que una imagen hecha de la forma obvia pesa un orden de magnitud más de lo necesario.