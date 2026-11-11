# Actividad 3.3: Cierra la puerta y echa la llave

## 🌐 Contexto

En la Actividad 3.2 terminaste con una única puerta de entrada y tres réplicas de Escaparate:

```text
                              ┌── app-1 ──┐
cliente ──► web:Nginx ──► /api ├── app-2 ──┼──► bd
                              └── app-3 ──┘
                                   │
                                   ▼
                          volumen compartido
```

Hasta ahora el despliegue puede estar en uno de estos dos estados:

```text
equipo del aula
      o
instancia EC2
```

En esta actividad todos convergeréis en el mismo escenario: **Escaparate publicado desde una instancia EC2 accesible desde Internet**.

Después añadirás dos capas de seguridad diferentes:

```text
control de acceso
      +
protección del transporte con HTTPS
```

Primero protegerás `/informes/` con Basic Authentication y comprobarás por qué Base64 no protege unas credenciales que viajan mediante HTTP. Después prepararás un desafío ACME HTTP-01, obtendrás un certificado público para los dos nombres y harás que Nginx termine TLS.

La aplicación, las tres réplicas y PostgreSQL seguirán utilizando la arquitectura de la sesión anterior.

!!! abstract "Cómo vas a trabajar"
    **Publicar → restringir → observar → validar → cifrar → comprobar**

    La sintaxis delicada de Nginx, Certbot y TLS se proporciona cuando el objetivo no es memorizarla.

    Tu trabajo será aplicarla correctamente, comprobar qué ocurre y explicar qué protección aporta cada mecanismo.

## 🎯 Qué vas a practicar

Al terminar la actividad deberías ser capaz de:

- ejecutar en un host remoto el mismo despliegue que ya funciona con Docker Compose;
- mantener Nginx como única puerta pública;
- restringir una ruta desde Nginx sin modificar Spring Boot;
- explicar qué protege Basic Authentication y qué no protege por sí sola;
- reconocer que Base64 es una codificación reversible;
- explicar qué demuestra un desafío ACME HTTP-01;
- obtener un certificado público para varios nombres;
- terminar TLS en Nginx manteniendo HTTP en la red interna de Docker;
- redirigir tráfico HTTP normal hacia HTTPS sin romper HTTP-01;
- comprobar HSTS, `nosniff` y `Referrer-Policy`;
- mantener credenciales y claves privadas fuera de Git.

## 🧰 Requisitos previos

Necesitas:

- la Actividad 3.2 terminada y fusionada en `main`;
- una instancia Amazon EC2 de AWS Academy Learner Lab con:
  - acceso por SSH;
  - Docker;
  - Docker Compose;
  - TCP/80 permitido;
  - posibilidad de permitir TCP/443;
- las imágenes públicas utilizadas durante el tema;
- acceso a tu repositorio privado de GitHub.

Como la instancia se reutilizará en la siguiente sesión para Elasticsearch y Kibana, es recomendable que disponga de al menos **4 GiB de RAM**.

!!! info "Elastic IP recomendada"
    Si Learner Lab permite utilizar una **Elastic IP**, es recomendable asociarla **antes de emitir el certificado**.

    Con una IPv4 pública temporal puedes completar igualmente la actividad mientras la dirección no cambie.

    Si en una sesión posterior la instancia recibe otra IP:

    ```text
    escaparate.<IP-ANTIGUA>.nip.io
              ↓
    escaparate.<IP-NUEVA>.nip.io
    ```

    cambia también el nombre DNS. El certificado emitido para el nombre antiguo ya no será válido para el nuevo.

    No tendrás que modificar `server_name`, porque te recomendamos utilizar:

    ```nginx
    server_name escaparate.*;
    server_name docs.*;
    ```

    pero sí tendrías que emitir un certificado para los nuevos nombres.

!!! info "Evidencias"
    Solo se piden **tres evidencias**.

    Documenta resultados y reflexiones en:

    ```text
    entregas/tema3/actividad-3.3/actividad-3.3.md
    ```

    No hagas una captura de cada comando.

## ☁️ Paso 1: Lleva el despliegue a EC2

A partir de este paso toda la actividad se ejecutará sobre la instancia EC2.

Si ya realizaste las actividades 3.1 y 3.2 allí, no vuelvas a desplegar desde cero: actualiza el repositorio y comprueba el estado.

### 1.1. Prepara la rama

En el repositorio con el que vas a trabajar:

```bash
git switch main
git pull --ff-only
git switch -c sesion-08
```

Crea:

```text
entregas/
└── tema3/
    └── actividad-3.3/
        ├── actividad-3.3.md
        └── img/
```

### 1.2. Si todavía trabajabas en el equipo del aula

En EC2 clona tu repositorio utilizando tu mecanismo habitual de autenticación:

```bash
git clone <URL-DE-TU-REPOSITORIO>
cd <REPOSITORIO>
```

No escribas un PAT dentro de la URL.

Los ficheros locales y secretos no viajan mediante Git. Si es un clon nuevo, crea:

```bash
cd practicas/compose

cp .env.example .env
cp secrets/bd_clave.example.txt secrets/bd_clave.txt
```

Completa los valores necesarios en ambos ficheros.

!!! warning "No compiles la aplicación en EC2"
    La instancia es el **host de ejecución**.

    No necesitas instalar Java ni Maven ni volver a construir Escaparate:

    ```text
    Git
    → configuración del despliegue

    GHCR
    → imágenes ya construidas

    EC2
    → ejecuta ambas cosas
    ```

### 1.3. Si ya estabas trabajando en EC2

Actualiza el clon existente:

```bash
git switch main
git pull --ff-only
git switch -c sesion-08
```

Conserva tu `.env` y `secrets/bd_clave.txt` locales.

### 1.4. Define los nombres públicos

Consulta la IPv4 pública actual y guárdala:

```bash
read -r -p 'IPv4 pública de EC2: ' IP_PUBLICA
```

Construye los dos nombres que utilizarás durante la sesión:

```bash
ESCAPARATE="escaparate.${IP_PUBLICA}.nip.io"
DOCS="docs.${IP_PUBLICA}.nip.io"

printf 'Catálogo: %s\nDocs:     %s\n' "$ESCAPARATE" "$DOCS"
```

Desde el equipo del aula comprueba:

```bash
dig +short "$ESCAPARATE"
dig +short "$DOCS"
```

Ambos deben resolver hacia la misma IPv4 pública.

### 1.5. Levanta y valida el despliegue

En EC2, desde `practicas/compose/`:

```bash
docker compose pull
docker compose up -d --remove-orphans
docker compose ps
docker compose exec web nginx -t
```

Comprueba desde el equipo del aula:

```bash
curl -fsS "http://${ESCAPARATE}/" > /dev/null \
  && echo "catálogo remoto OK"

curl -fsS "http://${ESCAPARATE}/api/salud/listo"
```

Y verifica el reparto:

```bash
for i in $(seq 1 9); do
  curl -s "http://${ESCAPARATE}/api/instancia"
  echo
done
```

Deben seguir apareciendo:

```text
app-1
app-2
app-3
```

!!! question "Reflexiona"
    La EC2 no ha compilado Escaparate.

    ¿Qué artefacto ejecutan realmente `app-1`, `app-2` y `app-3`, y de dónde lo obtiene Docker?

## 🔐 Paso 2: Protege los informes

La documentación general seguirá siendo pública:

```text
http://docs.<IP>.nip.io/
```

pero:

```text
http://docs.<IP>.nip.io/informes/
```

requerirá credenciales.

### 2.1. Crea una cuenta exclusiva para el laboratorio

Desde la raíz del repositorio en EC2:

```bash
mkdir -p practicas/nginx/auth
```

Genera el hash de una **contraseña inventada exclusivamente para esta práctica**:

```bash
HASH="$(openssl passwd -apr1)"
printf 'alumno:%s\n' "$HASH" > practicas/nginx/auth/.htpasswd
unset HASH
```

Añade a `.gitignore`:

```gitignore
practicas/nginx/auth/.htpasswd
```

!!! danger "No reutilices una contraseña real"
    Durante esta actividad demostrarás que unas credenciales enviadas mediante HTTP pueden recuperarse.

    Utiliza una contraseña creada únicamente para el laboratorio.

### 2.2. Monta el fichero en Nginx

En el servicio `web` de `compose.yaml`, añade dentro de `volumes`:

```yaml
- ../nginx/auth/.htpasswd:/etc/nginx/auth/.htpasswd:ro
```

La ruta existe en el host:

```text
practicas/nginx/auth/.htpasswd
```

y Nginx la verá como:

```text
/etc/nginx/auth/.htpasswd
```

### 2.3. Protege únicamente `/informes/`

En el bloque `server` de `docs.*`, deja el `location` así:

```nginx
location /informes/ {
    autoindex on;

    auth_basic "Zona restringida";
    auth_basic_user_file /etc/nginx/auth/.htpasswd;
}
```

La portada de documentación no lleva estas directivas y continuará siendo pública.

Como has cambiado Compose y Nginx:

```bash
cd practicas/compose

docker compose config --quiet
docker compose up -d --force-recreate web
docker compose exec web nginx -t
docker compose exec web ls -l /etc/nginx/auth/.htpasswd
```

### 2.4. Comprueba tres casos

Ejecuta desde el equipo del aula:

```bash
curl -s -o /dev/null \
  -w "sin credenciales: %{http_code}\n" \
  "http://${DOCS}/informes/"

curl -s -o /dev/null \
  -w "credenciales incorrectas: %{http_code}\n" \
  -u alumno:incorrecta \
  "http://${DOCS}/informes/"

curl -s -o /dev/null \
  -w "credenciales correctas: %{http_code}\n" \
  -u alumno:<clave-laboratorio> \
  "http://${DOCS}/informes/"
```

Debes obtener:

```text
sin credenciales: 401
credenciales incorrectas: 401
credenciales correctas: 200
```

Comprueba también:

```bash
curl -s -o /dev/null \
  -w "portada docs: %{http_code}\n" \
  "http://${DOCS}/"
```

Debe continuar respondiendo `200`.

## 👀 Paso 3: Comprueba qué envía Basic Authentication

Basic Authentication controla quién puede acceder a la ruta, pero todavía estás utilizando HTTP.

Ahora comprobarás por qué eso no es suficiente.

### 3.1. Observa la cabecera que genera el cliente

Desde el equipo del aula ejecuta:

```bash
curl -v \
  -u alumno:<clave-laboratorio> \
  "http://${DOCS}/informes/" \
  -o /dev/null
```

En la salida busca una línea semejante a:

```text
> Authorization: Basic ...
```

Copia únicamente el valor situado después de `Basic`.

### 3.2. Descodifica el valor

Ejecuta:

```bash
echo '<valor-base64>' | base64 -d
echo
```

Debes recuperar:

```text
alumno:<clave-laboratorio>
```

Base64 no ha cifrado las credenciales: únicamente ha cambiado su representación.

!!! question "Reflexiona"
    Responde en `actividad-3.3.md`:

    1. ¿qué protege Basic Authentication?
    2. ¿qué no protege mientras la petición viaje mediante HTTP?
    3. ¿por qué HTTPS cambia esta situación aunque Basic Authentication siga utilizando Base64?

!!! tip "Ampliación opcional"
    Si quieres observar la cabecera en tránsito en lugar de verla únicamente desde el cliente, puedes capturar **tu propio tráfico de laboratorio** con `tcpdump`.

    No es necesario para completar la actividad principal.

**Evidencia 1:** los resultados `401 / 401 / 200` y la demostración de que el valor Base64 puede descodificarse. Oculta la contraseña en la captura.

## 🔏 Paso 4: Prepara ACME HTTP-01

Para obtener un certificado público, una autoridad de certificación debe comprobar que controlas los nombres que solicitas.

Utilizaremos HTTP-01:

```text
autoridad de certificación
          │
          │ GET http://nombre/.well-known/acme-challenge/token
          ▼
       Nginx :80
          │
          ▼
 /var/www/certbot
          ▲
          │
       Certbot
```

Nginx servirá el fichero y Certbot será quien solicite el certificado.

### 4.1. Añade Certbot como herramienta puntual

Desde `practicas/compose/`, añade dentro de `services:`:

```yaml
  certbot:
    image: certbot/certbot
    profiles: ["tools"]
    volumes:
      - ../nginx/certbot/www:/var/www/certbot
      - ../nginx/certbot/conf:/etc/letsencrypt
```

`profiles: ["tools"]` evita que Certbot permanezca ejecutándose junto al resto del despliegue. Solo se creará cuando invoques expresamente:

```bash
docker compose run --rm certbot ...
```

### 4.2. Prepara los directorios compartidos

Desde `practicas/compose/`:

```bash
mkdir -p ../nginx/certbot/www/.well-known/acme-challenge
mkdir -p ../nginx/certbot/conf
```

Añade también al servicio `web`:

```yaml
- ../nginx/certbot/www:/var/www/certbot:ro
- ../nginx/certbot/conf:/etc/letsencrypt:ro
```

Y excluye el contenido generado de Git:

```gitignore
practicas/nginx/certbot/conf/
practicas/nginx/certbot/www/
```

Valida:

```bash
docker compose config --quiet
docker compose run --rm certbot --version
```

### 4.3. Haz que ambos sitios sirvan el challenge

En los bloques HTTP de `escaparate.*` y `docs.*`, añade:

```nginx
location ^~ /.well-known/acme-challenge/ {
    root /var/www/certbot;
    default_type text/plain;
}
```

Como has añadido montajes nuevos:

```bash
docker compose up -d --force-recreate web
docker compose exec web nginx -t
```

### 4.4. Prueba el recorrido sin contactar todavía con la CA

Crea un fichero:

```bash
echo 'acme-ok' > \
  ../nginx/certbot/www/.well-known/acme-challenge/prueba
```

Comprueba desde el contenedor:

```bash
docker compose exec web \
  cat /var/www/certbot/.well-known/acme-challenge/prueba
```

Y desde el equipo del aula:

```bash
curl -fsS "http://${ESCAPARATE}/.well-known/acme-challenge/prueba"
echo

curl -fsS "http://${DOCS}/.well-known/acme-challenge/prueba"
echo
```

Las dos respuestas deben ser:

```text
acme-ok
```

!!! warning "No continúes si esta prueba falla"
    Certbot y la autoridad de certificación utilizarán exactamente ese recorrido.

    Si alguno de los dos nombres devuelve `404`, corrige primero Nginx y los montajes.

### 4.5. Interpreta HTTP-01

!!! question "Antes de pedir el certificado"
    Los dos nombres resuelven hacia tu EC2 y una petición pública al token llega hasta el fichero que ha preparado Certbot.

    ¿Qué está demostrando esto ante la autoridad de certificación?

## 📜 Paso 5: Valida en staging y emite el certificado

Utilizarás **un único certificado para los dos nombres**.

Antes de emitirlo, comprueba que la IPv4 pública no ha cambiado:

```bash
printf '%s\n' "$IP_PUBLICA"
dig +short "$ESCAPARATE"
dig +short "$DOCS"
```

### 5.1. Prueba primero contra staging

Desde `practicas/compose/`:

```bash
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  --dry-run \
  --agree-tos --non-interactive \
  --email <correo> \
  -d "$ESCAPARATE" \
  -d "$DOCS"
```

!!! warning "Staging antes que producción"
    Si el `dry-run` falla, no pruebes comandos de producción al azar.

    Diagnostica primero:

    ```text
    DNS
      ↓
    puerto 80
      ↓
    Nginx
      ↓
    /.well-known/acme-challenge/
      ↓
    webroot compartido
    ```

### 5.2. Emite una sola vez en producción

Cuando el `dry-run` sea correcto:

```bash
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  --cert-name daw-sesion08 \
  --agree-tos --non-interactive \
  --email <correo> \
  -d "$ESCAPARATE" \
  -d "$DOCS"
```

Si la CA indica un límite de emisión, **no repitas el comando**. Conserva la evidencia del `dry-run` y comunica la incidencia.

### 5.3. Comprueba qué se ha generado

Ejecuta:

```bash
docker compose run --rm certbot certificates
```

Debes identificar:

```text
Certificate Name: daw-sesion08
```

y los dos nombres utilizados.

Las rutas que utilizará Nginx serán:

```text
/etc/letsencrypt/live/daw-sesion08/fullchain.pem
/etc/letsencrypt/live/daw-sesion08/privkey.pem
```

!!! info "Certificado y clave privada"
    `fullchain.pem` contiene la información pública necesaria para presentar el certificado.

    `privkey.pem` contiene la **clave privada** y no debe publicarse ni versionarse.

!!! question "Reflexiona"
    La autoridad no te ha pedido un documento de identidad.

    ¿Qué control has demostrado mediante HTTP-01? ¿Qué habría ocurrido si los nombres resolvieran hacia otra máquina?

**Evidencia 2:** `dry-run` correcto y `certbot certificates` mostrando `daw-sesion08` y los dos nombres. No es necesario capturar el contenido de los certificados.

## 🔒 Paso 6: Termina TLS en Nginx

Ahora Nginx recibirá las conexiones HTTPS públicas en el puerto 443.

Las aplicaciones seguirán hablando HTTP dentro de Docker:

```text
cliente
  │
  │ HTTPS :443
  ▼
Nginx
  │
  │ HTTP interno
  ├── app-1:8080
  ├── app-2:8080
  └── app-3:8080
```

### 6.1. Abre el puerto 443

En el grupo de seguridad de EC2 permite tráfico entrante:

```text
TCP/443
```

Mantén también:

```text
TCP/80
```

HTTP-01 seguirá necesitando el puerto 80.

### 6.2. Publica 443 desde `web`

En `compose.yaml`, el servicio `web` debe publicar:

```yaml
ports:
  - "80:80"
  - "443:443"
```

No añadas puertos a:

```text
app-1
app-2
app-3
bd
```

### 6.3. Reorganiza `sitios.conf`

Conserva el `upstream backend_pool` de la Actividad 3.2 y el `default_server` que devuelve `404`.

La configuración final de los dos sitios debe seguir este patrón.

#### Catálogo por HTTP

```nginx
server {
    listen 80;
    server_name escaparate.*;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        default_type text/plain;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}
```

#### Catálogo por HTTPS

```nginx
server {
    listen 443 ssl;
    server_name escaparate.*;

    ssl_certificate /etc/letsencrypt/live/daw-sesion08/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/daw-sesion08/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=300" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    root /srv/www/escaparate;
    index index.html;

    gzip on;
    gzip_vary on;
    gzip_types text/css application/javascript application/json;

    location /api/ {
        proxy_pass http://backend_pool;

        proxy_connect_timeout 2s;
        proxy_next_upstream error timeout;
        proxy_next_upstream_tries 3;

        proxy_set_header Host              $host;
        proxy_set_header X-Forwarded-For   $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(css|js|png|jpg|jpeg|svg|webp)$ {
        expires 7d;
    }
}
```

#### Documentación por HTTP

```nginx
server {
    listen 80;
    server_name docs.*;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/certbot;
        default_type text/plain;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}
```

#### Documentación por HTTPS

```nginx
server {
    listen 443 ssl;
    server_name docs.*;

    ssl_certificate /etc/letsencrypt/live/daw-sesion08/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/daw-sesion08/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=300" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    root /srv/www/docs;
    index index.html;

    location /informes/ {
        autoindex on;

        auth_basic "Zona restringida";
        auth_basic_user_file /etc/nginx/auth/.htpasswd;
    }
}
```

!!! info "Qué ha cambiado"
    La configuración funcional no ha desaparecido: se ha trasladado a los bloques HTTPS.

    Ahora:

    ```text
    :80
    → challenge ACME o redirección

    :443
    → contenido real del sitio
    ```

    El `upstream` no usa HTTPS porque las réplicas están dentro de la red Docker y Nginx termina TLS en la entrada.

### 6.4. Valida y aplica

Primero comprueba que Nginx puede ver los certificados:

```bash
docker compose exec web \
  ls -l /etc/letsencrypt/live/daw-sesion08/
```

Valida la configuración:

```bash
docker compose exec web nginx -t
```

Después aplica el cambio de publicación de puertos:

```bash
docker compose up -d --force-recreate web
docker compose ps
docker compose exec web nginx -t
```

En `docker compose ps` solo `web` debe publicar puertos hacia el host:

```text
80
443
```

## ✅ Paso 7: Comprueba el resultado completo

No des por terminada la configuración porque el navegador muestre un candado. Comprueba cada propiedad por separado.

### 7.1. HTTP normal redirige

```bash
curl -sI "http://${ESCAPARATE}/" | head -n 3
curl -sI "http://${DOCS}/" | head -n 3
```

Debes reconocer:

```text
301
Location: https://...
```

### 7.2. HTTP-01 sigue disponible por el puerto 80

```bash
curl -fsS \
  "http://${ESCAPARATE}/.well-known/acme-challenge/prueba"
echo

curl -fsS \
  "http://${DOCS}/.well-known/acme-challenge/prueba"
echo
```

Ambos deben seguir devolviendo:

```text
acme-ok
```

### 7.3. HTTPS funciona sin desactivar la validación

```bash
curl -fsS "https://${ESCAPARATE}/" > /dev/null \
  && echo "Escaparate HTTPS OK"

curl -fsS "https://${DOCS}/" > /dev/null \
  && echo "Docs HTTPS OK"
```

No utilices:

```text
-k
```

El objetivo es comprobar que el certificado es válido y de confianza.

### 7.4. El backend y las tres réplicas siguen funcionando

```bash
curl -fsS "https://${ESCAPARATE}/api/salud/listo"
```

Después:

```bash
for i in $(seq 1 9); do
  curl -s "https://${ESCAPARATE}/api/instancia"
  echo
done
```

Deben seguir apareciendo las tres réplicas.

### 7.5. Basic Authentication funciona ahora sobre HTTPS

```bash
curl -s -o /dev/null \
  -w "sin credenciales: %{http_code}\n" \
  "https://${DOCS}/informes/"

curl -s -o /dev/null \
  -w "con credenciales: %{http_code}\n" \
  -u alumno:<clave-laboratorio> \
  "https://${DOCS}/informes/"
```

Debes obtener:

```text
sin credenciales: 401
con credenciales: 200
```

### 7.6. Comprueba los dos nombres del certificado

```bash
echo | openssl s_client \
  -connect "${ESCAPARATE}:443" \
  -servername "$ESCAPARATE" 2>/dev/null \
  | openssl x509 \
      -noout -issuer -subject -dates -ext subjectAltName
```

En `Subject Alternative Name` deben aparecer los dos nombres.

### 7.7. Comprueba las cabeceras

```bash
curl -sI "https://${ESCAPARATE}/" \
  | grep -iE 'strict-transport|x-content-type|referrer'
```

Debes reconocer:

```text
Strict-Transport-Security: max-age=300
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
```

!!! question "Interpreta"
    Responde brevemente:

    1. ¿por qué el puerto 80 continúa abierto si queremos utilizar HTTPS?
    2. ¿dónde termina la conexión TLS?
    3. ¿por qué `app-1`, `app-2` y `app-3` no necesitan certificados?
    4. ¿qué aporta HSTS que no aporte por sí sola una redirección `301`?
    5. ¿por qué hemos utilizado un `max-age` corto para HSTS durante el laboratorio?

**Evidencia 3:** una comprobación final donde puedan reconocerse la redirección HTTP→HTTPS, HTTPS válido, `401/200` de `/informes/` y las cabeceras de seguridad. Puedes agrupar varias salidas en una única captura de terminal.

## 📝 Paso 8: Documenta e integra

La parte técnica está terminada. Cierra la actividad dejando el despliegue reproducible y sin secretos versionados.

### 8.1. Actualiza el README

Añade únicamente la información nueva:

```text
Catálogo:
https://escaparate.<IP>.nip.io/

Documentación:
https://docs.<IP>.nip.io/

Informes:
https://docs.<IP>.nip.io/informes/
```

Documenta también:

- Nginx publica 80 y 443;
- HTTP normal redirige a HTTPS;
- `/informes/` utiliza Basic Authentication;
- Nginx termina TLS;
- `.htpasswd` y el material privado de ACME no se versionan;
- las aplicaciones y PostgreSQL continúan siendo servicios internos.

### 8.2. Revisa los secretos antes de publicar

Desde la raíz del repositorio:

```bash
git status

git check-ignore -v \
  practicas/nginx/auth/.htpasswd

git check-ignore -v \
  practicas/nginx/certbot/conf/
```

Comprueba que no vas a publicar:

```text
.htpasswd
privkey.pem
cuentas de ACME
.env
bd_clave.txt
```

!!! danger "No hagas `git add` a ciegas"
    Antes de registrar cambios relacionados con certificados y autenticación, revisa siempre `git status`.

    La configuración de Nginx y Compose sí debe versionarse. Las credenciales y claves privadas, no.

### 8.3. Revisa `actividad-3.3.md`

Debe contener:

- reflexión sobre el despliegue remoto;
- explicación de Basic Authentication y Base64;
- interpretación de HTTP-01;
- reflexión sobre TLS y las cabeceras de seguridad;
- las tres evidencias;
- incidencias relevantes y cómo las resolviste.

No copies configuraciones completas que ya están versionadas.

### 8.4. Integra la rama

Registra los cambios:

```bash
git add .
git status
```

Comprueba de nuevo qué se añadirá y crea el commit:

```bash
git commit -m "Protege la publicación con autenticación y HTTPS"
git push -u origin sesion-08
```

Abre:

```text
sesion-08 → main
```

Fusiona mediante **Create a merge commit** cuando las comprobaciones sean correctas.

Finalmente:

```bash
git switch main
git pull --ff-only
```

## 🔧 Si algo falla

Diagnostica desde la capa más externa hacia dentro y cambia una cosa cada vez.

| Síntoma | Causa probable | Qué comprobar |
|---|---|---|
| `dig` devuelve otra IP | la IPv4 de EC2 ha cambiado | actualiza `IP_PUBLICA`, `ESCAPARATE` y `DOCS` antes de emitir certificados |
| El sitio no responde por HTTP | puerto 80 cerrado o contenedor `web` caído | grupo de seguridad, `docker compose ps`, logs de `web` |
| `/informes/` devuelve siempre `200` | Basic Auth no está en el `location` correcto | bloque `docs.*` y `/informes/` |
| `/informes/` devuelve `500` | Nginx no puede leer `.htpasswd` | montaje, ruta y `docker compose exec web ls -l ...` |
| El challenge devuelve `404` | ruta o montaje incorrectos | prueba manual `acme-ok` antes de ejecutar Certbot |
| El `dry-run` falla para un nombre | DNS o HTTP-01 no llega correctamente | `dig`, puerto 80 y `curl` del challenge para ambos nombres |
| Certbot alcanza un límite de producción | se han repetido emisiones reales | no insistas; utiliza staging para diagnosticar |
| `nginx -t` no encuentra el certificado | ruta incorrecta o emisión no realizada | `certbot certificates` y `/etc/letsencrypt/live/daw-sesion08/` |
| HTTPS no conecta | 443 no está publicado o permitido | Compose y grupo de seguridad |
| `curl` solo funciona con `-k` | certificado/nombre no coinciden o cadena inválida | nombre solicitado, SAN y certificado configurado |
| HTTP devuelve `404` en vez de `301` | el `default_server` está recibiendo el nombre | revisa `server_name escaparate.*` / `docs.*` |
| El challenge recibe `301` | la redirección está capturando también `/.well-known/` | conserva el `location ^~` específico antes del `location /` |
| Tras recrear `web` desaparece el backend | problema de resolución/recreación de réplicas | `docker compose ps`, comprobación individual y `backend_pool` |
| Git muestra certificados o `.htpasswd` | reglas de exclusión incompletas | corrige `.gitignore` antes de hacer commit |

!!! tip "Orden de diagnóstico"
    Para HTTPS piensa siempre:

    ```text
    DNS
      ↓
    Security Group
      ↓
    puerto publicado por Docker
      ↓
    Nginx
      ↓
    certificado
      ↓
    sitio / proxy
    ```

## 📦 Qué se entrega

Antes de terminar, comprueba:

- [ ] despliegue funcionando en EC2;
- [ ] solo `web` publica puertos hacia el host;
- [ ] `/informes/` protegido mediante Basic Authentication;
- [ ] comprobación de que Base64 es reversible;
- [ ] prueba manual de HTTP-01 correcta para ambos nombres;
- [ ] `dry-run` ACME correcto;
- [ ] certificado público para los dos nombres;
- [ ] HTTP normal redirige a HTTPS;
- [ ] HTTP-01 continúa disponible por HTTP;
- [ ] HTTPS funciona sin `-k`;
- [ ] las tres réplicas siguen detrás del proxy;
- [ ] HSTS, `nosniff` y `Referrer-Policy` comprobadas;
- [ ] `.htpasswd`, claves privadas, `.env` y secreto de BD fuera de Git;
- [ ] README actualizado;
- [ ] `actividad-3.3.md` con tres evidencias y reflexiones;
- [ ] Pull Request `sesion-08 → main` fusionada.

## 🧩 Ampliación opcional: renovación

Los certificados públicos caducan. Si terminas la parte principal, comprueba que el procedimiento de renovación sigue siendo válido:

```bash
cd practicas/compose

docker compose run --rm certbot renew --dry-run
```

La idea que debes reconocer es:

```text
Certbot renueva
      ↓
escribe nuevos ficheros
      ↓
Nginx debe recargarlos
```

La automatización periódica mediante `cron`, `systemd timer` u otra herramienta queda fuera de la parte obligatoria.

!!! question "Reflexiona"
    Si Certbot escribiera un certificado nuevo en disco, ¿por qué podría ser necesario recargar Nginx para empezar a utilizarlo?

## ✅ Cierre

La arquitectura interna de Escaparate no ha cambiado:

```text
                         ┌── app-1 ──┐
cliente ──► web:Nginx ───┼── app-2 ──┼──► PostgreSQL
                         └── app-3 ──┘
                              │
                              ▼
                     volumen compartido
```

Lo que ha cambiado es la responsabilidad de la puerta de entrada.

Nginx ahora:

```text
identifica el sitio
        +
reparte /api/
        +
protege /informes/
        +
termina TLS
```

La aplicación no ha necesitado gestionar certificados ni autenticación para la documentación. La seguridad de publicación se ha incorporado alrededor de ella, en la capa responsable de recibir el tráfico externo.
