# 🧪 Actividad 3.1: Dos sitios, un servidor y un nombre

!!! warning "Descarga los materiales"
    Para esta actividad necesitas:

    - 📦 [`escaparate-estatico.zip`](files/escaparate-estatico.zip){target="_blank" rel="noopener"}
    - 📦 [`escaparate-docs.zip`](files/escaparate-docs.zip){target="_blank" rel="noopener"}

## Contexto

Escaparate ya se levanta con un único `docker compose up -d`, pero el resultado del Tema 2 todavía tiene una arquitectura muy directa:

```text
Navegador
    │ :8080
    ▼
app
(frontend + API)
    │
    ▼
bd
```

En esta sesión aparece una nueva pieza. **Nginx será la única puerta de entrada del despliegue**. Servirá directamente una distribución estática del frontend y un segundo sitio con la documentación del proyecto. Las peticiones dinámicas seguirán llegando a la aplicación Java, pero lo harán a través de Nginx.

El objetivo final es:

```text
                         ┌── frontend estático
Navegador ──► web:Nginx ├── documentación
                         │
                         └── /api/ ──► app ──► bd
```

Desde el equipo anfitrión solo debe publicarse el puerto 80 de `web`.

## Qué vas a practicar

- **Incorporar** un servidor web como nueva puerta de entrada de un despliegue existente.
- **Servir** contenido estático desde Nginx y mantener la API detrás de la red interna.
- **Configurar** dos hosts virtuales por nombre sobre la misma dirección y puerto.
- **Resolver** nombres con DNS y comprobar el resultado con `dig`.
- **Diagnosticar** problemas de permisos, raíz de documentos y tipos MIME.
- **Configurar** un servidor por defecto para nombres no reconocidos.
- **Medir** el efecto real de la compresión y comprobar las cabeceras de caché.
- **Versionar** la configuración del servidor como parte del despliegue.

## Requisitos previos

- La Actividad 2.3 terminada, con `app` y `bd` funcionando mediante `practicas/compose/compose.yaml`.
- La imagen pública:

```text
ghcr.io/<usuario>/escaparate:sesion-04
```

- La imagen pública de la base de datos de la Actividad 2.1.
- El paquete `escaparate-estatico.zip`.
- El paquete `escaparate-docs.zip`.
- El repositorio `daw-despliegue` actualizado.
- Tu rama de esta sesión:

```bash
git switch main
git pull --ff-only
git switch -c sesion-06
```

Crea al comenzar la entrega:

```text
entregas/
└── tema3/
    └── actividad-3.1/
        ├── actividad-3.1.md
        └── img/
```

Documenta en `actividad-3.1.md` las respuestas, mediciones y reflexiones de la práctica. Guarda las capturas en `img/` y enlázalas mediante rutas relativas.

Los ficheros técnicos no se duplican en `entregas/`. Trabajarás con esta estructura:

```text
practicas/
├── compose/
│   ├── compose.yaml
│   ├── .env
│   └── .env.example
└── nginx/
    ├── conf.d/
    │   └── sitios.conf
    ├── sitio-escaparate/
    │   └── escaparate/
    └── sitio-docs/
```

!!! info "Qué se versiona"
    Al descomprimir `escaparate-estatico.zip` en `sitio-escaparate/`, el ZIP crea una carpeta `escaparate/`. Por tanto, el frontend quedará en `practicas/nginx/sitio-escaparate/escaparate/`. Ese contenido **sí se versiona** en este repositorio docente, porque será el frontend que llevarás contigo a la instancia en la siguiente sesión.

    Descomprime `escaparate-docs.zip` en `practicas/nginx/sitio-docs/`. Este contenido formará parte del repositorio.

!!! info "Reparto de tiempo orientativo"
    - Pasos 1 y 2: unos 45 minutos.
    - Pasos 3 y 4: unos 40 minutos.
    - Paso 5: unos 20 minutos.

---

## Paso 1: Añade la nueva puerta de entrada

Descomprime `escaparate-estatico.zip` dentro de:

```text
practicas/nginx/sitio-escaparate/
```

El ZIP crea automáticamente una carpeta `escaparate/`, por lo que el resultado esperado es:

```text
practicas/
└── nginx/
    └── sitio-escaparate/
        └── escaparate/
            ├── index.html
            └── ...
```

Comprueba que existe:

```text
practicas/nginx/sitio-escaparate/escaparate/index.html
```

No muevas ni aplanes el contenido del ZIP.

Ahora modifica `practicas/compose/compose.yaml` para añadir un servicio:

```text
web
```

Debe cumplir estas condiciones:

- imagen `nginx:1.30.4-alpine`;
- publicar `80:80`;
- montar `../nginx/conf.d/` sobre `/etc/nginx/conf.d/` en modo de solo lectura;
- montar `../nginx/sitio-escaparate/escaparate/` sobre `/srv/www/escaparate/` en modo de solo lectura;
- depender de `app` para que la aplicación se inicie antes;
- `app` debe dejar de publicar su puerto 8080 hacia el anfitrión;
- `bd` continúa sin publicar PostgreSQL.

!!! info "¿Por qué `/srv/www/...`?"
    La imagen oficial de Nginx utiliza `/usr/share/nginx/html` como raíz web predeterminada. Aquí no vamos a depender de esa ubicación: montaremos nuestros dos sitios bajo `/srv/www/escaparate` y `/srv/www/docs`. Así las dos raíces quedan organizadas de forma simétrica y tendrás que declararlas explícitamente con `root` en cada host virtual.

Crea `practicas/nginx/conf.d/sitios.conf` con el primer sitio del despliegue. Configura tú `listen`, `server_name`, `root` e `index` utilizando lo visto en teoría.

El sitio responderá al nombre:

```text
escaparate.127.0.0.1.nip.io
```

Dentro de ese bloque incluye **exactamente este fragmento**, que hoy funciona como caja negra:

```nginx
location /api/ {
    proxy_pass http://app:8080;
}
```

!!! danger "El bloque `/api/` no se modifica hoy"
    Su función es permitir que el JavaScript servido por Nginx siga accediendo a la API. No cambies la directiva ni añadas todavía cabeceras de proxy. En la Actividad 3.2 escribirás y explicarás esta parte completa.

Levanta el conjunto:

```bash
docker compose up -d
```

Comprueba:

```bash
docker compose ps
```

El resultado debe mostrar una única publicación hacia el anfitrión:

```text
web → puerto 80
```

`app` y `bd` no deben mostrar puertos publicados.

Valida además la configuración:

```bash
docker compose exec web nginx -t
```

Antes de utilizar el navegador, prueba directamente el host virtual:

```bash
curl -I -H "Host: escaparate.127.0.0.1.nip.io" http://127.0.0.1/
```

Y comprueba también la parte dinámica:

```bash
curl -fsS -H "Host: escaparate.127.0.0.1.nip.io" \
  http://127.0.0.1/api/salud/listo
```

**Comprueba:** el frontend lo sirve Nginx, `/api/salud/listo` sigue respondiendo y solo `web` publica un puerto.

**Captura:** `docker compose ps`, validación de Nginx y catálogo funcionando.

!!! question "Reflexiona"
    La imagen `app` sigue conteniendo una copia integrada del frontend. Sin embargo, el navegador ya no la está utilizando. ¿Qué cambio en la arquitectura hace que ahora los ficheros públicos procedan de Nginx y no de Spring Boot?

---

## Paso 2: Deja de escribir direcciones y publica un segundo sitio

Resuelve primero:

```bash
dig escaparate.127.0.0.1.nip.io
```

Identifica en la respuesta:

- tipo de registro;
- dirección obtenida;
- TTL;
- servidor que ha contestado.

Repite la consulta y observa el TTL. No es obligatorio que siempre disminuya de la misma forma, porque puede intervenir la caché del resolutor. Lo importante es interpretar qué representa.

Abre después:

```text
http://escaparate.127.0.0.1.nip.io/
```

No escribas ningún puerto.

Ahora descomprime `escaparate-docs.zip` en:

```text
practicas/nginx/sitio-docs/
```

Asegúrate de que `sitio-docs/` **no** está excluido por `.gitignore`. Si habías añadido una regla para ignorarlo durante una prueba anterior, elimínala.

Añade al servicio `web` un montaje de esa carpeta sobre:

```text
/srv/www/docs
```

También en modo de solo lectura.

Crea en `sitios.conf` un **segundo bloque `server`** para:

```text
docs.127.0.0.1.nip.io
```

Debe:

- utilizar `/srv/www/docs` como raíz;
- servir la página inicial de la documentación;
- permitir el listado automático únicamente en `/informes/`.

Después de cambiar la configuración:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

**Comprueba:**

```text
http://escaparate.127.0.0.1.nip.io/
→ catálogo con productos

http://docs.127.0.0.1.nip.io/
→ documentación

http://docs.127.0.0.1.nip.io/informes/
→ listado navegable de informes
```

**Captura:** salida de `dig`, catálogo, documentación y listado de informes.

!!! question "Reflexiona"
    Los dos nombres resuelven a `127.0.0.1` y llegan al puerto 80 del mismo contenedor. ¿Qué dato de la petición HTTP permite a Nginx saber qué bloque `server` debe utilizar?

!!! warning "Si `nip.io` no resuelve en la red del centro"
    Añade temporalmente a `/etc/hosts`:

    ```text
    127.0.0.1 escaparate.127.0.0.1.nip.io
    127.0.0.1 docs.127.0.0.1.nip.io
    ```

    El navegador utilizará esas entradas. `dig`, sin embargo, seguirá consultando DNS directamente y no utilizará `/etc/hosts`.

    Para conservar la parte de DNS de la actividad, realiza entonces `dig` sobre un nombre público que sí resuelva y documenta su tipo de registro y TTL.

---

## Paso 3: Decide qué ocurre con un nombre desconocido

Prueba un nombre que no hayas configurado:

```bash
curl -i http://cualquier-cosa.127.0.0.1.nip.io/
```

Observa qué sitio responde.

Añade después un bloque `server` explícito que actúe como **servidor por defecto** y cuya única respuesta sea:

```text
404
```

Valida y recarga Nginx.

Repite la petición.

**Comprueba:**

- los dos nombres válidos siguen funcionando;
- un nombre desconocido devuelve `404`;
- no se muestra accidentalmente ni el catálogo ni la documentación.

**Captura:** respuesta del nombre desconocido antes y después de configurar el servidor por defecto.

!!! question "Reflexiona"
    ¿Por qué es más seguro declarar explícitamente qué debe ocurrir con un nombre desconocido que aceptar el comportamiento por defecto del servidor?

---

## Paso 4: Comprime, cachea y mídelo

Utiliza el fichero CSS principal de la distribución entregada:

```text
/css/app.css
```

Antes de cambiar la configuración:

```bash
curl -I -H "Accept-Encoding: gzip" \
  http://escaparate.127.0.0.1.nip.io/css/app.css
```

Comprueba que todavía no aparece una respuesta comprimida.

Configura después el sitio del catálogo para:

1. activar gzip sobre HTML, CSS y JavaScript;
2. no intentar comprimir imágenes ya comprimidas;
3. aplicar una política de caché larga a CSS, JavaScript e imágenes;
4. evitar aplicar al HTML la misma política larga.

Valida y recarga antes de probar.

Inspecciona las cabeceras:

```bash
curl -I -H "Accept-Encoding: gzip" \
  http://escaparate.127.0.0.1.nip.io/css/app.css
```

Después mide los bytes del cuerpo:

```bash
curl -s -H "Accept-Encoding: identity" -o /dev/null \
  -w "sin comprimir: %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io/css/app.css

curl -s -H "Accept-Encoding: gzip" -o /dev/null \
  -w "comprimido:   %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io/css/app.css
```

Completa en `actividad-3.1.md`:

| Recurso | Bytes con `identity` | Bytes con `gzip` | `Content-Encoding` | `Cache-Control` |
|---|---:|---:|---|---|
| CSS principal | | | | |
| Una imagen | | | | |
| `index.html` | | | | |

**Comprueba:**

- el CSS viaja con menos bytes al aceptar gzip;
- aparece `Content-Encoding: gzip` para el contenido textual;
- una imagen ya comprimida no obtiene una mejora equivalente;
- el HTML no recibe la misma política larga de caché que los recursos estáticos.

**Captura:** cabeceras antes y después, mediciones de bytes y tabla completada.

!!! question "Reflexiona"
    El fichero CSS almacenado dentro del contenedor no cambia de tamaño. ¿En qué momento se comprime y quién realiza la operación inversa? ¿Por qué aplicar otra compresión a un JPEG o PNG suele aportar poco o nada?

---

## Paso 5: Deja el despliegue preparado para otra persona

Antes de cerrar, comprueba la configuración completa:

```bash
docker compose exec web nginx -t
```

Revisa también:

```bash
docker compose ps
git status
```

El resultado final debe cumplir:

```text
web
→ publica 80
→ sirve sitio-escaparate
→ sirve sitio-docs
→ reenvía /api/ hacia app

app
→ sin puerto publicado

bd
→ sin puerto publicado
```

Actualiza `README.md` con:

- ubicación del `compose.yaml`;
- cómo crear `.env` desde `.env.example`;
- de dónde salió inicialmente `sitio-docs/` y que queda versionado junto al resto del despliegue;
- los dos nombres utilizados;
- URL del catálogo;
- URL de la documentación;
- URL de los informes;
- endpoint `/api/salud/listo` a través de Nginx;
- cómo comprobar gzip sobre `/css/app.css`;
- comandos para validar y recargar Nginx;
- cómo levantar y desmontar el conjunto.

Revisa `entregas/tema3/actividad-3.1/actividad-3.1.md` y comprueba que contiene todas las respuestas y capturas solicitadas.

Después sigue el flujo habitual:

1. registra los cambios utilizando la convención de commits del módulo;
2. publica `sesion-06`;
3. abre una Pull Request hacia `main`;
4. revisa la PR;
5. fusiona mediante **Create a merge commit**;
6. actualiza tu `main` local.

**Captura:** configuración validada, `README` renderizado y Pull Request antes de fusionarla.

---

## Verificación

La práctica se comprobará desde un clon limpio. Ese clon debe contener ya el frontend, la documentación y la configuración necesarias para levantar ambos sitios.

```bash
git clone https://github.com/<usuario>/daw-despliegue.git verifica
cd verifica/practicas/compose

cp .env.example .env
# se completarán los valores locales necesarios


docker compose up -d
docker compose ps

docker compose exec web nginx -t
```

Se comprobará:

```bash
curl -fsS http://escaparate.127.0.0.1.nip.io/ > /dev/null
curl -fsS http://escaparate.127.0.0.1.nip.io/api/salud/listo
curl -fsS http://docs.127.0.0.1.nip.io/ > /dev/null
curl -fsS http://docs.127.0.0.1.nip.io/informes/ | head
```

También:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Host: desconocido.127.0.0.1.nip.io" http://127.0.0.1/
```

Debe devolver:

```text
404
```

Se inspeccionará `docker compose ps` para comprobar que:

```text
web → publica 80
app → sin publicación
bd  → sin publicación
```

Y se repetirá la comprobación de gzip sobre `http://escaparate.127.0.0.1.nip.io/css/app.css`.

Finalmente se verificará que:

- `practicas/nginx/conf.d/sitios.conf` está versionado;
- `practicas/nginx/sitio-escaparate/escaparate/` está versionado;
- `practicas/nginx/sitio-docs/` está versionado;
- `README.md` permite reconstruir el despliegue;
- la actividad y las capturas están en `entregas/tema3/actividad-3.1/`;
- la rama `sesion-06` llegó a `main` mediante Pull Request.

---

## Qué se entrega

- [ ] `entregas/tema3/actividad-3.1/actividad-3.1.md` con resultados, mediciones y reflexiones.
- [ ] `entregas/tema3/actividad-3.1/img/` con las capturas enlazadas mediante rutas relativas.
- [ ] `practicas/compose/compose.yaml` actualizado con `web`, `app` y `bd`.
- [ ] `practicas/nginx/conf.d/sitios.conf` con los dos hosts y el servidor por defecto.
- [ ] Frontend estático versionado en `practicas/nginx/sitio-escaparate/escaparate/`.
- [ ] Documentación versionada en `practicas/nginx/sitio-docs/`.
- [ ] Catálogo y documentación accesibles mediante nombres distintos en el puerto 80.
- [ ] `app` y `bd` sin puertos publicados al anfitrión.
- [ ] Medición de gzip y comprobación de las políticas de caché.
- [ ] `README.md` actualizado con el procedimiento reproducible.
- [ ] Pull Request `sesion-06 → main` fusionada mediante merge commit.

---

## ✅ Cierre

El despliegue ya no expone directamente Spring Boot. Nginx se ha convertido en la puerta de entrada, sirve los recursos estáticos con reglas propias y decide qué sitio debe responder según el nombre solicitado.

También has comprobado que DNS y HTTP resuelven problemas diferentes: DNS lleva el nombre hasta una dirección, mientras que la cabecera `Host` permite al servidor decidir qué sitio debe responder una vez establecida la conexión.

En la siguiente sesión mantendrás exactamente esa puerta, pero abrirás el bloque `/api/` que hoy has utilizado como caja negra. Allí aprenderás qué significa actuar como proxy inverso, qué información hay que reenviar y cómo repartir el tráfico entre varias copias de Escaparate.
