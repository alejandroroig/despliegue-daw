# 🧪 Actividad 3.3: Cierra la puerta y echa la llave

## Contexto

En la Actividad 3.2 terminaste con una arquitectura local formada por una única puerta de entrada y tres réplicas:

```text
                              ┌── app-1 ──┐
Navegador ──► web:Nginx ──► /api ├── app-2 ──┼──► bd
                              └── app-3 ──┘
                                   │
                                   ▼
                          volumen compartido
```

Hasta ahora todo el conjunto se ha probado en tu equipo. En esta sesión vas a dar dos pasos nuevos:

```text
1. llevar el mismo despliegue a una máquina accesible desde Internet
2. proteger su única puerta pública
```

La instancia remota será únicamente el **host Docker**. No vas a recompilar la aplicación allí: descargará las imágenes ya construidas y ejecutará la configuración versionada en el repositorio.

Después protegerás `/informes/` con autenticación básica y comprobarás por qué esa autenticación necesita HTTPS. Finalmente obtendrás un certificado público mediante ACME y harás que Nginx termine TLS.

!!! info "Tiempo orientativo"
    La actividad está diseñada para una sesión de aproximadamente **100 minutos** si la instancia EC2 indicada en los requisitos ya está creada y accesible.

    La creación detallada de VPC, subredes o infraestructura cloud no forma parte de esta práctica.

## Qué vas a practicar

- **Desplegar** en un host remoto el mismo conjunto que funciona en local.
- **Mantener** una única puerta pública para el sistema.
- **Restringir** una ruta desde Nginx sin modificar la aplicación.
- **Comprobar** por qué Base64 no protege unas credenciales que viajan por HTTP.
- **Explicar** qué demuestra un desafío ACME HTTP-01.
- **Obtener** un certificado público para dos nombres.
- **Terminar TLS** en Nginx manteniendo HTTP en la red interna de Docker.
- **Redirigir** HTTP a HTTPS.
- **Comprobar** HSTS y otras cabeceras sencillas de seguridad.

## Requisitos previos

- Actividad 3.2 terminada y fusionada en `main`.
- Las imágenes públicas utilizadas durante el tema.
- Una instancia **Amazon EC2 de AWS Academy Learner Lab** ya con:
  - acceso por SSH;
  - Docker y Docker Compose;
  - al menos 4 GiB de RAM si la instancia se reutilizará después para Elasticsearch/Kibana;
  - TCP/80 permitido;
  - posibilidad de permitir TCP/443.
- Una IPv4 pública que vaya a mantenerse durante la sesión.
- Si Learner Lab permite utilizar una **Elastic IP**, puedes asociarla antes de emitir el certificado.

!!! note "La IP pública"
    Una Elastic IP simplifica las siguientes sesiones porque el nombre `nip.io` no cambia al detener y arrancar la instancia. Si el laboratorio no permite utilizarla, trabaja con la IPv4 pública actual y evita detener la instancia mientras realizas esta práctica.

Prepara la rama:

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

---

## Paso 1: Lleva el despliegue a EC2

### 1.1. Haz que los nombres funcionen tanto en local como en remoto

En la sesión 6 los bloques `server` utilizaban nombres locales completos.

Cambia los nombres de los dos sitios para aceptar la parte variable que contendrá la IP:

```nginx
server_name escaparate.*;
```

y:

```nginx
server_name docs.*;
```

El `default_server` que devuelve `404` se mantiene.

La idea es:

```text
escaparate.127.0.0.1.nip.io
escaparate.203.0.113.25.nip.io
                │
                └── ambos encajan en escaparate.*
```

Esto **no sustituye a DNS**. `nip.io` sigue resolviendo cada nombre hacia la IP escrita en él. El comodín solo evita tener que modificar `server_name` cuando cambia esa parte.

Registra el cambio y publica temporalmente la rama para poder obtenerla desde EC2:

```bash
git add .
git commit -m "Preparar despliegue remoto de la sesión 08"
git push -u origin sesion-08
```

### 1.2. Despliega desde el repositorio y las imágenes

Arranca Learner Lab y la instancia.

Anota su IPv4 pública y forma:

```text
escaparate.<ip>.nip.io
docs.<ip>.nip.io
```

Desde tu equipo:

```bash
dig +short escaparate.<ip>.nip.io
dig +short docs.<ip>.nip.io
```

Ambos nombres deben resolver hacia la misma IP.

En EC2, clona tu rama utilizando el mecanismo de autenticación Git que tengas configurado. **No escribas un PAT dentro de la URL del repositorio.**

Crea `.env` a partir de `.env.example` y completa los valores locales.

Desde `practicas/compose/`:

```bash
docker compose pull
docker compose up -d
docker compose ps
docker compose exec web nginx -t
```

No instales Java ni Maven y no construyas la aplicación en la instancia.

Desde tu equipo comprueba:

```bash
curl -fsS http://escaparate.<ip>.nip.io/ > /dev/null \
  && echo "catálogo remoto OK"

curl -fsS http://escaparate.<ip>.nip.io/api/salud/listo
```

Y verifica que siguen existiendo tres réplicas:

```bash
for i in 1 2 3 4 5 6; do
  curl -s http://escaparate.<ip>.nip.io/api/instancia
  echo
done
```

**Captura 1:** `docker compose ps` en EC2 y comprobación del catálogo remoto.

!!! question "Reflexiona"
    La EC2 no necesita compilar la aplicación. ¿Qué **unidad de despliegue** descarga Docker y qué **artefacto Java** termina ejecutándose dentro de cada `app-*`? Relaciónalo con lo aprendido en el Tema 2.

---

## Paso 2: Protege `/informes/` y observa qué envía Basic Authentication

La portada de documentación seguirá siendo pública:

```text
http://docs.<ip>.nip.io/
```

pero:

```text
http://docs.<ip>.nip.io/informes/
```

requerirá usuario y contraseña.

### 2.1. Crea las credenciales del laboratorio

En el clon del repositorio de EC2:

```bash
mkdir -p practicas/nginx/auth

HASH="$(openssl passwd -apr1)"
printf 'alumno:%s\n' "$HASH" > practicas/nginx/auth/.htpasswd
unset HASH
```

Utiliza una contraseña **inventada exclusivamente para esta práctica**.

Añade a `.gitignore`:

```gitignore
practicas/nginx/auth/.htpasswd
```

Monta el fichero en `web`:

```yaml
- ../nginx/auth/.htpasswd:/etc/nginx/auth/.htpasswd:ro
```

En el `location /informes/` añade:

```nginx
auth_basic "Zona restringida";
auth_basic_user_file /etc/nginx/auth/.htpasswd;
```

Aplica el cambio:

```bash
docker compose config
docker compose up -d --force-recreate web
docker compose exec web nginx -t
```

Comprueba:

```bash
curl -s -o /dev/null -w "sin credenciales: %{http_code}\n" \
  http://docs.<ip>.nip.io/informes/

curl -s -o /dev/null -w "con credenciales: %{http_code}\n" \
  -u alumno:<clave-laboratorio> \
  http://docs.<ip>.nip.io/informes/
```

Debes obtener:

```text
401
200
```

### 2.2. Base64 no es cifrado

Ejecuta desde tu equipo una petición verbosa:

```bash
curl -v -u alumno:<clave-laboratorio> \
  http://docs.<ip>.nip.io/informes/ \
  -o /dev/null
```

En la salida localiza:

```text
Authorization: Basic ...
```

Copia únicamente el valor situado después de `Basic` y descodifícalo:

```bash
echo '<valor-base64>' | base64 -d
```

Debes recuperar:

```text
alumno:<clave-laboratorio>
```

No incluyas la contraseña visible en ninguna captura.

!!! question "Reflexiona"
    ¿Qué protege Basic Authentication y qué **no** protege mientras el transporte siga siendo HTTP?

!!! tip "Ampliación opcional"
    Si quieres observar la cabecera desde otro punto de la comunicación, puedes capturar **tu propio tráfico de laboratorio** con `tcpdump`. No es necesario para completar la actividad.

---

## Paso 3: Prepara y valida ACME

Para emitir un certificado público, la autoridad debe comprobar que controlas los nombres solicitados.

Utilizaremos **HTTP-01**:

```text
CA
 │
 │ GET http://nombre/.well-known/acme-challenge/token
 ▼
Nginx :80
 │
 ▼
webroot compartido con Certbot
```

### 3.1. Añade Certbot como herramienta puntual

Entra en:

```bash
cd practicas/compose
```

A partir de este punto, los comandos de esta sección se ejecutan desde ese directorio.

Añade a `services:` de `compose.yaml`:

```yaml
certbot:
  image: certbot/certbot
  profiles: ["tools"]
  volumes:
    - ../nginx/certbot/www:/var/www/certbot
    - ../nginx/certbot/conf:/etc/letsencrypt
```

Crea:

```bash
mkdir -p ../nginx/certbot/www/.well-known/acme-challenge
mkdir -p ../nginx/certbot/conf
```

Añade a `web`:

```yaml
- ../nginx/certbot/www:/var/www/certbot:ro
- ../nginx/certbot/conf:/etc/letsencrypt:ro
```

Y a `.gitignore`:

```gitignore
practicas/nginx/certbot/conf/
practicas/nginx/certbot/www/
```

### 3.2. Sirve el desafío

En **los dos bloques HTTP** añade:

```nginx
location ^~ /.well-known/acme-challenge/ {
    root /var/www/certbot;
    default_type text/plain;
}
```

Aplica:

```bash
docker compose config
docker compose up -d --force-recreate web
docker compose exec web nginx -t
```

Crea una prueba:

```bash
echo 'acme-ok' > \
  ../nginx/certbot/www/.well-known/acme-challenge/prueba
```

Desde tu equipo:

```bash
curl http://escaparate.<ip>.nip.io/.well-known/acme-challenge/prueba
curl http://docs.<ip>.nip.io/.well-known/acme-challenge/prueba
```

Las dos respuestas deben ser:

```text
acme-ok
```

### 3.3. Prueba primero en staging y emite una sola vez

Desde `practicas/compose/`:

```bash
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  --dry-run \
  --agree-tos --non-interactive \
  --email <correo> \
  -d escaparate.<ip>.nip.io \
  -d docs.<ip>.nip.io
```

**No continúes si el `dry-run` falla.**

Cuando funcione, realiza una sola emisión real:

```bash
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  --cert-name daw-sesion08 \
  --agree-tos --non-interactive \
  --email <correo> \
  -d escaparate.<ip>.nip.io \
  -d docs.<ip>.nip.io
```

Comprueba:

```bash
docker compose run --rm certbot certificates
```

Debes localizar:

```text
/etc/letsencrypt/live/daw-sesion08/fullchain.pem
/etc/letsencrypt/live/daw-sesion08/privkey.pem
```

!!! warning "No pruebes emisiones reales al azar"
    Si una emisión de producción falla por límites de la CA, no repitas órdenes de producción. Conserva la evidencia del `dry-run` correcto y revisa el problema antes de volver a intentarlo.

!!! question "Reflexiona"
    HTTP-01 no demuestra quién eres. ¿Qué demuestra realmente ante la autoridad de certificación?

---

## Paso 4: Termina TLS en Nginx

Abre TCP/443 en el grupo de seguridad de la instancia.

En `compose.yaml`, `web` debe publicar:

```yaml
ports:
  - "80:80"
  - "443:443"
```

Ninguna réplica ni PostgreSQL debe publicar puertos.

### 4.1. Mantén HTTP solo como entrada de transición

Para cada uno de los dos nombres, el bloque del puerto 80 debe:

1. seguir sirviendo el desafío ACME;
2. redirigir cualquier otra petición a HTTPS.

Patrón:

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

Haz lo equivalente para `docs.*`.

### 4.2. Añade los bloques HTTPS

Utiliza este patrón en ambos sitios:

```nginx
listen 443 ssl;

ssl_certificate /etc/letsencrypt/live/daw-sesion08/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/daw-sesion08/privkey.pem;
ssl_protocols TLSv1.2 TLSv1.3;

add_header Strict-Transport-Security "max-age=300" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

Después **traslada** al bloque HTTPS la configuración funcional que ya tenías:

```text
escaparate.* :443
├── root e index
├── gzip y caché
└── location /api/ → backend_pool

docs.* :443
├── root e index
└── /informes/ → Basic Authentication
```

El tráfico queda:

```text
cliente
  │ HTTPS
  ▼
Nginx :443
  │ HTTP interno
  ├── app-1
  ├── app-2
  └── app-3
```

Valida y aplica:

```bash
docker compose exec web nginx -t
docker compose up -d --force-recreate web
docker compose ps
docker compose exec web nginx -t
```

---

## Paso 5: Comprueba el resultado y cierra

Desde tu equipo:

### HTTP redirige

```bash
curl -sI http://escaparate.<ip>.nip.io/ | head -n 3
```

Debe aparecer un `301` hacia `https://...`.

### El desafío continúa accesible por HTTP

```bash
curl -s http://docs.<ip>.nip.io/.well-known/acme-challenge/prueba
```

Debe devolver:

```text
acme-ok
```

### HTTPS es válido

```bash
curl -fsS https://escaparate.<ip>.nip.io/ > /dev/null \
  && echo "HTTPS OK"
```

No utilices `-k`.

### Las réplicas siguen detrás del proxy

```bash
for i in 1 2 3 4 5 6; do
  curl -s https://escaparate.<ip>.nip.io/api/instancia
  echo
done
```

### La zona protegida sigue protegida

```bash
curl -s -o /dev/null -w "sin credenciales: %{http_code}\n" \
  https://docs.<ip>.nip.io/informes/

curl -s -o /dev/null -w "con credenciales: %{http_code}\n" \
  -u alumno:<clave-laboratorio> \
  https://docs.<ip>.nip.io/informes/
```

Debe devolver:

```text
401
200
```

### Comprueba las cabeceras

```bash
curl -sI https://escaparate.<ip>.nip.io/ \
  | grep -iE 'strict-transport|x-content-type|referrer'
```

**Captura 2:** `dry-run` correcto y certificado emitido para los dos nombres.

**Captura 3:** redirección HTTP→HTTPS, acceso HTTPS válido y comprobación `401/200` de `/informes/`.

### Documentación mínima

Añade al `README.md` únicamente:

- los dos nombres públicos utilizados;
- que Nginx publica 80/443 y termina TLS;
- que `/informes/` utiliza Basic Authentication;
- que `.htpasswd` y el material privado de ACME no se versionan;
- que la aplicación y PostgreSQL siguen siendo servicios internos.

En `actividad-3.3.md` responde a las reflexiones y enlaza las tres capturas.

Desde el clon de EC2:

```bash
git status
git check-ignore -v practicas/nginx/auth/.htpasswd
git check-ignore -v practicas/nginx/certbot/conf/
```

Comprueba que no vas a versionar credenciales ni claves privadas.

Después:

1. registra los últimos cambios;
2. publica `sesion-08`;
3. abre una Pull Request hacia `main`;
4. fusiónala mediante **Create a merge commit**.

---

## Verificación

Al finalizar debe cumplirse:

- el conjunto se ejecuta en EC2 desde imágenes ya construidas;
- `web` es el único servicio con puertos publicados;
- HTTP normal redirige a HTTPS;
- HTTP-01 sigue siendo accesible por el puerto 80;
- los dos nombres funcionan por HTTPS sin desactivar la validación;
- el certificado cubre los dos nombres;
- `/api/instancia` sigue mostrando las tres réplicas;
- `/informes/` devuelve `401` sin credenciales y `200` con las correctas;
- HSTS, `nosniff` y `Referrer-Policy` aparecen en las respuestas HTTPS;
- `.htpasswd` y el material privado de ACME permanecen fuera de Git;
- la rama llega a `main` mediante Pull Request.

---

## Qué se entrega

- [ ] `actividad-3.3.md` con resultados y reflexiones.
- [ ] Tres capturas.
- [ ] Despliegue funcionando en EC2.
- [ ] `/informes/` protegido con Basic Authentication.
- [ ] Comprobación de que Base64 es reversible.
- [ ] `dry-run` ACME correcto y certificado público para los dos nombres.
- [ ] HTTPS terminado en Nginx.
- [ ] Redirección HTTP → HTTPS manteniendo HTTP-01.
- [ ] Cabeceras básicas de seguridad.
- [ ] Ficheros sensibles fuera de Git.
- [ ] Pull Request `sesion-08 → main` fusionada.

---

## Ampliación opcional: renovación

Un certificado caduca. Si terminas la actividad principal, prueba:

```bash
docker compose run --rm certbot renew --dry-run
```

La idea que debes reconocer es:

```text
renovar certificado
      ↓
nuevos ficheros en disco
      ↓
recargar Nginx
      ↓
nuevo certificado en uso
```

La automatización periódica de esta operación queda fuera de la parte obligatoria.

---

## ✅ Cierre

El mismo despliegue que funcionaba en tu equipo se ejecuta ahora sobre un host remoto sin recompilar la aplicación.

Nginx sigue siendo la única puerta de entrada y ha añadido dos responsabilidades nuevas:

```text
control de acceso
+
terminación TLS
```

Las tres réplicas y PostgreSQL continúan dentro de la red Docker. La mejora de seguridad se ha concentrado en el punto de entrada sin obligar a modificar el código de la aplicación.
