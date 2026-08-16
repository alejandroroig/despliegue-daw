# 🧪 Actividad 3.2: Una puerta, tres copias

!!! warning "Descarga la plantilla"
    📄 [Plantilla 3.2 — Una puerta, tres copias](plantillas/Actividad_3_2_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

Escaparate lleva cinco semanas funcionando en tu portátil. Hoy se publica: el mismo conjunto, con las mismas imágenes y el mismo procedimiento, pasa a correr en una instancia con dirección pública, alcanzable desde cualquier sitio. Es el momento en el que dejas de tener una práctica y empiezas a tener un servicio.

Y con la publicación llega el primer requisito de negocio serio: **el catálogo no puede caerse porque se pare una pieza**. Así que detrás de la puerta no habrá una copia de la aplicación, sino tres. Tu servidor web seguirá sirviendo los ficheros del front desde el disco y, además, repartirá entre esas tres copias las peticiones que lleguen a `/api`. Por el camino se abre por fin ese bloque que llevas usando a ciegas desde la sesión 5, y te vas a encontrar con un fallo que solo existe cuando hay más de una copia y que no aparece en ningún registro.

## Qué vas a practicar

- **Desplegar** el conjunto en una máquina remota descargando las imágenes de un registro público.
- **Escribir** el reenvío por ruta y las cabeceras que identifican al visitante original.
- **Repartir** el tráfico entre tres copias de la aplicación y demostrar el reparto.
- **Comprobar** que el servicio sobrevive a la caída de una copia.
- **Diagnosticar y resolver** un fallo de estado compartido entre copias.

## Requisitos previos

- La actividad 3.1 terminada: los dos hosts virtuales funcionando y la configuración de Nginx versionada en tu repositorio.
- La **instancia** creada el miércoles, con Docker instalado, tu clave de acceso y el puerto de la web abierto a internet en su grupo de seguridad.
- El paquete de la actividad, con el `compose.yaml` de partida de esta sesión: incluye ya los tres servicios de la API declarados uno a uno.
- Tus imágenes publicadas como paquetes **públicos** en `ghcr.io`. La instancia no va a iniciar sesión en ningún registro.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main && git pull
git switch -c sesion-07
```

!!! danger "El laboratorio caduca entre sesiones"
    La instancia vuelve con **otra dirección pública** cada semana, y las credenciales se renuevan. El primer paso de hoy es rearrancarla y anotar la dirección nueva; todo lo que escribas después tiene que seguir funcionando la semana que viene con una dirección distinta. Si en algún sitio de tu configuración acabas escribiendo una IP a mano, algo has hecho mal.

!!! warning "Punto de rescate: 25 minutos"
    El objetivo de hoy es el proxy y el reparto, no reparar el despliegue de la sesión 5. **Si a los 25 minutos no tienes Escaparate funcionando en la instancia, avisa y continúa desde el punto de rescate** que se entrega con la actividad: es un despliegue equivalente al que deberías tener, listo para levantar. Usarlo no penaliza los pasos 3 a 6; lo que se pierde es la evidencia del paso 2, y eso se anota en la plantilla explicando qué falló.

!!! info "Reparto de tiempo orientativo"
    Pasos 1 y 2, unos 25 minutos. Pasos 3 y 4, unos 40. Paso 5, unos 25. Paso 6, unos 15. **Empieza por el paso 1 aunque no hayas leído el resto**: mientras la instancia arranca, se lee.

---

## Paso 1 — Rearranca y toma tierra

Pon en marcha el laboratorio, arranca la instancia y anota su dirección pública. Conéctate a ella y comprueba dos cosas antes de nada: que el motor de contenedores responde, y que **no hay nada tuyo corriendo** de sesiones anteriores.

Construye a partir de la dirección los dos nombres con los que vas a trabajar hoy, con el mismo esquema de la semana pasada: `escaparate.<dirección>.nip.io` y `docs.<dirección>.nip.io`. Resuelve el primero desde tu equipo para confirmar que apunta donde debe.

**Comprueba**: el nombre resuelve a la dirección pública de tu instancia.
**Captura**: la salida de la resolución del nombre y la del motor de contenedores en la instancia.

---

## Paso 2 — El despliegue se muda

Lleva el conjunto a la instancia partiendo **únicamente de tu repositorio y de tu `README`**, tal y como lo haría alguien que no ha estado en esta clase. No copies ficheros desde tu portátil ni edites nada por dentro: si el procedimiento que escribiste no basta, el arreglo es arreglar el procedimiento.

Fíjate en lo que ocurre al levantar el conjunto, porque es la primera vez que lo ves: la máquina no tiene ni Java, ni Maven, ni tu código, y aun así Escaparate arranca.

**Comprueba**: el catálogo responde en `http://escaparate.<dirección>.nip.io/` con sus productos, desde el navegador de tu equipo y no desde la instancia.
**Captura**: la salida de la descarga de imágenes, el listado de servicios en marcha y el catálogo abierto en tu navegador con el nombre visible.

!!! question "Reflexiona"
    En esa máquina no hay compilador, ni dependencias, ni código fuente, y sin embargo hay una aplicación Java funcionando. **¿Dónde y cuándo se compiló exactamente lo que se está ejecutando ahí?** Y la consecuencia práctica: si mañana necesitaras diez máquinas iguales, ¿qué parte del trabajo de hoy tendrías que repetir?

!!! warning "Una sola puerta, y ahora da a internet"
    Antes de seguir, comprueba desde tu equipo que la base de datos y la API **no** responden por sus puertos en la dirección pública. Lo que en tu portátil era una buena práctica, aquí es la diferencia entre un despliegue y un incidente.

---

## Paso 3 — Abre la caja negra y pon tres copias detrás

Ahora el trabajo del día, y va todo en el mismo fichero de configuración. El bloque que sirve los ficheros del front se queda como está: lo que cambia es lo que ocurre con las peticiones a `/api`.

1. **Declara el conjunto de destinos** con las tres copias de la API que trae el `compose.yaml` de hoy, y haz que el reenvío de `/api` apunte a ese conjunto en lugar de a una máquina concreta.
2. **Reenvía las cabeceras** que la aplicación necesita para saber quién le está hablando de verdad: la dirección desde la que tu proxy ha recibido la conexión, el nombre que se escribió en el navegador y el protocolo por el que llegó. Como tu Nginx es la única puerta de confianza, el valor de la dirección lo escribes tú, no lo aceptas de la petición.
3. Asegúrate de que la ruta llega **íntegra** al destino: el endpoint de salud tiene que responder igual a través del proxy que directamente.

Levanta las tres copias, valida la configuración y recárgala. Después demuestra el reparto pidiendo el identificador de instancia varias veces seguidas:

```bash
for i in $(seq 1 9); do curl -s http://escaparate.<dirección>.nip.io/api/instancia; echo; done
```

**Comprueba**: el catálogo funciona con normalidad y en esas nueve respuestas aparecen tres identificadores distintos.
**Captura**: el bloque de configuración que has escrito, la salida completa del bucle y la respuesta del endpoint de salud a través del proxy.

!!! tip "Si algo falla aquí, mira en este orden"
    Un `502` significa que el proxy no ha podido hablar con ningún destino: revisa los nombres y los puertos del conjunto, que son los de la red interna. Un `404` sobre una ruta que sí responde cuando se la pides directamente a una copia es casi siempre la barra final del reenvío. Y si siempre responde **el mismo identificador**, comprueba que has declarado las tres copias en el bloque de destinos y que las tres están realmente en marcha.

---

## Paso 4 — Que se caiga una

Con el servicio en marcha, **para una de las tres copias** de la aplicación. No la reinicies: párala y déjala parada.

Vuelve a lanzar el bucle del paso anterior y observa qué ocurre: cuántos identificadores distintos aparecen ahora, si se cuela algún error y cuánto tarda el proxy en dejar de intentarlo. Después arranca de nuevo la copia parada, espera unos segundos y repite el bucle para ver que vuelve al reparto.

**Comprueba**: el catálogo sigue funcionando con una copia menos, y al arrancarla vuelve a aparecer su identificador.
**Captura**: el listado de servicios con una copia parada, la salida del bucle en ese estado, y la línea del registro del proxy donde se ve el intento fallido.

!!! question "Reflexiona"
    Nadie ha tocado la configuración y sin embargo el tráfico ha dejado de ir a la copia parada. **¿Quién ha tomado esa decisión y con qué información contaba para tomarla?** Concreta dos cosas: qué tuvo que ocurrir antes de que la descartara, y cómo se enteró de que había vuelto —fíjate en que a nadie le avisaste de que la habías arrancado otra vez—.

---

## Paso 5 — La foto que unas veces está y otras no

Sube desde el catálogo la imagen de un producto. Recarga la ficha de ese producto **varias veces seguidas**.

Vas a ver un comportamiento que no tiene ninguna lógica aparente: la imagen aparece y desaparece sin patrón, y en los registros no hay ni un solo error. Antes de tocar nada, **escribe en la plantilla tu hipótesis** de qué está pasando y cómo piensas confirmarla; después confírmala mirando dentro de los contenedores.

Con el diagnóstico hecho, resuélvelo: las tres copias tienen que ver los mismos ficheros subidos. Vuelve a subir una imagen nueva y demuestra que ahora se ve siempre, recargando varias veces.

**Comprueba**: la imagen recién subida se ve en todas las recargas, y el fichero está accesible desde las tres copias.
**Captura**: la ficha del producto con la imagen y sin ella antes del arreglo, la comprobación desde dentro de dos copias distintas, el bloque del `compose.yaml` que lo resuelve y la ficha estable después.

!!! question "Reflexiona"
    Este fallo no existía la semana pasada y no lo ha provocado ningún error de programación. **¿Qué propiedad tenía el despliegue de la semana pasada que hoy ha desaparecido?** Escribe en una sola frase la regla general que se deduce de esto, y di qué otra cosa de una aplicación web te esperarías que sufriera exactamente el mismo problema.

---

## Paso 6 — Deja constancia

Amplía el `README.md` con lo que hoy ha cambiado, pensando en quien despliegue esto la semana que viene con otra dirección:

- Cómo se obtiene la dirección de la instancia y cómo se construyen los nombres a partir de ella.
- Cuántas copias de la aplicación se levantan y cómo se comprueba que el reparto funciona.
- Qué puertos están abiertos al exterior y cuáles no, con una línea explicando por qué.
- Cómo se comprueba que el servicio sigue en pie con una copia caída.

Añade también, en `entregas/tema3/`, el diagrama del despliegue: quién recibe la petición, por dónde entra, qué copias hay detrás y qué comparten. Vale con un `mermaid` de seis nodos.

Abre la petición de fusión de `sesion-07` hacia la rama principal.

**Captura**: el `README` renderizado y el diagrama.

---

## Si te sobra tiempo

**Cambia la forma de repartir.** Sustituye el reparto por turnos por el que envía la petición a la copia con menos conexiones abiertas, repite el bucle y observa si notas alguna diferencia con este tráfico. Después razona en qué situación sí la habría.

**Comprueba qué ve la aplicación.** Mira los registros de una de las copias y localiza qué dirección aparece como origen de las peticiones. Después manda tú una petición con un `X-Forwarded-For` inventado —`curl` permite añadir cabeceras— y vuelve a mirar el registro: comprueba si tu valor falso ha llegado a la aplicación o si el proxy lo ha sustituido por la dirección real. Con eso contestado, explica por qué una aplicación no debería tomar decisiones de permisos con una cabecera que no ha escrito su propio proxy.

---

## Verificación

Sobre la instancia en marcha, sustituyendo `<ip>` por su dirección pública:

```bash
curl -s -o /dev/null -w "catalogo %{http_code}\n" http://escaparate.<ip>.nip.io/
curl -s http://escaparate.<ip>.nip.io/api/salud

for i in $(seq 1 9); do curl -s http://escaparate.<ip>.nip.io/api/instancia; echo; done

curl -s --max-time 3 http://<ip>:5432 ; echo "bd: $?"
curl -s --max-time 3 http://<ip>:8080 ; echo "api: $?"

# con una copia parada
docker compose stop api-2
for i in $(seq 1 4); do curl -s -o /dev/null -w "%{http_code} " http://escaparate.<ip>.nip.io/; done; echo
docker compose start api-2
```

Y debe observarse:

- Que el catálogo responde **por su nombre**, en el puerto de la web, con sus productos.
- Que el endpoint de salud responde **a través del proxy**, con la ruta intacta.
- Que las nueve llamadas al identificador de instancia devuelven **tres valores distintos**.
- Que ni la base de datos ni la API responden desde el exterior.
- Que con una copia parada el catálogo **sigue devolviendo `200`**.
- Que una imagen subida se ve en recargas sucesivas, y que el fichero está accesible desde más de una copia.
- Que en el repositorio están la configuración del proxy, el `compose.yaml` con las tres copias y el almacenamiento compartido, el `README` actualizado y el diagrama.
- Que la entrega llega a la rama principal **a través de una petición de fusión** desde `sesion-07`.

---

## Qué se entrega

- [ ] El despliegue funcionando en la instancia, con la captura de la descarga de imágenes —o la nota explicando desde qué punto se continuó y por qué—.
- [ ] La reflexión sobre dónde y cuándo se compiló lo que se está ejecutando.
- [ ] La comprobación de que solo el puerto de la web responde desde el exterior.
- [ ] El bloque de reenvío escrito por ti, con el conjunto de destinos y las tres cabeceras.
- [ ] La salida del bucle mostrando los tres identificadores de instancia.
- [ ] El servicio funcionando con una copia parada, con la salida del bucle, la línea del registro y la reflexión sobre quién lo decidió.
- [ ] La hipótesis escrita antes de tocar nada, el diagnóstico del fallo de las imágenes y su resolución demostrada.
- [ ] La regla general sobre el estado compartido, en una frase.
- [ ] El `README` actualizado y el diagrama del despliegue en `entregas/tema3/`.
- [ ] La petición de fusión de `sesion-07`, fusionada.
- [ ] La plantilla de la actividad entregada en Moodle, con la URL de la petición de fusión.

---

## ✅ Cierre

Tienes un servicio publicado en internet con una sola puerta abierta, tres copias de la aplicación detrás repartiéndose el trabajo, y la capacidad de perder una de ellas sin que ningún visitante se entere. Eso ya no es un despliegue de prácticas: es la forma en la que están montados la mayoría de los servicios que usas a diario.

Y has visto de frente el problema que aparece siempre que algo se replica: **lo que vive dentro de una copia deja de existir para las demás**. Hoy era una foto y se ha resuelto con un almacén común. Recuerda la frase que has escrito, porque el mismo problema volverá con la sesión del usuario en la sesión 10 y otra vez, en su versión más difícil, cuando las copias dejen de compartir máquina.

Queda lo más urgente. Tu servicio viaja **entero en claro**: cualquiera con acceso al camino puede leer las peticiones, las respuestas y todo lo que alguien escriba en un formulario. Además, las dos rutas de tu servidor están abiertas a cualquiera que sepa el nombre, y los informes de pruebas seguramente no deberían estarlo. En la próxima sesión se cierran las dos cosas: una zona protegida con credenciales y un certificado de verdad, emitido para tu subdominio, con su renovación automática funcionando.