# 🧪 Actividad 3.4: Que te lo cuente el sistema

!!! warning "Descarga la plantilla"
    📄 [Plantilla 3.4 — Que te lo cuente el sistema](plantillas/Actividad_3_4_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

Tu servicio está publicado, repartido entre tres copias, cifrado y con una zona privada. Lo único que no tiene es alguien que mire. Si el martes por la noche una de las copias se hubiera quedado devolviendo errores, te habrías enterado cuando alguien te lo dijera, y para entonces las pruebas estarían repartidas por los registros de cuatro contenedores en una máquina que se apaga entre sesiones.

El encargo de hoy es el último del tema y es el que convierte un despliegue en un servicio administrable: **recoger en un solo sitio todo lo que tus piezas escriben, poder preguntarle cosas concretas y decidir qué merecería una llamada a las tres de la madrugada**. Y como cierre del bloque, entregarás la memoria de configuración y administración segura del servidor web: cuatro sesiones de trabajo en un documento.

## Qué vas a practicar

- **Levantar** una pila de recolección, almacenamiento y visualización junto a tu despliegue.
- **Reescribir** el registro de acceso en formato estructurado, con los campos que te hacen falta.
- **Consultar** datos reales para responder preguntas concretas sobre el tráfico.
- **Calcular** percentiles de latencia y demostrar con datos el reparto entre las tres copias.
- **Diseñar** dos alertas con su umbral y su ventana, y demostrar que la condición se habría cumplido.

## Requisitos previos

- La actividad 3.3 terminada: el servicio funcionando por HTTPS, con las tres copias y la zona protegida.
- La instancia rearrancada, con el subdominio apuntando a su dirección de hoy.
- El paquete de la actividad: `compose.observabilidad.yaml` con la pila **Fluent Bit → Elasticsearch → Kibana** ya configurada, y el guion `genera-trafico.sh`.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main && git pull
git switch -c sesion-09
```

!!! danger "La pila no se publica en internet"
    El fichero que se te entrega deja Elasticsearch y Kibana escuchando **solo en la propia máquina**, y no lleva autenticación. No abras esos puertos en el grupo de seguridad ni los publiques a través del proxy: se accede por un túnel SSH, como se indica en el paso 1. Un Kibana abierto al mundo es un buscador con todo tu tráfico dentro, direcciones de visitantes incluidas.

!!! info "Reparto de tiempo orientativo"
    Pasos 1 y 2, unos 35 minutos. Paso 3, unos 10. Paso 4, unos 35. Paso 5, unos 20. El paso 6 se empieza en clase y se termina fuera: es el documento de cierre del tema.

---

## Paso 1 — Levanta la pila y comprueba que llega algo

Arranca la instancia y tu servicio. Añade después la pila de observabilidad que se entrega con la actividad, levantándola **junto** a tu conjunto para que comparta su red. No tienes que configurarla: viene preparada, y lo que se aprende hoy es qué hacer con los datos, no administrar un motor de búsqueda.

Para llegar a Kibana desde tu equipo sin abrir ningún puerto nuevo, redirige el puerto por el túnel de tu conexión SSH y abre la interfaz en `http://localhost:5601`.

Antes de tocar nada, la comprobación que importa: pide unas cuantas páginas del catálogo y confirma que **están llegando registros del proxy y de las tres copias de la API**, no solo de una pieza. Identifica en la herramienta el campo que dice de qué contenedor viene cada línea.

**Comprueba**: en Kibana aparecen entradas del proxy y de al menos dos copias distintas, con marcas de tiempo de hace unos segundos.
**Captura**: el listado de servicios de las dos pilas en marcha y la vista de Kibana con registros de contenedores distintos.

!!! warning "Si Elasticsearch no arranca"
    Es la pieza más pesada de todo el curso. Si el contenedor se para solo al arrancar, mira sus registros: casi siempre es memoria, y se resuelve con el límite que viene comentado en el fichero. Comprueba también cuánta memoria libre tiene la instancia antes de dar por hecho que la culpa es de la configuración.

---

## Paso 2 — Registros que se pueden consultar

Los registros que están entrando son texto. Cambia el registro de acceso de tu proxy a **formato estructurado**, con un campo por dato, para que se pueda filtrar y agrupar sin inventar expresiones regulares.

Como mínimo tienen que aparecer, con nombre propio: la marca de tiempo, la dirección del visitante **original**, el método, la ruta pedida, el código de estado, **la duración de la petición** y **el destino que la ha atendido**. Los dos últimos son los que hacen posible el paso 4.

Dos avisos antes de escribirlo:

- Pídele a Nginx que **escape los valores como JSON**. Es una palabra en la declaración del formato, y sin ella la primera URL con comillas —el guion del paso 3 pide unas cuantas rutas raras— genera una línea que ya no es JSON válido y que el recolector descarta sin decir nada.
- El campo del destino no va a contener `api-1`, sino una dirección y un puerto. Es lo esperado: lo que importa es que aparezcan **tres valores distintos**.

Valida la configuración, recarga y genera unas cuantas peticiones, incluida alguna que falle.

**Comprueba**: en Kibana cada línea de acceso aparece con sus campos separados, y puedes filtrar por código de estado sin buscar texto.
**Captura**: el bloque de configuración del formato y una entrada desplegada en Kibana con todos sus campos.

!!! question "Reflexiona"
    Con el formato anterior, responder «cuántos errores ha habido en la ruta de productos» exigía buscar texto y contar a mano. **¿Qué es exactamente lo que ha cambiado para que ahora sea una consulta?** Y una comprobación que te conviene hacer: mira el campo de la dirección del visitante. ¿Coincide con la dirección desde la que estás haciendo las peticiones? Compara después ese dato con lo que registran las copias de la API y explica **por qué el proxy conoce directamente al cliente mientras que el backend necesita la información reenviada en la sesión 7**.

---

## Paso 3 — Tráfico de verdad

Un panel sobre cuarenta peticiones no dice nada. Ejecuta el guion `genera-trafico.sh` que se entrega con la actividad: lanza durante unos minutos una mezcla de peticiones correctas, rutas inexistentes, llamadas a la API y algunos errores provocados.

Míralo llegar mientras se ejecuta. Cuando termine, anota cuántas entradas has acumulado en total.

**Comprueba**: el volumen de registros crece de forma visible durante la ejecución, y al terminar hay suficientes entradas para observar con claridad la distribución de latencias, los errores y el reparto entre copias.
**Captura**: la gráfica de entradas por tiempo, con el pico del guion claramente visible.

---

## Paso 4 — Cuatro preguntas y sus respuestas

Construye un panel que responda, **con datos y no con impresiones**, a estas cuatro preguntas. Cada una es una visualización o una consulta guardada, y cada una se entrega con su respuesta escrita.

1. **¿Qué ruta falla más?** Rutas que devuelven `4xx` o `5xx`, ordenadas por número de peticiones.
2. **¿Cuántos errores de servidor ha habido en la última hora?** El recuento de `5xx` en esa ventana.
3. **¿Qué rutas van más lentas?** El percentil 95 de la duración, por ruta. Compáralo con la media de esas mismas rutas.
4. **¿Se están repartiendo el trabajo las tres copias?** El recuento de peticiones agrupado por el destino que las atendió.

Las dos últimas son las interesantes. En la tercera vas a ver por qué en la teoría insistimos en los percentiles; en la cuarta, lo que la semana pasada intuiste pidiendo el identificador de instancia nueve veces, ahora medido sobre todo el tráfico. Y el resultado no tiene por qué ser el que esperas.

**Comprueba**: el panel muestra las cuatro respuestas y se refresca al generar tráfico nuevo.
**Captura**: el panel completo y, en la plantilla, la respuesta concreta a cada una de las cuatro preguntas con sus números.

!!! question "Reflexiona"
    Dos preguntas sobre lo que has medido. La primera, sobre la latencia: **¿en qué se diferencian la media y el percentil 95 de tus rutas más lentas, y a qué usuario describe cada uno de los dos números?** La segunda, sobre el reparto: ¿están las tres copias igual de cargadas? Si hay diferencias, di si te parecen razonables y de dónde pueden venir. Y una que hoy ya puedes contestar sola: si una copia hubiera dejado de atender peticiones a las 18:20, ¿en cuál de las cuatro visualizaciones lo verías, y qué mirarías después para saber por qué?

---

## Paso 5 — Diseña dos alertas y cruza su umbral

Un panel sirve mientras alguien lo mira. Para que el sistema se vigile solo, algunas de estas consultas tienen que convertirse en **condiciones de alerta**.

Diseña dos:

1. **De errores**: demasiadas respuestas `5xx` durante una ventana de tiempo.
2. **De seguridad o de disponibilidad**, a elegir: demasiados `401` contra la zona de informes desde una misma dirección —que es exactamente el aspecto que tiene alguien probando contraseñas—, o una de las copias que deja de aparecer como destino durante un rato.

Para cada una indica en la plantilla:

- qué dato vigila y sobre qué consulta se apoya;
- qué umbral usarías;
- durante qué ventana;
- a quién avisaría;
- **qué debería hacer esa persona** al recibir el aviso.

Después **provoca la situación** —genera los errores, lanza los intentos fallidos o para una copia— y usa tus propias consultas para demostrar que la condición habría cruzado el umbral que has elegido. La infraestructura que envía el aviso no la montas hoy: en el módulo de nube convertirás una condición de este tipo en una alarma real que manda una notificación.

**Comprueba**: en el panel se ve el momento en que cada condición supera su umbral.
**Captura**: la definición de las dos alertas con sus cinco puntos, y la consulta o gráfica donde se ve la condición cumplida.

!!! question "Reflexiona"
    Justifica tus dos umbrales y tus dos ventanas: **¿por qué ese número y no la mitad?** Piensa qué pasaría con una ventana de diez segundos y qué pasaría con una de dos horas. Y la pregunta de fondo: si mañana llegaran veinte avisos al día y diecinueve no requiriesen hacer nada, ¿en qué se habría convertido tu sistema de alertas?

---

## Paso 6 — La memoria del tema

Este documento cierra el bloque y es la evidencia de las cuatro sesiones. Escríbelo en `entregas/tema3/memoria-servidor-web.md`, pensado para alguien que se hace cargo de tu servicio mañana:

- **La arquitectura**, en un diagrama: por dónde entra una petición, qué la atiende, qué hay detrás y qué comparten esas piezas.
- **La configuración del servidor web**, explicada por bloques: qué sirve cada sitio, cómo se enruta hacia la API, cómo se reparte entre copias y qué cabeceras se reenvían y por qué.
- **La administración segura**: qué está protegido y con qué, qué certificado hay y quién se encarga de renovarlo, qué puertos están abiertos y cuáles no, y qué cabeceras de seguridad se envían.
- **La observabilidad**: qué se recoge, dónde se consulta, qué condiciones vigilarías y qué debería hacer quien recibiera cada aviso.
- **Las tres incidencias del tema**: la foto que unas veces estaba y otras no, y otras dos que hayas sufrido tú. De cada una: qué síntoma diste, qué miraste y cómo se resolvió.

Abre la petición de fusión de `sesion-09` hacia la rama principal.

**Captura**: la memoria renderizada en el repositorio.

!!! tip "Apunta lo que te ha costado"
    Anota cuánto tiempo has dedicado hoy a levantar la pila y cuánta memoria consume en la instancia. Los vas a necesitar muy pronto para compararlos con algo.

---

## Si te sobra tiempo

**Sigue una petición entera.** Configura el proxy para que genere un identificador único por petición, lo escriba en su registro y lo reenvíe al backend en una cabecera. Después localiza una petición concreta y sigue su rastro por las dos piezas que la atendieron.

**Ponle un límite al almacén.** Averigua cuánto espacio están ocupando ya tus registros, calcula cuánto sería a este ritmo en un mes y decide cuántos días conservarías. Escribe la cifra en la memoria: es una decisión de administración, no un detalle.

---

## Verificación

Sobre la instancia en marcha, sustituyendo `<sub>` por el subdominio del equipo:

```bash
# el servicio sigue en pie tras todo lo de hoy
curl -s -o /dev/null -w "catalogo %{http_code}\n" https://<sub>/
curl -s -o /dev/null -w "informes sin credenciales %{http_code}\n" https://<sub>/informes/

# la pila está levantada y solo escucha en la propia máquina
docker compose ps

# entran registros y están estructurados
curl -s "localhost:9200/_cat/indices?v"
curl -s "localhost:9200/_search?size=1&sort=@timestamp:desc" | head -40

# el reparto entre copias, con datos
curl -s -H 'Content-Type: application/json' localhost:9200/_search -d '
{"size":0,"aggs":{"destinos":{"terms":{"field":"destino.keyword"}}}}'

# percentiles de latencia por ruta
curl -s -H 'Content-Type: application/json' localhost:9200/_search -d '
{"size":0,"aggs":{"rutas":{"terms":{"field":"ruta.keyword","size":5},
"aggs":{"p95":{"percentiles":{"field":"duracion","percents":[50,95]}}}}}}'

# errores de la última hora
curl -s -H 'Content-Type: application/json' localhost:9200/_search -d '
{"size":0,"query":{"bool":{"filter":[{"range":{"estado":{"gte":500}}},
{"range":{"@timestamp":{"gte":"now-1h"}}}]}}}'
```

Y debe observarse:

- Que el servicio de las sesiones anteriores **sigue funcionando**: HTTPS, zona protegida y tres copias.
- Que la pila de observabilidad está en marcha y **no publica ningún puerto al exterior**.
- Que llegan registros del proxy y de las tres copias, no solo de una pieza.
- Que las entradas de acceso están **estructuradas**, con la dirección del visitante original, la duración y el destino de cada petición.
- Que la agrupación por destino devuelve **tres valores** con recuentos del mismo orden de magnitud.
- Que la consulta devuelve p50 y p95 y el alumno interpreta correctamente qué representa cada uno.
- Que el panel responde a las cuatro preguntas.
- Que las dos alertas están diseñadas con umbral, ventana, destinatario y acción, y que hay evidencia de que la condición se cumplió.
- Que la memoria está en `entregas/tema3/` y cubre los cinco apartados.
- Que la entrega llega a la rama principal **a través de una petición de fusión** desde `sesion-09`.

---

## Qué se entrega

- [ ] La pila levantada junto al despliegue, con registros del proxy y de las copias.
- [ ] El registro de acceso en formato estructurado, con la duración y el destino de cada petición.
- [ ] La reflexión sobre qué ha cambiado al pasar a formato estructurado.
- [ ] La gráfica del tráfico generado, con el volumen total acumulado.
- [ ] Las cuatro preguntas respondidas con sus números y el panel que las contiene.
- [ ] La comparación entre media y percentil 95, y la reflexión sobre el reparto real entre las tres copias.
- [ ] Las dos alertas diseñadas, con sus cinco puntos y su justificación.
- [ ] La evidencia de que las dos condiciones cruzaron su umbral al provocarlas.
- [ ] `entregas/tema3/memoria-servidor-web.md` con sus cinco apartados y el diagrama.
- [ ] La petición de fusión de `sesion-09`, fusionada.
- [ ] La plantilla de la actividad entregada en Moodle, con la URL de la petición de fusión.

---

## ✅ Cierre

Con esto se cierra el bloque de administración de servidores web, y merece la pena mirar atrás cuatro semanas. Empezaste con un Nginx que servía ficheros de una carpeta y una configuración prestada que no podías abrir. Terminas con un servicio publicado en internet bajo su propio nombre, con una sola puerta, tres copias repartiéndose el trabajo, una zona privada, un certificado de verdad que se renueva sin ti, y un sistema que recoge lo que pasa y permite preguntárselo. Cada pieza la has puesto tú sabiendo qué problema resolvía.

Guarda los dos números que apuntaste en el paso 6 —lo que has tardado y lo que consume—, porque el miércoles vas a montar esto mismo en su versión gestionada y la comparación de esfuerzo, coste y visibilidad es la entrega conjunta de las dos asignaturas. Ahí es donde se ve para qué ha servido montarlo a mano, y ahí es donde esas dos alertas que hoy has diseñado se convertirán en avisos que llegan de verdad.

Y queda un asunto pendiente que llevamos aplazando desde el primer día. En la sesión 7, cuando pusiste