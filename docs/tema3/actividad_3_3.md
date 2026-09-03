# 🧪 Actividad 3.3: Cierra la puerta y echa la llave

## Contexto

La Actividad 3.2 terminó con una única puerta pública y tres copias de Escaparate detrás de Nginx:

```text
                              ┌── app-1 ──┐
Internet ──► web:Nginx ──► /api ├── app-2 ──┼──► bd
                              └── app-3 ──┘
```

El despliegue ya reparte tráfico y soporta la caída de una réplica, pero sigue utilizando HTTP. Además, los informes publicados en `docs` son accesibles para cualquiera que conozca la dirección.

En esta actividad vas a añadir dos capas diferentes:

```text
control de acceso
      +
protección del transporte con HTTPS
```

Primero protegerás `/informes/`. Después observarás en una captura de **tu propio tráfico** que Basic Authentication sobre HTTP permite recuperar las credenciales. Finalmente obtendrás un certificado público mediante ACME, terminarás TLS en Nginx, redirigirás HTTP a HTTPS y dejarás preparada la renovación.

## Qué vas a practicar

- **Restringir** una ruta desde Nginx sin modificar la aplicación.
- **Demostrar** por qué Base64 y cifrado no son lo mismo.
- **Capturar** tráfico propio y localizar una cabecera HTTP sensible.
- **Validar** mediante HTTP-01 que controlas dos nombres públicos.
- **Obtener** un único certificado válido para ambos nombres.
- **Terminar TLS** en Nginx manteniendo HTTP entre el proxy y las réplicas internas.
- **Redirigir** el tráfico normal de HTTP a HTTPS sin romper el desafío ACME.
- **Añadir** HSTS y otras cabeceras de seguridad.
- **Comprobar** el procedimiento de renovación y programar su ejecución periódica.

## Requisitos previos

- La Actividad 3.2 terminada y fusionada en `main`.
- El despliegue remoto de la sesión anterior funcionando en una instancia Amazon EC2 de AWS Academy Learner Lab.
- Docker y Docker Compose disponibles en la instancia.
- El puerto 80 permitido hacia la instancia.
- Posibilidad de permitir también el puerto 443.
- Los dos nombres de la sesión anterior:

```text
escaparate.<ip-publica>.nip.io
docs.<ip-publica>.nip.io
```

- El fichero de apoyo `actividad-3.3-soporte.zip`.

!!! info "Elastic IP recomendada"
    La actividad puede realizarse con la IPv4 pública temporal de la instancia AWS EC2 mientras esa dirección no cambie. Sin embargo, es recomendable asociar una **Elastic IP** antes de emitir el certificado.

    Así los nombres de `nip.io` permanecen estables aunque la instancia se detenga y vuelva a arrancar, y puedes continuar la práctica en otra sesión sin emitir para nombres nuevos.

    Esta es infraestructura que ya has trabajado en **Infraestructura en la Nube (INU)**. En DAW no se evalúa crear ni administrar la Elastic IP: nos interesa únicamente disponer de una entrada pública estable.

Prepara la rama:

```bash
git switch main
git pull --ff-only
git switch -c sesion-08
```

Y crea:

```text
entregas/
└── tema3/
    └── actividad-3.3/
        ├── actividad-3.3.md
        └── img/
```

Los ficheros técnicos continúan en `practicas/`. La carpeta `entregas/` contiene evidencias, resultados y reflexiones.

---

## Paso 1: Fija y comprueba la entrada pública

Arranca Learner Lab y la instancia EC2.

Si vas a utilizar una Elastic IP, asígnala **antes de continuar**. A partir de este momento trabaja siempre con la dirección definitiva elegida.

Construye de nuevo los dos nombres:

```text
escaparate.<ip>.nip.io
docs.<ip>.nip.io
```

Comprueba desde tu equipo que ambos resuelven hacia la misma dirección:

```bash
dig +short escaparate.<ip>.nip.io
dig +short docs.<ip>.nip.io
```

Comprueba también que siguen funcionando:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://escaparate.<ip>.nip.io/
curl -s -o /dev/null -w "%{http_code}\n" http://docs.<ip>.nip.io/
```

Añade al grupo de seguridad de la instancia tráfico HTTPS entrante por **TCP/443**. El 80 debe continuar abierto.

**Evidencia:** nombres resolviendo, ambos sitios respondiendo y reglas de entrada 80/443.

---

## Paso 2: Protege solo los informes

La documentación general debe continuar siendo pública, pero `/informes/` requerirá usuario y contraseña.

Crea el directorio local para el fichero de credenciales:

```bash
mkdir -p practicas/nginx/auth
```

Genera de forma interactiva un resumen de una **contraseña inventada exclusivamente para el laboratorio**:

```bash
HASH="$(openssl passwd -apr1)"
printf 'alumno:%s\n' "$HASH" > practicas/nginx/auth/.htpasswd
unset HASH
```

No utilices una contraseña real ni reutilizada.

Monta ese fichero en el servicio `web` de Compose como solo lectura:

```yaml
- ../nginx/auth/.htpasswd:/etc/nginx/auth/.htpasswd:ro
```

Añade también a `.gitignore`:

```gitignore
practicas/nginx/auth/.htpasswd
```

A partir de lo estudiado en teoría, modifica el `location /informes/` del sitio de documentación para utilizar `auth_basic` y `auth_basic_user_file`.

Valida antes de recargar:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Comprueba los tres casos desde tu equipo:

```bash
curl -s -o /dev/null -w "sin credenciales: %{http_code}\n" \
  http://docs.<ip>.nip.io/informes/

curl -s -o /dev/null -w "credenciales incorrectas: %{http_code}\n" \
  -u alumno:incorrecta http://docs.<ip>.nip.io/informes/

curl -s -o /dev/null -w "credenciales correctas: %{http_code}\n" \
  -u alumno:<clave-laboratorio> http://docs.<ip>.nip.io/informes/
```

Debes obtener:

```text
401
401
200
```

Comprueba además que la portada de documentación continúa siendo pública.

**Evidencia:** tres códigos de estado, configuración de Nginx y regla de `.gitignore`. No muestres la contraseña.

---

## Paso 3: Observa tus credenciales viajando por HTTP

Ahora vas a demostrar por qué la configuración anterior todavía no es segura.

En la instancia EC2 inicia una captura de **tu propio tráfico HTTP**:

```bash
sudo tcpdump -i any -A -s 0 'tcp port 80'
```

Desde tu equipo realiza una petición explícita con la cuenta del laboratorio:

```bash
curl -u alumno:<clave-laboratorio> \
  http://docs.<ip>.nip.io/informes/
```

En la salida de `tcpdump` localiza:

```http
Authorization: Basic ...
```

Copia únicamente el valor que aparece después de `Basic` y descodifícalo en tu equipo:

```bash
echo '<valor-base64>' | base64 -d
```

El resultado debe tener la forma:

```text
alumno:<clave-laboratorio>
```

Detén `tcpdump` con `Ctrl+C`.

!!! danger "La captura contiene una contraseña"
    Aunque sea una contraseña inventada para la práctica, **ocúltala en la captura que añadas a la entrega**. Lo que debes demostrar es que puede recuperarse, no publicar su valor.

**Reflexiona en `actividad-3.3.md`:** ¿qué posiciones de una red permitirían observar tráfico HTTP en tránsito? Además de leerlo, ¿qué podría intentar hacer un intermediario activo?

**Evidencia:** cabecera `Authorization` capturada y resultado descodificado con la contraseña ocultada.

---

## Paso 4: Prepara el desafío ACME

Descomprime `actividad-3.3-soporte.zip`. Contiene:

```text
actividad-3.3-soporte/
├── fragmento-certbot.yaml
└── renovar-certificados.sh
```

El fragmento proporciona un servicio Certbot de uso puntual. Incorpóralo a `practicas/compose/compose.yaml`.

El servicio utiliza un perfil `tools`, por lo que no se mantiene arrancado con el resto del conjunto. Cuando lo ejecutes explícitamente mediante `docker compose run certbot`, Compose lo habilitará para esa operación.

Crea los directorios compartidos:

```bash
mkdir -p practicas/nginx/certbot/www/.well-known/acme-challenge
mkdir -p practicas/nginx/certbot/conf
```

Añade al servicio `web` estos montajes:

```yaml
- ../nginx/certbot/www:/var/www/certbot:ro
- ../nginx/certbot/conf:/etc/letsencrypt:ro
```

Y excluye de Git el material generado por ACME:

```gitignore
practicas/nginx/certbot/conf/
practicas/nginx/certbot/www/
```

En **los dos bloques HTTP**, añade una excepción que sirva `/.well-known/acme-challenge/` desde `/var/www/certbot`, aplicando el patrón visto en teoría.

Recrea `web` para aplicar los nuevos montajes:

```bash
docker compose up -d web
docker compose exec web nginx -t
```

Antes de contactar con ninguna autoridad, prueba tú mismo el recorrido completo:

```bash
echo 'acme-ok' > \
  practicas/nginx/certbot/www/.well-known/acme-challenge/prueba
```

Desde tu equipo:

```bash
curl http://escaparate.<ip>.nip.io/.well-known/acme-challenge/prueba
curl http://docs.<ip>.nip.io/.well-known/acme-challenge/prueba
```

Ambas peticiones deben devolver:

```text
acme-ok
```

**No continúes si alguna falla.** Primero corrige DNS, grupo de seguridad, montaje o configuración de Nginx.

**Evidencia:** las dos peticiones HTTP-01 manuales funcionando.

---

## Paso 5: Valida en staging y emite un certificado

Utilizarás **un único certificado válido para los dos nombres**.

Primero prueba todo el procedimiento contra el entorno de pruebas de Let's Encrypt:

```bash
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  --dry-run \
  --agree-tos --non-interactive \
  --email <correo> \
  -d escaparate.<ip>.nip.io \
  -d docs.<ip>.nip.io
```

!!! warning "Producción solo después de staging"
    No repitas peticiones de producción para probar al azar. Si el `dry-run` falla, diagnostica el motivo y vuelve a ejecutar únicamente la prueba.

Cuando el `dry-run` termine correctamente, realiza **una única emisión de producción**:

```bash
docker compose run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  --cert-name daw-sesion08 \
  --agree-tos --non-interactive \
  --email <correo> \
  -d escaparate.<ip>.nip.io \
  -d docs.<ip>.nip.io
```

Comprueba lo que Certbot ha guardado:

```bash
docker compose run --rm certbot certificates
```

El certificado estará disponible para `web` bajo:

```text
/etc/letsencrypt/live/daw-sesion08/fullchain.pem
/etc/letsencrypt/live/daw-sesion08/privkey.pem
```

**Reflexiona:** la CA no te ha pedido un documento de identidad. ¿Qué control has demostrado realmente mediante HTTP-01? ¿Qué habría ocurrido si esos nombres resolvieran hacia otra máquina?

**Evidencia:** `dry-run` correcto, emisión de producción y salida de `certbot certificates`.

---

## Paso 6: Termina TLS en Nginx

Ahora configura HTTPS para los **dos sitios**.

Cada bloque de puerto 443 debe utilizar el mismo certificado:

```text
fullchain.pem
privkey.pem
```

Además:

- el sitio `escaparate.*` debe seguir sirviendo frontend y reenviando `/api/` al `upstream`;
- el sitio `docs.*` debe seguir sirviendo documentación y protegiendo `/informes/`;
- las réplicas continúan hablando HTTP dentro de la red Docker;
- Nginx es la única pieza que publica el puerto 443.

Utiliza como mínimo:

```nginx
listen 443 ssl;
ssl_protocols TLSv1.2 TLSv1.3;
```

Después transforma los bloques del puerto 80 para que:

```text
/.well-known/acme-challenge/...  → siga sirviéndose por HTTP
cualquier otra ruta             → 301 hacia HTTPS
```

Añade en los bloques HTTPS estas tres políticas:

```text
Strict-Transport-Security: max-age=300
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
```

Utiliza `always` al añadirlas desde Nginx.

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Comprueba la redirección:

```bash
curl -sI http://escaparate.<ip>.nip.io/ | head
```

Y comprueba que la excepción ACME continúa funcionando por HTTP:

```bash
curl http://docs.<ip>.nip.io/.well-known/acme-challenge/prueba
```

Inspecciona el certificado servido:

```bash
echo | openssl s_client \
  -connect escaparate.<ip>.nip.io:443 \
  -servername escaparate.<ip>.nip.io 2>/dev/null \
  | openssl x509 -noout -issuer -subject -dates -ext subjectAltName
```

Debes ver los dos nombres en `Subject Alternative Name`.

Comprueba también:

```bash
curl -sI https://escaparate.<ip>.nip.io/ \
  | grep -iE 'strict-transport|x-content-type|referrer'
```

Y visita ambos sitios en el navegador sin aceptar excepciones de certificado.

**Evidencia:** redirección, excepción ACME, certificado con ambos nombres, cabeceras y HTTPS válido en navegador.

---

## Paso 7: Deja preparada la renovación

Copia el script del paquete a:

```text
practicas/compose/scripts/renovar-certificados.sh
```

Dale permisos de ejecución:

```bash
chmod +x practicas/compose/scripts/renovar-certificados.sh
```

Lee el script antes de utilizarlo. Debes identificar sus dos operaciones:

```text
certbot renew
      ↓
nginx reload
```

Ejecuta primero la comprobación oficial de renovación:

```bash
docker compose run --rm certbot renew --dry-run
```

Después ejecuta una vez el script para comprobar que puede operar el conjunto:

```bash
practicas/compose/scripts/renovar-certificados.sh
```

Como el certificado acaba de emitirse, una ejecución normal probablemente indicará que **todavía no necesita renovarse**. Eso es correcto.

Ahora obtén la ruta absoluta del script:

```bash
realpath practicas/compose/scripts/renovar-certificados.sh
```

Edita tu `crontab`:

```bash
crontab -e
```

Programa una ejecución dos veces al día, por ejemplo:

```cron
17 3,15 * * * /ruta/absoluta/renovar-certificados.sh >> /ruta/absoluta/renovacion.log 2>&1
```

Comprueba:

```bash
crontab -l
```

!!! info "Qué estamos demostrando realmente"
    En el laboratorio no vamos a esperar a que el certificado se acerque a su caducidad. El `dry-run` demuestra que el procedimiento de renovación funciona; `crontab -l` demuestra que existe un mecanismo que lo intentará periódicamente.

    Si la instancia está apagada en la hora programada, `cron` no puede ejecutar nada. En un servidor que deba renovar certificados automáticamente, ese host debe estar operativo o utilizar un planificador que gestione ejecuciones perdidas.

**Reflexiona:** ¿por qué no basta con que Certbot escriba un certificado nuevo en disco? ¿Qué función cumple la recarga de Nginx?

**Evidencia:** `renew --dry-run`, contenido de `crontab -l` y las dos operaciones del script explicadas con tus palabras.

---

## Paso 8: Documenta y fusiona

Amplía `README.md` con una sección breve de seguridad que indique:

- qué zona está protegida y con qué mecanismo;
- dónde se genera el fichero de credenciales y por qué no se versiona;
- qué dos nombres cubre el certificado;
- qué servicio termina TLS;
- qué ocurre con el puerto 80;
- qué cabeceras de seguridad se añaden;
- cómo se prueba la renovación;
- qué mecanismo intenta renovarla periódicamente;
- por qué Nginx debe recargarse después.

En `entregas/tema3/actividad-3.3/actividad-3.3.md` incluye las evidencias y respuestas solicitadas durante los pasos anteriores.

Comprueba antes de publicar:

```bash
git status
git check-ignore -v practicas/nginx/auth/.htpasswd
git check-ignore -v practicas/nginx/certbot/conf/
```

No debe aparecer ningún certificado privado, cuenta ACME ni fichero `.htpasswd` entre los cambios que vas a versionar.

Publica la rama y abre una Pull Request hacia `main`:

```bash
git push -u origin sesion-08
```

Revísala y fusiónala siguiendo el flujo habitual del módulo.

---

## Verificación final

Sustituye `<ip>` y `<clave>`:

```bash
# HTTP redirige
curl -sI http://escaparate.<ip>.nip.io/ | head -n 2

# El desafío sigue disponible por HTTP
curl -s http://docs.<ip>.nip.io/.well-known/acme-challenge/prueba

# HTTPS responde
curl -s -o /dev/null -w "catalogo %{http_code}\n" \
  https://escaparate.<ip>.nip.io/

# Readiness sigue funcionando
curl -s https://escaparate.<ip>.nip.io/api/salud/listo

# Las tres réplicas continúan detrás del proxy
for i in 1 2 3 4 5 6; do
  curl -s https://escaparate.<ip>.nip.io/api/instancia
  echo
done

# Zona protegida
curl -s -o /dev/null -w "sin credenciales %{http_code}\n" \
  https://docs.<ip>.nip.io/informes/
curl -s -o /dev/null -w "con credenciales %{http_code}\n" \
  -u alumno:<clave> https://docs.<ip>.nip.io/informes/

# Certificado
printf '' | openssl s_client \
  -connect escaparate.<ip>.nip.io:443 \
  -servername escaparate.<ip>.nip.io 2>/dev/null \
  | openssl x509 -noout -issuer -subject -dates -ext subjectAltName

# Cabeceras
curl -sI https://escaparate.<ip>.nip.io/ \
  | grep -iE 'strict-transport|x-content-type|referrer'

# Renovación
crontab -l
docker compose run --rm certbot certificates
```

Al finalizar debe cumplirse:

- HTTP redirige permanentemente a HTTPS, salvo la ruta ACME.
- Los dos nombres funcionan por HTTPS sin desactivar la validación del certificado.
- Un mismo certificado incluye ambos nombres.
- `/informes/` devuelve `401` sin credenciales y `200` con las correctas.
- El catálogo y `/api/salud/listo` siguen funcionando.
- Las tres réplicas siguen apareciendo en `/api/instancia`.
- Se envían HSTS, `nosniff` y `Referrer-Policy`.
- `certbot renew --dry-run` funciona.
- Existe una ejecución periódica documentada para renovar y recargar Nginx.
- `.htpasswd` y `certbot/conf/` permanecen fuera de Git.
- Los cambios llegan a `main` mediante Pull Request.

---

## Qué se entrega

- [ ] `actividad-3.3.md` con las evidencias y reflexiones solicitadas.
- [ ] Protección de `/informes/` y comprobación `401 / 401 / 200`.
- [ ] Captura de tráfico HTTP propio con Basic Authentication descodificada y contraseña ocultada.
- [ ] Validación ACME en staging y certificado de producción para los dos nombres.
- [ ] HTTPS válido en ambos sitios y terminación TLS en Nginx.
- [ ] Redirección HTTP → HTTPS manteniendo disponible HTTP-01.
- [ ] HSTS, `nosniff` y `Referrer-Policy` comprobadas.
- [ ] `renew --dry-run` correcto y renovación periódica configurada.
- [ ] `README.md` actualizado.
- [ ] Pull Request `sesion-08` fusionada en `main`.

---

## ✅ Cierre

El despliegue conserva la arquitectura de la sesión anterior, pero Nginx asume ahora dos responsabilidades nuevas: **controla el acceso a una zona y termina la conexión TLS pública**.

La aplicación y PostgreSQL no han tenido que cambiar. Esta separación será importante en las siguientes sesiones: la seguridad de transporte, la observabilidad y la automatización pueden evolucionar alrededor de la aplicación sin convertir cada mejora operativa en un cambio de código.
