# 🔀 2. Proxy inverso y balanceo

!!!info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/proxy-inverso-balanceo.pptx){target="_blank" rel="noopener"}

---

La semana pasada tu servidor web aprendió a servir dos sitios desde una sola máquina, bajo dos nombres distintos y en el puerto de la web. Sirve ficheros del disco, comprimidos y con instrucciones de caché, que es exactamente para lo que nació.

Hoy le añades un segundo oficio: además de seguir sirviendo los ficheros del front, pasa a **reenviar hacia otros servidores determinadas peticiones según su ruta**. En tu despliegue, `/` seguirá sirviéndose desde el disco y `/api/` se enviará a la aplicación. Ese es el papel de proxy inverso, y es el cambio que convierte tu despliegue en una arquitectura. De paso se abre la caja negra que llevas usando desde la sesión 5 —esas cuatro líneas de `location /api` que nunca te hemos dejado mirar—, aparecen tres copias de Escaparate donde antes había una, y el conjunto se muda del portátil a una máquina en la nube. Y con las tres copias llega el primer problema de verdad de este módulo: cosas que funcionaban perfectamente con una sola dejan de funcionar sin que nadie haya roto nada.

---

## 🔁 Servir es una cosa, reenviar es otra

Un **proxy inverso** es un servidor que recibe peticiones de los clientes y, en lugar de responderlas con ficheros propios, se las pasa a otro servidor, espera su respuesta y se la devuelve al cliente. Para quien está fuera, el proxy **es** la aplicación: no hay forma de saber cuántas máquinas hay detrás ni cómo se llaman.

Conviene distinguirlo de su pariente, porque comparten nombre y no se parecen en nada:

| | Proxy directo | Proxy inverso |
|---|---|---|
| Lo pone | El cliente, o su organización | El dueño del servicio |
| A quién representa | Al que navega | Al servidor |
| Qué esconde | Quién pide | Qué hay detrás |
| Ejemplo típico | El filtro de salida del centro | Lo que vas a montar hoy |

Lo llamamos «inverso» porque está del otro lado: no protege ni filtra al usuario, sino que hace de fachada del servicio. Y esa fachada resuelve de golpe una lista de problemas que, por separado, cuestan mucho:

- **Punto único de entrada.** Una sola puerta abierta al exterior, sea cual sea el número de piezas que hay dentro.
- **Enrutado por ruta.** `/` se sirve del disco y `/api` se reenvía a la aplicación, sin que el navegador tenga que saber que son dos cosas.
- **Reparto de carga.** Varias copias detrás y un tráfico que se reparte entre ellas.
- **Terminación de TLS.** El cifrado se resuelve en un único sitio, y eso es la sesión que viene.
- **Un punto donde mirar.** Todo el tráfico pasa por aquí, así que aquí está el registro completo de lo que ocurre. La sesión 9 vive de eso.

---

## 🔍 Se abre la caja negra

Aquí está, por fin, el bloque que llevas cinco semanas usando sin abrir:

```nginx
location /api/ {
    proxy_pass http://api_escaparate;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

`location /api/` selecciona las peticiones cuya ruta empieza por ahí; el resto sigue sirviéndose como ficheros del disco. `proxy_pass` dice a dónde se reenvía lo que caiga en ese bloque. Las cuatro líneas de `proxy_set_header` reconstruyen información que, si no, se pierde por el camino, y las vemos en el apartado siguiente.

Y ahora el detalle que más disgustos da en producción, así que grábatelo: **la barra final de `proxy_pass` cambia la ruta que llega al destino**.

| `location` | `proxy_pass` | El cliente pide | Al destino le llega |
|---|---|---|---|
| `/api/` | `http://destino` | `/api/salud` | `/api/salud` |
| `/api/` | `http://destino/` | `/api/salud` | `/salud` |

Cuando `proxy_pass` termina en una ruta —aunque sea solo la barra—, Nginx **sustituye** el trozo que coincide con el `location`. Cuando no la lleva, pasa la ruta tal cual. Escaparate publica sus endpoints bajo `/api`, así que aquí no queremos sustituir nada. Si algún día un proxy te devuelve `404` sobre una ruta que existe y responde perfectamente cuando la pides directamente al backend, mira esta tabla antes que ninguna otra cosa.

!!! tip "Por eso la API nunca publicó un puerto"
    En la sesión 5 te pedimos que la base de datos y la API no publicaran nada, y funcionó. Ahora ya sabes por qué: el front es JavaScript que corre en el navegador de una persona, así que sus llamadas a `/api` salen desde fuera, llegan al **único** puerto abierto, y desde ahí el proxy las mete hacia dentro por la red interna. La API es alcanzable desde donde tiene que serlo y desde ningún otro sitio. Esa es la superficie mínima de la que hablábamos, y ahora la has visto entera.

---

## 🎭 Lo que el backend deja de saber

Al meter un intermediario, la aplicación deja de hablar con el cliente y pasa a hablar con el proxy. Y con ello pierde tres datos que antes le llegaban solos:

| Lo que la aplicación ve | Lo que era en realidad | La cabecera que lo recupera |
|---|---|---|
| La IP del proxy | La IP del visitante | `X-Forwarded-For` |
| El nombre interno del destino | El nombre que se escribió en el navegador | `Host` |
| `http` | `https`, si el proxy termina el cifrado | `X-Forwarded-Proto` |

`Host` forma parte de HTTP y es la misma cabecera con la que la semana pasada seleccionabas el host virtual. En cambio, `X-Forwarded-For` y `X-Forwarded-Proto` son **convenciones ampliamente utilizadas** por proxies y aplicaciones, aunque existe también una cabecera estándar equivalente, `Forwarded`. En este módulo usaremos las `X-Forwarded-*` porque son las que te vas a encontrar en cualquier configuración real y Nginx las escribe directamente.

Las consecuencias de no ponerlas son muy concretas y las vas a notar pronto. Los registros de tu aplicación dirán que **todo el tráfico del mundo viene de una sola dirección**, cualquier control por IP se vuelve inútil, y cualquier enlace que la aplicación construya con su propio nombre saldrá mal. La tercera cabecera será imprescindible la semana que viene: cuando el cifrado termine en el proxy, la aplicación recibirá peticiones en claro y necesitará saber que el visitante venía por HTTPS.

!!! danger "Una cabecera que llega de fuera no es una prueba"
    Cualquiera puede enviar en su petición un `X-Forwarded-For` inventado. Como **tu Nginx es la primera y única puerta de confianza**, lo que hacemos arriba es sobrescribir ese valor con `$remote_addr`, la dirección desde la que Nginx ha recibido realmente la conexión. En arquitecturas con varios proxies de confianza encadenados sí interesa conservar la lista completa de direcciones, pero entonces hay que declarar explícitamente qué proxies son fiables: creerse una cabecera que llega de fuera es regalar una forma de saltarse cualquier control basado en ella.

---

## ⚖️ Varias copias detrás: el bloque `upstream`

Hasta hoy había una sola copia de Escaparate. Si se para, no hay catálogo; si hay que actualizarla, hay corte. Un grupo de destinos intercambiables se declara así:

```nginx
upstream api_escaparate {
    server api-1:8080;
    server api-2:8080;
    server api-3:8080;
}
```

`upstream` da nombre a un conjunto de destinos, y ese nombre es el que aparece en el `proxy_pass` del apartado anterior. Cada `server` es una copia, identificada por su nombre de servicio dentro de la red interna y su puerto real: exactamente los mismos nombres que resuelve la red de Compose desde la sesión 5.

Con varias copias hay que decidir **cómo se reparte**:

| Algoritmo | Cómo decide | Cuándo interesa |
|---|---|---|
| Turno rotatorio (por defecto) | Una petición a cada uno, en orden | Copias iguales y peticiones de coste parecido |
| Menos conexiones | Al que menos conexiones abiertas tenga | Peticiones de duración muy desigual |
| Por dirección de origen | El mismo cliente siempre a la misma copia | Cuando el estado vive en la copia. Es un parche |
| Con pesos | Reparto proporcional al peso asignado | Máquinas de potencia distinta |

La tercera fila merece un aviso, porque es una trampa clásica. Mandar siempre al mismo visitante a la misma copia —lo que se llama **sesión pegajosa**— parece la solución elegante al problema del estado, y en realidad lo esconde: el reparto se desequilibra, cuando una copia cae sus usuarios pierden lo que tuvieran, y al añadir una máquina nueva nadie va a ella. Es un apaño válido para salir del paso, nunca el diseño objetivo. El problema de fondo, y su solución de verdad, tienen fecha: sesión 10.

Para ver el reparto con tus ojos, Escaparate publica un endpoint que devuelve el identificador de la copia que ha atendido la petición, y el catálogo lo muestra en su cabecera. Pedir esa ruta varias veces seguidas es toda la demostración que hace falta.

!!! info "Por qué hoy declaramos tres servicios"
    Podrías levantar tres copias del mismo servicio y dejar que el nombre resolviera a tres direcciones. Hoy no lo hacemos, por dos razones. La primera es didáctica: declarando `api-1`, `api-2` y `api-3` se ve **qué tres destinos** forman el reparto y se puede diagnosticar cada uno por separado. La segunda es práctica: con destinos declarados así, Nginx los resuelve al arrancar y no se entera de los cambios posteriores, de modo que una copia recreada con otra dirección puede quedarse fuera del reparto sin dar ningún error. Nginx también sabe trabajar con resolución dinámica de destinos, pero esa configuración queda fuera del objetivo de esta sesión.

---

## ❤️ Cómo sabe el proxy que una copia está viva

Tener tres copias no sirve de nada si el proxy sigue mandando tráfico a la que se ha caído. Lo que hace por defecto es una **comprobación pasiva**: no pregunta nada, observa.

Si al reenviar una petición un destino falla o no responde dentro del plazo, Nginx cuenta ese fallo. Cuando alcanza el límite configurado —**por defecto basta un fallo**— considera esa copia temporalmente no disponible y deja de seleccionarla durante un tiempo. Pasado ese tiempo vuelve a probarla con tráfico real; si responde, regresa al reparto. Además, ante ciertos fallos reintenta la petición en la siguiente copia, de forma que el visitante no llega a ver el error.

Aquí conviene no confundir dos mecanismos que se parecen y no son lo mismo:

| | Comprobación de salud de Compose | Comprobación del proxy |
|---|---|---|
| Quién la hace | El motor de contenedores | Nginx |
| Qué observa | Un comando dentro del contenedor | Cómo va respondiendo a tráfico real |
| Qué hace si falla | Marca el contenedor como `unhealthy`; durante el arranque puede hacer que los dependientes esperen a que esté sano | Deja de seleccionarlo durante un tiempo |
| Para qué sirve | Saber el estado y, con `depends_on`, coordinar el arranque | Que el visitante no note la avería |

Fíjate en la diferencia de la última fila, porque importa: la comprobación de Compose **no vigila el servicio mientras funciona** para apartarlo de nada; informa del estado y sirve sobre todo para ordenar el arranque, que es para lo que la usaste en la sesión 5. Quien decide en caliente a quién se le manda tráfico y a quién no es el proxy.

Los dos mecanismos usan la misma idea de fondo: **una pieza está lista cuando lo demuestra respondiendo, no cuando su proceso existe**. Escaparate tiene un endpoint de salud precisamente para eso, y volverás a apoyarte en él en la orquestación de la sesión 15, donde ese mismo concepto se llama sonda.

!!! info "Para saber más: comprobaciones activas"
    Preguntar periódicamente a cada copia «¿estás bien?» sin esperar a que falle una petición de un usuario es una función de la versión comercial de Nginx, y algo que traen de serie los balanceadores gestionados y los orquestadores. Con la versión libre se trabaja con la comprobación pasiva, que para lo nuestro es suficiente. Cuando veas un balanceador de una nube pública pidiendo una ruta de salud cada treinta segundos, ya sabes qué está haciendo y por qué.

---

## 🧺 El estado que estorba

Y ahora el problema que aparece hoy y que no existía cuando había una sola copia.

Escaparate deja subir la foto de un producto. Con una copia, la foto se guarda en su disco y se vuelve a leer desde ahí: perfecto. Con tres copias y un reparto por turnos, la petición que **sube** la foto llega a una copia y la que la **muestra** llega a otra, que no la tiene. El resultado es una imagen que unas veces se ve y otras no, aparentemente al azar y sin un solo error en los registros.

Esto no es un fallo de Escaparate: es la primera vez que ves de frente **el problema del estado compartido**. La regla general es corta y vale para todo lo que hagas a partir de hoy:

> Cuando hay varias copias intercambiables de un servicio, **nada que un usuario necesite volver a encontrar puede vivir dentro de una copia**.

Lo que vive dentro de una copia es su disco local y su memoria. Y hay dos casos típicos, con dos fechas distintas:

- **Ficheros subidos**: es lo de hoy. La solución inmediata es sacarlos de cada copia a un **almacenamiento compartido** que las tres vean: un volumen común mientras todo esté en la misma máquina. Es la solución correcta hasta que las copias dejen de compartir máquina; a partir de ahí hace falta almacenamiento de objetos, que es donde acabará este mismo problema.
- **La sesión del usuario**: si vive en la memoria de una copia, al usuario le pasará lo mismo que a la foto. Ese lo verás caer entero en la sesión 10, con el servidor de aplicaciones delante.

!!! example "Tres mostradores y un cajón"
    Tres empleados atienden en tres mostradores y da igual a cuál vayas. Si cada uno guarda los documentos en el cajón de su propia mesa, el día que vuelvas y te toque otro mostrador, tu documento no existe. La solución no es obligarte a volver siempre al mismo empleado —eso es la sesión pegajosa—: es que haya un archivo común detrás de los tres.

---

## ☁️ El despliegue sale del portátil

Última pieza del día, y es un cambio de escenario. Hasta ahora todo corría en tu equipo. A partir de hoy corre en una **instancia** en la nube, la que has creado el miércoles en el módulo de nube pública, y desde ahí es alcanzable desde cualquier sitio.

Lo importante es lo poco que cambia. El `compose.yaml` es el mismo, las imágenes son las mismas y el `README` es el mismo procedimiento. Lo que cambia es el contexto, y tiene tres consecuencias que hay que tener presentes:

- **En el servidor no se compila.** Esto se dijo el primer día y hoy se cobra: la máquina no tiene ni Java ni Maven ni tu código. Descarga las imágenes del registro público y las ejecuta. Ese es el sentido de haberlas publicado.
- **La única puerta abierta ahora da a internet.** En tu portátil, un puerto publicado lo veía tu red local; aquí lo ve todo el mundo. El grupo de seguridad de la instancia debe dejar entrar exactamente el puerto de la web y nada más. Lo que dentro no publica puerto, sigue sin publicarlo.
- **La dirección cambia cada semana.** El laboratorio caduca entre sesiones y la instancia vuelve con otra dirección pública. Por eso el esquema de nombres de la semana pasada te sale gratis: el mismo comodín funciona con la IP que toque, sin registrar nada ni esperar a ningún TTL. Empezar la sesión rearrancando el laboratorio y anotando la dirección nueva es parte de la rutina a partir de hoy.

!!! info "Para saber más: esto mismo, gestionado"
    Todo lo que has montado a mano —el punto único de entrada, el reparto entre copias, la comprobación de salud— existe como servicio gestionado en cualquier nube: se llama balanceador de carga y se configura en un formulario. Lo verás el miércoles que viene. La razón de montarlo hoy a mano es que, cuando lo veas gestionado, sepas exactamente qué está haciendo por ti cada casilla de ese formulario, y qué pasa cuando una de ellas está mal.

---

## 🎯 Qué debes saber hacer al salir de esta sesión

- Escribir un bloque de reenvío por ruta, sabiendo qué le llega al destino según `proxy_pass` lleve o no barra final.
- Reenviar al backend las cabeceras que identifican al visitante original, su nombre y su protocolo, y decir qué se rompe sin ellas.
- Declarar un conjunto de destinos y repartir el tráfico entre varias copias de Escaparate, demostrando el reparto con el identificador de instancia.
- Comprobar que el servicio sigue disponible cuando una de las copias se para, y explicar quién lo ha decidido y con qué información.
- Reconocer el problema del estado compartido entre copias y resolver el caso de los ficheros subidos con almacenamiento común.
- Levantar el mismo despliegue en una instancia remota descargando las imágenes del registro, con una sola puerta abierta al exterior.

Lo que basta con reconocer: los algoritmos de reparto más allá del turno rotatorio, las comprobaciones activas, las sesiones pegajosas, la cabecera `Forwarded` y la resolución dinámica de destinos.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - Un proxy inverso recibe peticiones y **reenvía a otros servidores aquellas que su configuración le indica**. Para el cliente, ese proxy funciona como punto de entrada al servicio: no ve qué hay detrás ni cuántos son.
    - Enrutar por ruta permite que `/` se sirva del disco y `/api` vaya a la aplicación, sin que el navegador sepa que son dos cosas y sin que la aplicación publique ningún puerto.
    - La barra final de `proxy_pass` decide si la ruta llega íntegra al destino o si se sustituye el trozo del `location`. Es la causa número uno de los `404` inexplicables detrás de un proxy.
    - Al interponer un proxy, la aplicación deja de ver la IP del visitante, el nombre que se tecleó y el protocolo. `Host` es estándar de HTTP; `X-Forwarded-For` y `X-Forwarded-Proto` son convenciones muy extendidas, con `Forwarded` como alternativa normalizada.
    - Como el proxy es la única puerta de confianza, sobrescribe `X-Forwarded-For` con la dirección real de la conexión: una cabecera que llega de fuera no prueba nada.
    - Un bloque `upstream` agrupa varias copias intercambiables. El reparto por defecto es por turnos; hay variantes por número de conexiones, por origen y con pesos.
    - La sesión pegajosa no resuelve el problema del estado: lo esconde, desequilibra el reparto y falla cuando cae una copia.
    - Con destinos declarados por nombre, Nginx los resuelve al arrancar: una copia recreada con otra dirección puede quedar fuera del reparto sin dar ningún error.
    - Nginx detecta las copias caídas de forma pasiva, contando fallos sobre tráfico real, y deja de seleccionarlas durante un tiempo antes de volver a probarlas. La comprobación de salud de Compose informa del estado y coordina el arranque, pero no reparte tráfico.
    - Con varias copias, nada que el usuario necesite volver a encontrar puede vivir en el disco local ni en la memoria de una de ellas. Los ficheros subidos se resuelven con almacenamiento compartido; la sesión de usuario, en la sesión 10.
    - En el servidor no se compila: se descargan imágenes ya construidas de un registro. Es para lo que se publicaron.
    - Un puerto publicado en una máquina con dirección pública lo ve internet entero. La superficie mínima deja de ser un ejercicio y pasa a ser una necesidad.

---

Con esto ya tienes las piezas para la **Actividad 3.2**, que además es la mitad de una entrega conjunta con el módulo de nube pública: allí se ha montado la infraestructura y aquí se despliega encima.

Vas a llevar el conjunto de Escaparate a la instancia, poner tu Nginx como puerta única y levantar tres copias de la aplicación detrás. Pedirás el identificador de instancia varias veces seguidas hasta ver que responden las tres, pararás una a propósito para comprobar que el servicio aguanta, y te encontrarás con la foto que unas veces está y otras no, que tendrás que diagnosticar y resolver.

Al terminar tendrás un servicio publicado en internet, con una sola puerta, que sobrevive a la caída de una de sus piezas. Le faltará lo más evidente: todo eso viaja en claro. Cualquiera que esté en medio del camino puede leerlo entero, y eso incluye lo que escribas en un formulario. La semana que viene se cierra esa puerta con un certificado de verdad emitido para tu subdominio.