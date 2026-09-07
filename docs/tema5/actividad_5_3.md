# 🧪 Actividad 5.3: El repositorio manda

## Contexto

Última sesión de contenido. El encargo es el de una empresa que ya ha decidido: la plataforma pasa a un clúster gestionado y los despliegues dejan de hacerse entrando en las máquinas. Tu trabajo de hoy es dejarlo montado y **demostrar con hechos** las dos afirmaciones que sostienen esa decisión: que el repositorio manda sobre el clúster, y que desplegar una versión nueva ya no necesita ninguna credencial del entorno de producción.

Fíjate en cómo está redactada esa segunda afirmación, porque hoy vas a comprobar por qué importa el matiz: para **crear** la plataforma seguirás necesitando credenciales muy potentes, y las vas a pegar en tu instancia en los próximos diez minutos.

Y hay una parte del encargo que no es técnica. Esta semana el mismo contenedor se ha ejecutado como servicio gestionado el miércoles en INU, y hoy se orquesta aquí. Con lo de hoy tendrás las cuatro formas de desplegar el catálogo, y la entrega conjunta consiste en compararlas con datos tuyos, no con opiniones.

## Qué vas a practicar

- Crear y destruir un clúster gestionado con código, midiendo lo que tarda cada cosa.
- Adaptar unos manifiestos a un entorno con restricciones distintas, justificando cada cambio.
- Instalar un agente de despliegue por extracción y comprobar en directo la reconciliación.
- Desplegar una versión nueva sin abrir una consola ni una sesión SSH.

## Requisitos previos

- Rama `sesion-16` abierta desde `main`, con `k8s/base/` completo tal como quedó la semana pasada.
- VS Code conectado por SSH a la instancia. **Toda la sesión se trabaja desde ahí**: es donde están las herramientas y donde vas a configurar las credenciales.
- No hace falta que sepas escribir infraestructura como código: el código va entregado y solo rellenas variables. Si cursas INU ya lo has usado; si no, hoy es andamiaje y no se evalúa.

Ficheros que se entregan hoy (carpeta `entregados/tema6/`):

- `infra/` — el código del clúster, con las variables que tienes que rellenar señaladas.
- `nginx-ic-eks.yaml` — el mismo controlador de Ingress de la semana pasada, con su Service de tipo `LoadBalancer`.
- `argocd-install.yaml` — manifiesto de instalación del agente, descargado.
- `fragmentos-6-3.yaml` — el Deployment de la base de datos sin volumen persistente, el objeto `Application` con huecos, y un bloque de reparto entre nodos con instrucciones de dónde pegarlo.

!!! info "Reparto de tiempo orientativo"
    **10:00** Paso 1: credenciales y lanzamiento del clúster, **antes de la teoría**. **10:15 a 11:00**, teoría, mientras se construye. Paso 2 y **punto de control a las 11:15**. Paso 3, hasta las 11:45. Paso 4, hasta las 11:55. Paso 5, hasta las 12:20. Paso 6, hasta las 12:45. **12:45: alto obligatorio, Paso 7 — se lanza la destrucción.** Mientras destruye, Paso 8 y pull request.

---

## Paso 1 — Lanzar el clúster antes de que empiece la clase

Cuatro cosas seguidas, sin pararte a entenderlas del todo: hay tiempo para eso mientras se construye.

Arranca el laboratorio y **configura sus credenciales en la instancia**. Son temporales y llevan tres piezas, no dos: identificador, clave secreta y **token de sesión**. Si te dejas el token, todo fallará con un error de autenticación que no lo menciona.

Después, **averigua los identificadores de los dos roles** que el laboratorio te da hechos: uno para el plano de control y otro para los nodos. No los puedes crear tú y **su nombre cambia cada semana**, así que se consultan ahora y se pegan en las variables. Ese es el motivo de que no estén escritos en el repositorio.

Rellena el resto de variables —nombre del clúster con el identificador de tu equipo, tipo de nodo y número de nodos: **dos, ni uno ni tres**— y lanza la creación. Anota la hora exacta.

No te quedes mirando. Cuando esté lanzado, empieza la teoría.

**Comprueba**: la creación arranca sin errores de credenciales ni de permisos.
**Captura**: `entregas/tema6/6-3-tiempos.txt`, que hoy es un cuaderno de bitácora. Primera línea: hora del lanzamiento.

!!! question "Reflexiona"
    Acabas de pegar en tu instancia unas credenciales capaces de crear y destruir un clúster entero. **(a)** ¿Por qué el código **referencia** dos roles existentes en vez de crearlos, y qué harías distinto en una cuenta de empresa? **(b)** Al final de la sesión te preguntaré qué credenciales han hecho falta para desplegar la aplicación. Apunta ya, aquí, cuáles han hecho falta para **crear la plataforma**: no es la misma lista.

---

## Paso 2 — Entrar, y ver qué falta

Cuando el clúster esté listo, configura el acceso y comprueba los nodos. Anota en la bitácora la hora a la que aparecieron dos nodos listos y calcula cuánto ha pasado desde el Paso 1.

Después mira dos cosas concretas: qué clases de almacenamiento hay utilizables, y qué controladores de Ingress hay instalados.

**Comprueba**: dos nodos en estado listo; **ninguna clase de almacenamiento utilizable** y **ningún controlador de Ingress**.
**Captura**: la salida de las tres consultas en la bitácora, con la hora.

!!! danger "Punto de control — 11:15"
    Si a las 11:15 no tienes dos nodos listos, **se abandona el clúster gestionado**. Documenta en la bitácora qué error hubo y en qué punto se quedó, destruye lo que se haya creado, y recrea tu clúster local de la semana pasada. Los pasos 5 y 6 se hacen allí: pierdes la evidencia del entorno gestionado, pero **el aprendizaje central de hoy —extracción, reconciliación y deriva— no depende de quién sea el dueño del plano de control**. Avísame para que te dé la variante del enunciado.

!!! question "Reflexiona"
    Los manifiestos de la semana pasada incluían una reclamación de volumen y un Ingress. Con lo que acabas de ver, ¿cuál de los dos fallaría con un error claro y cuál se quedaría creado sin que nadie te avisara de nada? Predícelo ahora; lo compruebas en el paso siguiente.

---

## Paso 3 — El catálogo, adaptado, con una sola puerta

Crea `k8s/eks/` partiendo de `k8s/base/` y adáptalo. Cada cambio, justificado en un comentario dentro del propio fichero:

- **La base de datos pierde el volumen persistente.** Usa el fragmento entregado: mismo Deployment, mismo Service, mismo nombre, con almacenamiento efímero.
- **El Ingress pierde el nombre y el TLS.** Misma regla, pero sin `host` y sin bloque de certificado, para que responda por cualquier nombre con el que llegue el tráfico.
- **El Deployment del catálogo recibe el bloque de reparto entre nodos** que va en el fragmento. Pégalo donde indica el comentario y no te preocupes por su sintaxis: no es evaluable. Está para obligar al planificador a repartir las copias entre las dos máquinas, y sin él lo que vas a comprobar en el paso siguiente podría no ocurrir.
- **El Secret** se aplica a mano desde tu plantilla, como siempre.

Instala después el controlador con el manifiesto entregado. Su Service es de tipo `LoadBalancer`, así que al aplicarlo AWS te construye un balanceador de verdad: espera a que tenga nombre asignado, puede tardar un par de minutos.

Aplica todo con `kubectl` esta primera vez.

**Comprueba**: pods listos y **repartidos entre los dos nodos**; el catálogo carga por el nombre del balanceador y muestra productos.
**Captura**: `entregas/tema6/6-3-catalogo.png` con el catálogo respondiendo por la dirección del balanceador, y la salida del listado de pods **mostrando en qué nodo está cada uno**.

!!! question "Reflexiona"
    Hoy tienes **un solo** balanceador. **(a)** Si mañana publicaras tres aplicaciones más, ¿cuánto crecería tu factura de balanceadores con el montaje que acabas de hacer, y cuánto habría crecido publicando cada una con su propio Service de tipo `LoadBalancer`? **(b)** La semana pasada el Service del controlador era de tipo puerto de nodo y hoy es de tipo balanceador. Los Ingress no han cambiado. ¿Qué te dice eso sobre dónde está la parte portable de tu configuración y dónde la que depende del sitio?

!!! tip "Punto de rescate — 11:35"
    Si a las 11:35 no cargas el catálogo, avísame y publico el directorio `k8s/eks/` resuelto: sin esto no hay Paso 5, y el Paso 5 es la sesión.

---

## Paso 4 — Las imágenes, por tercera vez

Antes de nada, confirma que tus copias están en máquinas distintas: si las tres hubieran caído en el mismo nodo, lo que viene no ocurriría y no aprenderías nada.

Sube una imagen a un producto desde el catálogo y recarga la página varias veces seguidas, con calma. Observa qué ocurre.

**Comprueba**: los pods están repartidos entre los dos nodos; la imagen aparece unas veces y otras no.
**Captura**: dos capturas del mismo producto, una con imagen y otra sin ella, en `entregas/tema6/`.

!!! question "Reflexiona"
    Este fallo ya lo viste en la S7 y volviste a rozarlo en la S14, donde no dio la cara. **(a)** ¿Por qué en la S14 funcionaba y hoy no? **(b)** La solución que aplicaste en la S7 no sirve aquí: ¿por qué? **(c)** Nombra la solución que sí valdría, y di dónde la has usado ya este curso.

---

## Paso 5 — El agente, y la deriva

Instala Argo CD con el manifiesto entregado y espera a que sus pods estén listos. Declara después, desde `argocd/escaparate-application.yaml`, una `Application` que apunte a **tu repositorio, rama `main`, directorio `k8s/eks`**, con sincronización automática, autorreparación y purga activadas.

Fíjate en que ese fichero vive **fuera** del directorio que el agente vigila: no queremos que la `Application` se administre a sí misma.

El estado inicial será sincronizado, porque el clúster es igual que el repositorio: lo aplicaste tú hace veinte minutos. A partir de ahora, el repositorio manda.

Y ahora provoca la deriva: **escala la API a cinco réplicas con una orden directa** y quédate mirando. Anota en la bitácora cuánto tarda en volver a tres.

**Comprueba**: la aplicación aparece sincronizada y sana; tras el escalado manual vuelven a existir tres réplicas sin que tú hagas nada.
**Captura**: `entregas/tema6/6-3-deriva.txt` con el listado de pods justo después de escalar y de nuevo un minuto más tarde, con la hora de cada uno.

!!! question "Reflexiona"
    Tu orden de escalado **funcionó**: durante unos segundos hubo cinco pods. Nadie te lo impidió y nadie te avisó. ¿Qué diferencia hay entre «no puedes hacer esto» y «esto no va a durar»? Y una segunda: si esas cinco réplicas hubieran sido una intervención de urgencia legítima un domingo por la noche, ¿qué habrías tenido que hacer para que sobreviviera?

---

## Paso 6 — Desplegar sin tocar nada

Ahora la prueba de fuego. Cambia la versión de la imagen en `k8s/eks/`, en tu rama, y llévalo a `main` **por pull request fusionado**, como todas las semanas. La versión de destino es la `v2.0.1`.

A partir del momento en que fusionas, **no toques el clúster**. No apliques nada y no entres en la interfaz del agente a forzar la sincronización. Anota la hora del `merge` y la hora a la que la portada cambia de aspecto.

Mientras esperas, mira qué comprobaciones se han ejecutado en ese pull request.

**Comprueba**: la portada del catálogo cambia sola; los pods muestran la etiqueta nueva.
**Captura**: la hora del `merge` y la del cambio visible en la bitácora, y `entregas/tema6/6-3-checks.png` con la lista de comprobaciones del pull request.

!!! question "Reflexiona"
    **(a)** Enumera las credenciales que han tenido que existir fuera del clúster **para que este despliegue ocurriera**, y compárala con la lista equivalente del despliegue de diciembre. Después vuelve a mirar lo que apuntaste en el Paso 1 y explica en dos líneas por qué son dos listas distintas y por qué eso es una mejora aunque la primera siga existiendo. **(b)** Has cambiado una etiqueta en un YAML y se han ejecutado la compilación, los tests, la cobertura y el escáner de una aplicación que nadie ha tocado. Di qué habría que filtrar para evitarlo — y, ojo, di también **qué se rompería** en la protección de `main` si simplemente impidieras que esas comprobaciones se ejecutaran.

---

## Paso 7 — Destruir. A las 12:45, pase lo que pase

!!! danger "Alto obligatorio — 12:45"
    A las 12:45 se deja de trabajar sobre el clúster y se lanza la destrucción, esté como esté el Paso 6.

    El orden importa y no es intuitivo. Primero **borra la `Application`**, porque un agente con autorreparación activada está para deshacer lo que borres. Después **borra el Service del controlador**, el de tipo balanceador: si lo dejas, AWS no puede liberar la red y la destrucción se queda esperando indefinidamente. Y solo entonces lanza la destrucción del clúster.

    Anota la hora de inicio y la de fin en la bitácora. **Un clúster vivo al salir del aula invalida la práctica entera.**

**Comprueba**: la destrucción termina sin recursos pendientes y no queda ningún balanceador en la cuenta.
**Captura**: la salida final de la destrucción y las dos horas en `6-3-tiempos.txt`.

!!! question "Reflexiona"
    Has tenido que desactivar el agente para poder apagar el sistema. En una frase: ¿qué te dice eso sobre la diferencia entre «el repositorio describe lo que quiero» y «el repositorio describe lo que hay»?

---

## Paso 8 — El informe del HITO C

Mientras el clúster se destruye, completa `docs/cuatro-formas.md` con una tabla de las cuatro maneras en que se ha ejecutado el mismo catálogo este curso: contenedores sobre instancia, servicio gestionado, clúster propio y clúster gestionado con despliegue por extracción. Cuatro columnas: **tiempo hasta tener el servicio en pie**, **coste aproximado al mes**, **esfuerzo operativo** y **cuándo lo elegirías**.

Los números de las tres formas de DAW son tuyos, medidos: los tienes en las bitácoras de hoy y de las dos sesiones anteriores. La columna del servicio gestionado sale del miércoles; si no cursas INU, usa las cifras de referencia publicadas en Moodle o los datos de un compañero, citando la fuente. **Esta parte de la entrega se corrige solo con la columna de orquestación**, así que nadie depende de haber estado el miércoles.

Cierra con un párrafo: para un cliente con una tienda pequeña, un despliegue a la semana y sin equipo de sistemas, cuál de las cuatro le venderías y qué le dirías que va a perder.

**Comprueba**: ninguna celda vacía y ninguna cifra sin origen declarado.

---

## Verificación

**Antes de las 12:45**, con el clúster vivo, se ejecuta y se guarda la salida en `entregas/tema6/6-3-verificacion.txt`:

```bash
kubectl get nodes
kubectl get svc -A | grep -i loadbalancer
kubectl get ingress
kubectl get pods -l app=escaparate -o wide
kubectl -n argocd get applications -o wide
kubectl get deploy escaparate -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
curl -sI http://<dns-del-balanceador>/ | head -1
curl -s http://<dns-del-balanceador>/api/productos | head -c 120; echo
```

**Después de la sesión**, sobre el repositorio:

```bash
git log --oneline -5 origin/main -- k8s/eks/
git log --merges --oneline -3 origin/main
```

Debe observarse:

- Dos nodos listos y **un único** Service de tipo `LoadBalancer` en todo el clúster, el del controlador.
- Un Ingress sin `host`, y tanto la raíz como `/api/productos` respondiendo por la dirección del balanceador.
- Tres pods del catálogo **en al menos dos nodos distintos**.
- La `Application` en estado sincronizado y sana, apuntando a `k8s/eks` de `main`.
- La imagen del Deployment igual a `v2.0.1`, y esa etiqueta llegada al clúster **por un commit de fusión**, no por un `apply` manual.
- Tres réplicas, después del escalado a cinco.
- En la bitácora: hora de lanzamiento, hora del clúster listo, hora del `merge`, hora del cambio visible, hora de inicio y fin de la destrucción.

---

## Qué se entrega

- [ ] Rama `sesion-16` fusionada en `main` mediante pull request, con su URL en la plantilla de Moodle.
- [ ] `k8s/eks/` con los cambios respecto a `k8s/base/`, cada uno justificado en un comentario del fichero.
- [ ] `argocd/escaparate-application.yaml`, fuera del directorio vigilado, con sincronización automática, autorreparación y purga.
- [ ] `entregas/tema6/6-3-tiempos.txt` completo, con las seis horas.
- [ ] Evidencias: catálogo por el balanceador, pods repartidos entre nodos, las dos capturas del fallo de las imágenes, deriva antes y después, comprobaciones del pull request, salida de la verificación y salida de la destrucción.
- [ ] `docs/cuatro-formas.md` con la tabla completa y el párrafo final.
- [ ] Sección del `README`: cómo se despliega ahora una versión nueva y cómo se deshace, **sin entrar en ninguna máquina**.
- [ ] Respuestas a las siete reflexiones, con las dos partes de las del Paso 1, el Paso 3 y el Paso 6, y las tres del Paso 4.
- [ ] **Clúster destruido**, acreditado con la salida final.

---

## ✅ Cierre

Con esto cierras la UD6 y cierras el temario del módulo. Empezaste en septiembre mirando tres sitios web con las herramientas del navegador para adivinar cómo estaban desplegados; terminas con el mismo catálogo funcionando de cuatro maneras, una de ellas gobernada por un repositorio que ningún operador necesita tocar.

Y terminas también con una lista de cosas que sabes que están mal y por qué: las imágenes en disco local, que hoy se han roto por tercera vez; la base de datos sin almacenamiento persistente, que ha funcionado porque la clase dura tres horas; el secreto que sigue aplicándose a mano; el plano de control que factura mientras duermes. Saber nombrar los defectos de lo que has construido vale más que un despliegue sin defectos que no sabrías explicar.

La semana que viene empieza el proyecto integrador: tres semanas para elegir una arquitectura, montarla entera y documentarla. En la defensa se proyecta la tabla de las cuatro formas y se contesta a una sola pregunta —a este cliente, ¿cuál le vendes?—. Toda la respuesta la has medido tú.