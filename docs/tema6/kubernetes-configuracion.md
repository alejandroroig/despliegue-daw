# 🔧 2. Configuración, sondas y actualizaciones progresivas

!!!info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/kubernetes-configuracion.pptx){target="_blank" rel="noopener"}

---

La semana pasada dejaste el catálogo corriendo sobre el clúster con tres copias que se reponen solas. También lo dejaste, y lo dijimos en voz alta, **peor publicado que en diciembre**: sin nombre, sin HTTPS y accesible por un puerto de nodo que no le enseñarías a nadie. Y con la contraseña de la base de datos escrita en texto plano dentro de un fichero que has subido al repositorio, que es exactamente lo que llevas seis meses aprendiendo a no hacer.

Hoy se arregla eso. Pero lo importante de la sesión es otra cosa: es la sesión en la que se paga la deuda más antigua del Tema 5. En diciembre desplegabas con `docker compose pull` seguido de `up -d`, y el apunte decía sin adornos que **eso no es una actualización progresiva**: para y arranca, hay un hueco, y nadie comprueba que la copia nueva sirva antes de retirar la vieja. Tenías los requisitos —copias intercambiables desde la S10, cuando sacaste la sesión a Redis—, pero no el mecanismo. El mecanismo es de hoy.

---

## 🔑 La configuración, fuera de la imagen (otra vez)

Es la tercera vez que te encuentras esta idea. En la S3 sacaste las credenciales de la imagen de la base de datos; en la S5 las juntaste en un `.env` fuera del repositorio; y en la S13 descubriste por las malas que **la configuración es código que no pasa por ninguna puerta de calidad**: el `config.js` de una línea apuntando a donde no debía atravesó los tests, la cobertura y el escáner, y solo se manifestó al arrancar.

Kubernetes separa esa configuración en dos objetos:

- Un **ConfigMap** guarda pares clave-valor no sensibles: la URL de la API para el front, el perfil de Spring, el nivel de registro.
- Un **Secret** guarda lo sensible: la contraseña de Postgres, la clave de Redis.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: escaparate-config
data:
  SPRING_PROFILES_ACTIVE: "produccion"
  config.js: |
    window.API_BASE = "https://equipoNN.midominio.es/api";
---
apiVersion: v1
kind: Secret
metadata:
  name: escaparate-secret
type: Opaque
stringData:
  POSTGRES_PASSWORD: "la-de-tu-.env"
```

Dos detalles del bloque que no son evidentes. `data` admite valores de varias líneas con `|`, y por eso el `config.js` entero cabe dentro de un ConfigMap: es un fichero, no una variable. Y `stringData` te deja escribir el valor en claro, mientras que `data` exige el valor ya codificado en base64.

!!! danger "Un Secret no está cifrado"
    Que se llame Secret y que salga codificado en base64 no significa nada: base64 es codificación, no cifrado, y cualquiera con acceso de lectura al clúster lo descodifica en una orden. En un clúster serio se activa el cifrado del almacén de estado y se restringe quién puede leer Secrets. Lo que sí ganas de entrada es que **el valor deja de estar dentro de la imagen y dentro del manifiesto**, y eso ya era la mitad del problema. En el repositorio subirás una plantilla, igual que con el `.env`.

### Cómo llega el valor al contenedor, y qué pasa cuando lo cambias

Hay dos formas de consumirlos, y la diferencia no es cosmética:

| Forma | Cómo se declara | Qué ocurre si cambias el ConfigMap |
|---|---|---|
| Como variable de entorno | `envFrom` o `env.valueFrom` | **No cambia nada** en los pods que ya corren. Las variables se fijan al crear el contenedor |
| Como fichero montado | `volumes` + `volumeMounts` | El fichero **sí** se actualiza solo, sin reiniciar nada: el kubelet lo refresca en su sincronización periódica. La documentación habla de un retardo de hasta un minuto de sincronización más un minuto de caché |

Con una excepción que muerde: si montas **un fichero suelto** con `subPath` en vez del directorio, ese fichero deja de actualizarse. Es una limitación conocida y es la causa habitual de «he cambiado el ConfigMap y no pasa nada».

Y hay una segunda condición: **que la aplicación vuelva a leer el fichero**. Spring Boot lee sus propiedades al arrancar, así que para Escaparate un cambio de configuración significa reiniciar los pods de forma ordenada — que es justo lo que vas a aprender dentro de un rato.

---

## 🚪 Ingress: el proxy inverso, declarado

Un Service reparte conexiones, pero no entiende de nombres, ni de rutas, ni de certificados. Todo lo que montaste entre la S6 y la S8 —dos hosts virtuales, rutas separadas para informes y documentación, redirección de 80 a 443, HSTS— necesita algo por encima.

Ese algo son **dos piezas distintas** que conviene no confundir:

- El **objeto Ingress** es tu declaración: «para este nombre y esta ruta, manda a este Service».
- El **controlador de Ingress** es un programa que corre dentro del clúster, lee todos los Ingress y configura un proxy real. En tu clúster local vas a instalar el **NGINX Ingress Controller de F5**, así que literalmente habrá un Nginx dentro del clúster con un `nginx.conf` escrito a partir de tus objetos: el mismo fichero que en octubre escribías tú a mano, generado ahora por un programa que observa la API del clúster.

!!! danger "Dos proyectos con nombres casi idénticos"
    Durante años el controlador más usado fue `kubernetes/ingress-nginx`, mantenido por la comunidad de Kubernetes. **Ese proyecto se retiró en marzo de 2026**: sus repositorios pasaron a solo lectura y no recibe versiones, correcciones ni parches de seguridad. Lo que usamos aquí es otra cosa: el **NGINX Ingress Controller de F5** (`nginx/kubernetes-ingress`), un proyecto distinto y mantenido. Si buscas documentación por tu cuenta, comprueba siempre en cuál de los dos has caído — los ejemplos no son intercambiables y medio internet todavía apunta al retirado. Y un aviso práctico: el **complemento de Ingress que trae minikube instala el proyecto retirado**, así que no lo actives. Hoy el controlador se instala con el manifiesto entregado.

!!! warning "Sin controlador, un Ingress no hace nada"
    Puedes aplicar un Ingress perfecto en un clúster sin controlador: se creará, `kubectl get ingress` lo listará, y no responderá nadie. No hay error, no hay aviso. Es el fallo más desconcertante del tema.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: escaparate
spec:
  ingressClassName: nginx
  tls:
    - hosts: ["equipoNN.midominio.es"]
      secretName: escaparate-tls
  rules:
    - host: equipoNN.midominio.es
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: escaparate-api
                port:
                  number: 8080
          - path: /
            pathType: Prefix
            backend:
              service:
                name: escaparate-front
                port:
                  number: 80
```

`ingressClassName` elige qué controlador atiende esta regla, por si hay varios. El valor sigue siendo `nginx` porque es la clase que declara por defecto el controlador de F5; no es un resto del proyecto retirado. `tls.secretName` apunta a un Secret de tipo `kubernetes.io/tls`, que no es más que tu certificado y tu clave privada metidos en un objeto del clúster: los mismos ficheros que ACME te dejó en la instancia en noviembre. Y el orden de las rutas no importa: gana la coincidencia más específica, no la primera escrita, al revés que en la lista de `location` a la que estás acostumbrado.

Para que todo esto sea alcanzable desde fuera hace falta una cosa más, y ya está hecha: el controlador escucha en los puertos 80 y 443 **del nodo**, y tu nodo es un contenedor dentro de la instancia. Por eso el clúster nació el viernes pasado con esos dos puertos publicados: eran para hoy.

| Lo que hacías a mano | Su equivalente declarado | Qué se pierde por el camino |
|---|---|---|
| Bloque `location` | `paths[].path` con su `pathType` | El control fino de la coincidencia |
| `proxy_pass` a un `upstream` | `backend.service` | Nada: la lista de destinos ahora se mantiene sola |
| Redirección 80→443, HSTS, compresión, cabeceras | **Anotaciones del controlador** | Aquí sí: cada controlador tiene las suyas y **no son portables** |

Esa última fila importa. El Ingress estándar cubre lo básico —nombre, ruta, servicio, TLS— y todo lo demás se configura con anotaciones específicas del controlador que uses. Es decir: la promesa de «esto vale igual en cualquier clúster» se cumple para la estructura y no para los detalles.

!!! info "Para saber más"
    La API Ingress es estable y no está previsto que desaparezca, pero está **congelada**: no se le añaden capacidades nuevas. El trabajo se ha movido a **Gateway API**, un conjunto de objetos más expresivo que separa quién administra la entrada de quién declara las rutas. Para lo que necesitas hoy, Ingress sobra; si dentro de dos años te toca montar esto en una empresa, mira primero qué usa su plataforma.

---

## 🩺 Sondas: la salud, declarada en vez de vigilada

Hasta hoy la salud de tu servicio se miraba **desde fuera**. Nginx marcaba una copia como caída cuando fallaba una petición real (S7). El panel de Kibana te avisaba de los 5xx (S9). La prueba de humo de la S13 comprobaba el servicio **después** de desplegarlo. Las tres cosas son observación: alguien mira y avisa.

Una sonda es otra cosa. Es una comprobación que tú declaras **dentro** del pod y que el clúster usa para **decidir**. Hay tres, y confundirlas es la fuente número uno de incidentes propios en Kubernetes:

| Sonda | Pregunta que responde | Qué hace el clúster si falla |
|---|---|---|
| De vida (`liveness`) | ¿El proceso está atascado? | **Reinicia el contenedor** |
| De disponibilidad (`readiness`) | ¿Puede atender peticiones ahora mismo? | **Saca el pod del Service.** No lo reinicia |
| De arranque (`startup`) | ¿Ha terminado de arrancar? | Mientras no pase, **desactiva las otras dos** |

La de disponibilidad es la protagonista de hoy, y la frase que conviene memorizar es esta: **una sonda de disponibilidad no es un monitor, es un interruptor de tráfico**. Cuando falla, el pod sigue vivo, sigue en la lista de pods, y simplemente deja de recibir peticiones. Cuando vuelve a pasar, vuelve a recibirlas. Nadie te avisa de nada; el efecto es que el usuario no llega a un sitio donde no le pueden atender.

La de arranque existe porque Spring Boot tarda en levantar. Sin ella tendrías que poner una sonda de vida muy permisiva para que no matara el contenedor durante el arranque, y entonces sería inútil el resto del tiempo.

!!! warning "Dos formas clásicas de hacerse daño"
    Una sonda de vida demasiado agresiva convierte un pico de carga en una tormenta de reinicios: la aplicación va lenta, la sonda falla, el contenedor se reinicia, arranca en frío y va más lenta todavía. Y una sonda de vida que consulta una **dependencia externa** —la base de datos, por ejemplo— reinicia todas tus copias a la vez cuando esa dependencia tiene un mal minuto, aunque tu aplicación estuviera perfectamente. Ante la duda: sonda de vida barata y sobre el propio proceso; comprobaciones de dependencias, en la de disponibilidad.

### Cómo las declaramos aquí, y qué demuestra cada una

| Sonda | Cómo se declara | Qué demuestra exactamente |
|---|---|---|
| Arranque | Conexión al puerto 8080, con umbral generoso | Que el proceso ha llegado a escuchar. Nada más |
| Vida | Conexión al puerto 8080 | Que el proceso sigue aceptando conexiones. No dice que atienda bien |
| Disponibilidad | Petición HTTP a `/api/salud` | Que ese endpoint responde con un código correcto |

Que la sonda de vida sea una comprobación de socket y no una petición a `/api/salud` no es pereza: es la aplicación directa del aviso de arriba. Una conexión al puerto **no depende de nadie más**, así que ningún mal minuto de la base de datos puede provocar el reinicio simultáneo de tus tres copias.

Y fíjate en cómo está redactada la tercera fila. Dice que el endpoint responde, **no** que la aplicación funcione, y la diferencia no es retórica: en todo el curso hemos usado `/api/salud` como un endpoint que contesta, sin documentar si por dentro consulta PostgreSQL, Redis o solo el proceso. Si lo consultara, esa sonda te estaría diciendo algo sobre la cadena completa; si no, solo te dice que la aplicación te contesta. **Antes de apoyar un procedimiento de guardia sobre una sonda hay que abrir el código y mirar qué comprueba por dentro.** Mientras no lo hagas, la afirmación honesta es la de la tabla.

---

## 🔄 La actualización progresiva, esta vez de verdad

Con sondas de disponibilidad puestas, la estrategia por defecto de un Deployment hace lo que en diciembre no podías hacer. Al cambiar la imagen del molde, el Deployment crea un ReplicaSet nuevo y **va trasvasando copias**: levanta pods nuevos, **espera a que su sonda de disponibilidad pase**, los mete en el Service y solo entonces retira pods viejos. Repite hasta terminar.

Dos números controlan el trasvase, y por defecto ambos valen el 25 %:

- `maxSurge`: cuántas copias de más se toleran durante el cambio. Con tres réplicas y el valor por defecto, puede haber cuatro pods a la vez.
- `maxUnavailable`: cuántas copias pueden faltar. Con el valor por defecto puedes quedarte en dos sirviendo.

Si lo que quieres es **capacidad completa durante todo el cambio**, pones `maxUnavailable: 0` y dejas `maxSurge` en al menos uno. Ojo con la combinación imposible: los dos a cero deja el despliegue bloqueado para siempre, porque ni puede sobrar ni puede faltar nadie.

| | Diciembre: `pull` + `up -d` | Hoy: actualización progresiva |
|---|---|---|
| Orden de operaciones | Para todas, arranca todas | Levanta una, la comprueba, retira una |
| ¿Se comprueba la copia nueva? | No | Sí, con la sonda de disponibilidad |
| Copias disponibles durante el cambio | Cero durante unos segundos | Nunca por debajo de tres, con `maxUnavailable: 0` |
| Si la nueva versión no arranca | Ya has tirado la vieja | Las viejas siguen sirviendo |
| Requisito previo | — | Que las copias sean intercambiables |

!!! warning "«No baja la capacidad» no es «no se pierde ninguna petición»"
    `maxUnavailable: 0` garantiza una cosa concreta: que el número de copias **consideradas disponibles** no baja del declarado. No garantiza que ninguna petición falle. Cuando un pod se retira puede tener conexiones en vuelo, su salida del reparto tarda en propagarse a todos los nodos, y la aplicación puede cortar una respuesta a medias. Por eso en la actividad **no vas a demostrar cero pérdidas: vas a medirlas**, y a informar de lo que hayas observado en tu prueba. Es una diferencia que ya conoces del Tema 5: un resultado medido no es una garantía.

Ese requisito previo de la última fila lo pagaste en la S10. Si la sesión de usuario siguiera viviendo dentro de cada copia, un trasvase progresivo iría echando gente a la calle de tres en tres. Funciona porque en noviembre sacaste la sesión a Redis.

---

## 🕰️ Atasco, historial y vuelta atrás

Cuando la versión nueva **no** llega a estar disponible, el trasvase no termina: se queda con los pods viejos sirviendo y los nuevos sin entrar. Eso es exactamente lo que quieres, y merece que lo veas con tus ojos antes de creértelo.

Ahora, tres precisiones que separan a quien ha leído de quien ha operado:

1. **El clúster no deshace nada solo.** Existe un plazo —`progressDeadlineSeconds`, diez minutos por defecto— tras el cual el Deployment se marca como no progresando. Y ahí acaba su intervención: la documentación oficial dice que Kubernetes no toma ninguna otra medida más que apuntar esa condición. La vuelta atrás la lanza una persona o una automatización de más arriba.
2. **Mientras tanto, el servicio no se ha degradado.** Los pods viejos siguen atendiendo. Un despliegue atascado no es una caída; es un despliegue que no ha ocurrido. Y esa distinción cambia por completo la urgencia con la que lo atiendes.
3. **La vuelta atrás es barata porque el ReplicaSet viejo sigue ahí.** El Deployment conserva un historial de revisiones, y deshacer significa volver a escalar un ReplicaSet que ya existe. No hay reconstrucción, y muchas veces ni siquiera hay descarga de imagen.

| | Rollback de la S13 | Vuelta atrás de hoy |
|---|---|---|
| Qué se lanza | El mismo workflow con la etiqueta anterior | Una orden de deshacer sobre el Deployment |
| Qué tiene que ocurrir | Entrar por SSH, descargar la imagen, recrear el stack | Escalar un ReplicaSet que ya existe |
| Coste típico | Minutos | Segundos |
| ¿Hubo corte para el usuario? | Sí, dos veces: al romper y al arreglar | Idealmente ninguna |

---

## ⚠️ Lo que una sonda no puede saber

Y aquí llega la parte que no te va a gustar, que es la razón por la que esta sección existe.

Vas a desplegar hoy, con todo el mecanismo montado, la `v2.0.0` que ya conoces: la que en diciembre atravesó los tests, la cobertura, el escáner y la protección de rama, y rompió la aplicación al arrancar por un `config.js` mal apuntado. Y la actualización progresiva **la va a desplegar sin despeinarse**. Las tres copias nuevas pasarán su sonda, entrarán en el Service, las viejas se retirarán, y el despliegue terminará en verde.

¿Por qué? Porque tu sonda pregunta si el proceso responde, y el proceso responde. La aplicación no está caída: está mal configurada. El fallo vive en un fichero que sirve el front y que solo se manifiesta en el navegador de un usuario.

La conclusión no es que el mecanismo sea inútil. Es esta: **lo que te protege no es la herramienta, es lo que decidiste comprobar**. Un despliegue progresivo garantiza que no retiras una copia vieja hasta que la nueva pasa *tu* comprobación; si tu comprobación es pobre, despliegas el fallo con mucha elegancia. Para haber parado esa versión concreta habría hecho falta una comprobación que verificase de verdad la cadena completa —que el front alcanza su API y que la API alcanza sus datos—, o una estrategia que expusiera la versión nueva a una fracción del tráfico antes de completar el cambio. Ninguna de las dos es gratis, y las dos se llaman igual: decidir qué significa «funciona» antes de automatizar nada.

Es la misma lección de diciembre, un peldaño más arriba. Entonces descubriste que un pipeline en verde no significa que el servicio funcione. Hoy descubres que un despliegue sin corte tampoco.

Con esto tienes todo para la **Actividad 6.2**: sacarás la configuración y las credenciales a ConfigMap y Secret, publicarás el catálogo por su nombre con un Ingress, pondrás sondas, verás atascarse un despliegue sin que se caiga el servicio, y desplegarás la `v2.0.0` para comprobar por ti mismo hasta dónde llega la protección que acabas de montar.

---

## 🎯 Qué debes saber hacer al salir de esta sesión

- Trasladar el contenido de tu `.env` a un ConfigMap y un Secret, consumirlos desde el Deployment y dejar solo plantillas en el repositorio.
- Publicar el catálogo por el nombre del equipo con un Ingress y su Secret de tipo TLS, instalando antes el controlador.
- Declarar las tres sondas —arranque y vida como conexión al puerto, disponibilidad como petición a `/api/salud`— y explicar qué hace el clúster con cada una y qué demuestra cada una.
- Ejecutar una actualización progresiva a una versión nueva con `maxUnavailable: 0` y medir con tráfico encima cuántas peticiones fallaron.
- Provocar un despliegue que se atasque, reconocer que el servicio no se ha degradado, y deshacerlo.
- Leer el historial de revisiones de un Deployment y explicar por qué deshacer es más rápido que el rollback de la S13.

**Lo que basta con reconocer**: los parámetros finos de las sondas (retardo inicial, periodo, umbrales), las anotaciones concretas del controlador de Ingress y su instalación.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - Un ConfigMap y un Secret sacan la configuración del manifiesto igual que el `.env` la sacó de la imagen. Un Secret está codificado en base64, no cifrado.
    - Un valor consumido como variable de entorno no cambia en un pod que ya corre; montado como fichero sí se refresca solo, salvo si se monta con `subPath`.
    - Que el fichero se actualice no implica que la aplicación lo relea: Spring Boot lee sus propiedades al arrancar.
    - Un Ingress sin controlador instalado no da error: simplemente no responde nadie.
    - El controlador que usamos es el NGINX Ingress Controller de F5. El antiguo `kubernetes/ingress-nginx` de la comunidad se retiró en marzo de 2026, y es el que instala el complemento que trae minikube: por eso no lo activamos.
    - Lo estándar del Ingress es nombre, ruta, servicio y TLS. Todo lo demás son anotaciones propias de cada controlador y no son portables.
    - Sonda de vida: reinicia el contenedor. Sonda de disponibilidad: lo saca del reparto sin reiniciarlo. Sonda de arranque: desactiva a las otras dos hasta que la aplicación levanta.
    - La sonda de vida se hace sobre el propio proceso para que una dependencia caída no reinicie todas las copias a la vez.
    - Una sonda demuestra lo que comprueba, ni una coma más. Si no has mirado qué hay detrás del endpoint de salud, solo sabes que responde.
    - La actualización progresiva levanta copias nuevas, espera a que su sonda pase y solo entonces retira las viejas. Con `maxUnavailable: 0` no baja el número de copias disponibles, que no es lo mismo que no perder ninguna petición.
    - Funciona porque las copias son intercambiables desde que la sesión salió a Redis en la S10.
    - Un despliegue atascado no es una caída: las copias viejas siguen sirviendo. Y Kubernetes no deshace solo; solo anota que no progresa.
    - Deshacer es cuestión de segundos porque el ReplicaSet anterior sigue existiendo: no se reconstruye nada.
    - Un despliegue sin corte no garantiza que la versión desplegada funcione. Solo garantiza que pasó la comprobación que tú escribiste.