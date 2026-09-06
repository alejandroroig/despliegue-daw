# 🌐 Servidores web y DNS

!!! info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/servidores-web-dns.pptx){target="_blank" rel="noopener"}

---

Un servidor web puede asumir la entrada HTTP de un sistema y servir directamente recursos estáticos. Esto permite separar el trabajo de **entregar ficheros** del trabajo de **ejecutar lógica de aplicación**.

En esta sesión incorporaremos Nginx delante de una aplicación web y utilizaremos dos sitios distintos para estudiar raíces de documentos, hosts virtuales, compresión, caché y resolución de nombres. El reenvío de peticiones dinámicas aparecerá únicamente como conexión con el backend; su funcionamiento se estudiará en la siguiente sesión.

La idea central será separar dos responsabilidades:

```text
recursos estáticos
→ el servidor web los lee y los entrega

respuestas dinámicas
→ la aplicación las genera cuando recibe la petición
```

A partir de esa separación veremos por qué un servidor web puede aplicar políticas específicas de entrega, como compresión o caché, sin confundirlas con el comportamiento de la aplicación.

---

## 🗂️ 1. Qué hace un servidor web

Un **servidor web** es un programa que escucha peticiones HTTP y devuelve respuestas HTTP. Cuando sirve contenido estático, su trabajo fundamental consiste en relacionar una URL con un recurso del sistema de ficheros y entregarlo con las cabeceras adecuadas.

Una configuración suele definir una **raíz de documentos**:

| El navegador pide | El servidor busca | Si no está |
|---|---|---|
| `/` | el fichero índice de la raíz | normalmente `403` si no hay índice y no se permite listar |
| `/css/estilos.css` | `<raíz>/css/estilos.css` | `404` |
| `/img/logo.png` | `<raíz>/img/logo.png` | `404` |

La raíz marca el punto desde el que el servidor busca los recursos que puede publicar. Eso no significa que absolutamente todo su contenido tenga que ser accesible, porque las reglas pueden restringir rutas concretas, pero sí convierte esos ficheros en candidatos a ser servidos.

Por eso no deben aparecer accidentalmente en una raíz pública:

```text
.env
copias de seguridad
scripts SQL
credenciales
notas internas
```

Un servidor web tampoco sustituye a la aplicación. Nginx no ejecuta por sí mismo la lógica de negocio de una aplicación. Para contenido dinámico necesita reenviar la petición al proceso que sí puede generarlo.

En esta sesión la separación será:

```text
HTML, CSS, JS, imágenes
→ Nginx los lee del disco

/api/...
→ la aplicación genera la respuesta
```

---

## ⚙️ 2. Apache y Nginx

**Apache HTTP Server** y **Nginx** son dos servidores web maduros capaces de servir contenido estático, aplicar reglas HTTP y actuar como proxy.

Su diseño histórico es diferente. Apache es muy modular y admite varios modelos de procesamiento mediante sus MPM. Nginx nació con una arquitectura orientada a eventos y se popularizó especialmente como servidor de estáticos y como punto de entrada delante de otras aplicaciones.

En este módulo trabajaremos con **Nginx** porque una sola herramienta nos permitirá estudiar progresivamente:

```text
sesión 6 → contenido estático y hosts virtuales
sesión 7 → proxy inverso y balanceo
sesión 8 → HTTPS y control de acceso
sesión 9 → registro del tráfico
```

No se trata de afirmar que Nginx sea universalmente mejor. Apache sigue siendo una opción perfectamente válida y muy extendida.

!!! info "Para saber más: diferencias habituales"
    | | Apache | Nginx |
    |---|---|---|
    | Procesamiento | Configurable mediante varios MPM | Orientado a eventos desde el diseño |
    | Configuración | Central y, si se habilita, por directorio con `.htaccess` | Central |
    | Módulos | Ecosistema muy amplio | Módulos integrados o dinámicos |
    | Uso frecuente | Hosting, aplicaciones, servidor general | Estáticos, proxy y punto de entrada |

---

## 🧱 3. Incorporar Nginx al despliegue

Nginx no estaba en el despliegue del Tema 2. En esta sesión lo añadiremos como un nuevo servicio de Compose.

### 3.1. Servicio, configuración y contenido

Un esquema simplificado será:

```yaml
services:
  web:
    image: nginx:1.30.4-alpine
    ports:
      - "80:80"
    volumes:
      - <configuracion>:/etc/nginx/conf.d:ro
      - <sitio-web>:/srv/www/web:ro
      - <documentacion>:/srv/www/docs:ro
```

Hay tres ideas importantes:

- **La imagen de Nginx es genérica.** No necesitamos construir una nueva para cambiar un sitio durante esta sesión.
- **La configuración entra desde fuera** mediante un montaje de solo lectura.
- **El contenido estático también puede montarse desde fuera**, de forma que Nginx se limite a servirlo.

!!! info "La raíz predeterminada de la imagen oficial"
    La imagen oficial de Nginx trae preparado `/usr/share/nginx/html` como directorio web predeterminado. Es una convención de la imagen, no una ruta obligatoria. Podemos utilizar raíces propias como `/srv/www/web` y `/srv/www/docs`; la directiva `root` de cada bloque `server` decide qué contenido sirve cada sitio.

La aplicación `app` continúa existiendo, pero deja de publicar su puerto 8080 hacia el anfitrión. El único puerto público del conjunto será el 80 de `web`.

```mermaid
flowchart LR
    C["Navegador"] --> N["Nginx<br/>web"]
    N --> E["Contenido<br/>estático"]
    N --> A["Aplicación<br/>app"]
    A --> D[("Base de datos<br/>bd")]
```

El frontend integrado que todavía contiene la imagen de `app` no desaparece físicamente, pero deja de ser la copia que recibe el navegador. Desde esta sesión, los ficheros públicos los sirve Nginx.

### 3.2. Validar y recargar antes de aplicar

La configuración de un servidor web es código de infraestructura. Un error de sintaxis puede impedir que el servicio arranque o que una recarga se aplique.

Nginx puede validar su configuración:

```bash
nginx -t
```

En un contenedor Compose:

```bash
docker compose exec web nginx -t
```

Si la validación es correcta, se puede pedir a Nginx que recargue la configuración:

```bash
docker compose exec web nginx -s reload
```

La secuencia profesional es:

```text
editar
↓
validar
↓
recargar
↓
comprobar
```

Reiniciar el contenedor entero para aplicar cada cambio funciona, pero oculta una capacidad importante del servidor: **puede recargar su configuración sin sustituir el proceso por un despliegue completamente nuevo**.

---

## 🧾 4. Diagnóstico básico al servir ficheros

Cuando un sitio está correctamente montado pero no se muestra como esperas, tres detalles explican muchos fallos.

### Tipo MIME

La respuesta HTTP incluye una cabecera `Content-Type`, por ejemplo:

```text
text/html
text/css
application/javascript
image/png
```

El navegador utiliza ese valor para interpretar el recurso. Un CSS puede existir y descargarse, pero no aplicarse correctamente si llega con un tipo inesperado.

### Índice

Cuando se solicita un directorio, el servidor suele buscar un fichero como:

```text
index.html
```

Si no existe, puede devolver `403` o, si se ha configurado expresamente, mostrar el listado del directorio mediante `autoindex`.

### Permisos y rutas

Nginx debe poder:

```text
leer los ficheros
+
atravesar los directorios que los contienen
```

Con montajes desde el anfitrión también conviene comprobar que la ruta configurada con `root` coincide con la ruta que realmente existe dentro del contenedor.

!!! tip "Tres síntomas que conviene reconocer"
    - `404` con el recurso aparentemente existente → revisa `root`, la ruta solicitada y el montaje.
    - `403` sobre un directorio → revisa permisos, fichero índice o si el listado está permitido.
    - Página sin estilos o recurso rechazado → revisa la URL del recurso y su `Content-Type`.

No es necesario memorizar todas las causas posibles. Lo importante es relacionar el síntoma con el primer lugar que conviene inspeccionar.

---

## 🏠 5. Hosts virtuales por nombre

Una misma dirección IP y un mismo puerto pueden servir varios sitios diferentes.

### 5.1. La cabecera `Host` decide qué sitio responde

Cuando escribes:

```text
http://docs.ejemplo.test/informes/
```

primero el nombre se resuelve a una dirección IP. Después el nombre también viaja dentro de la petición HTTP:

```http
GET /informes/ HTTP/1.1
Host: docs.ejemplo.test
```

Nginx puede utilizar `Host` para escoger un bloque `server`:

```nginx
server {
    listen 80;
    server_name docs.ejemplo.test;

    root /srv/www/docs;
    index index.html;

    location /informes/ {
        autoindex on;
    }
}
```

Las directivas principales son:

| Directiva | Función |
|---|---|
| `listen 80` | puerto en el que atiende el bloque |
| `server_name` | nombre o nombres que seleccionan el sitio |
| `root` | raíz de documentos |
| `index` | fichero que se busca al pedir un directorio |
| `location` | reglas aplicadas a determinadas rutas |
| `autoindex on` | permite listar un directorio sin índice |

Así pueden coexistir:

```text
web.ejemplo.test
→ sitio principal

docs.ejemplo.test
→ documentación e informes
```

Los dos nombres pueden apuntar a la misma dirección IP, utilizar el puerto 80 y terminar en el mismo servidor Nginx.

La secuencia completa puede visualizarse así:

```text
docs.ejemplo.test
      │
      │ DNS
      ▼
  203.0.113.10
      │
      │ petición HTTP
      │ Host: docs.ejemplo.test
      ▼
    Nginx
   ┌──┴───┐
   ▼      ▼
  web    docs
```

DNS permite llegar hasta la máquina correcta. La cabecera HTTP `Host` permite después decidir qué sitio debe responder.

### 5.2. Qué ocurre cuando ningún nombre coincide

Si ningún `server_name` coincide, Nginx utiliza un servidor por defecto. Si no se declara explícitamente, el comportamiento puede sorprender porque uno de los sitios configurados termina respondiendo a nombres que no eran suyos.

Es preferible expresar la decisión:

```nginx
server {
    listen 80 default_server;
    server_name _;
    return 404;
}
```

Ahora la política es clara:

```text
nombre conocido
→ sitio correspondiente

nombre desconocido
→ 404
```

---

## 🗜️ 6. Compresión y caché de contenido estático

Un servidor web también puede optimizar cómo entrega los ficheros.

### 6.1. Compresión

HTML, CSS y JavaScript son texto y suelen comprimirse muy bien.

El cliente anuncia qué codificaciones acepta:

```http
Accept-Encoding: gzip
```

Si Nginx decide comprimir la respuesta, devuelve:

```http
Content-Encoding: gzip
```

Una configuración habitual incluye también:

```nginx
gzip_vary on;
```

Esto añade la cabecera `Vary: Accept-Encoding`, útil para que una caché pueda distinguir entre la variante comprimida y la no comprimida de una misma respuesta.

La compresión se realiza **al servir la respuesta**. El fichero guardado en disco no cambia. El navegador recibe los bytes comprimidos y los descomprime automáticamente.

No tiene sentido aplicar gzip indiscriminadamente a formatos como JPEG o PNG, que ya están comprimidos.

Para inspeccionar cabeceras:

```bash
curl -I -H "Accept-Encoding: gzip" \
  http://web.ejemplo.test/css/estilos.css
```

Pero `-I` realiza una petición `HEAD`, por lo que no descarga el cuerpo. Para medir bytes reales necesitas una petición completa:

```bash
curl -s -H "Accept-Encoding: identity" -o /dev/null \
  -w "sin comprimir: %{size_download} bytes\n" \
  http://web.ejemplo.test/css/estilos.css

curl -s -H "Accept-Encoding: gzip" -o /dev/null \
  -w "comprimido:   %{size_download} bytes\n" \
  http://web.ejemplo.test/css/estilos.css
```

### 6.2. Caché del navegador

`Cache-Control` indica durante cuánto tiempo puede reutilizar el navegador una respuesta.

No todos los recursos tienen la misma volatilidad:

```text
HTML
→ cambia con frecuencia
→ caché corta o revalidación

CSS, JS, imágenes versionadas
→ cambian menos
→ caché más larga
```

En Nginx una configuración puede aplicar reglas por extensión. Por ejemplo:

```nginx
location ~* \.(css|js|png|jpg|jpeg|svg|webp)$ {
    expires 7d;
}
```

La directiva `expires` genera cabeceras de expiración y una política `Cache-Control` coherente con el plazo indicado.

El inconveniente de una caché larga aparece al publicar una versión nueva. Si un fichero mantiene el mismo nombre, un navegador podría conservar la copia anterior. Por eso en frontends reales es habitual generar nombres versionados o con huellas de contenido.

### 6.3. Estático y dinámico no se entregan igual

La separación entre servidor web y aplicación también ayuda a decidir cómo tratar cada respuesta:

| Recurso | Quién genera la respuesta | Tratamiento habitual |
|---|---|---|
| `/css/app.css` | Nginx lee un fichero | buen candidato a gzip y caché larga |
| `/img/logo.png` | Nginx lee un fichero | buen candidato a caché; gzip aporta poco |
| `/index.html` | Nginx lee un fichero | suele recibir una caché más corta |
| `/api/productos` | la aplicación genera la respuesta | la política depende del dato y de cuánto puede cambiar |

La regla importante no es:

```text
dinámico
→ nunca se cachea
```

sino:

```text
cada respuesta
→ necesita una política coherente con su naturaleza
```

Una API también puede utilizar caché, pero no conviene aplicar automáticamente a sus respuestas la misma política larga que a un CSS, JavaScript o una imagen versionada.

---


## 🧭 7. DNS: convertir nombres en direcciones

Los hosts virtuales funcionan por nombre, pero antes ese nombre debe conducir a la máquina correcta.

### 7.1. Resolución del sistema y `dig`

Una aplicación normal pregunta al **resolutor del sistema operativo**. Este puede consultar varias fuentes. En Linux, `/etc/hosts` puede proporcionar una respuesta local antes de acudir al DNS configurado.

Por eso esta línea:

```text
127.0.0.1 ejemplo.local
```

puede hacer que el navegador encuentre `ejemplo.local` sin que exista ningún registro DNS real.

`dig` funciona de otra forma: consulta DNS directamente. No utiliza `/etc/hosts` para resolver el nombre solicitado.

Así pueden darse simultáneamente estas dos situaciones:

```text
navegador
→ nombre funciona por /etc/hosts

dig
→ el DNS dice que ese nombre no existe
```

No hay contradicción. Están consultando fuentes diferentes.

### 7.2. Registros A, CNAME, TTL y `nip.io`

En esta sesión trabajarás principalmente con registros `A`, que relacionan un nombre con una dirección IPv4:

| Registro | Contiene | Uso típico |
|---|---|---|
| `A` | una dirección IPv4 | nombre que apunta directamente a una dirección |

También debes **reconocer** un registro `CNAME`: no contiene una dirección, sino otro nombre y se utiliza como alias. No necesitas configurarlo en esta práctica.

El **TTL** indica durante cuánto tiempo puede conservarse una respuesta DNS en caché antes de volver a consultarla.

Un TTL alto reduce consultas, pero hace más lenta una migración. Por eso, si se sabe que un nombre va a cambiar de destino, el TTL se reduce **antes** de la migración, con tiempo suficiente para que caduquen las respuestas antiguas.

Puedes inspeccionar una respuesta con:

```bash
dig web.127.0.0.1.nip.io
dig +short web.127.0.0.1.nip.io
```

Para el laboratorio utilizaremos `nip.io`, un servicio DNS comodín que permite codificar una dirección IP dentro de un nombre. Así:

```text
web.127.0.0.1.nip.io
docs.127.0.0.1.nip.io
```

pueden resolver a `127.0.0.1` sin registrar un dominio propio.

Esto nos permite practicar simultáneamente:

```text
DNS real
+
hosts virtuales por nombre
+
una sola máquina
```

En la siguiente sesión aplicarás la misma idea sobre la dirección pública de una instancia remota.

---

## 🎯 Qué debes saber hacer al salir de esta sesión

Al terminar, deberías poder:

- Explicar qué responsabilidad tiene un servidor web y qué sigue perteneciendo a la aplicación.
- Incorporar Nginx como servicio de entrada delante de una aplicación.
- Servir contenido desde una raíz de documentos.
- Montar configuración y contenido desde Compose en modo de solo lectura.
- Validar la configuración antes de recargarla.
- Configurar dos hosts virtuales por nombre sobre una misma dirección y puerto.
- Explicar la secuencia `DNS → dirección IP → Host HTTP → sitio`.
- Declarar un servidor por defecto para nombres no reconocidos.
- Aplicar y comprobar gzip sobre contenido textual.
- Aplicar una política de caché a recursos estáticos y explicar por qué no debe trasladarse automáticamente a cualquier respuesta dinámica.
- Utilizar `dig` para reconocer una respuesta DNS, una dirección IPv4 y su TTL.

Lo que basta con **reconocer**:

- diferencias generales entre Apache y Nginx;
- síntomas básicos relacionados con `404`, `403`, permisos y tipos MIME;
- qué representa un registro `CNAME`;
- por qué `/etc/hosts` puede afectar al navegador sin modificar lo que devuelve `dig`.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - Nginx puede convertirse en la puerta HTTP del sistema y servir directamente recursos estáticos.
    - El servidor web puede ser la única pieza publicada, mientras aplicación y base de datos permanecen dentro de la red interna.
    - `root`, `index` y los montajes determinan qué ficheros puede servir cada sitio.
    - Ante un fallo conviene distinguir síntomas: `404`, `403` y un tipo MIME incorrecto suelen apuntar a problemas diferentes.
    - La configuración se valida con `nginx -t` antes de recargarla.
    - DNS permite llegar a una dirección; después la cabecera HTTP `Host` permite seleccionar el host virtual.
    - Un `default_server` explícito evita que un nombre desconocido muestre accidentalmente otro sitio.
    - Gzip reduce los bytes enviados para contenido textual. `Vary: Accept-Encoding` permite distinguir variantes comprimidas y no comprimidas en cachés.
    - Los recursos estáticos suelen ser buenos candidatos para caché prolongada; una respuesta dinámica necesita una política acorde con la naturaleza del dato.
    - Un registro `A` relaciona nombre e IPv4. Un `CNAME` representa un alias y basta con reconocerlo en esta sesión.
    - El TTL indica cuánto tiempo puede conservarse una respuesta DNS en caché.
    - El resolutor del sistema puede utilizar `/etc/hosts`; `dig` consulta DNS directamente.


---

En la actividad aplicarás estos patrones sobre el proyecto del módulo, pero los conceptos de esta sesión son generales: dos sitios pueden compartir servidor, dirección y puerto; DNS conduce hasta la máquina; HTTP selecciona el sitio; y el servidor web puede aplicar políticas específicas a los recursos estáticos. El reenvío de `/api/` se mantendrá como una caja negra hasta la siguiente sesión.
