# 🧪 Actividad 3.1: Dos sitios, un servidor y un nombre

!!! warning "Descarga la plantilla"
    📄 [Plantilla 3.1 — Dos sitios, un servidor y un nombre](plantillas/Actividad_3_1_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

El despliegue de Escaparate funciona, pero se accede a él escribiendo `localhost:8080`, que es una dirección que solo existe en el ordenador donde está corriendo. Nadie de fuera del equipo puede usar eso, y el día que esto se mueva a un servidor de verdad —la semana que viene— dejará de valer del todo.

Además hay una deuda pendiente desde que empaquetaste la aplicación: al compilar Escaparate se generan la documentación del código y los informes de las pruebas. Existen, son útiles para el equipo y hasta ahora no han tenido dónde vivir. El encargo de hoy resuelve las dos cosas a la vez: **el catálogo y la documentación, servidos por el mismo servidor y en la misma máquina, pero en dos nombres distintos**, y la configuración de ese servidor versionada junto al proyecto como una parte más del despliegue.

## Qué vas a practicar

- **Publicar** un sitio bajo un nombre en lugar de una dirección, y comprobar con `dig` qué hay detrás de ese nombre.
- **Configurar** dos hosts virtuales por nombre en un único servidor, cada uno con su raíz de documentos.
- **Medir** el efecto real de activar la compresión, y comprobar por las cabeceras qué está haciendo el servidor.
- **Diagnosticar** los errores clásicos de un servidor de ficheros: permisos, raíz equivocada y tipo MIME.
- **Documentar** la configuración del servidor web como parte del procedimiento de despliegue.

## Requisitos previos

- La actividad 2.3 terminada: tu `compose.yaml` levantando el conjunto completo, con la base de datos y la API sin puertos publicados.
- El paquete de la actividad, que incluye `escaparate-docs.zip` con la documentación del código y los informes de pruebas de Escaparate.
- El fichero de configuración de Nginx que vienes usando desde la sesión 5, ya en tu repositorio.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main && git pull
git switch -c sesion-06
```

!!! danger "El bloque `/api` no se toca"
    En la configuración que arrastras hay un `location /api` con una directiva `proxy_pass`. Hoy sigue siendo caja negra: no lo modifiques ni lo muevas de sitio. Al terminar la actividad el catálogo tiene que seguir mostrando productos, y eso depende de esas líneas. La semana que viene lo escribes tú entero.

!!! info "Reparto de tiempo orientativo"
    Pasos 1 y 2, unos 35 minutos. Pasos 3 y 4, unos 40. Paso 5, unos 20.

---

## Paso 1 — Deja de escribir direcciones

Cambia dos cosas en tu despliegue: que el servidor web atienda en el **puerto de la web**, no en el 8080, y que se llegue a él por un **nombre**.

Para el nombre no hay que registrar nada. Usa `escaparate.127.0.0.1.nip.io`: es un servicio DNS comodín que devuelve la dirección que el propio nombre lleva dentro. Antes de tocar el navegador, resuélvelo con `dig` y mira qué te contesta: qué tipo de registro es, qué valor tiene y qué TTL trae. Repite la consulta un par de veces seguidas y fíjate en si el número cambia.

Después abre el catálogo en el navegador con ese nombre, sin puerto.

**Comprueba**: el catálogo se ve en `http://escaparate.127.0.0.1.nip.io/` y sigue mostrando los productos.
**Captura**: la salida completa de `dig` y el catálogo funcionando en el navegador, con el nombre visible en la barra de direcciones.

!!! question "Reflexiona"
    Ese nombre no lo has registrado tú y aun así resuelve desde cualquier equipo del aula. **¿Quién ha respondido a esa consulta y por qué la respuesta apunta a tu propia máquina?** Y sobre el TTL: ¿qué representa exactamente ese número y qué consecuencia tendría tenerlo muy alto el día que hay que cambiar a qué dirección apunta un nombre?

!!! warning "Si los nombres no resuelven"
    Puede ocurrir que el filtro de red del centro bloquee este tipo de comodines. En ese caso, añade a `/etc/hosts` **los dos nombres** que vas a usar hoy, ambos apuntando a `127.0.0.1`:

```text
    127.0.0.1 escaparate.127.0.0.1.nip.io
    127.0.0.1 docs.127.0.0.1.nip.io
```

    El navegador los usará sin problema, pero `dig` **seguirá preguntando al DNS y no leerá esas entradas locales**, así que su consulta continuará fallando. Anótalo en la plantilla y explica por qué las dos cosas son compatibles. Para no quedarte sin la parte de DNS, haz el `dig` del paso 1 contra cualquier nombre público que sí resuelva —el del centro, por ejemplo— e identifica ahí el tipo de registro y el TTL.

---

## Paso 2 — El segundo sitio

Descomprime `escaparate-docs.zip` en una carpeta del proyecto —llámala `sitio-docs/`— y consigue que el servidor la sirva bajo un nombre **distinto** del catálogo: `docs.127.0.0.1.nip.io`. Misma máquina, mismo puerto, mismo contenedor.

Dentro hay dos cosas: la documentación del código, que tiene su propia página de inicio, y la carpeta de informes de pruebas, que no la tiene. Para esa segunda el objetivo es que quien entre **vea el listado de ficheros** y pueda abrir cualquiera de ellos.

Una condición sobre el contenido: esos ficheros se generan al compilar, así que **no se versionan**. Igual que hiciste con el `.env`, deja la carpeta fuera del repositorio y documenta en el `README` de dónde sale.

**Comprueba**: `docs.127.0.0.1.nip.io` muestra la documentación de Escaparate y `docs.127.0.0.1.nip.io/informes/` muestra el listado de informes; mientras tanto, el catálogo sigue respondiendo en su nombre sin haber cambiado nada.
**Captura**: los dos sitios en el navegador, uno junto a otro, y la línea del `.gitignore` que excluye la carpeta.

!!! tip "Si aquí te sale un error, es uno de tres"
    Un `403` sobre un fichero que existe suele ser de permisos: el proceso del servidor no es administrador y necesita poder leer los ficheros y atravesar sus carpetas. Un `404` sobre un fichero que también existe casi siempre significa que la raíz de documentos no apunta donde crees o que el montaje no ha llegado dentro del contenedor; entra a mirarlo desde dentro. Y si la página aparece en crudo o sin estilos, abre la consola del navegador y mira qué `Content-Type` ha llegado.

!!! question "Reflexiona"
    Los dos nombres resuelven a la misma dirección IP y llegan al mismo puerto del mismo contenedor. **¿Qué información concreta usa el servidor para decidir cuál de los dos sitios responde, y en qué parte de la petición viaja?**

---

## Paso 3 — Cuando el nombre no coincide

Pide ahora al servidor una dirección con un nombre que no hayas configurado —vale cualquiera acabado en `.127.0.0.1.nip.io`— y observa qué te devuelve.

No es lo que la mayoría espera. Corrígelo: declara explícitamente un **servidor por defecto** cuya única misión sea responder con un **`404`** cuando el nombre pedido no coincida con ninguno de tus dos sitios.

**Comprueba**: un nombre no configurado devuelve `404` y ya no muestra ninguno de tus sitios; los dos nombres buenos siguen funcionando exactamente igual.
**Captura**: la respuesta a un nombre desconocido antes y después del cambio, con su código de estado en ambos casos.

---

## Paso 4 — Comprimir y cachear, y demostrarlo

Toca mejorar cómo se entregan los ficheros del catálogo. Aquí hay que usar **dos instrumentos distintos**, porque miden cosas distintas.

Para saber **qué cabeceras** devuelve el servidor:

```bash
curl -I -H "Accept-Encoding: gzip" http://escaparate.127.0.0.1.nip.io/<ruta-del-fichero>
```

Para saber **cuántos bytes viajan de verdad**, que es lo que `-I` no puede decirte porque no descarga el cuerpo:

```bash
curl -s -H "Accept-Encoding: identity" -o /dev/null \
  -w "sin comprimir: %{size_download} bytes\n" http://escaparate.127.0.0.1.nip.io/<ruta-del-fichero>

curl -s -H "Accept-Encoding: gzip" -o /dev/null \
  -w "comprimido:   %{size_download} bytes\n" http://escaparate.127.0.0.1.nip.io/<ruta-del-fichero>
```

Antes de modificar nada, usa `curl -I` sobre el fichero de estilos de tu front para comprobar que **todavía no se entrega comprimido**. Después, en la configuración del sitio del catálogo:

1. Activa la **compresión** para los tipos de contenido de texto: HTML, CSS y JavaScript. Deja fuera las imágenes.
2. Añade una **cabecera de caché** a los ficheros que no cambian a diario —estilos, scripts e imágenes— con un plazo largo, y asegúrate de que el HTML **no** hereda ese plazo.

Vuelve a consultar las cabeceras y, ahora sí, lanza las dos peticiones completas —`identity` y `gzip`— para medir la diferencia real de bytes. Rellena la tabla en la plantilla:

| Fichero | Bytes sin comprimir | Bytes comprimido | `Content-Encoding` | `Cache-Control` |
|---|---|---|---|---|
| Estilos | | | | |
| Una imagen del catálogo | | | | |
| La página del catálogo | | | | |

**Comprueba**: en la fila de los estilos las dos cifras de bytes son claramente distintas y aparece la cabecera de compresión; en la de la imagen las dos cifras son iguales y no aparece; y el plazo de caché de la página no es el mismo que el de los estilos.
**Captura**: la cabecera del fichero de estilos antes y después de activar la compresión, las dos mediciones finales de bytes, y la tabla rellena.

!!! question "Reflexiona"
    El fichero del disco no ha cambiado de tamaño y sin embargo por la red viaja mucho menos. **¿En qué momento exacto ocurre esa reducción y quién la deshace?** Y sobre la imagen: ¿por qué comprimirla habría sido gastar procesador para nada?

---

## Paso 5 — La configuración es parte del despliegue

Cierra la sesión dejando el trabajo en condiciones de que otra persona lo repita.

- **Valida** el fichero de configuración con la propia herramienta del servidor antes de aplicarlo, y aplica el cambio **recargando**, no reiniciando el contenedor. Comprueba que la recarga no ha cortado nada.
- Asegúrate de que la configuración se monta **de solo lectura** dentro del contenedor.
- Amplía el `README.md` con lo de hoy: los dos nombres publicados y qué sirve cada uno, el puerto en el que atiende el servidor, qué hay que descomprimir y dónde antes de levantar el conjunto, y cómo comprobar que ambos sitios responden.

Abre la petición de fusión de `sesion-06` hacia la rama principal.

**Comprueba**: la validación no da errores y, tras recargar, los dos sitios y el catálogo con sus productos siguen funcionando.
**Captura**: la salida de la validación, el `README` renderizado en el repositorio y la petición de fusión abierta.

---

## Si te sobra tiempo

**Cierra el círculo de la sesión 4.** Los informes y la documentación que hoy has servido salieron de una compilación de Escaparate, pero te los hemos dado hechos. Añade a tu `Dockerfile` una etapa que no acabe en imagen y extrae esos ficheros tú mismo con `--target` y `--output`, tal como se explicaba en el apunte de aquella sesión. Compara lo que obtienes con lo que se te ha entregado.

**URL amigables.** Configura el sitio del catálogo para que, cuando se pida una ruta que no existe como fichero, se sirva la página principal en lugar de un `404`. Es lo que necesita cualquier front que gestione la navegación por su cuenta, y se resuelve con una sola directiva que prueba varias rutas en orden.

---

## Verificación

Sobre un equipo limpio, partiendo de tu repositorio y del paquete de la actividad:

```bash
git clone <tu-repositorio> && cd daw-despliegue
cp .env.example .env                        # y se rellenarán los valores
unzip escaparate-docs.zip -d sitio-docs
docker compose up -d && sleep 20

docker compose exec front nginx -t
dig +short escaparate.127.0.0.1.nip.io

curl -s -o /dev/null -w "catalogo %{http_code}\n" http://escaparate.127.0.0.1.nip.io/
curl -s http://escaparate.127.0.0.1.nip.io/api/salud
curl -s -o /dev/null -w "docs %{http_code}\n" http://docs.127.0.0.1.nip.io/
curl -s http://docs.127.0.0.1.nip.io/informes/ | head
curl -s -o /dev/null -w "desconocido %{http_code}\n" \
  -H "Host: nada.127.0.0.1.nip.io" http://127.0.0.1/

curl -sI -H "Accept-Encoding: gzip" \
  http://escaparate.127.0.0.1.nip.io/<ruta del fichero de estilos indicada en tu README>
curl -s -H "Accept-Encoding: identity" -o /dev/null -w "identity %{size_download}\n" \
  http://escaparate.127.0.0.1.nip.io/<misma ruta>
curl -s -H "Accept-Encoding: gzip" -o /dev/null -w "gzip     %{size_download}\n" \
  http://escaparate.127.0.0.1.nip.io/<misma ruta>
```

Y debe observarse:

- Que el conjunto levanta **siguiendo solo el `README`**, incluido el paso de descomprimir la documentación.
- Que la configuración de Nginx es **válida** y está montada de solo lectura.
- Que el catálogo responde en su nombre, en el puerto de la web y **con sus productos**: la API sigue alcanzándose a través del servidor web.
- Que el sitio de documentación responde en su nombre y que la ruta de informes devuelve un **listado navegable**.
- Que un nombre no configurado devuelve **`404`** y no muestra ninguno de los dos sitios.
- Que el fichero de estilos llega con `Content-Encoding` y una cabecera de caché de plazo largo, y que **los bytes descargados con `gzip` son claramente menos** que con `identity`.
- Que en el repositorio no está la carpeta de documentación generada.
- Que la entrega llega a la rama principal **a través de una petición de fusión** desde `sesion-06`.

---

## Qué se entrega

- [ ] La consulta `dig` con su tipo de registro y su TTL, y la reflexión sobre quién responde.
- [ ] El catálogo servido bajo su nombre, en el puerto de la web y sin puerto en la URL.
- [ ] El segundo sitio bajo su propio nombre, con la documentación y el listado de informes.
- [ ] La reflexión sobre qué decide cuál de los dos sitios responde.
- [ ] El servidor por defecto devolviendo `404`, con la respuesta antes y después.
- [ ] La cabecera antes y después de activar la compresión, y las dos mediciones de bytes.
- [ ] La tabla del paso 4 completa, con su reflexión.
- [ ] El fichero de configuración de Nginx versionado y montado de solo lectura, y la carpeta generada excluida del repositorio.
- [ ] El `README` con los dos nombres, el puerto, el paso previo de descompresión y las comprobaciones.
- [ ] La petición de fusión de `sesion-06`, fusionada.
- [ ] La plantilla de la actividad entregada en Moodle, con la URL de la petición de fusión.

---

## ✅ Cierre

Tu despliegue ha dejado de ser una cosa que funciona en tu portátil para parecerse a un sitio publicado: se llega por un nombre, atiende en el puerto de la web, sirve dos sitios distintos desde una sola máquina y entrega sus ficheros comprimidos y con instrucciones de caché. Y la configuración que decide todo eso ya no es un fichero prestado: está en tu repositorio, se valida antes de aplicarse y se recarga sin cortar el servicio.

De paso has resuelto la deuda que arrastrabas desde que empaquetaste la aplicación. La documentación y los informes de pruebas existían desde la sesión 4 y no tenían dónde vivir; ahora tienen su propia dirección, y quien la necesite no tiene que pedírtela.

Lo que sigue habiendo es **una sola copia** de Escaparate. Si el contenedor de la API se para, no hay catálogo, y no hay nada que pueda hacer nada al respecto. En la próxima sesión el servidor web, además de seguir sirviendo los ficheros del front, pasará a repartir las peticiones de la API entre tres copias de la aplicación: ahí se abre por fin ese bloque `/api` que hoy no has tocado, y ahí aparece el primer problema serio de trabajar con varias copias a la vez. Y el conjunto se muda del portátil a la instancia que habrás creado el miércoles en el módulo de nube.