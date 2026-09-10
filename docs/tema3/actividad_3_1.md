# 🧪 Actividad 3.1: Dos sitios, un servidor y dos nombres

!!! warning "Descarga los materiales"
    Para esta actividad necesitas:

    - 📦 [`escaparate-estatico.zip`](descargas/escaparate-estatico.zip){target="_blank" rel="noopener"}
    - 📦 [`escaparate-docs.zip`](descargas/escaparate-docs.zip){target="_blank" rel="noopener"}

## Contexto

Al terminar el Tema 2, el navegador accedía directamente a Escaparate:

```text
navegador → app → bd
```

En esta sesión aparece una nueva puerta de entrada:

```text
                 ┌→ frontend estático
navegador → web ─┼→ documentación
                 └→ /api/ → app → bd
```

`web` será Nginx. Desde el anfitrión solo se publicará su puerto 80; `app` y `bd` permanecerán dentro de la red de Compose.

!!! abstract "Cómo vas a trabajar"
    **Añadir → nombrar → separar → proteger → optimizar → comprobar**

    La actividad no pretende que aprendas todavía proxy inverso. El bloque `/api/` se utilizará como una **caja negra** para mantener la aplicación funcionando mientras te centras en Nginx, hosts virtuales, DNS y contenido estático.

---

## Qué vas a practicar

- Añadir Nginx como nueva puerta de entrada del despliegue.
- Servir contenido estático desde raíces distintas.
- Configurar dos hosts virtuales sobre una misma dirección y puerto.
- Resolver nombres con DNS y relacionarlos con la cabecera `Host`.
- Definir qué ocurre con nombres no reconocidos.
- Comprobar gzip y caché sobre recursos estáticos.
- Validar y recargar configuración sin recrear el contenedor.
- Versionar la configuración del servidor web.

---

## Requisitos previos

Necesitas:

- la Actividad 2.3 terminada;
- `ghcr.io/<usuario>/escaparate:sesion-04`;
- la imagen de base de datos de la Actividad 2.1;
- `escaparate-estatico.zip`;
- `escaparate-docs.zip`;
- el repositorio actualizado.

Prepara la rama:

```bash
git switch main
git pull --ff-only
git switch -c sesion-06
```

Crea:

```text
entregas/
└── tema3/
    └── actividad-3.1/
        ├── actividad-3.1.md
        └── img/
```

Los ficheros técnicos quedarán en:

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

!!! info "Evidencias"
    Solo se piden **tres evidencias**. Documenta decisiones, resultados y reflexiones; no conviertas `actividad-3.1.md` en una transcripción de comandos.

---

## Paso 1: Añade Nginx como puerta de entrada

Descomprime `escaparate-estatico.zip` dentro de:

```text
practicas/nginx/sitio-escaparate/
```

El ZIP crea la carpeta `escaparate/`. Comprueba que existe:

```text
practicas/nginx/sitio-escaparate/escaparate/index.html
```

No muevas ni aplanes su contenido.

### 1.1. Añade el servicio `web`

Modifica `practicas/compose/compose.yaml`.

El nuevo servicio debe:

- utilizar `nginx:1.30.4-alpine`;
- publicar `80:80`;
- montar `../nginx/conf.d/` en `/etc/nginx/conf.d/` como solo lectura;
- montar el frontend en `/srv/www/escaparate/` como solo lectura;
- depender de `app`.

Al mismo tiempo:

- elimina la publicación de `8080` de `app`;
- mantiene `bd` sin puerto publicado.

!!! info "Por qué usamos `/srv/www/...`"
    La imagen oficial de Nginx tiene una raíz predeterminada, pero aquí utilizaremos rutas propias. Eso obliga a declarar explícitamente qué directorio pertenece a cada sitio y facilita separar catálogo y documentación.

### 1.2. Configura el primer sitio

Crea:

```text
practicas/nginx/conf.d/sitios.conf
```

Configura un bloque `server` para:

```text
escaparate.127.0.0.1.nip.io
```

Debes decidir, usando la teoría:

- `listen`;
- `server_name`;
- `root`;
- `index`.

Añade además **exactamente este bloque**:

```nginx
location /api/ {
    proxy_pass http://app:8080;
}
```

!!! danger "El bloque `/api/` es una caja negra"
    No modifiques `proxy_pass` ni añadas todavía cabeceras. Su única función hoy es que el frontend pueda seguir accediendo a la API.

    En la Actividad 3.2 estudiarás esta parte con detalle.

Levanta:

```bash
cd practicas/compose
docker compose up -d
```

Diagnostica:

```bash
docker compose ps
docker compose logs web
```

Valida Nginx:

```bash
docker compose exec web nginx -t
```

Prueba directamente el host virtual:

```bash
curl -I \
  -H "Host: escaparate.127.0.0.1.nip.io" \
  http://127.0.0.1/
```

y la parte dinámica:

```bash
curl -fsS \
  -H "Host: escaparate.127.0.0.1.nip.io" \
  http://127.0.0.1/api/salud/listo
```

**Comprueba:**

- solo `web` publica un puerto;
- el frontend lo sirve Nginx;
- `/api/salud/listo` continúa respondiendo.

**Evidencia 1:** `docker compose ps` mostrando que solo `web` publica puerto y una comprobación correcta del sitio.

!!! question "Reflexiona"
    `app` continúa conteniendo una copia del frontend. ¿Por qué el navegador está utilizando ahora la copia servida por Nginx?

---

## Paso 2: Publica dos sitios sobre la misma dirección

Antes de configurar el segundo sitio, observa cómo se resuelve el nombre:

```bash
dig escaparate.127.0.0.1.nip.io
```

En `actividad-3.1.md` identifica:

- tipo de registro;
- dirección obtenida;
- TTL.

No necesitas copiar toda la salida de `dig`.

Abre:

```text
http://escaparate.127.0.0.1.nip.io/
```

No escribas ningún puerto.

### 2.1. Añade la documentación

Descomprime `escaparate-docs.zip` dentro de:

```text
practicas/nginx/sitio-docs/
```

Añade al servicio `web` un montaje de esa carpeta sobre:

```text
/srv/www/docs
```

en modo de solo lectura.

Crea un segundo bloque `server` para:

```text
docs.127.0.0.1.nip.io
```

Debe:

- utilizar `/srv/www/docs` como raíz;
- servir su página inicial;
- permitir `autoindex` únicamente en `/informes/`.

Después:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Comprueba:

```text
http://escaparate.127.0.0.1.nip.io/
→ catálogo

http://docs.127.0.0.1.nip.io/
→ documentación

http://docs.127.0.0.1.nip.io/informes/
→ listado de informes
```

**Evidencia 2:** catálogo y documentación funcionando mediante los dos nombres diferentes.

!!! question "Reflexiona"
    Ambos nombres resuelven a `127.0.0.1` y llegan al puerto 80 del mismo Nginx. ¿Qué información de la petición HTTP permite seleccionar un bloque `server` distinto?

!!! warning "Si `nip.io` no funciona en la red del centro"
    Puedes añadir temporalmente:

    ```text
    127.0.0.1 escaparate.127.0.0.1.nip.io
    127.0.0.1 docs.127.0.0.1.nip.io
    ```

    a `/etc/hosts` para que el navegador pueda resolver ambos nombres.

    Recuerda: `dig` seguirá consultando DNS y no utilizará `/etc/hosts`. Si la red bloquea `nip.io`, documenta ese hecho y realiza la observación DNS sobre otro nombre público que sí pueda resolver.

---

## Paso 3: Decide qué ocurre con un nombre desconocido

Prueba:

```bash
curl -i http://cualquier-cosa.127.0.0.1.nip.io/
```

Observa qué sitio responde antes de modificar la configuración.

Añade un bloque `server` explícito que:

- sea `default_server`;
- no represente ninguno de tus sitios;
- devuelva siempre `404`.

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Repite la petición.

**Comprueba:**

- los dos nombres válidos siguen funcionando;
- un nombre desconocido devuelve `404`;
- no aparece accidentalmente catálogo ni documentación.

En `actividad-3.1.md` explica brevemente qué ocurría antes y después de declarar el servidor por defecto.

!!! question "Reflexiona"
    ¿Por qué es preferible decidir explícitamente qué debe responder un nombre desconocido?

---

## Paso 4: Aplica políticas al contenido estático

Nginx ya sirve directamente HTML, CSS, JavaScript e imágenes. Ahora aplicarás dos políticas propias de esa capa: **compresión** y **caché**.

Añade al bloque del catálogo:

```nginx
gzip on;
gzip_vary on;
gzip_types text/css application/javascript application/json;

location ~* \.(css|js|png|jpg|jpeg|svg|webp)$ {
    expires 7d;
}
```

Interpreta cada decisión:

| Directiva | Objetivo |
|---|---|
| `gzip on` | activar compresión |
| `gzip_vary on` | distinguir variantes según `Accept-Encoding` |
| `gzip_types` | añadir tipos textuales comprimibles |
| `expires 7d` | permitir una caché más larga para ciertos recursos |

`text/html` ya está contemplado por el módulo gzip. No añadimos JPEG o PNG a `gzip_types` porque ya utilizan compresión propia.

!!! note "No copies la misma política a `/api/`"
    Una respuesta dinámica también puede utilizar caché, pero su política debe decidirse según el significado y la volatilidad del dato.

### 4.1. Valida y recarga

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

### 4.2. Comprueba el CSS

Consulta:

```bash
curl -I \
  -H "Accept-Encoding: gzip" \
  http://escaparate.127.0.0.1.nip.io/css/app.css
```

Busca:

```text
Content-Encoding: gzip
Cache-Control: ...
```

Mide después el cuerpo real:

```bash
curl -s \
  -H "Accept-Encoding: identity" \
  -o /dev/null \
  -w "sin comprimir: %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io/css/app.css

curl -s \
  -H "Accept-Encoding: gzip" \
  -o /dev/null \
  -w "comprimido:   %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io/css/app.css
```

Completa:

| Comprobación | Resultado |
|---|---|
| CSS sin comprimir | |
| CSS con gzip | |
| `Content-Encoding` | |
| `Cache-Control` | |

### 4.3. Contrasta con una respuesta dinámica

Consulta mediante `GET`:

```bash
curl -s -D - -o /dev/null \
  http://escaparate.127.0.0.1.nip.io/api/salud/listo
```

Compara sus cabeceras con las del CSS.

Finalmente:

```bash
curl -i http://cualquier-cosa.127.0.0.1.nip.io/
```

**Evidencia 3:** una captura de terminal donde se vean el `404` del nombre desconocido y las mediciones del CSS con y sin gzip.

!!! question "Reflexiona"
    1. ¿Por qué un CSS es buen candidato para compresión y caché?
    2. ¿Por qué no aplicarías automáticamente esa misma caché a todas las respuestas de `/api/`?
    3. ¿Por qué gzip aporta poco sobre JPEG o PNG?

---

## Paso 5: Documenta e integra

Valida el estado final:

```bash
docker compose exec web nginx -t
docker compose ps
git status
```

Debe cumplirse:

```text
web
→ publica 80
→ sirve catálogo
→ sirve documentación
→ reenvía /api/ hacia app

app
→ sin puerto publicado

bd
→ sin puerto publicado
```

### 5.1. Actualiza el README

No repitas toda la puesta en marcha de Compose. Añade únicamente la información nueva:

- URL del catálogo;
- URL de documentación;
- URL de informes;
- `/api/salud/listo` a través de Nginx;
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

### 5.2. Revisa la entrega

`actividad-3.1.md` debe contener:

- reflexión sobre la nueva puerta de entrada;
- interpretación básica de `dig`;
- reflexión sobre `Host`;
- comportamiento del `default_server`;
- mediciones y reflexión sobre gzip/caché;
- tres evidencias.

Registra los cambios, publica `sesion-06` y abre:

```text
sesion-06 → main
```

Fusiona mediante **Create a merge commit** cuando las comprobaciones habituales sean correctas.

Actualiza:

```bash
git switch main
git pull --ff-only
```

---

## Qué se entrega

Antes de terminar, comprueba:

- [ ] `practicas/compose/compose.yaml` con `web`, `app` y `bd`;
- [ ] `practicas/nginx/conf.d/sitios.conf`;
- [ ] frontend estático versionado;
- [ ] documentación versionada;
- [ ] dos hosts virtuales funcionando en el puerto 80;
- [ ] `default_server` para nombres desconocidos;
- [ ] `app` y `bd` sin puertos publicados;
- [ ] gzip y caché comprobados sobre `/css/app.css`;
- [ ] README con las nuevas URLs y comandos de Nginx;
- [ ] `actividad-3.1.md` con reflexiones, mediciones y tres evidencias;
- [ ] Pull Request `sesion-06 → main` fusionada.

!!! info "Dónde queda la entrega"
    Configuración, frontend, documentación y evidencias quedan versionados en el repositorio. No se genera un documento adicional fuera de él.

??? info "Cómo se comprobará"
    Desde un clon limpio se podrá levantar el conjunto y comprobar:

    ```bash
    docker compose up -d
    docker compose exec web nginx -t

    curl -fsS http://escaparate.127.0.0.1.nip.io/ > /dev/null
    curl -fsS http://escaparate.127.0.0.1.nip.io/api/salud/listo
    curl -fsS http://docs.127.0.0.1.nip.io/ > /dev/null
    curl -fsS http://docs.127.0.0.1.nip.io/informes/ > /dev/null
    ```

    También deberá comprobarse que:

    - solo `web` publica puerto;
    - un host desconocido devuelve `404`;
    - el CSS puede recibirse con gzip;
    - el CSS tiene una política de caché más larga que la respuesta de readiness;
    - todos los ficheros técnicos y evidencias están versionados.

---

## ✅ Cierre

Nginx se ha convertido en la **puerta de entrada pública** del despliegue. Puede servir directamente contenido estático, seleccionar sitios por nombre y aplicar políticas propias de entrega.

Spring Boot no ha desaparecido: su Tomcat embebido continúa ejecutando la aplicación detrás de Nginx.

En la siguiente sesión abrirás la caja negra de `/api/` y estudiarás cómo funciona realmente el **proxy inverso**.
