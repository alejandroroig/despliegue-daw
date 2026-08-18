# 🧩 2. Rendimiento y pruebas de carga

!!!info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/rendimiento-pruebas-carga.pptx){target="_blank" rel="noopener"}

---

La semana pasada dejaste el servicio a prueba de que el proxy te cambie de copia. Ninguna de las tres guarda ya nada que un usuario necesite volver a encontrar, y eso significa que ahora son intercambiables de verdad. Con eso resuelto aparece la pregunta que llevas evitando desde que montaste el proxy en la sesión 7 y que ningún despliegue del curso ha respondido todavía: **¿cuánta carga aguanta esto?**

Y hay una segunda pregunta escondida detrás, que es la de verdad interesante. Cuando pusiste tres copias en lugar de una, diste por hecho que el servicio aguantaría el triple. Nadie lo comprobó. Hoy lo vas a comprobar y te adelanto que la respuesta va a ser que no, ni de lejos. Entender **por qué** no es la lección de la sesión, y es una lección que mucha gente con años de experiencia no tiene clara.

La buena noticia es que no partes de cero. Desde la sesión 9 tu registro de acceso guarda, para cada petición, cuánto tardó y qué copia la atendió. Llevas dos semanas acumulando datos y todavía no le has hecho al panel la pregunta correcta.

---

## 📊 Tres magnitudes que se confunden

Cuando alguien dice «la aplicación va lenta» está mezclando tres cosas distintas que se miden aparte y se arreglan distinto.

La **latencia** es lo que tarda *una* petición desde que sale hasta que llega la respuesta completa. Se mide en milisegundos y es lo único que el usuario percibe. A un usuario le da exactamente igual cuántas peticiones más estés sirviendo: solo nota la suya.

El **throughput** —caudal, o peticiones por segundo— es cuántas peticiones despachas en una unidad de tiempo. Es lo que le importa a quien paga la infraestructura, porque determina cuánta gente cabe.

La **concurrencia** es cuántas peticiones tienes dentro del sistema a la vez, a medio atender. No es lo mismo que usuarios conectados: alguien leyendo una página no consume nada.

Las tres están atadas por una relación que conviene tener en la cabeza, válida **en régimen estable y con unidades coherentes**:

> concurrencia ≈ throughput × latencia

Si atiendes 200 peticiones por segundo y cada una tarda 0,25 segundos, en todo momento hay unas 50 peticiones dentro del sistema. La relación es útil porque explica el efecto más contraintuitivo del rendimiento: **si la latencia sube y las peticiones siguen llegando al mismo ritmo, la concurrencia sube sola**. Y como cada petición dentro del sistema ocupa un hilo, una conexión y memoria, el sistema que va un poco lento tiende a degradarse hasta ir muy lento. Por eso los servicios saturados no se degradan de forma suave.

---

## 📈 La media y la cola

Esto ya lo sufriste en la sesión 9, cuando el tiempo medio de respuesta del panel no reflejaba lo que estabas viendo en el navegador. Toca ponerle números.

Un **percentil** responde a «¿por debajo de qué valor está el X % de mis peticiones?». Si tu p95 es 400 ms, el 95 % tardó menos de eso y el 5 % restante tardó más. El problema de la media es que **por sí sola puede ocultar por completo la cola de latencia**: mil peticiones de 50 ms y diez de 8 segundos dan una media de 130 ms, un número que no describe bien ni a las mil ni a las diez.

| Percentil | Qué te dice | Para qué se usa |
|---|---|---|
| **p50** (mediana) | La experiencia típica | Comparar versiones entre sí |
| **p95** | Dónde empieza a doler | Es uno de los percentiles más utilizados al fijar objetivos |
| **p99** | El peor caso realista | Detectar saturación y pausas de recolección de basura |

Y hay una razón por la que el p99 importa más de lo que su nombre sugiere. Una página de Escaparate carga el HTML, el CSS, el script y una veintena de imágenes: unas veinticinco peticiones. Si cada una tuviera un 1 % de probabilidad de caer en el tramo lento, la probabilidad de que **alguna** de las veinticinco lo hiciera rondaría el 22 %. Un p99 malo no afecta al 1 % de las cargas de página, afecta a una de cada cinco. La cola de la distribución no es una anécdota estadística.

---

## 🕳️ Qué escalas de verdad cuando replicas

Aquí se recoge algo que quedó abierto la semana pasada: dijimos que tu código no controla los hilos, que de eso se encarga el contenedor de servlets, y que ese detalle era la mitad de la explicación de hoy. La otra mitad es esta.

En tu sistema hay **dos clases de límite**, y se comportan de forma muy distinta.

Los **límites lógicos** son números en un fichero de configuración: cuántas conexiones simultáneas admite el proxy, cuántos hilos tiene la reserva del contenedor de servlets, cuántas conexiones abre la aplicación contra la base de datos. Se pueden subir editando un valor.

Los **recursos físicos** son la CPU, la memoria, el disco y la red de la máquina. No se editan.

Y ahora el punto. Cuando en la sesión 7 pasaste de una copia a tres, **multiplicaste por tres los límites lógicos de la aplicación y no añadiste ni un núcleo de CPU**, porque las tres copias viven en la misma instancia:

```text
        1 copia                            3 copias

   ┌────────────────┐              ┌────────┬────────┐
   │   Escaparate   │              │  Esc.  │  Esc.  │
   │                │              ├────────┴────────┤
   │                │              │     Escaparate  │
   └───────┬────────┘              └────────┬────────┘
           │                                │
   ┌───────┴────────────────────────────────┴────────┐
   │   la misma instancia: los mismos núcleos,       │
   │   la misma memoria, el mismo disco y red        │
   └────────────────────────┬────────────────────────┘
                            │
                 PostgreSQL · Redis · la pila de logs
```

Añade a eso un detalle que cambia el argumento: **una sola copia de Escaparate ya atiende peticiones en paralelo y puede usar varios núcleos**, porque el contenedor de servlets reparte el trabajo entre su reserva de hilos. No es que una instancia sola deje la máquina esperando turno. Poner tres copias reparte ese mismo trabajo entre más procesos —y añade el coste de tener tres máquinas virtuales de Java en marcha en lugar de una—, pero no pone más núcleos sobre la mesa.

Entonces, ¿estaba mal poner tres copias? En absoluto. Estaban ahí por otra cosa, y esa cosa es real: **tolerancia al fallo de una copia y actualizaciones progresivas sin detener el servicio**. Una copia se cae y el servicio sigue; actualizas de una en una y nadie se entera. Ojo con el alcance de esa protección: te cubre del fallo de *una copia*, no del fallo de la máquina, porque las tres la comparten.

Lo que tres copias en la misma máquina no añaden es **capacidad física**. El rendimiento puede moverse algo al repartir el trabajo entre varios procesos, pero todas siguen compitiendo por los mismos núcleos, la misma memoria, el mismo disco y la misma red, así que no puedes esperar una mejora proporcional. Confundir disponibilidad con capacidad es uno de los errores más caros que se cometen en producción, porque se paga infraestructura que no rinde.

!!! warning "Y la base de datos tampoco se ha triplicado"
    Cuidado con la versión simplificada de esto. **Cada copia de Escaparate abre su propia reserva de conexiones** contra PostgreSQL, así que al replicar la aplicación puedes acabar con el triple de conexiones compitiendo por el mismo servidor. Lo que no se ha multiplicado es PostgreSQL: su CPU, su memoria, su disco y su capacidad de ejecutar consultas siguen siendo los mismos, y ahora tiene tres clientes en lugar de uno.

La conclusión, y merece la pena aprendérsela tal cual: **replicar en el mismo host reparte el trabajo, pero no añade recursos**. Solo escalas de verdad aquello que dejas de compartir.

---

## 🧪 Cómo se diseña una prueba que signifique algo

Una prueba de carga mal hecha es peor que no hacerla, porque produce números que la gente se cree. Estas son las decisiones que la hacen válida.

**Un experimento por fila de la tabla.** Es tentador escribir un único guion que suba la carga por escalones y sacar de ahí toda la tabla. No funciona: el resumen que k6 imprime al terminar **agrega la ejecución entera**, así que un guion con cuatro escalones te da un solo juego de números con todo mezclado. Lo que sí funciona es lo más simple: una ejecución corta por cada nivel de carga, cada una con su resumen. Una fila = un experimento.

**El calentamiento es una ejecución que se tira.** La máquina virtual de Java arranca interpretando el código y solo después de ejecutarlo miles de veces lo compila a instrucciones nativas optimizadas; las primeras peticiones pueden ir varias veces más lentas que el régimen normal. Un tramo de calentamiento *dentro* del mismo guion no desaparece del resumen, así que la forma limpia de hacerlo es lanzar una ejecución entera y descartarla.

**Una sola variable.** Si comparas una copia contra tres, todo lo demás tiene que ser idéntico: mismo guion, mismo nivel, misma duración, mismo endpoint, misma máquina. Y conviene apagar lo que no participe: el Tomcat externo de la semana pasada no forma parte del sistema que estás midiendo y sí consume memoria.

**Una repetición de control al final.** Cuando termines, vuelve a ejecutar la primera condición. Si no reproduce el primer resultado, la medida se te ha ido y las conclusiones no valen. Cuesta un minuto y es lo que separa medir de mirar.

!!! warning "Por qué esa repetición no es un capricho"
    Muchas instancias de nube son **ampliables por créditos**: tienen un rendimiento base bajo y solo van rápidas mientras les quedan créditos acumulados. Una serie de pruebas de carga los consume, y a partir de ahí la máquina se estrangula. El efecto es traicionero: **la última medición sale peor que la primera aunque no hayas cambiado nada**, y si no lo detectas atribuirás al número de copias algo que solo era el orden en que mediste. La repetición de control lo pone en evidencia.

**Y una advertencia honesta sobre dónde se genera la carga.** Hoy k6 se ejecuta en la propia instancia, para que la red del aula no contamine la medida y para que veinticinco personas midiendo a la vez no se estorben entre sí. Pero el generador de carga compite por CPU y memoria con el sistema que estás midiendo, y en la misma máquina tienes además la base de datos, el almacén de sesiones y la pila de logs. Así que lo que obtengas **no es la capacidad de Escaparate**: es la capacidad medida en las condiciones de este laboratorio. Sirve perfectamente para comparar dos configuraciones entre sí, que es lo que vamos a hacer, y no sirve para prometerle un número a un cliente. Decirlo en el informe no es una excusa, es rigor.

### El criterio de capacidad

Si cada uno decide por su cuenta qué latencia le parece aceptable, veinte personas calcularán veinte capacidades incomparables. Así que Escaparate tiene, para esta práctica, un compromiso escrito:

> **p95 por debajo de 500 ms y menos del 1 % de errores.**

No son números universales: cada servicio pacta los suyos según lo que haga. Pero son los nuestros, y tienen dos virtudes. Convierten «va lento» en algo comprobable, y hacen comparables los resultados de toda la clase. k6 sabe codificar ese criterio y decirte al terminar cada ejecución si lo cumple o no.

Con eso, **capacidad** deja de ser «donde la curva parece doblarse» y pasa a ser algo preciso: **el mayor nivel de carga que cumple los dos criterios**.

---

## 🔎 Leer los resultados y localizar la capa

Una serie de ejecuciones da una tabla con esta forma. Los números son de ejemplo, pero el patrón es el que vas a obtener:

| Configuración | Carga | Peticiones/s | p95 (ms) | Errores | ¿Cumple? |
|---|---|---|---|---|---|
| 3 copias | 10 | 92 | 140 | 0 % | sí |
| 3 copias | 25 | 168 | 310 | 0 % | sí |
| 3 copias | 50 | 181 | 690 | 0,2 % | **no** |
| 3 copias | 100 | 179 | 1.480 | 3,1 % | **no** |

La capacidad de esta configuración es **25**, porque es el mayor nivel que cumple el criterio. Fíjate en lo que pasa por encima: entre 50 y 100 no despachas ni una petición más —179 frente a 181— pero la espera se duplica y empiezan a aparecer errores. **Aceptar más carga de la que puedes servir no aumenta lo que sirves, solo empeora la experiencia de todos.** De ahí que en producción se pongan límites de admisión: más vale rechazar rápido a unos pocos que degradar a todos.

Esa forma de la curva —throughput que se aplana y latencia que crece— no es casualidad, es consecuencia de cómo mide k6. Un **usuario virtual** lanza una petición, espera la respuesta y solo entonces lanza la siguiente. Es un modelo cerrado: cuando el sistema se atasca, los usuarios virtuales se atascan con él y dejan de presionar. Por eso un usuario virtual no equivale a una persona —una persona real no espera a que le contestes para volver a pulsar— y por eso hablamos de *niveles de carga* y no de *número de usuarios*.

!!! info "Para saber más"
    Existe el modelo contrario, llamado de tasa de llegada, en el que la herramienta envía a un ritmo fijo pase lo que pase, como hace el tráfico real. Es más realista para estimar capacidad y bastante más agresivo, porque cuando el sistema se satura la cola crece sin freno. k6 lo soporta; nosotros nos quedamos en el modelo de usuarios virtuales, que basta para comparar.

### Hasta dónde puedes llegar con lo que tienes

Los resúmenes te dicen *que* hay un techo, no *dónde* está. Para eso tienes dos fuentes: el panel de la sesión 9, que registra de cada petición cuánto tardó en total y cuánto tardó el destino en contestar, y una foto del consumo de los contenedores tomada en el momento de máxima carga.

| Lo que observas | Qué puedes concluir |
|---|---|
| El proxy tarda mucho y el destino le contestó rápido | El límite está delante: proxy o red |
| El destino tarda solo en una copia | Problema de esa copia concreta, no de capacidad |
| El destino tarda en las tres **y** el host está saturado | Recurso de la máquina: CPU o memoria compartidas |
| El destino tarda en las tres **y** el host está holgado | Dependencia compartida: base de datos, almacén de sesiones, disco |

Ahí se acaba lo que tus datos demuestran, y conviene ser honesto con el límite. Para separar «PostgreSQL» de «el disco» o de «una pausa de recolección de basura» harían falta métricas que hoy no recoges: consultas lentas, conexiones activas, uso de memoria de la máquina virtual de Java. Así que el objetivo profesional no es cantar un culpable, es **localizar hasta donde los datos permitan y formular una hipótesis diciendo qué medirías a continuación para confirmarla**. Eso se escribe en un informe sin sonrojarse; señalar a la base de datos sin haberla medido, no.

Y el experimento que lo ata todo: **la misma serie con una copia y con tres, y el factor de mejora entre ambas**. Si con tres obtienes cerca del triple, replicar en esta máquina estaba aportando capacidad. Si obtienes un 1,1 o un 1,2 —que es lo más probable—, ya sabes que las copias están compartiendo el límite y que una cuarta no cambiaría nada.

---

## ⚖️ Repartir, ampliar u optimizar

Con el cuello localizado, la decisión tiene tres salidas y no es obvia cuál es la buena.

**Repartir** es sacar las copias de la misma máquina y ponerlas en máquinas distintas. Es la única forma de que replicar añada recursos de verdad, y es exactamente lo que hace un grupo de escalado automático como el que visteis en INU: por eso lanza instancias y no procesos.

**Ampliar** es darle más recursos a lo que se queda corto: una máquina mayor para la aplicación, o una base de datos mayor si el límite está ahí. Es rápido, no toca código, y tiene un techo y una factura.

**Optimizar** es hacer que cada petición cueste menos: una consulta que recorría la tabla entera, una llamada repetida en bucle, una imagen sin comprimir, una caché que no existe. Es más lento y requiere entender el código, pero el efecto es permanente y no aumenta el gasto mensual.

La regla no es «factor bajo, optimiza». Es más matizada y más útil: **un factor alto te dice que replicar esa capa aumenta capacidad; un factor bajo te dice que el límite está en algo que las copias siguen compartiendo, y el siguiente paso es identificar ese recurso y decidir si conviene repartirlo, ampliarlo o exigirle menos trabajo.** La respuesta depende de qué recurso sea, y por eso el diagnóstico va antes que la decisión.

Queda una cuarta salida que casi siempre gana cuando aplica: **no hacer el trabajo**. La petición más rápida es la que no llega al servidor porque el navegador ya tenía la respuesta.

---

## 🎯 Qué debes saber hacer al salir de esta sesión

- Ejecutar una serie de pruebas de carga cortas con k6, una por nivel, descartando la primera como calentamiento y repitiendo una condición como control.
- Interpretar el resumen de k6 —peticiones por segundo, p95 y tasa de error— y decir si esa ejecución cumple el criterio pactado.
- Determinar la capacidad de una configuración como el mayor nivel de carga que cumple el criterio.
- Comparar una copia contra tres, calcular el factor de mejora y explicar qué recurso no se ha multiplicado al replicar.
- Localizar el cuello de botella hasta donde los datos lo permitan, y formular una hipótesis sobre el recurso compartido diciendo qué medirías después para confirmarla.
- Elegir de forma razonada entre repartir, ampliar y optimizar, justificándolo con tus propios números.

**Lo que basta con reconocer**: los tipos de prueba que no hacemos hoy —estrés, resistencia y pico—, el modelo de tasa de llegada frente al de usuarios virtuales, los nombres concretos de los parámetros que fijan cada límite lógico, y los objetivos de nivel de servicio tal como se pactan en una empresa.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - Latencia es lo que percibe quien espera, throughput es cuánto despachas, y concurrencia es cuánto tienes dentro a la vez; en régimen estable, concurrencia ≈ throughput × latencia.
    - La media por sí sola puede ocultar la cola de latencia: por eso se miran percentiles, y el p95 es uno de los más usados al fijar objetivos.
    - Como una sola página lanza decenas de peticiones, un p99 malo no afecta al 1 % de las cargas de página sino a una de cada pocas.
    - Hay límites lógicos, que se editan en un fichero, y recursos físicos, que no. Replicar multiplica los primeros y no añade ni uno de los segundos.
    - Una sola copia ya atiende peticiones en paralelo sobre varios núcleos: replicar en el mismo host reparte el trabajo entre más procesos, pero no añade recursos.
    - Tres copias en una máquina dan tolerancia al fallo de una copia y actualizaciones progresivas, no capacidad ni protección frente a la caída de la propia máquina.
    - Cada copia abre su propia reserva de conexiones: al replicar puedes aumentar la presión sobre la base de datos, que sigue siendo una sola.
    - El resumen de k6 agrega la ejecución entera, así que cada nivel de carga se mide en una ejecución propia y el calentamiento es una ejecución que se descarta.
    - Capacidad es el mayor nivel de carga que cumple un criterio pactado de latencia y errores, no el punto donde la curva parece doblarse.
    - Medir con el generador de carga dentro de la máquina medida sirve para comparar configuraciones, no para prometer un número: eso hay que escribirlo en el informe.
    - Un factor de mejora bajo indica un recurso compartido; identificarlo decide si toca repartir, ampliar u optimizar.

---

Con esto ya tienes las piezas para la **Actividad 4.2**: vas a medir tu propio despliegue con tres copias y con una, vas a determinar la capacidad de cada configuración con un criterio común, y vas a usar el panel que montaste hace dos semanas para señalar hasta dónde llegan tus datos y dónde empieza tu hipótesis. El informe con el que la cierres es también el cierre del bloque de servidores de aplicaciones: a partir de la semana que viene el asunto ya no es cómo se ejecuta Escaparate, sino cómo consigues que se despliegue sola.