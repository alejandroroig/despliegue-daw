# 🧪 Actividad 4.2: Cuánta carga aguanta y qué se rinde primero

!!! warning "Descarga la plantilla"
    📄 [Plantilla 4.2 — Rendimiento y pruebas de carga](plantillas/Actividad_4_2_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

Dirección quiere lanzar Escaparate en campaña de Navidad con publicidad pagada, y antes de firmar te hacen dos preguntas que no admiten un «yo creo que sí»: **cuánta carga aguanta el servicio tal como está**, y **qué habría que hacer si esperan el doble**. La segunda tiene trampa, porque la respuesta cómoda —poner más copias— cuesta dinero todos los meses y puede no servir absolutamente de nada.

Llevas desde la sesión 7 con tres copias detrás del proxy sin que nadie haya comprobado si tres copias rinden más que una. Hoy lo mides con un criterio pactado, compruebas que tu medida se sostiene, y usas el panel que montaste hace dos semanas para llegar tan lejos como tus datos te dejen. Sales de aquí con un número defendible, con sus condiciones escritas al lado, y con una recomendación que puedes justificar delante de quien firma el gasto.

## Qué vas a practicar

- Ejecutar una serie de pruebas de carga cortas con k6, una por nivel, con calentamiento descartado.
- Determinar la capacidad de una configuración según un criterio de latencia y errores pactado de antemano.
- Comparar tres copias contra una y calcular el factor de mejora real.
- Comprobar con una repetición de control que tu medida se sostiene.
- Localizar el cuello de botella hasta donde los datos permitan y formular una hipótesis sobre lo demás.

## Requisitos previos

- Tu instancia rearrancada hoy, con el tipo ampliado de estas dos sesiones, y el registro del subdominio de tu equipo apuntado a la IP nueva según el procedimiento de tu `README`.
- `main` al día tras fusionar la sesión 10: tres copias, almacén de sesiones y servidor externo.
- La pila de logs de la sesión 9 en marcha y recibiendo, con su panel.
- El fichero `prueba-carga.js` que publico hoy en el aula virtual. Lleva dentro el criterio de aceptación, apunta al endpoint que hace trabajo real, y trae en la cabecera la orden de ejecución completa: el nivel de carga y la duración se le pasan por la línea de órdenes, así que **el mismo guion sirve para todas las ejecuciones**.

!!! info "El criterio de hoy, y por qué es de todos"
    Escaparate se considera dentro de servicio cuando el **p95 está por debajo de 500 ms y los errores por debajo del 1 %**. No son números universales; son los que pactamos para esta práctica, y sirven para que la capacidad deje de ser una opinión y para que los resultados de toda la clase sean comparables. **La capacidad de una configuración es el mayor nivel de carga que cumple los dos.** El guion los lleva escritos y te dice al terminar cada ejecución si los cumple.

!!! info "Reparto de tiempo orientativo"
    Teoría hasta las **11:10**. Paso 1, hasta las **11:22**. Paso 2, hasta las **11:55**. Paso 3, hasta las **12:15**. Paso 4, hasta las **12:35**. Paso 5 y cierre, hasta las **13:10**.

---

## Paso 1 — Recuperar el servicio y despejar la máquina

Rearranca el laboratorio, apunta el registro del subdominio a la IP nueva y levanta el stack desde `main`. Abre la rama `sesion-11`.

Antes de medir hay que hacer dos cosas más, porque medir sobre un sistema que no está entero produce números que no valen. La primera: comprueba que **el panel está recibiendo líneas ahora mismo**. Si la pila de logs se quedó atrás en el rearranque lo vas a descubrir dentro de una hora, cuando ya no puedas repetir las pruebas. La segunda: **para el Tomcat externo de la semana pasada**. No forma parte del sistema que vas a medir y sí consume memoria de la misma máquina; dejarlo encendido metería ruido en todas las mediciones del día.

**Comprueba**: tres peticiones seguidas a `/api/instancia` devuelven identificadores distintos, esas peticiones aparecen en el panel en menos de un minuto, y el servidor externo ya no está en marcha.

**Captura**: el panel mostrando el tráfico que acabas de generar, con la hora visible, y el listado de servicios activos.

## Paso 2 — Medir el servicio tal como está

Empieza lanzando una ejecución completa **que vas a tirar**. Es el calentamiento: las primeras peticiones contra una aplicación Java recién arrancada no representan su régimen normal, y como el resumen de k6 agrega la ejecución entera, la única forma limpia de descartar el arranque es descartar una ejecución entera.

A partir de ahí, con las tres copias en marcha, **una ejecución corta por cada nivel de carga**, subiendo: 10, 25, 50 y 100. Cada ejecución te da su propio resumen y su propio veredicto sobre el criterio, y cada resumen es una fila de tu tabla. Cuando un nivel incumpla el criterio, **ejecuta todavía un nivel más y entonces párate**: ese último no cambia la capacidad que vas a declarar, pero es el que te deja ver qué le pasa a un sistema al que sigues empujando cuando ya no da más de sí.

Ve apuntando de cada ejecución el nivel, las peticiones por segundo, el p95, la tasa de error y si cumple. Y guarda los resúmenes completos, uno detrás de otro, en `entregas/tema4/k6-3copias.txt`.

Dos cosas que hacer **mientras** corre la ejecución de mayor carga, porque después no vas a poder reconstruirlas: mira el panel, que es la única vez del curso en que verás tu sistema saturarse en directo, y toma una foto del consumo de recursos de todos los contenedores en ese momento.

**Comprueba**: tienes una fila por nivel medido, con su veredicto, y puedes decir cuál es la capacidad de esta configuración.

**Captura**: la tabla de las ejecuciones, el panel en el momento de máxima carga y la foto de consumo de los contenedores.

!!! question "Reflexiona"
    ¿En qué nivel dejó de cumplirse el criterio, y cuál de los dos límites se rompió antes, la latencia o los errores? Mira ahora las peticiones por segundo de ese nivel y del siguiente: **¿cuántas peticiones más estás sirviendo a cambio de toda esa espera de más?**

!!! tip "Punto de rescate — 11:35"
    Aquí tienen que encajar el contenedor de k6, la red de tu stack y la dirección de tu servicio. Si a las 11:35 no has conseguido lanzar la primera ejecución, avísame y te doy la orden ya resuelta para tu despliegue. Sin mediciones no hay actividad, así que no pierdas más de diez minutos peleándote con esto.

## Paso 3 — Medir con una sola copia

Reduce el servicio a **una sola copia** y repite exactamente la misma serie: mismo guion, mismos niveles, misma duración, misma máquina. No cambies nada más; si cambias dos cosas a la vez, la comparación deja de significar nada.

Un detalle a tu favor: al reducir de tres a una, la copia que queda ya está caliente, así que no arrastras el sesgo del arranque. Guarda los resúmenes en `entregas/tema4/k6-1copia.txt`.

**Comprueba**: tienes la misma tabla para esta configuración, con su capacidad, y puedes calcular el factor de mejora dividiendo las peticiones por segundo de las tres copias entre las de una, tomadas **en el mismo nivel de carga**.

**Captura**: la tabla de esta serie y el factor calculado.

!!! question "Reflexiona"
    ¿Cuánto te ha salido el factor? Si no se parece a 3, **nombra qué recursos no se han multiplicado al multiplicar las copias**. Ayúdate de la foto de consumo del paso anterior: ¿estaba la máquina saturada cuando medías con tres copias?

## Paso 4 — Control y diagnóstico

Antes de concluir nada, vuelve a ejecutar **la primera condición**: tres copias, al nivel que habías declarado como su capacidad. Si el resultado no se parece al de hace media hora, tu medida se ha ido —la máquina se ha degradado durante la sesión— y todas tus comparaciones quedan en entredicho. Comprobarlo cuesta un minuto y es la diferencia entre medir y mirar.

Con el control hecho, toca el diagnóstico. Tienes dos fuentes: el panel, que de cada petición guarda cuánto tardó en total y cuánto tardó el destino en contestar, y la foto de consumo del momento de máxima carga. Con eso puedes distinguir cuatro situaciones, y solo cuatro:

```text
proxy lento + destino rápido            → el límite está delante
destino lento en una sola copia         → problema de esa copia
destino lento en las tres + host saturado → recurso de la máquina
destino lento en las tres + host holgado  → dependencia compartida
```

Tu objetivo es **llegar hasta donde tus datos lleguen y no un paso más**. Si acabas en «dependencia compartida», tus registros no distinguen si es la base de datos, el disco o una pausa de la máquina virtual de Java: eso se formula como hipótesis y se acompaña de qué medirías después para confirmarla. Señalar a un culpable sin haberlo medido es justo lo que esta actividad enseña a no hacer.

**Comprueba**: la repetición de control reproduce el primer resultado, y puedes situar el límite en una de las cuatro casillas señalando el dato concreto que lo sostiene.

**Captura**: el resumen de la ejecución de control junto al de la original, y las visualizaciones del panel que sostienen tu conclusión, con la ventana temporal visible.

!!! question "Reflexiona"
    Si mañana añades una cuarta copia en esta misma máquina, ¿qué esperas que le pase a la capacidad? Responde con el factor que has medido, no con intuición. Y una segunda: las tres copias de la sesión 7, entonces, ¿para qué sirven? Concreta **de qué fallo te protegen y de cuál no**.

!!! tip "Alto obligatorio — 12:35"
    A las 12:35 se deja de medir, tengas lo que tengas. Una medición incompleta bien explicada vale; una medición perfecta sin informe, no. Si te faltan datos, escribe en el informe qué te falta y por qué.

## Paso 5 — El informe

Escribe `entregas/tema4/informe-rendimiento.md`. Va dirigido a quien tiene que decidir si firma la campaña, así que **una página bien hecha vale más que cinco**. Debe contener:

- Las **dos tablas**, una por configuración: nivel de carga, peticiones por segundo, p95, tasa de error y si cumple el criterio.
- La **capacidad de cada configuración** y el **factor de mejora**, con el nivel en el que lo has calculado.
- El resultado de la **repetición de control** y qué te permite afirmar.
- Las **condiciones de la medida**, en una frase honesta: el generador de carga se ejecutaba en la misma máquina que el sistema medido, junto a la base de datos y la pila de logs, así que estos números sirven para comparar configuraciones entre sí y no para prometerle una cifra a un cliente.
- **Dónde está el límite** hasta donde los datos lo demuestran, la **hipótesis** sobre el resto, y **qué medirías a continuación** para confirmarla.
- Una **recomendación**: repartir, ampliar u optimizar, qué harías exactamente y por qué esperas que funcione.

Añade al `README` una línea con la capacidad medida, el criterio usado y la fecha. Deja las capturas en `entregas/tema4/` y lanza el pull request de `sesion-11` hacia `main`.

**Comprueba**: el pull request está fusionado y el informe se entiende sin haber estado en clase.

---

## Si te sobra tiempo

Para la pila de logs y repite la medición de capacidad con tres copias. Lo que suba es **lo que te estaba costando observar tu propio sistema**, y es un dato que en una empresa se discute en serio. Vuelve a arrancarla antes de irte y no entregues nada de esto: es para la puesta en común.

---

## Verificación

Para dar por válida la práctica se ejecutará, con `main` fusionado:

```bash
URL=https://<subdominio-del-equipo>
RED=<red-de-tu-stack>

# 1. El stack levanta desde cero
docker compose up -d && docker compose ps --format '{{.Service}}\t{{.Status}}'

# 2. Hay varias ejecuciones registradas en cada configuración
grep -c 'http_reqs' entregas/tema4/k6-3copias.txt
grep -c 'http_reqs' entregas/tema4/k6-1copia.txt
grep -E 'http_reqs|p\(95\)|http_req_failed' entregas/tema4/k6-3copias.txt

# 3. Se reproduce en vivo el nivel declarado como capacidad
docker run --rm -i --network $RED -e URL=$URL \
  grafana/k6 run --vus <capacidad-declarada> --duration 60s - < prueba-carga.js

# 4. El informe trae capacidad, factor, control, hipótesis y decisión
grep -iE 'capacidad|factor|control|hipótesis|repartir|ampliar|optimizar' \
     entregas/tema4/informe-rendimiento.md

# 5. El reparto sigue en pie con las tres copias
for i in 1 2 3; do curl -s $URL/api/instancia; echo; done
```

Y debe observarse:

- Todos los servicios del stack en estado saludable, incluidos el almacén de sesiones de la semana pasada.
- Al menos tres ejecuciones registradas en cada fichero de medidas.
- En la comprobación 3, **las dos condiciones del criterio en verde**: si el nivel declarado como capacidad no las cumple al reproducirlo, el número declarado no es válido.
- En el informe, la frase sobre las condiciones de la medida. Un informe que declare «capacidad de Escaparate» sin ese matiz no se da por bueno.
- Tres identificadores de instancia distintos.

*Sustituye la red y el subdominio por los tuyos si no coinciden.*

---

## Qué se entrega

- [ ] Rama `sesion-11` con pull request **fusionado** en `main`.
- [ ] `entregas/tema4/k6-3copias.txt` y `entregas/tema4/k6-1copia.txt` con los resúmenes de todas las ejecuciones, incluida la de control.
- [ ] Captura del panel en el momento de máxima carga y foto del consumo de los contenedores en ese mismo momento.
- [ ] Capturas de las visualizaciones que sostienen el diagnóstico, con la ventana temporal visible.
- [ ] `entregas/tema4/informe-rendimiento.md` con las dos tablas, capacidad de cada configuración, factor de mejora, resultado del control, condiciones de la medida, límite demostrado, hipótesis y recomendación.
- [ ] Respuesta escrita a qué recursos no se han multiplicado al multiplicar las copias.
- [ ] Respuesta escrita a para qué sirven entonces las tres copias de la sesión 7, precisando de qué fallo protegen y de cuál no.
- [ ] Línea de capacidad, criterio y fecha añadida al `README`.
- [ ] Plantilla `.docx` subida a Moodle con la URL del pull request fusionado.

---

## ✅ Cierre

Con este informe cierras el bloque de servidores de aplicaciones y, con él, el resultado de aprendizaje 3. En seis semanas has pasado de un servidor web que servía ficheros a un servicio del que sabes **qué lo ejecuta, dónde guarda el estado, cuánta carga admite bajo qué criterio y qué recurso lo limita**.

Y te llevas una corrección importante sobre algo que dabas por hecho desde octubre: tres copias en una máquina no dan el triple de capacidad. Dan **tolerancia al fallo de una copia** y permiten **actualizar de forma progresiva sin detener el servicio**, que era para lo que estaban ahí. Lo que no te dan es protección frente a la caída de la propia máquina, porque las tres la comparten. Replicar en el mismo host reparte el trabajo; para añadir recursos hay que dejar de compartirlos. Cualquiera sabe añadir copias; saber decir en qué casos añadir copias no sirve de nada, y demostrarlo con una tabla, es un argumento que se paga.

Guarda bien el número y el criterio. En diciembre vas a desplegar versiones nuevas de forma automática, y en enero vas a dejar que un orquestador decida cuántas copias hacen falta y sobre cuántas máquinas repartirlas; en los dos casos la pregunta de fondo será la de hoy, y vas a agradecer tener el dato medido en lugar de la intuición.

La semana que viene cambia el asunto por completo. Hasta ahora todo lo que has montado lo has montado tú, a mano, cada viernes. A partir del próximo, el objetivo es que Escaparate se compile, se pruebe y se empaquete sola cada vez que alguien toque el código, y que tú solo tengas que mirar si el semáforo está en verde.