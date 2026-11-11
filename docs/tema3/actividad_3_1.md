# 🧪 Actividad 3.1: Dos sitios, un servidor y dos nombres

!!! warning "Descarga los materiales"
    Para esta actividad necesitas:

    - 📦 [`escaparate-estatico.zip`](descargas/escaparate-estatico.zip){target="_blank" rel="noopener"}
    - 📦 [`escaparate-docs.zip`](descargas/escaparate-docs.zip){target="_blank" rel="noopener"}

## 🌐 Contexto

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

`web` será Nginx. Desde el anfitrión solo se publicará Nginx; `app` y `bd` permanecerán dentro de la red de Compose.

En los equipos del aula el puerto 80 ya está ocupado por Apache. Para no modificar servicios del sistema, publicaremos el puerto 80 del contenedor Nginx como **8088 en el anfitrión**:

```text
equipo del aula :8088
        ↓
contenedor Nginx :80
```

No debes detener ni deshabilitar Apache.

!!! abstract "Cómo vas a trabajar"
    **Añadir → comprobar → nombrar → comparar → proteger → medir → interpretar**

    La configuración base se proporciona cuando el objetivo no es memorizar sintaxis. Tu trabajo será aplicarla correctamente, comprobar qué ocurre y explicar por qué.

    El bloque `/api/` funcionará todavía como una **caja negra**. En la Actividad 3.2 estudiarás el proxy inverso con detalle.

!!! note "Si ya estás trabajando en EC2"
    Si trasladastes el proyecto a EC2 por la incidencia del puerto 80, no necesitas volver a local.

    En EC2 puedes mantener:

    ```yaml
    ports:
      - "80:80"
    ```

    y sustituir en las comprobaciones:

    ```text
    escaparate.127.0.0.1.nip.io:8088
    docs.127.0.0.1.nip.io:8088
    ```

    por:

    ```text
    escaparate.<IP-PUBLICA>.nip.io
    docs.<IP-PUBLICA>.nip.io
    ```

    El resto de la actividad es equivalente.

## 🎯 Qué vas a practicar

Al terminar la actividad deberías ser capaz de:

- incorporar Nginx como única puerta de entrada del despliegue;
- distinguir entre el puerto del anfitrión y el puerto del contenedor;
- servir contenido estático desde raíces distintas;
- comprobar cómo dos nombres pueden llegar al mismo Nginx y seleccionar sitios diferentes;
- relacionar DNS, dirección IP, cabecera `Host` y `server_name`;
- definir explícitamente qué ocurre con nombres no reconocidos;
- comprobar gzip y una política de caché sobre recursos estáticos;
- distinguir entre recargar Nginx y recrear un contenedor cuando cambia Compose;
- validar la configuración antes de aplicarla;
- versionar la configuración del servidor web.

## 🧰 Requisitos previos

Necesitas haber terminado la Actividad 2.3 y conservar su configuración de Compose.

Debes disponer de:

```text
ghcr.io/<usuario>/escaparate:sesion-04
ghcr.io/<usuario>/escaparate-db:1.0.0
```

y de los dos paquetes descargables indicados al inicio.

Conserva:

- `.env` para la configuración local;
- `secrets/bd_clave.txt` para la contraseña;
- el volumen de PostgreSQL;
- el `healthcheck` de `bd`;
- la dependencia `service_healthy` de `app`.

Prepara tu rama:

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
    Solo se piden **tres evidencias**.

    Documenta decisiones, resultados y reflexiones. No conviertas `actividad-3.1.md` en una transcripción de todos los comandos.

## 🚪 Paso 1: Añade Nginx como puerta de entrada

El primer objetivo es que el navegador deje de entrar directamente a Spring Boot. Nginx servirá el frontend y reenviará únicamente `/api/` hacia la aplicación.

### 1.1. Coloca el frontend estático

Descomprime `escaparate-estatico.zip` dentro de:

```text
practicas/nginx/sitio-escaparate/
```

Debe existir:

```text
practicas/nginx/sitio-escaparate/escaparate/index.html
```

No muevas ni aplanes su contenido.

El frontend está formado por archivos estáticos, pero los productos se consultan en la API. Abre `config.js` y comprueba que contiene:

```javascript
window.APP_CONFIG = {
    modo: "api",
    apiBase: "/api"
};
```

El navegador pedirá así los datos a `/api/` en el mismo sitio desde el que recibe HTML, CSS y JavaScript.

### 1.2. Añade el servicio `web`

Abre `practicas/compose/compose.yaml` y añade este servicio:

```yaml
  web:
    image: nginx:1.30.4-alpine
    ports:
      - "8088:80"
    volumes:
      - ../nginx/conf.d/:/etc/nginx/conf.d/:ro
      - ../nginx/sitio-escaparate/escaparate/:/srv/www/escaparate/:ro
    depends_on:
      - app
```

Al mismo tiempo, elimina la publicación de `8080` de `app` borrando la siguiente línea:

```yaml
ports:
  - "8080:8080"
```

No cambies el resto de `app` ni de `bd`.

La arquitectura debe quedar:

```text
ANFITRIÓN

localhost:8088
      │
      ▼
   web:Nginx
      │
      └── /api/ → app:8080 → bd:5432


RED DE COMPOSE

web → app → bd
```

!!! question "Comprueba que entiendes el puerto"
    En:

    ```yaml
    - "8088:80"
    ```

    ¿qué número corresponde al equipo del aula y cuál al contenedor Nginx?

    ¿Por qué `app` puede seguir escuchando en `8080` aunque ya no publique ese puerto hacia el anfitrión?

### 1.3. Configura el primer sitio

Crea:

```text
practicas/nginx/conf.d/sitios.conf
```

con este contenido:

```nginx
server {
    listen 80;
    server_name escaparate.*;

    root /srv/www/escaparate;
    index index.html;

    location /api/ {
        proxy_pass http://app:8080;
    }
}
```

Lee el bloque antes de continuar:

| Directiva | Qué estamos indicando |
|---|---|
| `listen 80` | Nginx escucha en el puerto 80 **dentro del contenedor** |
| `server_name` | este bloque atiende el nombre del catálogo |
| `root` | los ficheros públicos están en `/srv/www/escaparate` |
| `index` | `index.html` es la página inicial |
| `location /api/` | las peticiones dinámicas se entregan a `app` |

!!! info "Por qué usamos `escaparate.*`"
    En lugar de escribir un nombre completo, utilizamos:

    ```nginx
    server_name escaparate.*;
    ```

    Así la misma configuración de Nginx sirve en los dos entornos del módulo:

    ```text
    aula → escaparate.127.0.0.1.nip.io
    EC2  → escaparate.<IP-PUBLICA>.nip.io
    ```

    El `*` **no hace que el nombre exista ni sustituye a DNS**. El cliente debe resolver primero el nombre a una dirección IP y llegar hasta Nginx.

    Una vez recibida la petición, Nginx compara la cabecera HTTP `Host` con sus `server_name`. En este laboratorio, `escaparate.*` permite reconocer los nombres del catálogo aunque cambie la parte que contiene la dirección IP.

    Más adelante utilizaremos de la misma forma:

    ```nginx
    server_name docs.*;
    ```

    para el sitio de documentación.

!!! info "Todavía no estudies `proxy_pass`"
    Copia el bloque `/api/` tal como aparece.

    En esta sesión solo necesitamos que la API siga funcionando detrás de Nginx. En la Actividad 3.2 estudiarás qué hace realmente `proxy_pass`.

### 1.4. Levanta y valida

Desde `practicas/compose/` ejecuta:

```bash
docker compose config --quiet
docker compose up -d
docker compose ps
docker compose exec web nginx -t
```

Debes comprobar que:

- `web` publica `8088`;
- `app` no publica ningún puerto;
- `bd` no publica ningún puerto;
- `nginx -t` termina correctamente.

Prueba el sitio indicando explícitamente la cabecera `Host`:

```bash
curl -I \
  -H "Host: escaparate.127.0.0.1.nip.io" \
  http://127.0.0.1:8088/
```

Y comprueba la parte dinámica:

```bash
curl -fsS \
  -H "Host: escaparate.127.0.0.1.nip.io" \
  http://127.0.0.1:8088/api/salud/listo
```

Abre también:

```text
http://escaparate.127.0.0.1.nip.io:8088/
```

**Evidencia 1:** `docker compose ps` mostrando que solo `web` publica puerto y una comprobación correcta del catálogo a través de Nginx.

!!! question "Reflexiona"
    Las dos peticiones anteriores llegan primero a Nginx.

    ¿Por qué `/` puede resolverse leyendo un fichero mientras `/api/salud/listo` termina ejecutando código de Spring Boot?

## 🌍 Paso 2: Publica dos sitios sobre la misma dirección

Ahora vas a servir dos contenidos distintos desde el mismo Nginx. Los dos nombres llegarán a la misma dirección y al mismo puerto, pero Nginx seleccionará un sitio diferente.

### 2.1. Observa la resolución DNS

Ejecuta:

```bash
dig escaparate.127.0.0.1.nip.io
```

En `actividad-3.1.md` anota únicamente:

| Dato | Resultado |
|---|---|
| Tipo de registro | |
| Dirección IPv4 | |
| TTL | |

No copies toda la salida de `dig`.

Después ejecuta:

```bash
dig docs.127.0.0.1.nip.io
```

Los dos nombres deberían resolver hacia:

```text
127.0.0.1
```

!!! question "Antes de continuar"
    Si ambos nombres llegan a la misma IP y al mismo puerto `8088`, ¿qué información necesitará Nginx para decidir qué contenido debe devolver?

### 2.2. Coloca la documentación

Descomprime `escaparate-docs.zip` dentro de:

```text
practicas/nginx/sitio-docs/
```

Debe existir:

```text
practicas/nginx/sitio-docs/index.html
```

### 2.3. Monta la documentación en Nginx

Añade al servicio `web`:

```yaml
      - ../nginx/sitio-docs/:/srv/www/docs/:ro
```

El bloque de volúmenes de `web` debe incluir ahora tres montajes:

```yaml
volumes:
  - ../nginx/conf.d/:/etc/nginx/conf.d/:ro
  - ../nginx/sitio-escaparate/escaparate/:/srv/www/escaparate/:ro
  - ../nginx/sitio-docs/:/srv/www/docs/:ro
```

### 2.4. Añade el segundo sitio

Añade al final de `sitios.conf`:

```nginx
server {
    listen 80;
    server_name docs.*;

    root /srv/www/docs;
    index index.html;

    location /informes/ {
        autoindex on;
    }
}
```

Compara los dos bloques `server`.

Solo cambian las decisiones que distinguen un sitio de otro:

```text
nombre
raíz de documentos
reglas específicas de rutas
```

### 2.5. Aplica correctamente el cambio

Has modificado **Compose** añadiendo un nuevo montaje. Por tanto, una recarga de Nginx no es suficiente para incorporar ese directorio al contenedor.

Ejecuta:

```bash
docker compose config --quiet
docker compose up -d web
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

!!! info "`up -d` y `reload` no hacen lo mismo"
    `docker compose up -d web` aplica cambios en la definición del contenedor, como nuevos montajes.

    `nginx -s reload` únicamente hace que Nginx vuelva a leer su configuración.

Comprueba:

| URL | Debe mostrar |
|---|---|
| `http://escaparate.127.0.0.1.nip.io:8088/` | catálogo |
| `http://docs.127.0.0.1.nip.io:8088/` | documentación |
| `http://docs.127.0.0.1.nip.io:8088/informes/` | listado de informes |

**Evidencia 2:** catálogo y documentación funcionando mediante los dos nombres diferentes.

!!! question "Reflexiona"
    Los dos nombres resuelven a `127.0.0.1` y llegan al mismo Nginx.

    ¿Qué información de la petición HTTP permite a Nginx seleccionar un bloque `server` diferente?

!!! warning "Si `nip.io` no funciona en la red del centro"
    Puedes añadir temporalmente:

    ```text
    127.0.0.1 escaparate.127.0.0.1.nip.io
    127.0.0.1 docs.127.0.0.1.nip.io
    ```

    a `/etc/hosts`.

    `dig` seguirá consultando DNS y no utilizará `/etc/hosts`. Si la red bloquea `nip.io`, documenta el hecho y realiza la observación DNS sobre otro nombre público que sí pueda resolver.

## 🛡️ Paso 3: Decide qué ocurre con un nombre desconocido

Hasta ahora has declarado dos nombres válidos. Ahora comprobarás qué hace Nginx cuando recibe una petición cuyo `Host` no coincide con ninguno de ellos.

### 3.1. Observa el comportamiento actual

Ejecuta:

```bash
curl -i \
  http://cualquier-cosa.127.0.0.1.nip.io:8088/
```

Anota qué sitio responde.

No cambies todavía la configuración.

!!! question "Predice"
    ¿Crees que es conveniente que un nombre que no has configurado termine mostrando accidentalmente uno de tus sitios?

### 3.2. Declara un servidor por defecto

Añade **al principio** de `sitios.conf`:

```nginx
server {
    listen 80 default_server;
    server_name _;
    return 404;
}
```

Como solo has modificado la configuración de Nginx, ahora no necesitas recrear el contenedor.

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Repite:

```bash
curl -i \
  http://cualquier-cosa.127.0.0.1.nip.io:8088/
```

Comprueba también que los dos sitios válidos continúan funcionando.

En `actividad-3.1.md` explica brevemente:

1. qué ocurría antes de declarar `default_server`;
2. qué ocurre después;
3. por qué es preferible definir explícitamente este comportamiento.

## 📦 Paso 4: Aplica políticas al contenido estático

Nginx está sirviendo HTML, CSS, JavaScript e imágenes directamente. Ahora aplicarás dos políticas propias de esta capa: **compresión** y **caché**.

No necesitas inventar la sintaxis. Debes aplicarla, medir su efecto e interpretar el resultado.

### 4.1. Configura gzip y caché

Dentro del bloque del catálogo añade:

```nginx
gzip on;
gzip_vary on;
gzip_types text/css application/javascript application/json;

location ~* \.(css|js|png|jpg|jpeg|svg|webp)$ {
    expires 7d;
}
```

La configuración se aplica únicamente al sitio del catálogo.

No copies esta política dentro de `/api/`.

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

### 4.2. Comprueba las cabeceras del CSS

Ejecuta:

```bash
curl -I \
  -H "Accept-Encoding: gzip" \
  http://escaparate.127.0.0.1.nip.io:8088/css/app.css
```

Busca:

```text
Content-Encoding: gzip
Cache-Control: ...
```

### 4.3. Mide el efecto de la compresión

Ejecuta:

```bash
curl -s \
  -H "Accept-Encoding: identity" \
  -o /dev/null \
  -w "sin comprimir: %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io:8088/css/app.css

curl -s \
  -H "Accept-Encoding: gzip" \
  -o /dev/null \
  -w "comprimido:   %{size_download} bytes\n" \
  http://escaparate.127.0.0.1.nip.io:8088/css/app.css
```

Completa en `actividad-3.1.md`:

| Comprobación | Resultado |
|---|---|
| CSS sin comprimir | |
| CSS con gzip | |
| `Content-Encoding` | |
| `Cache-Control` | |

### 4.4. Contrasta con una respuesta dinámica

Consulta:

```bash
curl -s -D - -o /dev/null \
  http://escaparate.127.0.0.1.nip.io:8088/api/salud/listo
```

Compara sus cabeceras con las del CSS.

Finalmente repite:

```bash
curl -i \
  http://cualquier-cosa.127.0.0.1.nip.io:8088/
```

**Evidencia 3:** una captura de terminal donde se vean el `404` del nombre desconocido y las mediciones del CSS con y sin gzip.

!!! question "Interpreta"
    Responde brevemente:

    1. ¿Por qué un CSS es buen candidato para compresión?
    2. ¿Por qué puede tener sentido almacenarlo varios días en caché?
    3. ¿Por qué no aplicarías automáticamente la misma política de caché a todas las respuestas de `/api/`?
    4. ¿Por qué gzip suele aportar poco sobre formatos como JPEG o PNG?

## 📝 Paso 5: Documenta e integra

La arquitectura técnica ya está construida. Antes de cerrar, comprueba que el repositorio permite entender qué ha cambiado respecto al Tema 2.

### 5.1. Valida el estado final

Ejecuta:

```bash
docker compose exec web nginx -t
docker compose ps
git status
```

Debe cumplirse:

```text
web
→ publica 8088 en el anfitrión
→ escucha en 80 dentro del contenedor
→ sirve catálogo
→ sirve documentación
→ reenvía /api/ hacia app

app
→ sin puerto publicado

bd
→ sin puerto publicado
```

### 5.2. Actualiza el README

No repitas toda la puesta en marcha de Compose.

Añade únicamente:

```text
Catálogo:
http://escaparate.127.0.0.1.nip.io:8088/

Documentación:
http://docs.127.0.0.1.nip.io:8088/

Informes:
http://docs.127.0.0.1.nip.io:8088/informes/

Readiness:
http://escaparate.127.0.0.1.nip.io:8088/api/salud/listo
```

Añade también los comandos para:

```text
validar Nginx
recargar Nginx
```

### 5.3. Revisa la entrega

`actividad-3.1.md` debe contener:

- reflexión sobre la nueva puerta de entrada y los puertos;
- tipo de registro, dirección y TTL observados con `dig`;
- reflexión sobre la cabecera `Host`;
- comportamiento antes y después de `default_server`;
- tabla de gzip y caché;
- respuestas de interpretación del paso 4;
- las tres evidencias;
- cualquier incidencia relevante y cómo la resolviste.

No copies configuraciones completas que ya están versionadas.

### 5.4. Integra la rama

Revisa primero:

```bash
git status
```

Después registra los cambios:

```bash
git add .
git commit -m "Añade Nginx con dos sitios, gzip y caché"
git push -u origin sesion-06
```

Abre:

```text
sesion-06 → main
```

Fusiona mediante **Create a merge commit** cuando las comprobaciones sean correctas.

Finalmente:

```bash
git switch main
git pull --ff-only
```

## 🔧 Si algo falla

Utiliza los síntomas para localizar el problema antes de cambiar varias cosas a la vez.

| Síntoma | Causa probable | Qué comprobar |
|---|---|---|
| `docker compose up` indica que `0.0.0.0:80` está ocupado | Se ha utilizado `80:80` en el aula | Usa `8088:80`; no detengas Apache |
| El catálogo carga pero no aparecen productos | `/api/` no llega a `app` | `docker compose ps`, `docker compose logs app` y bloque `/api/` |
| `/api/salud/listo` devuelve `404` | Se ha modificado `proxy_pass` | Debe ser exactamente `proxy_pass http://app:8080;` |
| `/` devuelve `403 Forbidden` | Nginx no encuentra `index.html` en su `root` | Revisa el montaje de `sitio-escaparate/escaparate/` |
| La documentación devuelve `404` | El nuevo montaje no está en el contenedor | Ejecuta `docker compose up -d web` |
| `/informes/` devuelve `403` | Falta `autoindex on` o está en otra ruta | Revisa `location /informes/` |
| Aparece un listado en otras carpetas | `autoindex on` se ha declarado demasiado arriba | Déjalo únicamente en `/informes/` |
| Un nombre desconocido muestra catálogo o documentación | Falta el `default_server` o no se ha recargado | `nginx -t` y `nginx -s reload` |
| `nginx -t` falla | Error de sintaxis | Lee la línea exacta indicada antes de modificar otra cosa |
| El navegador no resuelve `*.nip.io` | La red del centro bloquea la resolución | Utiliza temporalmente `/etc/hosts` |
| No aparece `Content-Encoding: gzip` | El cliente no solicita gzip o la configuración no está activa | Revisa `Accept-Encoding`, `gzip on` y recarga |
| Has cambiado un montaje pero Nginx no ve el fichero | Solo has hecho `reload` | Un montaje exige `docker compose up -d web` |

!!! tip "Regla de trabajo"
    - Si cambia `sitios.conf`: `nginx -t` → `nginx -s reload`.
    - Si cambia `compose.yaml`: `docker compose config --quiet` → `docker compose up -d`.
    - No cambies varias cosas a la vez cuando estés diagnosticando.

## ✅ Qué se entrega

Antes de terminar, comprueba:

- [ ] `practicas/compose/compose.yaml` con `web`, `app` y `bd`;
- [ ] `web` publicado como `8088:80` en los equipos del aula;
- [ ] `app` y `bd` sin puertos publicados;
- [ ] `practicas/nginx/conf.d/sitios.conf`;
- [ ] frontend estático versionado;
- [ ] documentación versionada;
- [ ] dos hosts virtuales funcionando;
- [ ] `default_server` para nombres desconocidos;
- [ ] gzip y caché comprobados sobre `/css/app.css`;
- [ ] README con las nuevas URL y los comandos de Nginx;
- [ ] `actividad-3.1.md` con reflexiones, mediciones y tres evidencias;
- [ ] Pull Request `sesion-06 → main` fusionada.

!!! info "Dónde queda la entrega"
    La configuración técnica y las evidencias quedan versionadas en el repositorio.

    `.env` y `secrets/bd_clave.txt` continúan siendo archivos locales y no deben añadirse a Git.

## 🧠 Cierre

Nginx se ha convertido en la **única puerta de entrada del despliegue**.

Has comprobado que una misma instancia puede recibir peticiones para nombres diferentes y seleccionar el sitio correcto a partir de la petición HTTP:

```text
nombre
  ↓
DNS
  ↓
dirección IP
  ↓
Nginx
  ↓
cabecera Host
  ↓
server_name
  ↓
sitio correcto
```

También has aplicado decisiones propias de un servidor web sobre el contenido estático: compresión, caché y comportamiento ante nombres desconocidos.

Spring Boot no ha desaparecido. Su Tomcat embebido continúa ejecutando la lógica dinámica detrás de Nginx.

En la siguiente actividad abrirás la caja negra de `/api/`: sustituirás una única aplicación por varias réplicas y estudiarás cómo Nginx reparte el tráfico entre ellas.
