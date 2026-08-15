# 🧪 Actividad 2.2: Empaquetar Escaparate

!!! warning "Descarga la plantilla"
    📄 [Plantilla 2.2 — Empaquetar Escaparate](plantillas/Actividad_2_2_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

El equipo ha aceptado la imagen de base de datos que dejaste la semana pasada y ahora viene el encargo de verdad: **empaquetar la aplicación**. El objetivo declarado es que cualquiera del equipo, y mañana el servidor de producción, pueda arrancar Escaparate sin tener instalado ni Java ni Maven ni el código fuente.

Vas a hacerlo dos veces. La primera, de la forma obvia; la segunda, como se hace en una empresa. Y en medio vas a medir la diferencia, porque «esto es mejor» no es un argumento: los argumentos son megabytes y segundos.

## Qué vas a practicar

- **Escribir** un `Dockerfile` completo para una aplicación que hay que compilar.
- **Medir** el efecto del orden de las instrucciones sobre el tiempo de construcción.
- **Reducir** el tamaño de una imagen con construcción multietapa.
- **Publicar** una imagen etiquetada y comprobar que arranca desde el registro.

## Requisitos previos

- La actividad 2.1 terminada: tu imagen de base de datos publicada en `ghcr.io` y funcionando.
- El código de Escaparate, que está en `escaparate/` desde la actividad 1.2. Trabajarás sobre esa carpeta.
- Sesión iniciada en `ghcr.io` con el token que creaste la semana pasada.
- Docker funcionando y espacio libre en disco: hoy vas a construir cuatro veces.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main && git pull
git switch -c sesion-04
```

!!! info "Reparto de tiempo orientativo"
    Pasos 1 a 3, unos 35 minutos. Pasos 4 y 5, unos 30. Pasos 6 a 8, unos 25. **Mientras una construcción avanza, ve rellenando la plantilla**: hoy hay esperas y se aprovechan.

---

## Paso 1 — Reconoce el terreno

Antes de escribir nada, abre el proyecto y responde en la plantilla:

- Con qué se compila y qué fichero declara sus dependencias.
- Qué artefacto genera y en qué carpeta aparece.
- Qué necesita saber la aplicación al arrancar para encontrar su base de datos, y por dónde se le pasa. Está documentado en el propio proyecto.

Algunas de estas respuestas ya las miraste en la sesión 2, cuando incorporaste Escaparate al repositorio. Confírmalas ahora con el proyecto delante, porque de ellas dependen las cinco líneas que vas a escribir en el paso siguiente.

---

## Paso 2 — La versión ingenua

Escribe un `Dockerfile` de cinco líneas que parta de una imagen con Maven y Java 21, copie el proyecto entero, lo compile y deje dicho qué ejecutar al arrancar. Constrúyelo etiquetándolo como `escaparate:ingenua`.

**Anota la duración total de la construcción** que informa el propio proceso al terminar, y el tamaño de la imagen resultante.

Ahora arráncala. Como Escaparate necesita su base de datos, tendrás que poner en marcha también la imagen que publicaste la semana pasada y **conseguir que los dos contenedores se vean entre sí**: crea para ello una red propia llamada `escaparate-red` y conecta ambos a ella. Pásale a la aplicación la configuración que identificaste en el paso 1.

**Comprueba**: el catálogo se ve en el navegador con sus productos.
**Captura**: Escaparate funcionando, la duración de la construcción y el listado de imágenes con su tamaño.

!!! tip "Si la aplicación no encuentra la base de datos"
    Antes de tocar nada, mira sus logs. Casi siempre te está diciendo con qué nombre o dirección ha intentado conectarse. Y recuerda lo que viste la semana pasada sobre qué red resuelve nombres y cuál no.

---

## Paso 3 — Toca una coma

Abre cualquier fichero de código fuente del proyecto y haz un cambio insignificante: un comentario, un espacio. Vuelve a construir la misma imagen.

**Anota la duración de esta segunda construcción** y fíjate especialmente en si el proceso ha vuelto a descargar las dependencias de Maven.

**Captura**: la salida de la construcción, donde se vea qué pasos se han rehecho.

---

## Paso 4 — La versión decente

Ahora escribe el `Dockerfile` bueno, con tres cambios respecto al anterior:

1. Un fichero `.dockerignore` que deje fuera del contexto lo que no debe viajar: historial de versiones, compilaciones locales y cualquier fichero de variables.
2. **Orden de capas**: primero el fichero de dependencias y su descarga, después el código y la compilación.
3. **Construcción multietapa**: una etapa que compile con Maven y una final que solo tenga entorno de ejecución de Java y el artefacto.

Constrúyela como `escaparate:1.0.0` y anota duración y tamaño. Comprueba que funciona exactamente igual que la ingenua.

**Comprueba**: el catálogo vuelve a verse, con la imagen nueva.
**Captura**: el `Dockerfile`, el `.dockerignore` y el listado de imágenes con los dos tamaños uno junto al otro.

---

## Paso 5 — Toca otra coma

Repite el cambio insignificante en el código y vuelve a construir la versión buena. **Anota la duración.**

**Captura**: la salida, donde debe verse qué pasos se han reutilizado.

---

## Paso 6 — La cuenta

Rellena esta tabla con tus cuatro mediciones:

| | Construcción desde cero | Construcción tras tocar el código | Tamaño final |
|---|---|---|---|
| Versión ingenua | | | |
| Versión multietapa | | | |

Y responde a dos preguntas con lo que tienes delante, en tres o cuatro líneas cada una:

1. **La diferencia de tiempo tras tocar una coma.** ¿Qué paso concreto se ha saltado la segunda versión y por qué la primera no podía saltárselo?
2. **La diferencia de tamaño.** ¿Qué hay dentro de la imagen ingenua que no está en la multietapa? Nombra al menos tres cosas.

!!! question "Reflexiona"
    Las dos imágenes hacen exactamente lo mismo. La segunda pesa una fracción de la primera. De todo lo que sobra en la ingenua, **¿qué parte dirías que es solo peso muerto y qué parte es además un riesgo?**

---

## Paso 7 — Quítale los privilegios

Añade a la imagen final un usuario sin privilegios y haz que la aplicación arranque con él. Compruébalo desde dentro del contenedor en marcha: quién dice ser el proceso.

**Comprueba**: el usuario dentro del contenedor no es el administrador, y Escaparate sigue respondiendo.
**Captura**: la identidad del usuario dentro del contenedor y el catálogo funcionando.

---

## Paso 8 — Publica

Etiqueta tu imagen para `ghcr.io` con **dos etiquetas**: una inmutable con la versión exacta y otra móvil. Súbelas, comprueba que el paquete aparece en tu perfil y configúralo como **público**.

**Comprueba**: el paquete se ve en tu perfil de GitHub y muestra las dos etiquetas.
**Captura**: la página del paquete publicado.

Cierra la sesión abriendo la petición de fusión de `sesion-04` hacia la rama principal, con los dos `Dockerfile`, el `.dockerignore` y la tabla de mediciones dentro.

!!! question "Reflexiona"
    Si un compañero descargase hoy la etiqueta móvil y mañana tú publicaras una versión rota con esa misma etiqueta, **¿qué pasaría la próxima vez que él arrancase «la misma» imagen?** Con eso contestado, di cuál de las dos etiquetas pondrías en un procedimiento de despliegue.

---

## Si te sobra tiempo

**La prueba de fuego, en cinco minutos.** Pásale a quien tengas al lado el nombre completo de tu imagen y que él te pase el suyo. Descarga la del otro y arráncala contra tu propia base de datos. Si el catálogo aparece, acabas de ejecutar en tu equipo una aplicación Java compilada con Maven sin tener ni Java ni Maven ni el código. Piensa dónde ocurrió esa compilación y cuándo.

**Mira dentro.** Revisa el historial de capas de las dos imágenes e identifica las tres más pesadas de cada una. Se ve muy bien de dónde sale la diferencia de tamaño que has medido en el paso 6.

---

## Verificación

Para dar por válida la práctica se ejecutará, sustituyendo `<usuario>` por el tuyo:

```bash
docker rmi ghcr.io/<usuario>/escaparate:1.0.0 2>/dev/null
docker pull ghcr.io/<usuario>/escaparate:1.0.0
docker image inspect ghcr.io/<usuario>/escaparate:1.0.0 --format '{{.Size}}'
docker run -d --name verifica --network escaparate-red -p 8080:8080 \
  <las variables de configuración documentadas en tu README> \
  ghcr.io/<usuario>/escaparate:1.0.0
sleep 15
curl -s localhost:8080/api/salud
docker exec verifica id
docker rm -f verifica
```

Y debe observarse:

- Que la imagen **se descarga sin iniciar sesión**: es pública, y con sus dos etiquetas.
- Que su tamaño está en el orden de magnitud de una imagen multietapa, no de la ingenua.
- Que Escaparate arranca y su endpoint de salud responde.
- Que el proceso **no corre como administrador**.
- Que en el repositorio están los dos `Dockerfile`, el `.dockerignore`, la tabla de mediciones y el `README` con el comando exacto para arrancar la imagen.
- Que la entrega llega a la rama principal **a través de una petición de fusión** desde `sesion-04`.

---

## Qué se entrega

- [ ] El `Dockerfile` ingenuo y Escaparate funcionando contra su base de datos.
- [ ] El `Dockerfile` multietapa y el `.dockerignore`.
- [ ] La tabla con las cuatro mediciones de tiempo y los dos tamaños.
- [ ] Las dos respuestas del paso 6 y la reflexión sobre qué sobra y qué es un riesgo.
- [ ] La comprobación de que el proceso no corre como administrador.
- [ ] La imagen publicada en `ghcr.io` como paquete público, con etiqueta inmutable y móvil.
- [ ] El `README` con el comando exacto para arrancar la imagen y sus variables.
- [ ] La petición de fusión de `sesion-04`, fusionada.

---

## ✅ Cierre

Escaparate ya es un paquete. Está publicado, cualquiera puede descargarlo y arrancarlo sin tener nada instalado, pesa lo que debe pesar, no lleva dentro tu código fuente y no corre como administrador.

Y tienes cuatro números que valen más que cualquier explicación: la diferencia entre construir bien y construir mal es medible, y la has medido tú.

Pero fíjate en lo que te ha costado ponerlo en marcha hoy: crear una red a mano, arrancar la base de datos con sus variables, arrancar la aplicación con las suyas, acordarte del orden y de los nombres, y repetirlo entero cada vez que reinicias el equipo. Son dos contenedores. En producción serán más.

En la próxima sesión, todo eso —imágenes, puertos, variables, volúmenes, red y orden de arranque— se escribe una sola vez en un fichero que se versiona con el proyecto, y el conjunto entero se levanta con un comando. Y aprovecharemos para tomar la primera decisión de seguridad real del módulo: dejar de publicar puertos que nadie de fuera necesita.