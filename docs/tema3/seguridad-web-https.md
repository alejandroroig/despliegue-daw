# 🔒 Seguridad web y HTTPS

Publicar una aplicación no consiste solo en conseguir que responda. Cuando un servicio pasa de un entorno local a una máquina accesible desde Internet aparecen dos preguntas nuevas:

```text
¿quién puede acceder?
¿cómo protegemos el tráfico mientras viaja?
```

En esta sesión estudiaremos ambas capas. Antes veremos también qué cambia —y qué no— al trasladar un despliegue Docker desde nuestro equipo a un host remoto.

---

## 1. Del laboratorio local a un host remoto

Un despliegue reproducible no debería depender de reconstruir manualmente la aplicación en cada servidor.

El flujo habitual es:

```text
código
  ↓
build + pruebas
  ↓
imagen publicada
  ↓
servidor remoto
  ↓
pull + ejecución
```

El servidor de destino necesita:

- un motor de contenedores;
- la descripción del despliegue;
- la configuración necesaria;
- acceso a las imágenes publicadas.

No necesita Maven, Node o el código fuente **para ejecutar una imagen que ya fue construida**.

### 1.1. Una IP pública cambia el significado de `ports`

En local:

```yaml
ports:
  - "80:80"
```

permite entrar desde nuestro equipo.

En un host con dirección pública, esa misma publicación puede convertir el servicio en accesible desde Internet.

Por eso seguimos buscando:

```text
Internet
   │
   │ 80 / 443
   ▼
 Nginx
   │
   ├── aplicación
   └── base de datos
```

Solo el punto de entrada necesita publicar puertos. Los servicios internos siguen comunicándose mediante la red de Docker.

### 1.2. Un nombre debe resolver públicamente

Para practicar con nombres públicos sin registrar un dominio propio puede utilizarse un servicio DNS comodín. Por ejemplo:

```text
web.203.0.113.25.nip.io
docs.203.0.113.25.nip.io
```

pueden resolver hacia:

```text
203.0.113.25
```

El DNS permite llegar hasta la máquina. Después Nginx sigue utilizando la cabecera `Host` para seleccionar el sitio correspondiente.

---

## 2. Control de acceso en el servidor web

### 2.1. HTTP Basic Authentication

Un servidor web puede proteger una ruta sin modificar la aplicación.

El intercambio básico es:

```text
cliente pide /privado/
        ↓
servidor responde 401
+ WWW-Authenticate
        ↓
cliente reintenta
+ Authorization: Basic ...
```

La cabecera `Authorization` contiene una representación Base64 de:

```text
usuario:contraseña
```

Base64 **no cifra**. Es una codificación reversible.

| Petición | Resultado habitual |
|---|---|
| Sin credenciales | `401 Unauthorized` |
| Credenciales incorrectas | `401 Unauthorized` |
| Credenciales correctas | la petición continúa |

En Nginx:

```nginx
location /privado/ {
    auth_basic "Zona restringida";
    auth_basic_user_file /etc/nginx/auth/usuarios.htpasswd;
}
```

El fichero de usuarios contiene líneas como:

```text
usuario:resumen-de-la-contraseña
```

Aunque la contraseña no esté guardada en claro, ese fichero sigue siendo **material sensible** y no debe versionarse.

!!! warning "Autenticar no significa cifrar"
    Basic Authentication decide si unas credenciales son aceptadas. No protege la cabecera `Authorization` mientras viaje por HTTP.

---

## 3. Qué problema tiene HTTP

HTTP por sí solo no ofrece confidencialidad ni integridad al transporte.

Un observador situado en un punto por el que pase la comunicación puede llegar a ver:

```text
rutas
cabeceras
cookies
credenciales Basic
cuerpos
respuestas
```

Y un intermediario activo podría intentar modificar el tráfico.

La idea importante es:

```text
Basic Authentication
→ controla acceso

HTTPS
→ protege el transporte
```

Son problemas distintos.

---

## 4. HTTPS: HTTP protegido por TLS

Cuando HTTP circula dentro de TLS hablamos de **HTTPS**.

TLS proporciona tres garantías principales:

| Garantía | Qué aporta |
|---|---|
| **Confidencialidad** | el contenido no puede leerse directamente desde la red |
| **Integridad** | una modificación del tráfico puede detectarse |
| **Autenticidad del servidor** | el cliente puede validar que el certificado corresponde al nombre solicitado |

De forma simplificada:

```text
cliente inicia TLS
      ↓
servidor presenta certificado
      ↓
cliente valida nombre, fechas y confianza
      ↓
se acuerdan claves de sesión
      ↓
HTTP viaja protegido
```

El certificado no cifra cada petición HTTP con la clave pública. En TLS moderno ayuda a autenticar al servidor durante el establecimiento de la conexión; después se utilizan claves de sesión eficientes para proteger el tráfico.

### 4.1. HTTPS no hace segura toda la aplicación

Un certificado válido no evita:

- errores de programación;
- autorización incorrecta;
- contraseñas débiles;
- dependencias vulnerables;
- un servidor comprometido.

HTTPS protege principalmente **la comunicación entre los extremos de la conexión TLS**.

---

## 5. Certificados y confianza

Un certificado X.509 contiene, entre otros datos:

- nombres para los que es válido;
- una clave pública;
- fechas de validez;
- información del emisor;
- una firma.

Un mismo certificado puede cubrir varios nombres mediante **Subject Alternative Name (SAN)**:

```text
certificado
├── web.ejemplo.test
└── docs.ejemplo.test
```

La clave privada asociada **no forma parte del certificado público** y debe permanecer secreta.

### 5.1. Cadena de confianza

Habitualmente:

```text
certificado del sitio
        ↓
CA intermedia
        ↓
CA raíz de confianza
```

El cliente valida que puede construir una cadena hasta una autoridad que ya considera confiable y comprueba también el nombre y las fechas.

### 5.2. Autofirmado y CA pública

Un certificado autofirmado puede cifrar el transporte, pero el navegador no confía automáticamente en quién afirma ser ese servidor.

Un certificado emitido por una CA pública reconocida puede validarse sin instalar confianza adicional en cada cliente.

!!! info "ACME y la CA no son lo mismo"
    **ACME** es un protocolo de automatización. Una autoridad como Let's Encrypt puede utilizar ACME para validar el control de un nombre y emitir certificados.

---

## 6. ACME y HTTP-01

Una CA pública no necesita comprobar la identidad personal del administrador para un certificado de validación de dominio. Necesita comprobar que controla el nombre solicitado.

Con **HTTP-01**:

```text
cliente ACME
  │ escribe token
  ▼
webroot
  ▲
  │ GET /.well-known/acme-challenge/...
  │
 CA
```

La autoridad:

1. resuelve el nombre públicamente;
2. conecta al puerto 80;
3. solicita un token concreto;
4. comprueba que recibe el valor esperado.

Por eso HTTP-01 necesita:

```text
DNS público correcto
+
puerto 80 accesible
+
ruta del challenge correctamente servida
```

### 6.1. Probar antes de emitir

Las CA aplican límites de emisión. Durante configuración y diagnóstico debe utilizarse un entorno de **staging**.

El flujo recomendable es:

```text
probar
  ↓
corregir
  ↓
volver a probar
  ↓
emitir en producción una vez
```

### 6.2. Los certificados caducan

Obtener un certificado una vez no resuelve el problema para siempre.

```text
renovar
  ↓
ficheros nuevos
  ↓
recargar servidor
  ↓
certificado nuevo en uso
```

En esta sesión basta con comprender el ciclo. La automatización periódica puede quedar como ampliación.

---

## 7. Terminación TLS en el proxy

Cuando Nginx es la única entrada pública, puede concentrar también TLS:

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

Nginx:

1. presenta el certificado;
2. establece y descifra la conexión TLS exterior;
3. selecciona un backend;
4. reenvía internamente la petición.

Las réplicas no necesitan gestionar certificados individualmente.

Un bloque mínimo:

```nginx
server {
    listen 443 ssl;
    server_name web.ejemplo.test;

    ssl_certificate     /etc/letsencrypt/live/mi-cert/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mi-cert/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;

    # contenido o proxy
}
```

Esto presupone que la red entre proxy y backend es una red interna de confianza. En otras arquitecturas también puede ser necesario cifrar ese tramo.

`X-Forwarded-Proto` permite informar al backend de que el cliente original llegó por HTTPS aunque la conexión interna sea HTTP.

---

## 8. HTTP, redirección y HSTS

### 8.1. Redirigir hacia HTTPS

Una vez disponible HTTPS, el puerto 80 puede actuar como entrada de transición:

```text
http://web.ejemplo.test/recurso
        ↓ 301
https://web.ejemplo.test/recurso
```

Si seguimos utilizando HTTP-01, podemos conservar una excepción:

```nginx
location ^~ /.well-known/acme-challenge/ {
    root /var/www/acme;
}

location / {
    return 301 https://$host$request_uri;
}
```

### 8.2. HSTS

HSTS permite indicar al navegador que ese nombre debe utilizar exclusivamente HTTPS durante un tiempo:

```http
Strict-Transport-Security: max-age=300
```

Durante un laboratorio conviene utilizar un tiempo corto.

HSTS se aprende mejor como una segunda capa:

```text
redirección HTTP → HTTPS
+
navegador recuerda que debe usar HTTPS
```

### 8.3. Otras cabeceras sencillas

```nginx
add_header Strict-Transport-Security "max-age=300" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

| Cabecera | Objetivo |
|---|---|
| `Strict-Transport-Security` | recordar el uso de HTTPS |
| `X-Content-Type-Options: nosniff` | impedir que el navegador adivine otro tipo MIME |
| `Referrer-Policy` | limitar la información de origen enviada al navegar |

Políticas más complejas, como Content Security Policy, deben diseñarse según los recursos reales de cada aplicación y quedan fuera de esta sesión.

---

## 9. Las capas no sustituyen unas a otras

Una visión útil es:

```text
transporte       → TLS
acceso           → autenticación y autorización
secretos         → credenciales y claves
aplicación       → validación y programación segura
infraestructura  → puertos, permisos y actualizaciones
```

Añadir HTTPS no elimina la necesidad de las demás capas.

---

## 🎯 Qué debes saber hacer al salir de esta sesión

Al terminar, deberías poder:

- Explicar qué cambia al desplegar el mismo Compose en una máquina pública.
- Explicar por qué el servidor remoto puede ejecutar imágenes sin recompilar la aplicación.
- Mantener una única puerta pública.
- Explicar qué hace Basic Authentication y por qué Base64 no es cifrado.
- Proteger una ruta con `auth_basic` y mantener las credenciales fuera del repositorio.
- Distinguir control de acceso y protección del transporte.
- Explicar confidencialidad, integridad y autenticidad del servidor en TLS.
- Reconocer qué contiene un certificado y qué representa SAN.
- Explicar el objetivo de una cadena de confianza.
- Explicar qué demuestra HTTP-01 y por qué necesita DNS público y puerto 80.
- Entender por qué conviene probar ACME en staging antes de emitir.
- Terminar TLS en un proxy inverso.
- Redirigir HTTP a HTTPS manteniendo accesible el challenge.
- Reconocer el objetivo de HSTS, `nosniff` y `Referrer-Policy`.
- Explicar por qué los certificados necesitan renovación.

Lo que basta con **reconocer**:

- detalles criptográficos del handshake TLS;
- certificados autofirmados como alternativa de laboratorio;
- otros desafíos ACME como DNS-01;
- automatización periódica de la renovación;
- políticas avanzadas como CSP.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - Un host remoto ejecuta las mismas imágenes y configuración; no debe recompilar necesariamente la aplicación.
    - Una publicación de puertos sobre una máquina pública crea una superficie de entrada real.
    - Basic Authentication controla acceso, pero Base64 es reversible y no protege el transporte.
    - HTTP no ofrece por sí solo confidencialidad ni integridad.
    - TLS aporta confidencialidad, integridad y autenticidad del servidor.
    - Un certificado vincula una clave pública con uno o varios nombres; la clave privada debe permanecer secreta.
    - ACME automatiza la validación y emisión; HTTP-01 demuestra control del nombre a través de DNS público y puerto 80.
    - Durante pruebas debe utilizarse staging y reservar la emisión real para cuando el recorrido funcione.
    - Terminar TLS en Nginx evita configurar certificados individualmente en cada réplica.
    - HTTP puede redirigir a HTTPS manteniendo una excepción para el challenge ACME.
    - HSTS y otras cabeceras complementan HTTPS, pero no sustituyen la seguridad de la aplicación.
    - Los certificados caducan y deben renovarse.

---

En la actividad trasladarás el despliegue del laboratorio a un host remoto, protegerás una zona concreta y convertirás Nginx en el punto donde termina TLS. El proyecto concreto sirve como aplicación práctica; los patrones de esta sesión son generales para cualquier arquitectura con un proxy inverso como entrada.
