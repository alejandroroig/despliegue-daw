# 🧪 Actividad 5.2: Publicar, vigilar y actualizar sin cortar

## Contexto

La semana pasada entregaste un informe diciendo que el catálogo ya corría sobre el clúster. La dirección técnica te ha contestado lo obvio: eso no es publicable. No tiene el nombre del equipo, no tiene HTTPS, y la contraseña de la base de datos está escrita en un fichero del repositorio. Tal como está, es un retroceso respecto a lo que había en diciembre.

El encargo de hoy es ponerlo al nivel de lo que sustituye, y además cobrar la promesa que llevas desde el 18 de diciembre sin cumplir: desplegar una versión nueva **midiendo cuántas peticiones se pierden por el camino**, con el mecanismo que hasta ahora no tenías. Al final vas a comprobar también hasta dónde llega esa protección, y la respuesta no va a ser tan cómoda como parece.

## Qué vas a practicar

- Reconstruir un entorno completo a partir de manifiestos versionados, sin instalar nada a mano.
- Externalizar configuración y credenciales a ConfigMap y Secret, dejando el repositorio limpio.
- Publicar la aplicación por el nombre del equipo con Ingress y TLS.
- Declarar las tres sondas y medir con tráfico encima qué ocurre durante una actualización progresiva.
- Provocar un despliegue atascado, distinguirlo de una caída y deshacerlo.

## Requisitos previos

- Rama `sesion-15` abierta desde `main`, con los manifiestos de la S14 ya en `k8s/base/`.
- Tu `.env` de la S5, que hoy se desmonta.
- La imagen `v2.0.1` publicada en GHCR al cerrar la S13. Hoy es el destino de la actualización.
- VS Code conectado por SSH a la instancia, como la semana pasada.
- El certificado del subdominio en la instancia. **Compruébalo antes de nada**: la renovación automática vivía en el stack de Compose que apagaste el 8 de enero, así que puede haber caducado. Si es el caso, avísame al empezar y pasas al certificado autofirmado, que sabes generar desde la S8 y vale igual para la actividad.

Ficheros que se entregan hoy (carpeta `entregados/tema6/`):

- `nginx-ic.yaml` — manifiesto del **NGINX Ingress Controller de F5**, ya descargado y ajustado para que escuche en los puertos 80 y 443 del nodo. Se aplica tal cual: instalarlo es andamiaje, no es objeto de estudio.
- `medir-despliegue.sh` — bucle de peticiones que cuenta códigos de respuesta y escribe un resumen. Es el instrumento de medida de hoy.
- `fragmentos-6-2.yaml` — los bloques de sondas y de estrategia de despliegue, con los valores en blanco para que los rellenes.

!!! danger "No confundas el controlador, y no actives el complemento de minikube"
    Hay dos proyectos con nombres casi idénticos. El de la comunidad, `kubernetes/ingress-nginx`, **está retirado desde marzo de 2026** y no recibe parches — y es precisamente el que instala `minikube addons enable ingress`. **No lo actives.** El que usas hoy es el **NGINX Ingress Controller de F5** (`nginx/kubernetes-ingress`), que sí está mantenido, y se instala con el manifiesto entregado. Sus ejemplos y sus anotaciones no son intercambiables.

!!! info "Reparto de tiempo orientativo"
    **10:00** arranque del laboratorio y de la instancia, se deja encendiendo. **10:10 a 10:55**, teoría. Paso 1, hasta las 11:15. Paso 2, hasta las 11:35. Paso 3, hasta las 12:00. Paso 4, hasta las 12:15. Paso 5, hasta las 12:45. **A las 12:45 se deja de medir, tengas lo que tengas.** Paso 6, hasta las 13:05. El resto, para la rama y el pull request.

---

## Paso 1 — Levantar el entorno entero desde el repositorio

La instancia ha estado apagada una semana. Haz la rutina de arranque de siempre —IP nueva, registro A— y después **borra el clúster de la semana pasada y vuelve a crearlo** con el script entregado. Cuando esté en pie, aplica todo lo que hay en `k8s/base/`.

No reconstruyas nada a mano ni copies ficheros de ningún sitio. Si algo no vuelve, es que la semana pasada no lo dejaste declarado.

**Comprueba**: nodo listo, base de datos con su volumen enlazado, tres pods del catálogo listos y respondiendo por el puerto de nodo.
**Captura**: `entregas/tema6/6-2-reconstruccion.txt` con el tiempo que has tardado desde que el clúster no existía hasta que el catálogo respondió, y el listado de objetos.

!!! question "Reflexiona"
    Compara ese tiempo con lo que te costaba en octubre dejar la instancia lista tras un rearranque. ¿Qué parte del trabajo ha desaparecido y qué parte sigue siendo tuya? Nombra al menos una cosa que **no** haya vuelto sola — mira las imágenes de los productos antes de contestar.

---

## Paso 2 — La configuración y las credenciales, fuera del manifiesto

Desmonta tu `.env`. Todo lo que no sea sensible pasa a un ConfigMap; la contraseña de la base de datos y cualquier otra credencial, a un Secret. El Deployment deja de tener valores escritos dentro y pasa a consumirlos de esos dos objetos.

En el repositorio se versiona el ConfigMap y **una plantilla del Secret en `docs/secret.example.yaml`**. Fíjate en dónde va: fuera de `k8s/`, porque ese directorio se aplica entero y una plantilla con valores vacíos machacaría el Secret real. El Secret de verdad se aplica a mano y se documenta en el `README` cómo generarlo. Añade el fichero real al `.gitignore` antes de crearlo, no después.

Cuando esté aplicado, provoca que los pods recojan los valores nuevos de forma ordenada, sin borrar nada a mano.

**Comprueba**: ningún valor sensible bajo `k8s/`; los tres pods en marcha con la configuración nueva; el catálogo sigue mostrando productos.
**Captura**: `entregas/tema6/6-2-configuracion.txt` con el listado de ConfigMaps y Secrets y la descripción del Deployment mostrando de dónde saca las variables.

!!! question "Reflexiona"
    Has consumido esos valores como variables de entorno. Si ahora cambias el ConfigMap y no haces nada más, ¿qué ven los pods que ya estaban corriendo? Contesta y después compruébalo.

---

## Paso 3 — El catálogo por su nombre, y con candado

Instala el controlador con el manifiesto entregado y espera a que su pod esté listo. Después:

- Mete el certificado y la clave del subdominio del equipo en un Secret de tipo TLS.
- Declara un Ingress de clase `nginx` que sirva el catálogo por el nombre del equipo, con TLS.

Te bastará **una sola regla**, porque tu aplicación empaqueta el front y la API en el mismo artefacto y ambos salen del mismo Service. No lo des por trivial: apúntalo, porque en la sesión que viene esa decisión vuelve a aparecer.

Cuando funcione, quita el puerto de nodo del Service. Ya no hace falta y no debe quedar una puerta trasera abierta.

**Comprueba**: `https://equipoNN.midominio.es/` carga el catálogo con datos; `/api/productos` responde por el mismo nombre; el Service ha vuelto a ser de tipo interno.
**Captura**: `entregas/tema6/6-2-ingress.png` con el navegador mostrando el catálogo por su nombre, y la salida del listado de Ingress.

!!! question "Reflexiona"
    **(a)** En la S6 y la S8 escribiste a mano un `server_name` y un `ssl_certificate`. Hoy no has escrito ninguna de las dos directivas, y sin embargo hay un Nginx configurado con ellas dentro del clúster. ¿Quién lo ha escrito y a partir de qué? **(b)** Si mañana el front se separase en su propia imagen y su propio Deployment, ¿cuánto tendrías que cambiar de lo que has montado hoy?

!!! tip "Punto de rescate — 11:55"
    Si a las 11:55 no cargas el catálogo por el nombre, avísame y publico el Ingress resuelto. Los tres fallos habituales: el controlador aún no está listo, el Secret TLS se creó con un tipo incorrecto, o el registro A todavía apunta a la IP de la semana pasada. Antes de pedir rescate, comprueba una cosa: si respondes por la IP de la instancia pero no por el nombre, el problema es DNS y no Kubernetes.

---

## Paso 4 — Declarar la salud

Añade al Deployment las tres sondas, con este reparto:

- **Arranque y vida**: comprobación de conexión al puerto 8080. La de arranque, con umbral generoso, porque Escaparate tarda en levantar.
- **Disponibilidad**: petición HTTP a `/api/salud`.

Rellena los valores del fragmento entregado con criterio, no al azar: una sonda de vida impaciente durante el arranque mata el contenedor antes de que llegue a servir.

Aplica y observa cómo los pods pasan por el estado no listo antes de entrar en el reparto.

**Comprueba**: los tres pods llegan a `1/1` y `Running`; durante el arranque hay un intervalo en el que aparecen como no listos.
**Captura**: `entregas/tema6/6-2-sondas.txt` con la parte de la descripción de un pod donde aparecen las tres sondas y sus valores.

!!! question "Reflexiona"
    Dos preguntas. **(a)** ¿Por qué la sonda de vida comprueba el puerto y no `/api/salud`? Piensa en qué pasaría con tus tres copias si la base de datos tuviera un mal minuto. **(b)** Con los valores que has escrito, si un pod empieza a responder muy despacio pero sigue respondiendo, ¿el clúster lo reinicia, lo saca del reparto, las dos cosas o ninguna?

---

## Paso 5 — Actualizar midiendo lo que se pierde

Configura la estrategia del Deployment para que durante el cambio **no baje el número de copias disponibles**, y deja margen para que sobre una.

Lanza el script de medida contra tu nombre público y, **con el bucle corriendo**, cambia la imagen a `v2.0.1` y aplica. Deja que el trasvase termine antes de parar la medición.

No des por hecho el resultado. Lo que entregas es lo que has medido: si aparece alguna respuesta distinta de 200, se anota y se explica cuándo ocurrió; eso no invalida la práctica, la completa.

**Comprueba**: la portada cambia de color al terminar; tienes un resumen del script con el recuento por código de respuesta.
**Captura**: `entregas/tema6/6-2-medicion.txt` con el resumen, y `6-2-rollout.txt` con el historial de revisiones del Deployment.

!!! question "Reflexiona"
    En diciembre, el mismo cambio de versión provocaba un hueco de varios segundos sin servicio. Hoy, según tu medición, ¿cuánto se ha perdido? **Dos cosas** han tenido que ser ciertas para que el mecanismo funcione: una la configuraste hace diez minutos y la otra la conseguiste en noviembre, en la sesión del servidor de aplicaciones. Nómbralas.

!!! warning "Alto obligatorio — 12:45"
    A las 12:45 se deja de medir. Si el bucle no ha terminado limpio, entregas lo que tengas con una línea explicando qué observaste: una medición incompleta bien descrita vale; una inventada, no.

---

## Paso 6 — Dos despliegues que no deberían llegar al usuario

**(a) El que se atasca.** Rompe a propósito la sonda de disponibilidad en el manifiesto: apúntala a una ruta que no existe y aplica. Observa durante un par de minutos qué les pasa a los pods nuevos, qué les pasa a los viejos y qué contesta tu nombre público mientras tanto. Después deshaz el despliegue y comprueba que vuelve a la revisión anterior.

**(b) El que pasa.** Cambia ahora la imagen a la `v2.0.0` —la rota de diciembre, la del `config.js` mal apuntado— y aplica con las sondas correctas. Observa el despliegue hasta el final y **abre el catálogo en el navegador**. Después, deshazlo.

**Comprueba**: en (a), los pods nuevos nunca llegan a listos, los viejos siguen sirviendo y el sitio responde con normalidad durante todo el episodio. En (b), el despliegue termina correctamente y la portada carga con el aspecto nuevo y sin productos.
**Captura**: `entregas/tema6/6-2-atasco.txt` con el listado de pods durante el episodio (a) y el estado del despliegue, y `6-2-v200.png` con la portada rota del episodio (b).

!!! question "Reflexiona"
    Tres preguntas, y la tercera es la de hoy. **(a)** Durante el atasco, ¿estaba el servicio caído? Justifícalo con lo que devolvía tu nombre público. **(b)** ¿Deshizo el clúster algo por su cuenta, o lo deshiciste tú? **(c)** El mecanismo que acaba de proteger al usuario en el caso (a) no lo ha protegido en el caso (b). ¿Qué pregunta hacía tu sonda, qué pregunta habría hecho falta hacer, y en qué se parece esto a lo que descubriste en diciembre con la prueba de humo?

---

## Si te sobra tiempo

Repite la actualización a `v2.0.1`, pero **pausa el despliegue a mitad del trasvase**. Con el despliegue pausado, mira cuántos pods hay de cada versión y pide la portada varias veces desde el navegador. Después reanúdalo.

En tres líneas: qué acabas de tener durante ese minuto, qué te permitiría hacer si tuvieras una forma de medir errores por versión, y por qué el apunte menciona esa técnica como una de las dos cosas que sí habrían parado la `v2.0.0`.

---

## Verificación

Para dar por válida la práctica se ejecutará, **en la instancia del equipo**:

```bash
kubectl get ingress,svc,configmap,secret
kubectl get svc escaparate -o jsonpath='{.spec.type}'; echo
kubectl get pods -l app=escaparate
curl -sI https://equipoNN.midominio.es/ | head -1
curl -s https://equipoNN.midominio.es/api/productos | head -c 200; echo
kubectl describe deploy escaparate | grep -E "Liveness|Readiness|Startup|RollingUpdate"
kubectl rollout history deploy escaparate
```

Y **sobre el repositorio**:

```bash
git grep -nEi "password|passwd|contrasena" -- k8s/
```

Debe observarse:

- Un Ingress de clase `nginx` con el nombre del equipo y una entrada TLS; el Service de tipo `ClusterIP`, sin puerto de nodo.
- Respuesta `200` por HTTPS en la raíz y productos con datos en `/api/productos`.
- Sondas de arranque y vida por conexión al puerto, sonda de disponibilidad por HTTP, todas con valores concretos, y `maxUnavailable: 0`.
- Un historial con **al menos cinco revisiones**: la inicial, la de configuración, la de `v2.0.1`, la de la sonda rota y la vuelta atrás.
- La última búsqueda **sin resultados**.

---

## Qué se entrega

- [ ] Rama `sesion-15` fusionada en `main` mediante pull request, con su URL en la plantilla de Moodle.
- [ ] `k8s/base/` con ConfigMap, Deployment, Service e Ingress con TLS. Ninguna plantilla de Secret dentro.
- [ ] `docs/secret.example.yaml` con los valores vacíos, `.gitignore` actualizado y procedimiento del Secret real documentado en el `README`.
- [ ] Las tres sondas declaradas con valores propios, no copiados del fragmento en blanco.
- [ ] Estrategia de despliegue con `maxUnavailable: 0`.
- [ ] Evidencias en `entregas/tema6/`: reconstrucción, configuración, Ingress, sondas, medición del despliegue, atasco y portada de la `v2.0.0`.
- [ ] Sección del `README` sobre cómo desplegar una versión nueva y cómo deshacerla en este entorno.
- [ ] Respuestas a las seis reflexiones, con las dos partes de las del Paso 3 y el Paso 4, y las tres del Paso 6.

---

## ✅ Cierre

El catálogo vuelve a estar al nivel de diciembre —nombre propio, HTTPS, credenciales fuera del repositorio— y por encima de él en una cosa concreta: ahora tienes una medición propia de lo que cuesta cambiar de versión, y volver atrás son segundos en vez de minutos. Has pagado la deuda que quedó abierta el 18 de diciembre, y has visto también dónde termina lo que compra: en el caso (b) el mecanismo funcionó perfectamente y el fallo llegó igual.

Quedan dos cosas incómodas apuntadas. La primera: todo esto vive en **tu** máquina, montada por ti, con un clúster que desaparece si se apaga la instancia — como has comprobado esta mañana al recrearlo. La segunda, más de fondo: para desplegar hoy has entrado por SSH y has ejecutado órdenes a mano, exactamente como en octubre. El repositorio tiene los manifiestos, pero nadie comprueba que lo que corre en el clúster se parezca a lo que dice el repositorio.

La semana que viene se cierran las dos. El clúster pasa a ser gestionado, el plano de control deja de ser tuyo —y empieza a facturar—, y un agente instalado dentro del clúster se encarga de leer el repositorio y hacer que la realidad se le parezca, sin que nadie entre por SSH y sin que el pipeline necesite ninguna llave de producción. Es la respuesta a lo que montaste en diciembre, y es la última sesión de contenido del curso.