# 🧪 Actividad 4.2: Del commit a producción, y vuelta

## Contexto

Escaparate lleva desde octubre publicado en tu subdominio y desde diciembre con una puerta de calidad delante de `main`. Lo que sigue pasando es que la única forma de que un cambio llegue a producción eres tú, un viernes por la mañana, entrando a la instancia. Mientras seas el único que despliega y solo despliegues una vez por semana, eso se aguanta. La gente que entra en enero no puede depender de que estés disponible, ni de que te acuerdes del orden exacto de los comandos.

Hoy cierras el circuito: un cambio tuyo tiene que poder llegar hasta el servidor sin que nadie toque el servidor. Y como todo lo que se despliega solo se despliega mal alguna vez, la segunda mitad de la sesión consiste en romperlo a propósito de la única forma que tu pipeline no sabe detectar, y recuperar el servicio en menos de lo que tardas en leer el error.

## Qué vas a practicar

- Autorizar a un tercero automático a entrar en tu servidor, y valorar honestamente cuánto poder le estás dando.
- Separar el workflow que publica una versión del que la despliega, para que volver atrás no dependa de reconstruir nada.
- Recorrer el ciclo completo desde un cambio tuyo hasta producción sin abrir una sesión en la instancia.
- Diagnosticar un despliegue defectuoso y recuperar el servicio volviendo a una versión anterior.
- Añadir una comprobación posterior al despliegue que detecte el incidente automáticamente y marque el despliegue como fallido.

## Requisitos previos

- La **rutina de arranque semanal** hecha: laboratorio rearrancado, registro A del subdominio apuntando a la IP nueva, stack levantado desde `main` y sitio respondiendo por HTTPS. Hoy es imprescindible, porque el pipeline va a hablar con esa máquina.
- El pipeline de la actividad 5.1 en verde y la regla de protección de `main` activa y sin excepciones.
- Rama `sesion-13` abierta.

**Lo que entrego yo hoy:**

- `preparar-despliegue.sh`, que crea en la instancia el usuario `despliegue`, su acceso al demonio de Docker y el directorio `/opt/escaparate` con el `compose.yaml` de producción, que lee la versión de la imagen de una variable de entorno. Es andamiaje, no objeto de estudio: se ejecuta y se lee después.
- Los **esqueletos de `release.yml` y `deploy.yml`**: los disparadores, las entradas, la invocación entre workflows y el bloque de permisos, con los pasos vacíos y comentados. La sintaxis de encadenar workflows es andamiaje; lo que hoy se aprende es qué va en cada uno y por qué.
- El **paquete de cambios de la versión 2.0.0**, con la nueva imagen de marca. Aplícalo tal cual.
- Un fragmento resuelto del paso de conexión por SSH, que publicaré **solo a las 11:55** y solo si hace falta.

!!! info "Reparto de tiempo orientativo"
    Arranque del laboratorio hasta las 10:15. Teoría hasta las 11:00. Descanso hasta las 11:15. Paso 1, hasta las 11:35. Paso 2, hasta las 12:00. Paso 3, hasta las 12:20. Paso 4, hasta las 12:35. Paso 5, hasta las 12:50. Paso 6, hasta las 13:00. Puesta en común y cierre de la unidad, hasta las 13:15.

---

## Paso 1 — Dejar entrar al pipeline, y solo al pipeline

Ejecuta en la instancia el guion que te doy y comprueba qué ha creado. Después prepara las tres cosas que el pipeline necesita para llegar hasta ahí:

1. **A dónde ir**: el subdominio de tu equipo, guardado como variable del repositorio. No es un secreto.
2. **Saber que es tu servidor**: obtén la clave pública SSH de tu instancia **desde dentro de la propia instancia**, compárala con la que ves desde fuera y guárdala como variable. Ojo al formato: lo que el ejecutor va a escribir es una línea de fichero de hosts conocidos, así que tiene que llevar **el nombre estable de tu servidor, el tipo de clave y la clave**, en ese orden. El fichero público que hay en la instancia no trae el nombre delante: si lo copias tal cual, la conexión fallará y perderás diez minutos buscando dónde.
3. **Con qué entrar**: genera un par de claves **nuevo, dedicado exclusivamente al despliegue**. La pública se autoriza para el usuario `despliegue`; la privada se guarda como secreto del entorno de producción.

Ni tu clave personal de septiembre, ni tu usuario de administración.

**Comprueba**: desde tu equipo, que la clave nueva entra como `despliegue` y que ese usuario no puede ejecutar órdenes de administración con `sudo`. En el repositorio, que el secreto figura asociado al entorno y no suelto.
**Captura**: la lista de secretos y variables del entorno de producción, con los nombres visibles y ningún valor.

!!! question "Reflexiona"
    Dos preguntas incómodas sobre lo que acabas de montar. La primera: para que un ejecutor de GitHub entre en tu instancia, el puerto 22 tiene que estar abierto a un rango de direcciones que no controlas ni conoces de antemano. ¿Qué has ganado y qué has perdido exactamente? La segunda: el usuario `despliegue` no tiene `sudo`, pero sí puede hablar con el demonio de Docker. Averigua qué puede hacer alguien con esa capacidad y decide si la frase «esta llave solo sirve para desplegar» es cierta.

!!! tip "Punto de rescate — 11:32"
    Si a las 11:32 la conexión con la clave nueva no funciona, avísame. Los tres sospechosos de siempre son los permisos del fichero de la clave, el grupo de seguridad y el usuario con el que estás entrando, y desde fuera se descarta en un minuto.

## Paso 2 — Que publicar una versión sea desplegar

Vas a repartir el trabajo en tres ficheros en vez de amontonarlo en uno:

- `ci.yml` se queda tal cual: las puertas de calidad de la sesión anterior.
- `deploy.yml` recibe **un número de versión** y pone esa versión a correr en la instancia. Se puede invocar desde otro workflow o lanzar a mano. **No construye ni publica nada.**
- `release.yml` se dispara al publicar una etiqueta que empiece por `v`. Y aquí hay una decisión que tienes que tomar tú antes de escribir nada: la imagen que este workflow va a publicar **no es la que se escaneó en el pull request**, porque aquella se construyó y se destruyó, y además una etiqueta puede crearse sobre cualquier estado del repositorio. Decide qué comprobaciones tiene que repetir `release.yml` sobre el código de la etiqueta antes de subir nada al registro, y déjalas escritas en ese orden.

Ojo con tres cosas que no dan error y estropean el resultado: dentro de `deploy.yml` la versión se lee de la entrada del workflow, nunca de la referencia desde la que se ha lanzado; el valor de la versión tiene que estar disponible para **todas** las órdenes que ejecutes en el servidor, no solo para la primera; y la publicación no puede ocurrir antes de las comprobaciones, o habrás subido al registro algo que no has mirado.

Con los tres ficheros fusionados, etiqueta el estado actual de `main` como `v1.0.0` y publica la etiqueta. No cambies nada del código: hoy `v1.0.0` es la versión que ya funciona, y va a ser importante dentro de media hora.

**Comprueba**: `release.yml` comprueba, publica y llama a `deploy.yml`; la imagen aparece en GHCR con la etiqueta `v1.0.0`; el sitio sigue respondiendo con normalidad. No has abierto ninguna sesión en la instancia para desplegar.
**Captura**: la ejecución con los dos workflows encadenados en verde, con los pasos de comprobación visibles antes del de publicación, y la vista del paquete en GHCR con la etiqueta.

!!! question "Reflexiona"
    Tres preguntas cortas sobre lo que acabas de escribir. ¿Qué permisos necesita el job que publica y por qué son más que los del workflow de los pull requests? ¿Por qué `release.yml` no le pasa a `deploy.yml` la llave del servidor, si es quien lo invoca? Y mirando hacia atrás: acabas de desplegar sin compilar nada en el servidor — recorre qué decisiones de sesiones anteriores lo han hecho posible y señala cuál de ellas se habría llevado por delante el despliegue de hoy.

!!! tip "Punto de rescate — 11:55"
    Si a las 11:55 el despliegue no completa, avísame y publico el fragmento resuelto. Este paso no es el objetivo de la sesión: los pasos 3 y 4 sí.

## Paso 3 — Publicar la 2.0.0 y verla caer

Aplica el paquete de cambios de la versión 2.0.0 que te he dado, ábrelo en un pull request y fusiónalo. Fíjate en lo que ocurre: pasa la construcción, pasan los tests, pasa la cobertura, pasa el escáner, y la regla de protección te deja fusionar. Todo verde.

Etiqueta entonces `v2.0.0`, publícala y abre tu subdominio en el navegador mientras se despliega.

**Comprueba**: la cabecera del catálogo tiene el aspecto nuevo, así que el despliegue automático ha funcionado de principio a fin. Y el catálogo no muestra productos. El pipeline está en verde y la aplicación está rota.
**Captura**: el pipeline en verde y el navegador con la versión nueva sin datos. Añade la consola del navegador, que dice más de lo que parece.

!!! question "Reflexiona"
    Localiza en el paquete de cambios la línea que ha provocado el fallo: está a la vista y la has fusionado igual. Ahora responde a dos cosas. Primera: ¿por qué ninguna de las cuatro comprobaciones del pipeline podía detectarla? Segunda: si esto hubiera pasado un martes a las cuatro de la tarde con clientes dentro, ¿en qué momento te habrías enterado?

!!! danger "Alto obligatorio — 12:20"
    A las 12:20 se ejecuta el rollback, tengas lo que tengas. Si llegas a esa hora sin haber conseguido desplegar la 2.0.0, documenta dónde te has quedado y pasa igualmente al paso 4 con la versión que tengas publicada: recuperar el servicio es lo que cierra esta unidad.

## Paso 4 — Recuperar el servicio sin arreglar nada

Tienes el fallo localizado y sabes cuál es la línea. **No lo arregles.** Ese es el ejercicio.

Vuelve a poner en producción la versión que funcionaba lanzando `deploy.yml` a mano e indicando `v1.0.0`. Antes de empezar, apunta la hora exacta; cuando el catálogo vuelva a mostrar productos, apunta la hora otra vez.

**Comprueba**: el sitio recupera el aspecto y el contenido de la versión anterior, sin que hayas tocado la instancia ni el código. En el registro de la ejecución **no aparece ninguna construcción ni ninguna subida al registro**: solo una descarga y un arranque.
**Captura**: la ejecución del despliegue de vuelta, el sitio recuperado, y las dos horas anotadas con el tiempo transcurrido.

!!! question "Reflexiona"
    Compara ese tiempo con lo que habrías tardado corrigiendo la línea, abriendo un pull request, esperando al pipeline, fusionando, etiquetando y desplegando. Y una segunda, sobre el diseño: ¿qué habría pasado si el rollback hubiera vuelto a construir y a publicar la `v1.0.0` en lugar de reutilizar la imagen que ya existía?

## Paso 5 — La comprobación que faltaba

Tu pipeline terminó en verde sin haber comprobado ni una vez el comportamiento real del servicio. A partir de ahora, un despliegue no se dará por bueno hasta superar una comprobación hecha contra producción.

Añade al final de `deploy.yml` una **prueba de humo** que se ejecute contra el sitio ya desplegado y que habría detectado este incidente concreto. No basta con preguntar si el servidor responde, porque respondía: tiene que comprobar que la aplicación que se está sirviendo **puede obtener sus datos de donde ella misma dice que va a buscarlos**. Si la comprobación falla, el job termina en rojo.

Como el despliegue está escrito una sola vez, esa prueba protege desde ya tanto a las versiones nuevas como a las vueltas atrás. Compruébalo: vuelve a lanzar el despliegue de `v2.0.0` y observa el resultado. Después deja la `v1.0.0` corriendo.

**Comprueba**: el despliegue de `v2.0.0` termina **en rojo** por la prueba de humo, no por la construcción. El de `v1.0.0` sigue en verde.
**Captura**: las dos ejecuciones una al lado de la otra, con el paso que falla señalado.

!!! question "Reflexiona"
    Tu prueba de humo no impide el incidente: lo detecta, y lo detecta cuando la versión defectuosa ya está sirviendo tráfico. Aun así has ganado algo importante — di exactamente qué. Y después: nombra qué haría falta para que el usuario no llegara a verla nunca, y relaciónalo con una de las cuatro estrategias de despliegue del apunte.

## Paso 6 — Dejar `main` desplegable y documentar el circuito

Producción está corriendo la `v1.0.0` y eso está bien: es una decisión, no un accidente. Lo que no puede quedarse así es `main`, que ahora mismo contiene un cambio que sabes que rompe la aplicación. Si entras en enero desde ahí, el siguiente que despliegue se llevará el fallo puesto.

Abre un último pull request que **corrija o revierta el cambio defectuoso** y fusiónalo. No hace falta que lo despliegues hoy: `main` vuelve a ser un candidato razonable y producción sigue deliberadamente en `v1.0.0`.

Después actualiza el `README` con el diagrama del pipeline completo —qué sucesos lo disparan, qué workflows se ejecutan en cada caso y qué comprueba cada uno—, el **procedimiento de emergencia** en tres líneas, y qué versión está corriendo en producción y desde cuándo, porque a partir de hoy eso ya no se deduce mirando `main`.

**Comprueba**: alguien que no haya estado hoy en clase puede volver a una versión anterior leyendo solo tu `README`, y sabe qué hay desplegado sin preguntarte.
**Captura**: no hace falta; el `README` y el pull request de corrección son la evidencia.

---

## Si te sobra tiempo

Publica la corrección como `v2.0.1` y despliégala, para terminar el trimestre con la imagen de marca nueva y funcionando. No puntúa.

---

## Verificación

Para dar por válida la práctica se ejecutará, con `REPO=<usuario>/daw-despliegue` y `HOST=<subdominio del equipo>`:

```bash
gh api repos/$REPO/contents/.github/workflows/deploy.yml --jq .content | base64 -d
gh api repos/$REPO/contents/.github/workflows/release.yml --jq .content | base64 -d
gh api repos/$REPO/tags --jq '.[].name'
gh run list --repo $REPO --limit 20
docker run --rm quay.io/skopeo/stable list-tags docker://ghcr.io/<usuario>/escaparate
curl -s -o /dev/null -w '%{http_code}\n' https://$HOST/
curl -s https://$HOST/config.js
```

Y debe observarse:

- `release.yml` se dispara con etiquetas `v*`, declara el permiso de escritura en el registro, **repite las comprobaciones sobre el código de la etiqueta antes de publicar** e invoca a `deploy.yml`. Ninguno de los dos se dispara con `pull_request`, y `release.yml` no reenvía la llave de despliegue.
- `deploy.yml` **no construye ni publica** nada, lee la versión de la entrada del workflow y no de la referencia de ejecución, y exporta esa versión antes de todas las órdenes remotas.
- Existen las etiquetas `v1.0.0` y `v2.0.0` en el repositorio, y las dos imágenes correspondientes en GHCR.
- En el historial aparece la secuencia completa: despliegue de `v1.0.0` en verde, despliegue de `v2.0.0` en verde, despliegue manual de vuelta a `v1.0.0` **sin job de publicación**, y un despliegue de `v2.0.0` **en rojo** una vez añadida la prueba de humo.
- La portada responde `200` y el `config.js` servido es el de la versión buena: la recuperación es real y no una captura antigua.
- `main` contiene la corrección del defecto y está en estado desplegable.
- El `README` contiene el diagrama, el procedimiento de vuelta atrás y qué versión está en producción.

---

## Qué se entrega

- [ ] Rama `sesion-13` con pull request **fusionado** hacia `main`.
- [ ] `ci.yml`, `release.yml` y `deploy.yml` en el repositorio, con las comprobaciones previas a la publicación en `release.yml` y la prueba de humo dentro de `deploy.yml`.
- [ ] Captura del paso 1: secretos y variables del entorno de producción.
- [ ] Respuesta escrita a la pregunta sobre qué puede hacer de verdad el usuario `despliegue`.
- [ ] Captura del paso 2: los dos workflows encadenados en verde y la etiqueta `v1.0.0` en GHCR.
- [ ] Captura del paso 3: pipeline en verde, aplicación rota y consola del navegador.
- [ ] Identificación por escrito de la línea causante y de por qué ninguna comprobación la vio.
- [ ] Captura del paso 4: despliegue de vuelta sin construcción, sitio recuperado y **tiempo de recuperación medido**.
- [ ] Captura del paso 5: el mismo despliegue de `v2.0.0` fallando por la prueba de humo.
- [ ] Pull request de corrección fusionado, con `main` desplegable.
- [ ] `README` con el diagrama, el procedimiento de emergencia y la versión que está en producción.
- [ ] Todo lo anterior en `entregas/tema5/`, y la plantilla en Moodle con la URL del pull request fusionado.

---

## ✅ Cierre

Sales del aula con un camino que va de un cambio tuyo hasta un servidor en internet sin que nadie entre en ese servidor, y habiendo visto su límite. Hoy tu pipeline te ha dado el visto bueno mientras el catálogo estaba vacío, y lo que ha salvado el servicio no ha sido una comprobación más: ha sido poder volver a una imagen que seguía existiendo, intacta, con su etiqueta de siempre. Esa etiqueta se decidió en septiembre, cuando parecía burocracia. Te llevas además una distinción que hasta hoy no necesitabas: `main` ya no identifica lo que está desplegado, sino el último estado integrado que ha superado las puertas —un candidato, que como has visto puede romper producción igual—, y lo que está realmente corriendo lo identifica la versión desplegada. Por eso lo has escrito en el `README`.

Con esto se cierra la **unidad 5** y el resultado de aprendizaje que empezaste el 18 de septiembre con un repositorio vacío. El 8 de enero cambia el terreno: empiezas la orquestación de contenedores, donde el problema deja de ser cómo llevar una versión a una máquina y pasa a ser cómo mantener un estado deseado en un grupo de ellas —y donde aparece, por fin, el mecanismo que hace de verdad las actualizaciones progresivas que hoy solo has podido nombrar—. Y en la última sesión del curso volverás a la pregunta que has dejado abierta en el paso 1: si en lugar de darle a un pipeline la llave de tu servidor, fuera el servidor el que preguntara qué versión le toca.