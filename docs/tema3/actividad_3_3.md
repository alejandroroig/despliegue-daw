# 🧪 Actividad 3.3: Cierra la puerta y echa la llave

!!! warning "Descarga la plantilla"
    📄 [Plantilla 3.3 — Cierra la puerta y echa la llave](plantillas/Actividad_3_3_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

Tu servicio está publicado en internet desde la semana pasada, con tres copias detrás y una sola puerta. Y esa puerta no tiene ni cerradura ni cortina: los informes de pruebas los puede leer cualquiera que sepa el nombre, y todo lo que va y viene —peticiones, respuestas y cualquier cosa que alguien escriba— viaja en texto plano por una red que no controlas.

Hoy vas a cerrar las dos cosas, y en el orden en el que se hacen en la vida real. Primero pondrás credenciales en la zona que no debería ser pública. Después **capturarás tu propio tráfico** y verás esas credenciales legibles: esa captura es el argumento de todo lo demás. Y con ese argumento delante, convertirás el servicio en un despliegue cifrado con un certificado de verdad, emitido para el subdominio de tu equipo, que además sigue funcionando cuando ese certificado caduque sin que nadie haga nada.

## Qué vas a practicar

- **Restringir** el acceso a una zona del sitio y comprobar los tres casos posibles.
- **Capturar** tráfico propio y recuperar de él unas credenciales enviadas sin cifrar.
- **Emitir** un certificado real por ACME y terminarlo en el proxy.
- **Dejar automatizada** la renovación del certificado y comprobar con una ejecución en seco que el procedimiento funciona.
- **Endurecer** la entrega con la redirección al puerto seguro y tres cabeceras de seguridad.

## Requisitos previos

- La actividad 3.2 terminada: el proxy con las tres copias detrás, funcionando en la instancia.
- La instancia rearrancada y **el subdominio de tu equipo apuntando a su dirección de hoy**. Sin esto no hay certificado: compruébalo antes de escribir una sola línea de configuración.
- El paquete de la actividad, que incluye el servicio de cliente ACME listo para añadir a tu `compose.yaml`.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main && git pull
git switch -c sesion-08
```

!!! danger "Empieza por el entorno de pruebas de la autoridad"
    Las autoridades de certificación aplican **límites de emisión y de validaciones fallidas** para evitar errores automatizados y abuso. Por eso todo el paso 4 se depura primero contra el **entorno de pruebas**, que tiene límites mucho más permisivos, y solo se repite contra producción cuando el proceso completo funciona de principio a fin. Si una validación falla, diagnostica la causa antes de volver a intentarlo: reintentar a ciegas es la forma más rápida de quedarte sin poder emitir.

!!! warning "Punto de rescate: 65 minutos"
    La emisión por ACME depende de que encajen a la vez el DNS, el puerto 80, el grupo de seguridad y el cliente. **Si a los 65 minutos no has conseguido emitir en el entorno de pruebas, avisa y continúa desde la configuración ACME funcional** que se entrega con la actividad: desde ahí harás tú la emisión de producción, la redirección y las comprobaciones. Se anota en la plantilla qué falló y en qué punto.

!!! info "Reparto de tiempo orientativo"
    Pasos 1 y 2, unos 25 minutos. Paso 3, unos 15. Paso 4, unos 45. Pasos 5 y 6, unos 25.

---

## Paso 1 — Arranca y comprueba el nombre

Rearranca el laboratorio y la instancia, levanta el conjunto y confirma que el catálogo sigue respondiendo y que las tres copias se reparten el trabajo.

Antes de seguir, resuelve el subdominio de tu equipo y comprueba que devuelve la dirección de hoy. Todo lo que viene después depende de esto: si el nombre no apunta a tu máquina, la autoridad de certificación no encontrará nada cuando venga a comprobarlo.

**Comprueba**: el subdominio resuelve a la IP actual de tu instancia y el catálogo responde por ese nombre.
**Captura**: la resolución del subdominio y el catálogo funcionando.

---

## Paso 2 — Una zona que no es de todos

Los informes de pruebas que publicaste en la sesión 6 no deberían ser públicos. Protégelos con usuario y contraseña desde el propio servidor web, sin tocar la aplicación.

Genera el fichero de credenciales con al menos un usuario, y **déjalo fuera del repositorio**: contiene resúmenes de contraseñas y no se versiona, igual que el `.env`. Documenta en el `README` cómo se genera.

Comprueba los **tres casos**, y los tres se entregan: sin credenciales, con credenciales incorrectas y con las correctas. Fíjate en el código de estado de cada uno.

**Comprueba**: la documentación del código sigue siendo pública y solo la ruta de informes pide identificación.
**Captura**: las tres respuestas con su código de estado y la línea del `.gitignore`.

!!! danger "Contraseña de laboratorio"
    Usa una contraseña inventada para esta práctica, que no utilices en ningún otro sitio. Dentro de dos pasos la vas a ver escrita en claro en una captura de red, y esa captura va a acabar en tu entrega.

---

## Paso 3 — Mira lo que viaja

Pon una captura de tráfico en marcha en la instancia, filtrando el **puerto 80**. Desde tu equipo, haz después una petición explícita con credenciales:

```bash
curl -u usuario:clave http://<subdominio>/informes/
```

Busca en la captura la cabecera con la que se ha enviado la identificación. Está codificada, no cifrada: descodifícala y comprueba que obtienes literalmente el usuario y la contraseña que acabas de escribir.

**Comprueba**: obtienes las credenciales en claro a partir del tráfico capturado.
**Captura**: la línea de la petición donde aparece la cabecera y el resultado de descodificarla. **Tapa la contraseña en la captura de pantalla**: lo que se entrega es que se puede leer, no cuál es.

!!! question "Reflexiona"
    Acabas de recuperar una contraseña sin tocar el servidor ni la aplicación, y sin explotar ningún fallo. **¿Qué posiciones concretas de la red permiten hacer exactamente lo mismo que has hecho tú?** Y con eso claro: además de leer, ¿qué otra cosa podría hacer quien esté ahí y que sería aún peor?

---

## Paso 4 — Un certificado de verdad

Consigue un certificado emitido por ACME para el subdominio de tu equipo, usando el cliente que viene en el paquete de la actividad. Recuerda: **primero contra el entorno de pruebas**, y solo cuando todo el proceso funcione de principio a fin, contra el de producción.

Configura el proxy para que sirva el sitio por HTTPS con ese certificado, terminando el cifrado ahí: las tres copias de la API siguen hablando en claro por la red interna y no se enteran de nada.

Cuando el navegador te muestre el candado, inspecciona el certificado desde el terminal y anota tres datos: **quién lo ha emitido**, para qué nombre es válido y entre qué fechas.

**Comprueba**: el sitio responde por HTTPS y el navegador no muestra ninguna advertencia; el catálogo sigue mostrando sus productos.
**Captura**: la salida de la emisión, la inspección del certificado con su emisor y sus fechas, y el candado en el navegador.

!!! question "Reflexiona"
    La autoridad ha emitido el certificado sin saber quién eres ni pedirte ningún documento. **¿Qué comprobó exactamente antes de emitirlo, y por qué esa comprobación basta para lo que el certificado promete?** Di también qué habría pasado si el registro DNS hubiera apuntado a otra máquina.

!!! tip "Si la validación falla"
    El error casi siempre está en uno de estos cuatro sitios, y en este orden: el nombre no resuelve a esta máquina, el puerto 80 no está abierto en el grupo de seguridad, la ruta del desafío no es alcanzable desde fuera, o el cliente no está escribiendo el fichero donde el servidor lo sirve. Compruébalos de uno en uno pidiendo tú mismo esa ruta desde tu equipo antes de volver a lanzar la emisión.

---

## Paso 5 — Que dure sin ti

Con el certificado bueno instalado, deja el servicio como debe quedar:

1. **Redirección permanente** de todo el tráfico del puerto 80 al seguro, **dejando accesible directamente por HTTP la ruta del desafío** que usa tu cliente ACME. El puerto 80 no se cierra: por ahí empieza cada renovación.
2. Añade estas tres cabeceras, tal cual:
   - `Strict-Transport-Security`, con un `max-age` **corto**.
   - `X-Content-Type-Options: nosniff`.
   - `Referrer-Policy: strict-origin-when-cross-origin`.
3. Comprueba con `curl -I` qué cabeceras devuelve tu sitio ahora, y compáralo con lo que devolvía antes de hoy.

Y ahora la parte que decide si este despliegue dura más que el certificado, que son **dos cosas distintas**:

- **Quién lo va a intentar.** Abre el servicio ACME de tu `compose.yaml` y localiza el mecanismo que ejecuta la renovación periódicamente sin que nadie escriba nada. Anota en la plantilla cada cuánto se ejecuta y qué hace después de renovar, porque un certificado nuevo en disco no sirve de nada si el proxy sigue sirviendo el viejo desde memoria.
- **Si funcionaría cuando lo intente.** Ejecuta la renovación **en seco** y comprueba que el proceso se completa sin errores.

**Comprueba**: el puerto 80 responde con una redirección permanente, las tres cabeceras aparecen en la respuesta, y la renovación en seco termina correctamente.
**Captura**: la respuesta del puerto 80 con su código, la tabla de cabeceras antes y después, el bloque del `compose.yaml` donde se ve el mecanismo periódico, y la salida completa de la renovación en seco.

!!! question "Reflexiona"
    Acabas de hacer dos comprobaciones que parecen la misma y no lo son. **Explica qué demuestra cada una y qué seguiría sin estar demostrado si solo hubieras hecho una de las dos.** Y sobre el plazo del HSTS: ¿por qué te hemos pedido que sea corto, si un plazo largo protege más?

---

## Paso 6 — La memoria de lo que has cerrado

Amplía el `README.md` con la parte de seguridad del despliegue:

- Qué rutas están protegidas, con qué mecanismo y cómo se genera el fichero de credenciales.
- Qué certificado usa el servicio, quién lo emite y cuándo caduca.
- **Qué hay que hacer para renovarlo**, y quién se encarga de que ocurra.
- Qué puertos están abiertos, qué hace el 80 y por qué no está cerrado del todo.
- Qué cabeceras de seguridad se envían.

Abre la petición de fusión de `sesion-08` hacia la rama principal.

**Captura**: el `README` renderizado en el repositorio.

---

## Si te sobra tiempo

**Ponle nota a tu sitio.** Pasa tu subdominio por uno de los servicios públicos que analizan cabeceras de seguridad y configuración TLS. Anota la calificación y las dos primeras recomendaciones que te haga. No hace falta aplicarlas: basta con entender qué te está pidiendo cada una.

**Comprueba la cadena.** Pide el certificado desde el terminal y mira **cuántos** certificados envía el servidor, no solo el tuyo. Identifica quién firma a quién hasta llegar a la autoridad raíz, y razona qué pasaría en un cliente que no tuviera guardado el intermedio.

**Restringe por origen.** Añade a la zona de informes una condición adicional por dirección IP y comprueba qué ocurre. Presta atención a qué dirección está viendo el servidor en cada petición y a qué tuviste que configurar la semana pasada para que esa dirección fuera la de verdad.

---

## Verificación

Sustituyendo `<sub>` por el subdominio de tu equipo:

```bash
curl -sI http://<sub>/ | head -n 1
curl -sI http://<sub>/ | grep -i location

curl -s -o /dev/null -w "catalogo %{http_code}\n" https://<sub>/
curl -s https://<sub>/api/salud

echo | openssl s_client -connect <sub>:443 -servername <sub> 2>/dev/null \
  | openssl x509 -noout -issuer -subject -dates

curl -s -o /dev/null -w "sin credenciales %{http_code}\n" https://<sub>/informes/
curl -s -o /dev/null -w "con credenciales %{http_code}\n" -u <usuario>:<clave> https://<sub>/informes/

curl -sI https://<sub>/ | grep -iE "strict-transport|x-content-type|referrer"
```

Y debe observarse:

- Que el puerto 80 responde con una **redirección permanente** al seguro.
- Que HTTPS funciona **sin advertencias y sin desactivar la verificación** del certificado.
- Que el emisor del certificado es una autoridad reconocida, el nombre es el del equipo y las fechas son de esta semana.
- Que la ruta de informes devuelve `401` sin credenciales y `200` con ellas.
- Que se envían las tres cabeceras pedidas.
- Que el catálogo sigue funcionando con sus productos y las tres copias siguen repartiéndose el tráfico.
- Que en el `compose.yaml` hay un **mecanismo que renueva periódicamente** y recarga el proxy después.
- Que en el repositorio está el `README` actualizado y **no** está el fichero de credenciales.
- Que la entrega llega a la rama principal **a través de una petición de fusión** desde `sesion-08`.

---

## Qué se entrega

- [ ] La zona de informes protegida, con los tres casos y sus códigos de estado.
- [ ] La captura de tráfico en claro con la cabecera de identificación descodificada, y su reflexión.
- [ ] El certificado emitido por ACME, con su emisor, su nombre y sus fechas.
- [ ] El candado en el navegador, sin advertencias, con el catálogo funcionando.
- [ ] La reflexión sobre qué comprobó la autoridad antes de emitir.
- [ ] La redirección del puerto 80 con la ruta del desafío accesible.
- [ ] La tabla de cabeceras antes y después, con las tres pedidas.
- [ ] El mecanismo de renovación periódica identificado, con cada cuánto se ejecuta y qué hace al terminar.
- [ ] La renovación en seco completada, y la reflexión sobre qué demuestra cada una de las dos comprobaciones.
- [ ] El `README` con la parte de seguridad del despliegue, incluido quién renueva el certificado.
- [ ] La petición de fusión de `sesion-08`, fusionada.
- [ ] La plantilla de la actividad entregada en Moodle, con la URL de la petición de fusión.

---

## ✅ Cierre

Repasa lo que tienes: un servicio publicado en internet, repartido entre tres copias, con una zona privada de verdad, cifrado con un certificado en el que confía cualquier navegador, con la redirección puesta, las cabeceras que endurecen la entrega y una renovación que no depende de que tú te acuerdes. Eso es un despliegue en condiciones, y lo has montado pieza a pieza sabiendo qué hace cada una.

Guarda especialmente la captura del paso 3. Es la mejor respuesta que vas a tener nunca a la pregunta de por qué HTTPS no es opcional, y vale más que cualquier explicación: una contraseña tuya, legible, sacada de tu propio tráfico sin explotar ningún fallo.

Lo que no tienes es **ni idea de qué está pasando ahí dentro**. Cuántas peticiones llegan y de dónde, qué rutas fallan, cuál de las tres copias atiende a quién, si el servicio ha estado caído mientras tú no mirabas, o si alguien lleva media hora probando contraseñas contra esa zona que acabas de proteger. Y fíjate en que eso incluye la renovación que acabas de dejar montada: si un día falla, hoy por hoy nadie se enteraría. Ahora mismo esa información existe, repartida en el registro de cada contenedor y muriéndose con cada uno de ellos. La próxima sesión la recoge en un solo sitio, la convierte en preguntas que se pueden responder y en alertas que avisan sin que nadie esté mirando. Y con ella se cierra el tema.