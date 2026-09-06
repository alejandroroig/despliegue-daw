# 🧪 Actividad 3.1: Dos sitios, un servidor y dos nombres

!!! warning "Descarga los materiales"
    Para esta actividad necesitas:

    - 📦 [`escaparate-estatico.zip`](descargas/escaparate-estatico.zip){target="_blank" rel="noopener"}
    - 📦 [`escaparate-docs.zip`](descargas/escaparate-docs.zip){target="_blank" rel="noopener"}

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
- **Configurar** un servidor por defecto para nombres no reconocidos.
- **Aplicar y comprobar** compresión y caché sobre contenido estático.
- **Distinguir** por qué la entrega de recursos estáticos y las respuestas dinámicas no reciben necesariamente la misma política de caché.
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

**Captura 1:** `docker compose ps` mostrando que solo `web` publica un puerto.

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

Copia en `actividad-3.1.md` las líneas relevantes de `dig` e indica qué representan el registro, la dirección y el TTL.

**Captura 2:** catálogo y documentación funcionando mediante los dos nombres distintos.

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

Anota en `actividad-3.1.md` qué sitio respondía antes de declarar el servidor por defecto y qué ocurre después.

La comprobación final de este `404` se incluirá junto con la evidencia del paso siguiente.

!!! question "Reflexiona"
    ¿Por qué es más seguro declarar explícitamente qué debe ocurrir con un nombre desconocido que aceptar el comportamiento por defecto del servidor?

---

## Paso 4: Entrega eficientemente el contenido estático

Hasta ahora Nginx ya sirve directamente HTML, CSS, JavaScript e imágenes. Ahora vas a comprobar otra ventaja de separar la entrega de contenido estático de la lógica de la aplicación: **el servidor web puede aplicar políticas específicas de compresión y caché**.

No necesitas descubrir la sintaxis por tu cuenta. Añade al bloque `server` del catálogo esta configuración:

```nginx
# Activa la compresión de respuestas.
gzip on;

# Informa a las cachés de que la respuesta puede variar
# según si el cliente acepta o no gzip.
gzip_vary on;

# HTML ya está contemplado por Nginx al activar gzip.
# Aquí añadimos otros tipos de contenido textual.
gzip_types text/css application/javascript application/json;

# Los recursos estáticos pueden conservarse durante más tiempo
# en la caché del navegador.
location ~* \.(css|js|png|jpg|jpeg|svg|webp)$ {
    expires 7d;
}
```

### 4.1. Qué hace cada directiva

| Directiva | Qué consigue |
|---|---|
| `gzip on` | permite comprimir respuestas antes de enviarlas |
| `gzip_vary on` | añade información para distinguir respuestas comprimidas y no comprimidas en las cachés |
| `gzip_types` | indica otros tipos de contenido textual que pueden comprimirse |
| `location ~* ...` | aplica una regla a determinados tipos de fichero |
| `expires 7d` | permite que esos recursos permanezcan más tiempo en la caché del navegador |

No incluimos JPEG o PNG en `gzip_types`: son formatos que ya almacenan la información comprimida y volver a comprimirlos suele aportar poco o nada.

Tampoco aplicamos esta política de siete días al HTML ni a `/api/`. Los recursos estáticos suelen cambiar de forma más controlada; una respuesta dinámica puede depender del estado actual de la aplicación.

!!! note "Dinámico no significa nunca cacheable"
    Una respuesta de una API también puede diseñarse para utilizar caché. La idea de esta práctica es más sencilla: **no debes aplicar automáticamente a una respuesta dinámica la misma política larga que a un CSS, un JavaScript o una imagen**.

### 4.2. Valida y recarga

Antes de comprobar nada:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

### 4.3. Comprueba gzip sobre un recurso estático

Utiliza el CSS principal:

```text
/css/app.css
```

Inspecciona las cabeceras:

```bash
curl -I -H "Accept-Encoding: gzip" \
  http://escaparate.127.0.0.1.nip.io/css/app.css
```

Localiza, como mínimo:

```text
Content-Encoding: gzip
Cache-Control: ...
```

Después mide los bytes transferidos con y sin compresión:

```bash
curl -s -H "Accept-Encoding: identity" -o /dev/null \
  -w "sin comprimir: %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io/css/app.css

curl -s -H "Accept-Encoding: gzip" -o /dev/null \
  -w "comprimido:   %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io/css/app.css
```

Anota los resultados:

| Comprobación | Resultado |
|---|---|
| CSS sin comprimir | |
| CSS con gzip | |
| `Content-Encoding` del CSS | |
| `Cache-Control` del CSS | |

### 4.4. Contrasta con una respuesta dinámica

Consulta ahora las cabeceras de readiness mediante una petición real `GET`:

```bash
curl -s -D - -o /dev/null \
  http://escaparate.127.0.0.1.nip.io/api/salud/listo
```

Compara esa respuesta con la del CSS.

!!! question "Reflexiona"
    1. ¿Por qué `app.css` puede beneficiarse tanto de gzip como de una caché relativamente larga?
    2. ¿Por qué no deberíamos aplicar automáticamente esa misma política de caché a todas las respuestas de `/api/`?
    3. ¿Por qué comprimir otra vez un JPEG o PNG suele aportar poco?

Para terminar, ejecuta también:

```bash
curl -i http://cualquier-cosa.127.0.0.1.nip.io/
```

**Captura 3:** una única captura de terminal donde se vea el `404` para el nombre desconocido y las mediciones del CSS con y sin gzip.

---

## Paso 5: Cierra la sesión

Antes de terminar, valida el estado final:

```bash
docker compose exec web nginx -t
docker compose ps
git status
```

El despliegue debe cumplir:

```text
web
→ publica 80
→ sirve el catálogo
→ sirve la documentación
→ reenvía /api/ hacia app

app
→ sin puerto publicado

bd
→ sin puerto publicado
```

### Actualiza solo la información nueva del `README.md`

El procedimiento general de Compose ya quedó documentado en la actividad anterior. No lo repitas.

Añade una sección breve con:

- URL del catálogo;
- URL de la documentación;
- URL de los informes;
- endpoint `/api/salud/listo` a través de Nginx;
- comandos para validar y recargar Nginx.

Por ejemplo:

```text
Catálogo:
http://escaparate.127.0.0.1.nip.io/

Documentación:
http://docs.127.0.0.1.nip.io/

Informes:
http://docs.127.0.0.1.nip.io/informes/
```

Revisa `entregas/tema3/actividad-3.1/actividad-3.1.md` y comprueba que contiene:

- respuestas a las reflexiones;
- interpretación básica de `dig`;
- mediciones de gzip;
- las tres capturas solicitadas.

Después sigue el flujo habitual:

1. registra los cambios utilizando la convención de commits del módulo;
2. publica `sesion-06`;
3. abre una Pull Request hacia `main`;
4. revisa la PR;
5. fusiona mediante **Create a merge commit**;
6. actualiza tu `main` local.

No necesitas añadir una captura específica de la Pull Request: su existencia y fusión podrán comprobarse directamente en el repositorio.

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

Un nombre no configurado debe devolver `404`:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Host: desconocido.127.0.0.1.nip.io" http://127.0.0.1/
```

También se comprobará que:

```text
web → publica 80
app → sin publicación
bd  → sin publicación
```

y que el CSS principal:

```text
/css/app.css
```

- puede enviarse con `Content-Encoding: gzip`;
- transfiere menos bytes al aceptar gzip;
- recibe una política de caché más larga que la aplicada por defecto a la respuesta dinámica de `/api/salud/listo`.

Finalmente se verificará que:

- `practicas/nginx/conf.d/sitios.conf` está versionado;
- `practicas/nginx/sitio-escaparate/escaparate/` está versionado;
- `practicas/nginx/sitio-docs/` está versionado;
- `README.md` contiene las nuevas URLs y los comandos básicos de Nginx;
- la actividad y sus tres capturas están en `entregas/tema3/actividad-3.1/`;
- la rama `sesion-06` llegó a `main` mediante Pull Request.

---

## Qué se entrega

- [ ] `entregas/tema3/actividad-3.1/actividad-3.1.md` con resultados y reflexiones.
- [ ] `entregas/tema3/actividad-3.1/img/` con **tres capturas** enlazadas mediante rutas relativas.
- [ ] `practicas/compose/compose.yaml` actualizado con `web`, `app` y `bd`.
- [ ] `practicas/nginx/conf.d/sitios.conf` con los dos hosts y el servidor por defecto.
- [ ] Frontend estático versionado en `practicas/nginx/sitio-escaparate/escaparate/`.
- [ ] Documentación versionada en `practicas/nginx/sitio-docs/`.
- [ ] Catálogo y documentación accesibles mediante nombres distintos en el puerto 80.
- [ ] `app` y `bd` sin puertos publicados al anfitrión.
- [ ] Comprobación de gzip y caché sobre `/css/app.css`.
- [ ] Comparación conceptual entre un recurso estático y una respuesta dinámica de `/api/`.
- [ ] `README.md` actualizado únicamente con la información nueva de Nginx.
- [ ] Pull Request `sesion-06 → main` fusionada mediante merge commit.

---

## ✅ Cierre

El despliegue ya no expone directamente Spring Boot. Nginx se ha convertido en la puerta de entrada, sirve los recursos estáticos con reglas propias y decide qué sitio debe responder según el nombre solicitado.

También has comprobado que DNS y HTTP resuelven problemas diferentes: DNS lleva el nombre hasta una dirección, mientras que la cabecera `Host` permite al servidor decidir qué sitio debe responder una vez establecida la conexión.

En la siguiente sesión mantendrás exactamente esa puerta, pero abrirás el bloque `/api/` que hoy has utilizado como caja negra. Allí aprenderás qué significa actuar como proxy inverso, qué información hay que reenviar y cómo repartir el tráfico entre varias copias de Escaparate.
