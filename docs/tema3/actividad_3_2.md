# 🧪 Actividad 3.2: Una puerta, tres copias

## 🌐 Contexto

En la Actividad 3.1 Nginx se convirtió en la única puerta de entrada del despliegue:

```text
                         ┌── frontend
navegador ──► web:Nginx ├── documentación
                         │
                         └── /api/ ──► app ──► bd
```

El bloque `/api/` se utilizó como una caja negra para mantener la aplicación funcionando mientras trabajabas con servidores web, nombres y contenido estático.

En esta actividad vas a abrir esa caja negra y modificar la arquitectura:

```text
                              ┌── app-1 ──┐
cliente ──► web:Nginx ──► /api ├── app-2 ──┼──► bd
                              └── app-3 ──┘
```

Las tres copias ejecutarán la misma imagen de Escaparate. Nginx repartirá las peticiones entre ellas y comprobarás qué ocurre cuando una deja de responder.

Después aparecerá un segundo problema: **replicar procesos no replica automáticamente su estado local**.

!!! abstract "Cómo vas a trabajar"
    **Replicar → repartir → provocar un fallo → observar → formular una hipótesis → compartir estado → comprobar**

    La configuración específica de Escaparate y las directivas de Nginx que no necesitas memorizar se proporcionan explícitamente.

    Tu trabajo será aplicarlas correctamente, comprobar el comportamiento del sistema, diagnosticar los problemas y explicar qué está ocurriendo.

!!! note "Continúa en el mismo entorno que en la 3.1"
    Si hiciste la Actividad 3.1 en el equipo del aula, continúa en local.

    Si ya trasladaste el proyecto a EC2 por el conflicto con el puerto 80, continúa en **la misma instancia EC2**. No necesitas volver al equipo local ni desplegar otra vez desde cero.

    La arquitectura interna de Compose es la misma en ambos casos.

## 🎯 Qué vas a practicar

Al terminar la actividad deberías ser capaz de:

- sustituir una única aplicación por varias copias equivalentes;
- interpretar un `upstream` de Nginx;
- comprobar que las peticiones se reparten entre varias réplicas;
- reenviar información sobre la petición original mediante cabeceras;
- comprobar que el servicio continúa respondiendo cuando una réplica falla;
- distinguir el `healthcheck` de Compose de la detección de fallos del proxy;
- diagnosticar un problema provocado por estado almacenado localmente;
- compartir ficheros entre varias réplicas ejecutadas en un mismo host;
- explicar por qué un volumen Docker no resolvería el mismo problema entre hosts diferentes.

## 🧰 Requisitos previos

Debes haber terminado y fusionado la Actividad 3.1.

Tu repositorio debe contener:

```text
practicas/
├── compose/
│   └── compose.yaml
└── nginx/
    ├── conf.d/
    │   └── sitios.conf
    ├── sitio-escaparate/
    └── sitio-docs/
```

Y debes conservar:

- `.env`;
- `secrets/bd_clave.txt`;
- el volumen de PostgreSQL;
- el `healthcheck` de `bd`;
- las imágenes públicas:

```text
ghcr.io/<usuario>/escaparate:sesion-04
ghcr.io/<usuario>/escaparate-db:1.0.0
```

Prepara la rama:

```bash
git switch main
git pull --ff-only
git switch -c sesion-07
```

Crea:

```text
entregas/
└── tema3/
    └── actividad-3.2/
        ├── actividad-3.2.md
        └── img/
```

!!! info "Evidencias"
    Solo se piden **tres evidencias**.

    Los ficheros técnicos permanecen en `practicas/`. En `entregas/` documentarás resultados, hipótesis, reflexiones e incidencias relevantes.

### 0.1. Define la URL que vas a utilizar

Para que los mismos comandos sirvan en local y en EC2, define una variable `URL`.

Si trabajas en el **equipo del aula**:

```bash
URL="http://escaparate.127.0.0.1.nip.io:8088"
```

Si trabajas en **EC2**:

```bash
read -r -p 'IPv4 pública de EC2: ' IP_PUBLICA
URL="http://escaparate.${IP_PUBLICA}.nip.io"
```

Comprueba:

```bash
printf '%s\n' "$URL"
```

A partir de ahora utilizaremos `$URL` en las comprobaciones HTTP.

## 🧩 Paso 1: Sustituye una aplicación por tres copias

Hasta ahora Compose tenía un único servicio `app`.

Lo sustituirás por:

```text
app-1
app-2
app-3
```

Las tres copias utilizarán la misma imagen y la misma base de datos, pero cada una tendrá un identificador distinto.

### 1.1. Observa cómo se configura Escaparate

Antes de añadir nuevas variables al `compose.yaml`, observa cómo la propia aplicación permite recibir configuración desde el exterior.

Abre:

```text
escaparate/src/main/resources/application.yml
```

No necesitas modificar este fichero. El objetivo es reconocer un patrón habitual en aplicaciones Spring Boot: una propiedad de la aplicación puede tomar su valor de una **variable de entorno**.

Busca en el fichero las referencias a:

```text
APP_STORAGE_TYPE
APP_STORAGE_PATH
APP_INSTANCE_NAME
```

Encontrarás expresiones con esta forma:

```yaml
propiedad:
  subpropiedad: ${VARIABLE_ENTORNO:valor-por-defecto}
```

La sintaxis:

```text
${VARIABLE_ENTORNO:valor-por-defecto}
```

significa:

```text
si existe VARIABLE_ENTORNO
        ↓
usar su valor

si no existe
        ↓
usar el valor por defecto
```

En Escaparate utilizaremos estas variables:

| Variable de entorno | Para qué la utilizaremos |
|---|---|
| `APP_STORAGE_TYPE` | seleccionar el mecanismo de almacenamiento de imágenes |
| `APP_STORAGE_PATH` | indicar dónde guardar las imágenes cuando usamos filesystem |
| `APP_INSTANCE_NAME` | identificar qué réplica ha respondido |

!!! info "Configuración externa"
    Estas variables **no son parámetros genéricos de Spring Boot** ni aparecen de forma arbitraria en Compose. Son parte del contrato de configuración de Escaparate.

    La misma imagen puede ejecutarse varias veces con valores distintos sin modificar ni recompilar el código:

    ```text
    misma imagen de Escaparate
            │
            ├── APP_INSTANCE_NAME=app-1
            ├── APP_INSTANCE_NAME=app-2
            └── APP_INSTANCE_NAME=app-3
    ```

    Docker Compose será el encargado de proporcionar esos valores a cada contenedor.

!!! question "Comprueba que entiendes la relación"
    Elige una de las tres variables y localízala en `application.yml`.

    Explica brevemente qué valor utilizaría la aplicación si la variable de entorno no estuviera definida y qué ocurrirá cuando Compose sí la proporcione.

No debes deducir ni memorizar los nombres de estas variables: la aplicación ya define qué configuración acepta y el despliegue se limita a proporcionarle valores adecuados.

### 1.2. Sustituye `app` por las tres réplicas

En `practicas/compose/compose.yaml`, elimina el antiguo servicio `app` y utiliza estos tres servicios:

```yaml
  app-1:
    image: ${IMAGEN_APP:?define IMAGEN_APP en .env}
    environment:
      DB_HOST: bd
      DB_PORT: "5432"
      DB_NAME: ${BD_NOMBRE:?define BD_NOMBRE en .env}
      DB_USER: ${BD_USUARIO:?define BD_USUARIO en .env}
      SPRING_CONFIG_IMPORT: "configtree:/run/secrets/"
      APP_STORAGE_TYPE: filesystem
      APP_STORAGE_PATH: /data/uploads
      APP_INSTANCE_NAME: app-1
    secrets:
      - source: bd_clave
        target: spring.datasource.password
    depends_on:
      bd:
        condition: service_healthy

  app-2:
    image: ${IMAGEN_APP:?define IMAGEN_APP en .env}
    environment:
      DB_HOST: bd
      DB_PORT: "5432"
      DB_NAME: ${BD_NOMBRE:?define BD_NOMBRE en .env}
      DB_USER: ${BD_USUARIO:?define BD_USUARIO en .env}
      SPRING_CONFIG_IMPORT: "configtree:/run/secrets/"
      APP_STORAGE_TYPE: filesystem
      APP_STORAGE_PATH: /data/uploads
      APP_INSTANCE_NAME: app-2
    secrets:
      - source: bd_clave
        target: spring.datasource.password
    depends_on:
      bd:
        condition: service_healthy

  app-3:
    image: ${IMAGEN_APP:?define IMAGEN_APP en .env}
    environment:
      DB_HOST: bd
      DB_PORT: "5432"
      DB_NAME: ${BD_NOMBRE:?define BD_NOMBRE en .env}
      DB_USER: ${BD_USUARIO:?define BD_USUARIO en .env}
      SPRING_CONFIG_IMPORT: "configtree:/run/secrets/"
      APP_STORAGE_TYPE: filesystem
      APP_STORAGE_PATH: /data/uploads
      APP_INSTANCE_NAME: app-3
    secrets:
      - source: bd_clave
        target: spring.datasource.password
    depends_on:
      bd:
        condition: service_healthy
```

Por ahora **no añadas ningún volumen a `/data/uploads`**.

Queremos comprobar primero qué ocurre cuando cada contenedor mantiene su propio filesystem.

!!! info "Qué comparten y qué no"
    Las tres copias:

    - usan la misma imagen;
    - se conectan a la misma PostgreSQL;
    - reciben el mismo secreto de base de datos;
    - ejecutan el mismo código.

    Pero todavía tienen **filesystems independientes**.

### 1.3. Actualiza `web`

El servicio `web` ya no depende de `app`.

Sustituye su dependencia por:

```yaml
depends_on:
  - app-1
  - app-2
  - app-3
```

Conserva los `ports` que correspondan a tu entorno:

```text
aula → 8088:80
EC2  → 80:80
```

No cambies `bd`, `datos-bd` ni la definición global del secreto.

### 1.4. Valida Compose antes de continuar

Desde `practicas/compose/` ejecuta:

```bash
docker compose config --quiet
```

Si el comando no devuelve errores, todavía **no levantes el conjunto**. Antes debes adaptar Nginx.

## 🔀 Paso 2: Convierte `/api/` en un proxy balanceado

En la actividad anterior Nginx enviaba `/api/` a un único destino:

```text
/api/ → app:8080
```

Ahora tendrá un grupo de destinos:

```text
              ┌── app-1:8080
/api/ → Nginx ├── app-2:8080
              └── app-3:8080
```

### 2.1. Declara el grupo de aplicaciones

Abre `practicas/nginx/conf.d/sitios.conf` y añade **fuera de todos los bloques `server`**:

```nginx
upstream backend_pool {
    zone backend_pool 64k;
    resolver 127.0.0.11 valid=5s ipv6=off;

    server app-1:8080 resolve;
    server app-2:8080 resolve;
    server app-3:8080 resolve;
}
```

Interpreta el bloque así:

| Directiva | Idea que debes conservar |
|---|---|
| `upstream backend_pool` | da un nombre al grupo de réplicas |
| `server` | añade una réplica al grupo |
| `resolver 127.0.0.11` | utiliza el DNS interno de Docker |
| `resolve` | permite volver a localizar un nombre si cambia su IP |
| `zone` | permite mantener el estado actualizado del grupo |

!!! info "No memorices esta sintaxis"
    Los nombres `app-1`, `app-2` y `app-3` son estables dentro de Compose, pero las IP internas de los contenedores pueden cambiar al recrearlos.

    En este laboratorio queremos que Nginx siga los **nombres de servicio**, no una IP antigua.

### 2.2. Sustituye el bloque `/api/`

En el bloque del catálogo, sustituye:

```nginx
location /api/ {
    proxy_pass http://app:8080;
}
```

por:

```nginx
location /api/ {
    proxy_pass http://backend_pool;

    proxy_connect_timeout 2s;
    proxy_next_upstream error timeout;
    proxy_next_upstream_tries 3;

    proxy_set_header Host              $host;
    proxy_set_header X-Forwarded-For   $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Lee primero el recorrido:

```text
cliente
  │
  ▼
Nginx
  │
  │ /api/...
  ▼
backend_pool
  ├── app-1
  ├── app-2
  └── app-3
```

Las tres líneas de timeout y reintento permiten que una réplica que no responde no bloquee demasiado tiempo una petición.

Las cabeceras `X-Forwarded-*` conservan información de la petición original porque el backend ya no recibe la conexión directamente desde el cliente: la recibe desde Nginx.

!!! warning "La barra final importa"
    Utiliza exactamente:

    ```nginx
    proxy_pass http://backend_pool;
    ```

    sin `/` después del nombre.

    Los endpoints reales de Escaparate empiezan por `/api/`. En esta configuración queremos que el backend siga recibiendo esa ruta completa.

### 2.3. Levanta el nuevo despliegue

Desde `practicas/compose/`:

```bash
docker compose config --quiet
docker compose up -d --remove-orphans
docker compose ps
docker compose exec web nginx -t
```

`--remove-orphans` permite eliminar el contenedor correspondiente al antiguo servicio `app`, que ya no existe en el Compose.

### 2.4. Comprueba cada réplica por separado

Antes de probar el balanceador, verifica que las tres aplicaciones están realmente preparadas.

Ejecuta:

```bash
for servicio in app-1 app-2 app-3; do
  preparado=no

  for intento in $(seq 1 60); do
    if docker compose exec -T web \
      wget -q -T 3 -O /dev/null \
      "http://${servicio}:8080/api/salud/listo" 2>/dev/null; then
      preparado=si
      break
    fi

    sleep 2
  done

  echo "$servicio preparado: $preparado"
done
```

Debes obtener:

```text
app-1 preparado: si
app-2 preparado: si
app-3 preparado: si
```

Si alguna indica `no`, detente y consulta:

```bash
docker compose logs --tail=60 <servicio>
```

!!! info "Por qué comprobamos una por una"
    Una respuesta correcta a través de Nginx solo demostraría que **alguna** réplica funciona.

    Antes de estudiar el reparto necesitamos saber que las tres están disponibles.

### 2.5. Observa el reparto

Escaparate dispone del endpoint:

```text
/api/instancia
```

preparado específicamente para este laboratorio. Devuelve el valor de `APP_INSTANCE_NAME` de la réplica que atiende la petición.

Ejecuta:

```bash
for i in $(seq 1 9); do
  curl --max-time 15 -s "$URL/api/instancia"
  echo
done
```

Deberían aparecer los tres identificadores:

```text
app-1
app-2
app-3
```

No importa el orden exacto.

**Evidencia 1:** `docker compose ps` y la salida del bucle donde puedan observarse las tres réplicas.

!!! question "Reflexiona"
    Antes `/api/` tenía un único destino. Ahora utiliza `backend_pool`.

    ¿Qué diferencia práctica has observado entre ambas configuraciones?

## 💥 Paso 3: Provoca la caída de una réplica

Ahora vas a comprobar qué ocurre cuando una de las tres aplicaciones deja de estar disponible.

### 3.1. Detén `app-2`

Ejecuta:

```bash
docker compose stop app-2
docker compose ps
```

Comprueba que `app-2` ya no está ejecutándose.

### 3.2. Comprueba si el servicio sigue disponible

Ejecuta nueve peticiones y muestra únicamente el código HTTP:

```bash
for i in $(seq 1 9); do
  curl --max-time 15 -s -o /dev/null \
    -w "%{http_code} " \
    "$URL/api/instancia"
done
echo
```

Anota el resultado.

### 3.3. Observa qué réplicas responden

Ejecuta:

```bash
for i in $(seq 1 9); do
  curl --max-time 15 -s "$URL/api/instancia"
  echo
done
```

Compara esta salida con la obtenida antes de detener `app-2`.

### 3.4. Busca la incidencia en Nginx

Consulta:

```bash
docker compose logs web --tail=50
```

Busca si Nginx muestra intentos fallidos relacionados con la réplica detenida.

No es necesario que el mensaje tenga exactamente el mismo texto en todos los equipos.

### 3.5. Recupera la réplica

Arranca de nuevo:

```bash
docker compose start app-2
```

Repite la comprobación individual del apartado 2.4.

Después vuelve a ejecutar:

```bash
for i in $(seq 1 9); do
  curl --max-time 15 -s "$URL/api/instancia"
  echo
done
```

**Evidencia 2:** `app-2` detenida y el servicio continuando disponible mediante las otras réplicas.

!!! question "Interpreta"
    Responde en `actividad-3.2.md`:

    1. ¿Cómo descubre Nginx en este laboratorio que una réplica ha dejado de responder?
    2. ¿Qué función tiene `proxy_connect_timeout`?
    3. ¿Qué función tiene `proxy_next_upstream`?
    4. ¿En qué se diferencia este comportamiento del `healthcheck` que utilizaste con PostgreSQL?

## 🧺 Paso 4: Descubre el problema del estado local

Las tres aplicaciones comparten PostgreSQL, pero cada contenedor sigue teniendo su propio filesystem.

Vas a comprobar qué consecuencia tiene esto cuando una petición guarda un fichero.

### 4.1. Comprueba que las tres copias están activas

Ejecuta:

```bash
docker compose start app-1 app-2 app-3
```

y repite, si es necesario, la comprobación del apartado 2.4.

### 4.2. Crea un producto con imagen

Desde el catálogo crea un producto nuevo e incluye una imagen.

Utiliza un nombre que puedas reconocer fácilmente.

### 4.3. Localiza su identificador

Puedes consultar:

```bash
curl -s "$URL/api/productos"
```

Busca el `id` del producto que acabas de crear.

### 4.4. Solicita repetidamente la imagen

Sustituye `<id>` por el identificador real:

```bash
for i in $(seq 1 12); do
  curl --max-time 15 -s -o /dev/null \
    -w "%{http_code} " \
    "$URL/api/productos/<id>/imagen"
done
echo
```

Observa los códigos obtenidos.

### 4.5. Formula una hipótesis antes de inspeccionar los contenedores

**Antes de ejecutar el siguiente comando**, responde en `actividad-3.2.md`:

1. ¿qué información del producto comparten las tres réplicas?;
2. ¿qué dato podría existir solo dentro de una de ellas?;
3. ¿por qué el balanceo haría visible ese problema?

No continúes hasta haber escrito una hipótesis.

### 4.6. Comprueba los filesystems

Ejecuta:

```bash
docker compose exec app-1 sh -c 'ls -la /data/uploads'
docker compose exec app-2 sh -c 'ls -la /data/uploads'
docker compose exec app-3 sh -c 'ls -la /data/uploads'
```

Compara el contenido.

!!! question "Contrasta tu hipótesis"
    Explica qué relación hay entre:

    ```text
    PostgreSQL compartida
    +
    filesystem local de cada contenedor
    +
    balanceo entre réplicas
    ```

    y los códigos HTTP que observaste.

## 💾 Paso 5: Comparte las imágenes entre las réplicas

Ahora vas a modificar el despliegue para que las tres copias vean la misma carpeta de imágenes.

### 5.1. Añade un volumen compartido

Añade a `app-1`:

```yaml
volumes:
  - imagenes-compartidas:/data/uploads
```

Haz exactamente lo mismo en:

```text
app-2
app-3
```

Después, en la sección global `volumes`, conserva `datos-bd` y añade:

```yaml
volumes:
  datos-bd:
  imagenes-compartidas:
```

La idea queda:

```text
                app-1 ─┐
                       │
                app-2 ─┼──► imagenes-compartidas
                       │
                app-3 ─┘
```

### 5.2. Aplica el cambio

Ejecuta:

```bash
docker compose config --quiet
docker compose up -d
docker compose ps
```

Después repite la comprobación de las tres réplicas del apartado 2.4.

!!! note "No reinicies Nginx manualmente"
    Al añadir el volumen, las aplicaciones pueden haberse recreado y recibir nuevas IP internas.

    El bloque `resolver` + `resolve` se ha configurado precisamente para que Nginx pueda seguir localizando `app-1`, `app-2` y `app-3` por sus nombres.

### 5.3. No reutilices la imagen anterior

El fichero creado en el paso 4 pertenecía al filesystem de uno de los contenedores anteriores.

Al montar ahora `/data/uploads` sobre un volumen, ese contenido antiguo **no sirve para demostrar que el problema está resuelto**.

Crea un **segundo producto con una imagen nueva**.

### 5.4. Repite la prueba

Localiza el nuevo identificador y ejecuta:

```bash
for i in $(seq 1 12); do
  curl --max-time 15 -s -o /dev/null \
    -w "%{http_code} " \
    "$URL/api/productos/<id-nuevo>/imagen"
done
echo
```

Compara esta secuencia con la obtenida antes de añadir el volumen.

### 5.5. Comprueba qué ven las tres réplicas

Ejecuta:

```bash
docker compose exec app-1 sh -c 'ls -la /data/uploads'
docker compose exec app-2 sh -c 'ls -la /data/uploads'
docker compose exec app-3 sh -c 'ls -la /data/uploads'
```

Ahora las tres copias deberían observar el mismo fichero nuevo.

**Evidencia 3:** comparación entre la secuencia inestable antes del volumen compartido y la secuencia estable después de añadirlo.

!!! question "Extrae la regla general"
    Responde en `actividad-3.2.md`:

    1. ¿qué condición debe cumplir un dato que necesiten recuperar varias réplicas equivalentes?;
    2. ¿por qué un volumen Docker compartido funciona en esta práctica?;
    3. ¿por qué este mismo volumen no resolvería el problema si `app-1`, `app-2` y `app-3` se ejecutaran en tres máquinas distintas?

## 📝 Paso 6: Documenta e integra

### 6.1. Actualiza el README

No repitas toda la configuración de Nginx ni de Compose.

Añade únicamente lo nuevo:

- existen tres réplicas: `app-1`, `app-2` y `app-3`;
- Nginx reparte `/api/` entre ellas;
- `/api/instancia` permite observar qué réplica responde;
- solo Nginx publica un puerto hacia el anfitrión;
- las tres aplicaciones comparten `/data/uploads` mediante `imagenes-compartidas`.

Incluye un diagrama sencillo:

```text
                     ┌── app-1 ──┐
cliente ──► Nginx ───┼── app-2 ──┼──► PostgreSQL
                     └── app-3 ──┘
                          │
                          ▼
                 volumen compartido
```

### 6.2. Revisa `actividad-3.2.md`

Debe contener:

- reflexión sobre `upstream`;
- respuestas sobre la caída de una réplica;
- hipótesis del problema de estado **escrita antes de inspeccionar `/data/uploads`**;
- contraste de esa hipótesis con los resultados;
- regla general sobre estado compartido;
- explicación del límite del volumen entre hosts diferentes;
- tres evidencias;
- incidencias relevantes y cómo las resolviste.

No copies el contenido completo de `compose.yaml` ni `sitios.conf`: esos ficheros ya están versionados.

### 6.3. Integra la rama

Comprueba:

```bash
git status
```

Registra los cambios:

```bash
git add .
git commit -m "Añade tres réplicas con balanceo y estado compartido"
git push -u origin sesion-07
```

Abre:

```text
sesion-07 → main
```

Fusiona mediante **Create a merge commit** cuando las comprobaciones sean correctas.

Finalmente:

```bash
git switch main
git pull --ff-only
```

## 🔧 Si algo falla

Utiliza primero el síntoma para acotar el problema.

| Síntoma | Causa probable | Qué comprobar |
|---|---|---|
| `nginx -t` indica que `upstream` no está permitido | El bloque está dentro de un `server` | Mueve `upstream backend_pool` fuera de todos los `server` |
| `/api/` devuelve `404` | Se ha añadido `/` al final de `proxy_pass` | Debe ser `proxy_pass http://backend_pool;` |
| `/api/` devuelve `502 Bad Gateway` | Alguna réplica no ha arrancado o Nginx no la localiza | Ejecuta la comprobación individual del 2.4 y consulta logs |
| Una réplica falla al conectar con PostgreSQL | Se ha perdido parte de la configuración heredada de 2.3 | Revisa variables, secreto y `service_healthy` |
| `/api/instancia` muestra siempre el mismo nombre | Los tres `APP_INSTANCE_NAME` no son distintos | Revisa `app-1`, `app-2` y `app-3` |
| Sigue apareciendo el antiguo servicio `app` | Queda un contenedor huérfano | Ejecuta `docker compose up -d --remove-orphans` |
| Al detener `app-2` algunas peticiones fallan | Falta el reintento o el timeout | Revisa el bloque `/api/` proporcionado |
| Después de añadir el volumen la imagen antigua sigue sin funcionar | Se está utilizando el fichero creado antes del cambio | Crea un producto y una imagen nuevos |
| Solo una réplica ve la imagen nueva | El volumen no está montado en las tres | Revisa `volumes` de cada `app-*` |
| Compose indica que el volumen no existe | Falta su declaración global | Añade `imagenes-compartidas:` a `volumes:` |
| Nginx deja de localizar réplicas tras recrearlas | Falta resolución dinámica | Revisa `resolver`, `resolve` y `zone` |
| En local los `curl` fallan por usar puerto 80 | Se ha olvidado el puerto del aula | Comprueba que `URL` contiene `:8088` |

!!! tip "Regla de trabajo"
    Antes de investigar el balanceador, comprueba siempre las tres réplicas individualmente.

    Una respuesta correcta a través de Nginx solo demuestra que funciona la réplica que atendió esa petición.

## ✅ Qué se entrega

Antes de terminar, comprueba:

- [ ] `compose.yaml` con `web`, `bd`, `app-1`, `app-2` y `app-3`;
- [ ] solo `web` publica puerto hacia el anfitrión;
- [ ] las tres réplicas conservan la configuración de PostgreSQL y el secreto;
- [ ] `APP_INSTANCE_NAME` es distinto en cada réplica;
- [ ] `sitios.conf` contiene `upstream backend_pool`;
- [ ] `/api/` utiliza el `upstream`;
- [ ] `/api/instancia` permite observar las tres copias;
- [ ] con `app-2` detenida el servicio continúa respondiendo;
- [ ] la hipótesis sobre el estado local está documentada;
- [ ] `imagenes-compartidas` está montado en `/data/uploads` de las tres réplicas;
- [ ] una imagen nueva responde correctamente independientemente de la réplica;
- [ ] README actualizado;
- [ ] `actividad-3.2.md` contiene reflexiones y tres evidencias;
- [ ] Pull Request `sesion-07 → main` fusionada.

!!! info "Dónde queda la entrega"
    La infraestructura y la documentación permanecen versionadas en el repositorio.

    `.env` y `secrets/bd_clave.txt` continúan siendo archivos locales y no deben añadirse a Git.

## 🧠 Cierre

Has pasado de:

```text
Nginx → una aplicación
```

a:

```text
          ┌── app-1
Nginx ────┼── app-2
          └── app-3
```

y has comprobado dos ideas diferentes.

La primera es de **disponibilidad**: si una réplica deja de responder, Nginx dispone de otras copias a las que enviar tráfico.

La segunda es de **estado**: ejecutar varias copias no hace que sus filesystems sean compartidos automáticamente.

La arquitectura final queda:

```text
                         ┌── app-1 ──┐
cliente ──► web:Nginx ───┼── app-2 ──┼──► PostgreSQL
                         └── app-3 ──┘
                              │
                              ▼
                     volumen compartido
```

En la siguiente actividad llevarás esta misma arquitectura a una máquina accesible desde Internet —si todavía trabajas en local— y protegerás su única puerta pública mediante control de acceso y HTTPS.
