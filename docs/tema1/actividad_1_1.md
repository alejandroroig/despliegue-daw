# 🧪 Actividad 1.1: Qué se puede saber de un despliegue desde fuera

## Contexto

Es tu primer día en una empresa que despliega y mantiene aplicaciones web. Antes de dejarte tocar ningún servidor, tu responsable te propone un ejercicio sencillo: **mirar dos sitios que ya están funcionando y averiguar qué puedes demostrar sobre ellos sin acceso a ninguna máquina**.

No se trata de adivinar la arquitectura. Cuando una aplicación vaya lenta, falle o responda de forma extraña, muchas veces empezarás exactamente así: observando desde fuera.

La parte más importante de esta actividad será distinguir entre:

- **lo que has observado**;
- **lo que puedes deducir razonablemente**;
- **lo que no puedes saber desde fuera**.

!!! abstract "Cómo vas a trabajar"
    **Predice → observa → recoge evidencias → interpreta → concluye**

    No intentes descubrir toda la arquitectura. El objetivo es aprender a distinguir entre **lo que puedes demostrar**, **lo que solo puedes inferir** y **lo que permanece oculto desde fuera**.

---

## Qué vas a practicar

- Obtener e interpretar cabeceras HTTP básicas.
- Reconocer indicios de caché, CDN o proxy.
- Analizar qué descarga realmente una página con DevTools.
- Distinguir contenido aparentemente estático y dinámico.
- Separar evidencia, inferencia y aspectos no observables desde fuera.

---

## Requisitos previos

- Navegador con herramientas de desarrollador.
- `curl` instalado.

Comprueba:

```bash
curl --version
```

---

## Paso 1: Observa antes de concluir

Trabajarás con estos dos sitios:

| Sitio | Qué representa |
|---|---|
| `https://alejandroroig.github.io/despliegue-daw/tema1/actividad_1_1/` | Documentación publicada como sitio estático |
| `https://www.amazon.es/` | Una aplicación web comercial compleja |

**No hace falta que inicies sesión ni que te crees ninguna cuenta**: todo lo que vas a mirar está disponible sin identificarse.

Crea una carpeta de trabajo:

```text
actividad-1.1/
├── actividad-1.1.md
└── img/
```

!!! info "Hoy no necesitas saber Markdown"
    Aunque el fichero termina en `.md`, **por ahora puedes escribir en él como si fuera un fichero de texto normal**. No necesitas conocer todavía títulos, listas, tablas, enlaces ni la sintaxis para insertar imágenes.

    Guarda las capturas solicitadas dentro de `img/` con nombres reconocibles. En el documento basta con indicar a qué captura te refieres, por ejemplo:

    ```text
    Evidencia: img/cabeceras.png
    ```

    En la próxima sesión aprenderás la sintaxis básica de Markdown, darás formato al documento y enlazarás correctamente las imágenes antes de incorporarlo a tu repositorio.

Antes de utilizar ninguna herramienta, en el fichero `actividad-1.1.md` escribe una breve predicción para cada sitio:

- ¿esperas que sea principalmente estático o dinámico?
- ¿esperas una carga sencilla o muchas peticiones y recursos?
- ¿crees que podrás descubrir fácilmente cómo está desplegado?

No se evalúa acertar. Al final volverás a estas predicciones.

---

## Paso 2: Pregunta por las cabeceras

Obtén las cabeceras de cada sitio sin descargar el cuerpo de la página:

```bash
curl -I https://alejandroroig.github.io/despliegue-daw/tema1/actividad_1_1/
curl -I https://www.amazon.es/
```

Si algún sitio rechaza la petición `HEAD`, usa un `GET` normal descartando el contenido:

```bash
curl -sS -D - -o /dev/null https://direccion
```

Antes de sacar conclusiones, busca **pistas** en la respuesta. Además de `Content-Type`, fíjate especialmente en cabeceras relacionadas con **servidor, caché, CDN, proxy o redirecciones**. No tienen por qué aparecer todas ni existe una única combinación correcta.

Recoge para cada sitio la información siguiente:

```text
WEB DE APUNTES
Código:
Content-Type:
Server, si aparece:
2 o 3 cabeceras que te den alguna pista:
Qué puedo afirmar a partir de ellas:

AMAZON
Código:
Content-Type:
Server, si aparece:
2 o 3 cabeceras que te den alguna pista:
Qué puedo afirmar a partir de ellas:
```

!!! tip "Sobre Amazon"
    Amazon puede responder a una petición hecha con `curl` de forma distinta a una realizada desde un navegador normal. Si aparece un `405`, un `202`, una respuesta de CloudFront o una cabecera de desafío, **no intentes sortearla ni forzar el acceso**: anota el código y las cabeceras. Esa diferencia también forma parte de la evidencia.

**Captura:** guarda la salida de las cabeceras en `img/` con un nombre reconocible. En `actividad-1.1.md` indica simplemente el nombre del fichero que contiene la evidencia.

---

## Paso 3: Repite y busca señales de caché

Pide dos veces seguidas las cabeceras de la web de apuntes:

```bash
curl -I https://alejandroroig.github.io/despliegue-daw/tema1/actividad_1_1/
curl -I https://alejandroroig.github.io/despliegue-daw/tema1/actividad_1_1/
```

Compara ambas respuestas. Fíjate si aparece alguna cabecera como:

```text
Age
X-Cache
X-Cache-Hits
Via
X-Served-By
Cache-Control
```

Responde brevemente:

1. ¿Hay alguna evidencia de caché, CDN o capa intermedia?
2. ¿Qué cambia entre las dos respuestas?
3. ¿Qué no puedes asegurar aunque veas esas cabeceras?

**Captura:** guarda una captura donde puedan compararse las dos respuestas. En `actividad-1.1.md` indica el nombre del fichero correspondiente.

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

Anota los datos como texto normal:

```text
WEB DE APUNTES
Peticiones totales aproximadas:
Datos transferidos aproximados:
¿Se descargan recursos desde otros dominios? ¿Cuáles?
¿Aparecen peticiones fetch/xhr?:

AMAZON
Peticiones totales aproximadas:
Datos transferidos aproximados:
¿Se descargan recursos desde otros dominios? ¿Cuáles?
¿Aparecen peticiones fetch/xhr?:
```

Después responde:

> ¿Qué diferencia te parece más significativa entre ambos sitios y qué evidencia la apoya?

**Capturas:** guarda en `img/` una captura de la pestaña Network de cada sitio con los totales visibles. En el documento basta con indicar los nombres de los ficheros.

---

## Paso 5: Evidencia, inferencia y límites

Ahora analiza Amazon utilizando las piezas básicas de un despliegue.

Para cada una, clasifica lo que sabes como:

- **Observado**: existe una evidencia directa.
- **Inferido**: hay indicios, pero no puedes demostrarlo.
- **No observable**: no dispones de información suficiente desde fuera.

```text
RECURSOS ESTÁTICOS
Clasificación: observado / inferido / no observable
Evidencia o razonamiento:

APLICACIÓN / RUNTIME
Clasificación: observado / inferido / no observable
Evidencia o razonamiento:

DATOS
Clasificación: observado / inferido / no observable
Evidencia o razonamiento:

CONFIGURACIÓN
Clasificación: observado / inferido / no observable
Evidencia o razonamiento:

SECRETOS
Clasificación: observado / inferido / no observable
Evidencia o razonamiento:
```

Cuando respondas **sí**, debes indicar qué has visto. Cuando respondas **no es observable desde fuera**, puedes añadir una inferencia razonable, pero dejando claro que no la has demostrado.

Cierra con una conclusión breve de 4 a 6 líneas que incluya:

1. algo que hayas podido demostrar;
2. algo que solo puedas inferir;
3. algo que no puedas saber desde fuera;
4. si tus predicciones iniciales se han mantenido o han cambiado.

---

## Si te sobra tiempo: cookies y rutas inexistentes

Realiza dos comprobaciones adicionales:

1. En **Application / Storage → Cookies**, mira las cookies guardadas en cada sitio. Anota cuántas hay y si alguna parece relacionada con sesión, idioma, preferencias o protección.
2. Solicita una ruta inventada que seguro no existe y observa qué código HTTP devuelve cada sitio.

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://direccion/ruta-inventada-12345
```

!!! warning "Cuidado con las apariencias"
    Un sitio podría devolver `404`, `403`, una redirección o incluso `200`. Si ocurre esto último, no demuestra necesariamente que la ruta exista.

---

## Qué se entrega

Entrega `actividad-1.1.zip` con esta estructura:

```text
actividad-1.1/
├── actividad-1.1.md
└── img/
    └── capturas...

El documento debe contener:

- [ ] predicciones iniciales;
- [ ] cabeceras y análisis de ambos sitios;
- [ ] comparación de caché;
- [ ] observaciones realizadas con DevTools;
- [ ] clasificación entre evidencia, inferencia y aspectos no observables;
- [ ] conclusión final.

!!! info "Importante"
    Comprime la carpeta completa `actividad-1.1/` como `actividad-1.1.zip` y súbela a la tarea correspondiente de Aules. Todavía no se evalúa la sintaxis Markdown ni es necesario insertar las imágenes en el documento. Conserva la carpeta: en la próxima sesión le darás formato, enlazarás las capturas y la incorporarás a tu repositorio.

---

## ✅ Cierre

Ya tienes un primer método para analizar un sistema web desde fuera: **observar, recoger evidencias e interpretar sin confundir hechos con suposiciones**.

Esa forma de trabajar será fundamental cuando tengas que diagnosticar tus propios despliegues.
