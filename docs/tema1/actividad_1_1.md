# 🧪 Actividad 1.1: Qué se puede saber de un despliegue desde fuera

!!! warning "Descarga la plantilla"
    📄 [Plantilla 1.1 — Qué se puede saber de un despliegue desde fuera](plantillas/Actividad_1_1_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

Es tu primer día en una empresa que despliega y mantiene las aplicaciones web de sus clientes. Antes de dejarte tocar nada, tu responsable te pone el ejercicio de calentamiento que aquí se hace siempre con la gente que entra: **mirar sitios que ya están funcionando y averiguar qué se puede deducir de ellos sin acceso a ninguna máquina**.

No es un juego de adivinar. Cuando dentro de unos meses te toque diagnosticar por qué un sitio va lento o devuelve errores, empezarás exactamente así: desde fuera, con las mismas dos herramientas que vas a usar hoy. Y la parte más importante del ejercicio no es lo que consigas averiguar, sino que sepas separarlo de lo que solo estás suponiendo.

## Qué vas a practicar

- **Pedir** la cabecera de una respuesta HTTP e interpretar lo que revela.
- **Reconocer** cuándo una respuesta viene de una caché y no del servidor original.
- **Inventariar** con el navegador qué descarga una página y de cuántos sitios distintos.
- **Distinguir** lo que has observado de lo que estás deduciendo.

## Requisitos previos

- Un navegador con herramientas de desarrollador (F12).
- `curl` disponible en el terminal:

```bash
curl --version
```

- La plantilla descargada, donde vas a ir volcando todo.

!!! info "Reparto de tiempo orientativo"
    Pasos 1 a 3, unos 35 minutos. Pasos 4 y 5, unos 30. El paso 6 y la redacción final, unos 25.

!!! warning "Solo observación"
    Todo lo que se te pide aquí es mirar lo que un servidor entrega voluntariamente a quien le pide una página: exactamente lo mismo que hace tu navegador cada vez que visitas un sitio. No lances peticiones repetidas en bucle, no uses herramientas de escaneo y no intentes acceder a nada que no sea público. La frontera entre inspeccionar y molestar es fina, y en esta profesión conviene tenerla clara desde el primer día.

---

## Paso 1 — Tus dos sujetos

Vas a trabajar con dos sitios elegidos para que sean lo más distintos posible entre sí:

| | Sitio | Qué representa |
|---|---|---|
| **1** | Esta misma web de apuntes | Documentación: ficheros que ya existían antes de que preguntaras |
| **2** | `https://www.amazon.es` | Una tienda con usuarios, sesión y catálogo enorme |

**No hace falta que inicies sesión ni que te crees ninguna cuenta**: todo lo que vas a mirar está disponible sin identificarse.

!!! info "Si el sitio externo cambia"
    Los sitios públicos pueden modificar su configuración, sus cabeceras o sus mecanismos de protección sin avisar. Si el segundo sitio no permite realizar alguna de las comprobaciones previstas, el profesor proporcionará una URL alternativa. Lo importante es aplicar el procedimiento de observación, no obtener unos valores concretos.

Antes de tocar nada, escribe en la plantilla, en dos o tres líneas por sitio, **qué esperas encontrar**. No es una adivinanza con premio: al final compararás y verás qué te dijo la evidencia que no te decía la intuición.

---

## Paso 2 — Pregunta solo por la cabecera

Consigue de cada uno de los dos sitios **únicamente la cabecera de la respuesta**, sin descargar el contenido. `curl` tiene una opción para eso; búscala en su ayuda.

Recoge en la plantilla, para cada sitio:

- El código de estado y la versión del protocolo.
- Qué software dice estar atendiendo la petición.
- El tipo de contenido devuelto.
- Qué dice sobre caché.
- Cuántas cookies te entrega sin que hayas iniciado sesión.
- Si hay alguna cabecera que delate que **entre tú y el servidor hay alguien más**.

**Comprueba**: obtienes una cabecera completa de cada sitio.
**Captura**: la salida de las dos peticiones.

!!! tip "Si Amazon te dice que ese método no está permitido"
    No es un fallo tuyo: le has hecho una petición con un método que ese servidor no acepta, y te lo está diciendo con el código correspondiente. Fíjate en que, aun negándose, te devuelve la cabecera entera, así que tienes todo lo que necesitas. Anota ese código: es un ejemplo perfecto de que un `4xx` no siempre significa que quien pregunta se haya equivocado de dirección.

---

## Paso 3 — La misma pregunta dos veces

Pide **dos veces seguidas** la cabecera de la web de apuntes y compara ambas salidas. Hay cabeceras que cambian entre la primera y la segunda: una cuenta segundos y otra dice si la respuesta se ha encontrado ya guardada o ha habido que ir a buscarla.

Anota cuáles son y qué valor toman en cada intento.

**Comprueba**: al menos una cabecera tiene distinto valor en la segunda petición.
**Captura**: las dos salidas, una debajo de la otra.

!!! tip "Si la segunda petición dice exactamente lo mismo que la primera"
    Repítela. Puede que te haya atendido un servidor de caché distinto del de la primera vez, y ese aún no tenga guardada la respuesta.

!!! question "Reflexiona"
    Si la segunda respuesta no la generó nadie de nuevo, **¿dónde estaba guardada y quién te la sirvió?** Señala la cabecera concreta que lo demuestra, no la intuición.

---

## Paso 4 — Cuenta lo que se descarga de verdad

Abre cada uno de los dos sitios con las herramientas de desarrollador en la pestaña de **red**, y recarga con el registro ya activo. **No enumeres nada**: quédate con los totales y con lo que dicen los filtros por tipo.

Para cada sitio, anota:

- Cuántas peticiones hacen falta para pintar la página y cuánto se transfiere en total.
- Cuántas hay de cada tipo: documento, estilos, script, imagen y llamadas a una API.
- Cuántos **dominios distintos** aparecen involucrados.

**Comprueba**: el número de peticiones deja de crecer cuando la página termina de cargar.
**Captura**: la pestaña de red de cada sitio, con el total de peticiones y de bytes visible.

!!! question "Reflexiona"
    Centrándote en el **documento HTML principal**, uno de los dos sitios puede entregarte contenido previamente generado mientras que el otro necesita producir una respuesta dinámica. **¿Qué diferencias observas y qué evidencias concretas las apoyan?** No intentes deducir más de lo que muestran las cabeceras y la pestaña de red.
---

## Paso 5 — La sesión y el error

Dos comprobaciones rápidas sobre los mismos dos sitios:

- En la pestaña de **almacenamiento** o **aplicación**, mira las cookies guardadas en cada uno. Anota cuántas hay y si alguna parece un identificador de sesión.
- Pide en el navegador una **ruta inventada** que seguro no existe, y observa con qué código responde cada sitio y qué te devuelve: una página de error propia, una genérica del servidor o una redirección.

**Comprueba**: los dos responden con algo distinto de `200` a la ruta inventada.
**Captura**: las cookies de la tienda y la respuesta de cada sitio a la ruta inexistente, con el código visible.

---

## Paso 6 — Lo que puedes demostrar y lo que solo supones

Esta es la parte que se corrige de verdad.

En el apunte de hoy has visto que una aplicación web desplegada tiene cinco piezas. Para **la tienda**, rellena esta tabla:

| Pieza | ¿Tienes evidencia de que existe? | ¿Cuál exactamente? |
|---|---|---|
| Estáticos | | |
| Artefacto y runtime | | |
| Datos | | |
| Configuración por entorno | | |
| Secretos | | |

La segunda columna solo admite tres respuestas: **sí**, **no** o **no es observable desde fuera**. Y la tercera columna es obligatoria cuando respondas que sí: hay que decir qué has visto en tus capturas que lo demuestre.

Escribir «tendrá una base de datos, seguro» no vale. Escribir «no puedo demostrarlo desde fuera, pero un catálogo de ese tamaño no se mantiene a mano» sí vale, porque estás diciendo abiertamente que es una deducción.

Cierra con dos párrafos cortos:

1. **Vuelve a tus predicciones del paso 1.** ¿En qué acertaste y en qué no? ¿Qué te dijo la evidencia que no te había dicho la intuición?
2. **¿En cuál de los dos sitios notarías antes, desde fuera, que algo se ha roto por dentro?** ¿Y cuál podría estar medio caído durante horas sin que un visitante ocasional se diera cuenta?

---

## Si te sobra tiempo

Nada de esto se entrega ni se corrige. Es para quien vaya sobrado.

**El que no te deja mirar.** Pide la cabecera de `https://www.filmaffinity.com/es` igual que hiciste en el paso 2. El sitio funciona perfectamente cuando lo abres en el navegador, pero a ti te va a decir que no. Mira qué software firma la respuesta y compáralo con el nombre del sitio: ¿quién te ha contestado? ¿Y qué habrá visto en tu petición que no ve en la de un navegador?

**La CDN que no cachea.** En la tienda hay evidencia de que existe una red de distribución por delante y, a la vez, la propia respuesta dice que el documento principal **no** debe guardarse en caché. Si no se puede cachear, ¿para qué sirve entonces esa red delante? Se te ocurrirán al menos dos utilidades.

---

## Verificación

Para dar por válida la práctica se ejecutará:

```bash
curl -I <dirección de esta web de apuntes>
curl -I <dirección de esta web de apuntes>
curl -I https://www.amazon.es
```

Y debe observarse:

- Que los datos recogidos en tu documento son **coherentes** con lo que devuelven los sitios al corregir.
- Que has identificado las cabeceras que cambian entre las dos peticiones seguidas y qué informan.
- Que has anotado el código con el que responde la tienda a la petición de cabecera y que no lo has confundido con un error tuyo.
- Que cada casilla marcada como «sí» en la tabla del paso 6 señala una evidencia concreta que aparece en tus capturas.

Si un sitio ha cambiado desde que hiciste la práctica, tu captura lo justifica: por eso se piden capturas y no transcripciones a mano.

---

## Qué se entrega

- [ ] Las cabeceras de los dos sitios, recogidas e interpretadas.
- [ ] Las dos peticiones seguidas a la web de apuntes, con las cabeceras que cambian identificadas.
- [ ] El inventario de red de los dos sitios: totales, tipos y dominios.
- [ ] Las cookies y la respuesta a la ruta inexistente.
- [ ] La tabla de las cinco piezas, con la columna de evidencia completa.
- [ ] Los dos párrafos de cierre del paso 6.
- [ ] El documento con las capturas legibles y las respuestas ordenadas.

!!! info "Dónde se entrega"
    Hoy, como documento suelto: el repositorio del módulo todavía no existe. Guárdalo tal cual, porque la semana que viene lo crearás y esta entrega será lo primero que registres en él.

---

## ✅ Cierre

Al terminar tienes un método para mirar cualquier sitio web desde fuera y hacerte una idea razonable de lo que hay detrás, con dos herramientas que vas a usar todo el curso. Y tienes algo más valioso: la costumbre de decir «esto lo he visto» y «esto lo estoy suponiendo» sin mezclarlo, que es la diferencia entre diagnosticar y adivinar.

También has visto de primera mano tres de las piezas que este módulo va a ir montando: contenido guardado en algún sitio intermedio que responde antes que el servidor, sesiones que viajan en cookies, y capas colocadas por delante que contestan por él.

Y tienes una lista implícita de todo lo que **no** se puede saber desde fuera: ahí empieza el resto del módulo. En la próxima sesión dejas de mirar despliegues ajenos y empiezas a preparar el tuyo. El primer paso no es una máquina ni un servidor: es el sitio donde van a vivir el código y el procedimiento, con una rama por cada sesión, etiquetas que identifican versiones desplegables y secretos que nunca entran.