# 🧪 Actividad 1.1: Qué se puede saber de un despliegue desde fuera

## Contexto

Es tu primer día en una empresa que despliega y mantiene aplicaciones web. Antes de dejarte tocar ningún servidor, tu responsable te propone un ejercicio sencillo: **mirar dos sitios que ya están funcionando y averiguar qué puedes demostrar sobre ellos sin acceso a ninguna máquina**.

No se trata de adivinar la arquitectura. Cuando una aplicación vaya lenta, falle o responda de forma extraña, muchas veces empezarás exactamente así: observando desde fuera.

La parte más importante de esta actividad será distinguir entre:

- **lo que has observado**;
- **lo que puedes deducir razonablemente**;
- **lo que no puedes saber desde fuera**.

---

## Qué vas a practicar

- Obtener e interpretar cabeceras HTTP.
- Distinguir una petición `HEAD` de una petición `GET`.
- Reconocer indicios de caché, CDN o proxy.
- Analizar qué recursos descarga realmente una página con DevTools.
- Observar cookies y respuestas de error.
- Separar evidencia de inferencia.

---

## Requisitos previos

- Navegador con herramientas de desarrollador.
- `curl` instalado.

Comprueba:

```bash
curl --version
```

!!! warning "Trabajar con otros sistemas operativos"
    En el aula se trabajará sobre Linux. Si utilizas tu propio portátil con Windows, los comandos son prácticamente los mismos, aunque hay tres diferencias que conviene recordar: en PowerShell es preferible escribir `curl.exe`; para descartar la salida, se utiliza `NUL` en lugar de `/dev/null`, y para dividir una orden en varias líneas, PowerShell usa el acento grave `` ` `` y CMD usa `^`.

!!! info "Tiempo orientativo"
    - Pasos 1 a 3: 35 min.
    - Pasos 4 y 5: 30 min.
    - Paso 6 y redacción final: 25 min.

## Paso 1: Tus dos sujetos

Trabajarás con estos dos sitios:

| Sitio | Qué representa |
|---|---|
| `https://alejandroroig.github.io/despliegue-daw/tema1/actividad_1_1/` | Documentación publicada como sitio estático |
| `https://www.amazon.es/` | Una aplicación web comercial compleja |

**No hace falta que inicies sesión ni que te crees ninguna cuenta**: todo lo que vas a mirar está disponible sin identificarse.

Antes de tocar nada, crea un fichero `actividad-1.1.md` en una carpeta de trabajo con nombre `actividad-1.1/`. Escribe en el fichero dos o tres líneas por sitio indicando **qué esperas encontrar**:

- contenido estático o dinámico;
- pocas o muchas peticiones;
- pocas o muchas cookies;
- posibles capas intermedias;
- arquitectura aparentemente sencilla o compleja.

No se evalúa acertar. Al final volverás a estas predicciones.

---

## Paso 2: Pregunta por las cabeceras

Consigue de cada uno de los dos sitios **únicamente la cabecera de la respuesta**, sin descargar el contenido. `curl` tiene una opción para eso; búscala en su ayuda.

Recoge en `actividad-1.1.md`, para cada sitio:

- código de estado;
- versión HTTP;
- `Server`, si aparece;
- `Content-Type`;
- cabeceras relacionadas con caché;
- cabeceras `Set-Cookie`;
- posibles indicios de CDN, proxy o cualquier capa intermedia.

!!! important "`HEAD` y `GET` no son la misma petición"
    Un servidor puede aceptar `GET` y rechazar `HEAD`.
    Si un sitio rechaza `HEAD`, realiza después un `GET` normal pero descartando el cuerpo:
    
    ```bash
    curl -sS -D - -o /dev/null https://direccion
    ```

    O, si trabajas desde Windows, en PowerShell puedes utilizar:

    ```powershell
    curl.exe -sS -D - -o NUL https://direccion
    ```

!!! tip "Sobre Amazon"
    Amazon puede responder a una petición hecha con `curl` de forma distinta a una realizada desde un navegador normal. Si aparece un `405`, un `202`, una respuesta de CloudFront o una cabecera de desafío, **no intentes sortearla ni forzar el acceso**: anota el código y las cabeceras. Esa diferencia también forma parte de la evidencia.

**Capturas:** conserva la salida de las peticiones realizadas.

!!! info "Imágenes en documentos Markdown"
    Guarda las capturas de la actividad en una subcarpeta `img/` e insértalas en el Markdown utilizando rutas relativas.


---

## Paso 3: La misma petición varias veces

Pide **dos veces seguidas** las cabeceras de la web de apuntes y compara ambas respuestas. Fíjate especialmente en las cabeceras relacionadas con caché: algunas pueden cambiar entre una petición y otra y otras pueden mantenerse. Intenta explicar qué indica cada una a partir de los valores que hayas obtenido.

Compara especialmente:

- `Age`;
- `X-Cache`;
- `X-Cache-Hits`;
- `Via`;
- `X-Served-By`;
- otras cabeceras relacionadas con caché.

No es obligatorio obtener una secuencia concreta como `MISS → HIT`.

!!! question "Reflexiona"
    1. ¿Hay evidencia de que existe una caché intermedia?
    2. Si una respuesta fue servida desde caché, ¿qué cabecera lo sugiere?
    3. ¿Qué significa que `Age` aumente?
    4. ¿Las dos peticiones han sido atendidas necesariamente por el mismo nodo?

**Captura:** coloca las dos salidas una debajo de la otra.

---

## Paso 4: Cuenta lo que descarga realmente el navegador

Ahora utiliza el navegador.

Para cada sitio:

1. Abre DevTools.
2. Ve a **Network / Red**.
3. Limpia el registro.
4. Recarga la página.
5. Espera unos 10 segundos después de la carga inicial.
6. Registra los datos.

Anota:

- Cuántas peticiones hacen falta para pintar la página y cuánto se transfiere en total.
- Cuántas hay de cada tipo: documento, estilos, script, imagen y peticiones Fetch/XHR. Si aparecen, identifica cuáles parecen corresponder a llamadas a una API.
- Cuántos **dominios distintos** aparecen involucrados.


!!! question "Reflexiona"
    Centrándote en el documento HTML principal:
    
    - ¿qué diferencias aprecias entre ambos sitios?;
    - ¿qué indicios apoyan la idea de un sitio mayoritariamente estático o de una aplicación más dinámica?;
    - ¿qué cosas sigues sin poder saber?

**Captura:** pestaña Network de ambos sitios con los totales visibles.

---

## Paso 5: Cookies y errores

Dos comprobaciones rápidas sobre los mismos dos sitios:

- En la pestaña de `Application / Storage → Cookies`, mira las **cookies** guardadas en cada uno. Anota cuántas hay y si alguna parece relacionada con sesión, idioma, preferencias o protección. ¿Observas atributos como `Secure`, `HttpOnly` o `SameSite`?
- Solicita en cada sitio una **ruta inventada** que seguro no existe y observa con qué código HTTP responde cada sitio, si hay redirección, qué contenido aparece y si la página de error parece propia del sitio o genérica.

!!! warning "¡Cuidado con las apariencias! Verifica el código real"
    Un sitio podría devolver `404`, `403`, una redirección o incluso `200`. Si ocurre esto último, explica por qué no demuestra necesariamente que la ruta exista.

**Capturas:** cookies de Amazon y respuesta de ambos sitios a la ruta inventada.

---

## Paso 6: Qué puedes demostrar y qué solo supones

Para Amazon, documenta en `actividad-1.1.md`:

| Pieza | ¿Tienes evidencia? | Evidencia concreta |
|---|---|---|
| Recursos estáticos | | |
| Artefacto y runtime del servidor | | |
| Datos | | |
| Configuración por entorno | | |
| Secretos | | |

La segunda columna solo admite tres respuestas: **sí**, **no** o **no es observable desde fuera**. Y la tercera columna es obligatoria cuando respondas que sí: hay que decir qué has visto en tus capturas que lo demuestre.

Escribir "tendrá una base de datos, seguro" no vale. Escribir "no puedo demostrarlo desde fuera, pero un catálogo de ese tamaño no se mantiene a mano" sí vale, porque estás diciendo abiertamente que es una deducción.

Cierra con dos párrafos cortos:

1. **Vuelve a tus predicciones del paso 1.** ¿En qué acertaste? ¿Qué te mostró la evidencia que no habías previsto?
2. **¿En cuál de los dos sitios crees que notarías antes desde fuera que algo interno se ha roto?** Justifica la respuesta sin afirmar nada que no puedas observar.

---

## Verificación

Para dar por válida la práctica se ejecutará:

```bash
curl -I https://alejandroroig.github.io/despliegue-daw/tema1/actividad_1_1/
curl -I https://alejandroroig.github.io/despliegue-daw/tema1/actividad_1_1/
curl -I https://www.amazon.es/
```

Y debe observarse:

- Que los datos recogidos en `actividad-1.1.md` son **coherentes** con lo que devuelven los sitios al corregir.
- Que has identificado e interpretado las principales cabeceras relacionadas con caché y has comparado su valor entre ambas peticiones.
- Que has anotado el código con el que responde la tienda a la petición de cabecera y que no lo has confundido con un error tuyo.
- Que cada casilla marcada como "sí" en la tabla del paso 6 señala una evidencia concreta que aparece en tus capturas.

Si un sitio ha cambiado desde que hiciste la práctica, tu captura lo justifica: por eso se piden capturas y no transcripciones a mano.

---

## Qué se entrega

- [ ] Las cabeceras de los dos sitios, recogidas e interpretadas.
- [ ] Las dos peticiones seguidas a la web de apuntes, con las cabeceras que cambian identificadas.
- [ ] El inventario de red de los dos sitios: totales, tipos y dominios.
- [ ] Las cookies y la respuesta a la ruta inexistente.
- [ ] La tabla de las cinco piezas, con la columna de evidencia completa.
- [ ] Los dos párrafos de cierre del paso 6.
- [ ] `actividad-1.1.md` con las respuestas y reflexiones.
- [ ] Carpeta `img/` con las capturas utilizadas y enlazadas mediante rutas relativas.
- [ ] Carpeta completa comprimida como `actividad-1.1.zip`.

!!! info "Dónde se entrega"
    Comprime la carpeta completa `actividad-1.1/` como `actividad-1.1.zip` y súbela a la tarea correspondiente de Aules. Debe incluir el fichero `actividad-1.1.md` y la carpeta `img/` con las capturas enlazadas desde el Markdown. **Conserva esta carpeta: la incorporarás a tu repositorio en la próxima sesión.**

---

## ✅ Cierre

Al terminar tienes un método para mirar cualquier sitio web desde fuera y hacerte una idea razonable de lo que hay detrás, con dos herramientas que vas a usar todo el curso. Y tienes algo más valioso: la costumbre de decir "esto lo he visto" y "esto lo estoy suponiendo" sin mezclarlo, que es la diferencia entre diagnosticar y adivinar.

También has visto de primera mano varias piezas que aparecerán durante el módulo: contenido que puede quedar almacenado en una caché intermedia, cookies que permiten conservar información entre peticiones y capas como CDN, proxies o mecanismos de protección que pueden responder antes de que la petición llegue a la aplicación de origen.

Y tienes una lista implícita de todo lo que no se puede saber desde fuera: ahí empieza el resto del módulo. En la próxima sesión dejas de mirar despliegues ajenos y empiezas a preparar el tuyo. El primer paso no es una máquina ni un servidor: es el sitio donde van a vivir el código y el procedimiento, con una rama por cada sesión, etiquetas que permiten identificar estados concretos del repositorio y secretos que nunca entran en el historial.