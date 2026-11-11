# Actividad 3.5: El backend por dentro

## 🧩 Contexto

Durante las actividades anteriores has trabajado principalmente con la entrada del sistema:

```text
Internet
   │
   │ HTTPS
   ▼
Nginx
   │
   └── /api/
        │
        ├── app-1
        ├── app-2
        └── app-3
```

Hasta ahora cada `app-*` se ha comportado casi como una caja negra:

```text
petición entra
      ↓
Escaparate responde
```

En esta actividad vas a abrir esa caja para estudiar tres cuestiones relacionadas con la ejecución de aplicaciones web:

```text
cómo se ejecuta el WAR
        ↓
dónde vive el estado de sesión
        ↓
qué ocurre al aumentar el número de réplicas
```

Escaparate seguirá utilizando como arquitectura final:

```text
Spring Boot
+
Tomcat embebido
+
contenedor Docker
```

Utilizarás temporalmente un **Tomcat externo** para comparar ambos modelos, reproducirás después un problema real de sesiones al balancear entre réplicas y terminarás comparando una y tres copias bajo la misma carga.

!!! abstract "Cómo vas a trabajar"
    **Observar → comparar → provocar → externalizar → medir → interpretar**

    La configuración específica de Spring Session, Redis y el laboratorio de Tomcat se proporciona cuando no aporta valor que tengas que deducirla.

    Tu trabajo será aplicar los cambios correctamente y explicar **qué problema resuelve cada uno**.

!!! info "Una actividad, dos sesiones"
    La Actividad 3.5 se desarrolla en dos sesiones pero constituye **una única actividad y una única entrega**.

    ```text
    SESIÓN 10
    ────────────────────────────────
    Tomcat embebido y externo
              ↓
    contexto del WAR
              ↓
    sesión local entre réplicas

    SESIÓN 11
    ────────────────────────────────
    Spring Session + Redis
              ↓
    sesión compartida
              ↓
    1 réplica frente a 3 con k6
              ↓
    memoria final del Tema 3
    ```

## 🎯 Qué vas a practicar

Al terminar la actividad deberías ser capaz de:

- explicar qué pieza ejecuta realmente Escaparate;
- diferenciar Tomcat embebido y Tomcat externo;
- identificar los directorios principales de una instalación de Tomcat;
- relacionar el nombre de un WAR con su contexto;
- comprobar qué ocurre cuando una sesión permanece en memoria de una réplica;
- externalizar el estado de sesión a un servicio compartido;
- comprobar que una misma sesión funciona aunque cambie la réplica;
- comparar throughput, p95 y errores bajo dos configuraciones;
- explicar por qué tres réplicas en una misma EC2 no equivalen a tres veces más recursos;
- completar la memoria conjunta de publicación y ejecución del Tema 3.

## 🧰 Requisitos previos

Necesitas:

- Actividad 3.4 terminada y fusionada en `main`;
- Escaparate funcionando en EC2;
- Nginx como única entrada pública;
- `app-1`, `app-2` y `app-3`;
- PostgreSQL;
- volumen `imagenes-compartidas`;
- pila de observabilidad disponible;
- `entregas/tema3/memoria-publicacion-ejecucion.md` iniciado en la actividad anterior;
- acceso desde el equipo del aula al repositorio y a Docker;
- acceso SSH a la EC2.

!!! info "Dos lugares de trabajo"
    Durante esta actividad aparecerán dos indicaciones:

    ```text
    EQUIPO LOCAL
    → modificar código
    → ejecutar Maven
    → construir y publicar imágenes
    → ejecutar k6

    EC2
    → actualizar el despliegue
    → Tomcat externo
    → Redis
    → pruebas de sesión
    ```

    Esto mantiene el mismo modelo de despliegue de la Actividad 3.3:

    ```text
    el servidor ejecuta imágenes
    no recompila la aplicación
    ```

!!! info "Evidencias"
    Solo se piden **tres evidencias**.

    Las tablas y reflexiones se escriben directamente en `actividad-3.5.md`.

## 🧪 Sesión 10: abre el servidor de aplicaciones

En esta primera parte comprobarás qué contiene realmente una réplica y reproducirás un problema de estado local.

### 1.1. Prepara la rama en el equipo local

Desde la raíz del repositorio:

```bash
git switch main
git pull --ff-only
git switch -c sesion-10
```

Crea:

```text
entregas/
└── tema3/
    └── actividad-3.5/
        ├── actividad-3.5.md
        └── img/
```

### 1.2. Comprueba cómo está preparado Escaparate

Entra en:

```bash
cd escaparate
```

Busca en `pom.xml`:

```bash
grep -n '<packaging>war</packaging>' pom.xml
grep -n -A4 -B2 'spring-boot-starter-tomcat' pom.xml
```

Debes reconocer:

```xml
<packaging>war</packaging>
```

y Tomcat con alcance:

```xml
<scope>provided</scope>
```

La idea es:

```text
mismo WAR
   │
   ├── ejecutable con Spring Boot + Tomcat embebido
   │
   └── desplegable en un Tomcat externo
```

!!! question "Interpreta"
    El proyecto lleva utilizando Tomcat desde el Tema 2 aunque nunca hayas instalado Tomcat manualmente.

    ¿Dónde estaba?

### 1.3. Añade un controlador docente de sesión

Necesitamos una forma pequeña de crear una sesión y comprobar después si una réplica sigue conociéndola.

Crea:

```text
escaparate/src/main/java/es/escaparate/infraestructura/SesionDemoController.java
```

con:

```java
package es.escaparate.infraestructura;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sesion")
public class SesionDemoController {

    @PostMapping("/abrir")
    public Map<String, String> abrir(
            @RequestParam(defaultValue = "demo") String usuario,
            HttpServletRequest request) {

        HttpSession session = request.getSession(true);
        session.setAttribute("usuario", usuario);

        return Map.of(
                "usuario", usuario,
                "sesion", session.getId()
        );
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, String>> estado(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("usuario") == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("estado", "sesion-no-encontrada"));
        }

        return ResponseEntity.ok(Map.of(
                "estado", "sesion-encontrada",
                "usuario", session.getAttribute("usuario").toString(),
                "sesion", session.getId()
        ));
    }
}
```

No necesitas estudiar Spring MVC en esta actividad.

Fíjate únicamente en:

```java
request.getSession(true)
```

que crea una sesión si es necesario, frente a:

```java
request.getSession(false)
```

que solo devuelve una sesión que ya exista en ese proceso.

### 1.4. Comprueba el cambio

Desde `escaparate/`:

```bash
./mvnw test
```

No continúes si la aplicación no compila.

## 📦 Paso 2: construye una imagen con sesión local

Todavía **no** vas a añadir Redis.

Queremos observar primero el comportamiento normal de `HttpSession` cuando cada JVM conserva sus sesiones en memoria.

### 2.1. Construye y publica desde el equipo local

Desde la raíz del repositorio:

```bash
docker build \
  -f practicas/docker/app/Dockerfile \
  -t ghcr.io/<usuario>/escaparate:sesion-10-local \
  escaparate
```

Publica:

```bash
docker push \
  ghcr.io/<usuario>/escaparate:sesion-10-local
```

### 2.2. Registra el código, no el valor local de `.env`

```bash
git add escaparate
git commit -m "Añade laboratorio de sesiones HTTP"
git push -u origin sesion-10
```

La imagen se selecciona mediante:

```text
IMAGEN_APP
```

en `.env`.

No necesitas cambiar tres veces `image:` en `compose.yaml`, porque desde la Actividad 3.2 las réplicas comparten:

```yaml
image: ${IMAGEN_APP:?define IMAGEN_APP en .env}
```

## 🏗️ Paso 3: ejecuta el mismo WAR en Tomcat externo

A partir de este paso trabaja por SSH en la EC2.

El Tomcat externo será un **laboratorio temporal**. No sustituirá a `app-1`, `app-2` y `app-3` como arquitectura final.

### 3.1. Actualiza la rama en EC2

En el clon de EC2:

```bash
git fetch origin
git switch sesion-10
git pull --ff-only
```

Descarga la imagen:

```bash
docker pull \
  ghcr.io/<usuario>/escaparate:sesion-10-local
```

### 3.2. Extrae el WAR de la imagen

Desde la raíz del repositorio:

```bash
mkdir -p practicas/aplicaciones/tomcat

CID=$(docker create \
  ghcr.io/<usuario>/escaparate:sesion-10-local)

docker cp \
  "$CID:/app/app.war" \
  practicas/aplicaciones/tomcat/escaparate.war

docker rm "$CID"
```

Comprueba:

```bash
ls -lh \
  practicas/aplicaciones/tomcat/escaparate.war
```

!!! question "Interpreta"
    Acabas de utilizar dos artefactos diferentes:

    ```text
    imagen Docker
    WAR
    ```

    ¿Qué aporta cada uno al despliegue?

### 3.3. Crea el Compose del laboratorio

Crea:

```text
practicas/compose/compose.sesion10-lab.yaml
```

con:

```yaml
services:

  tomcat-lab:
    image: tomcat:11.0.25-jre21-temurin-noble

    environment:
      DB_HOST: bd
      DB_PORT: "5432"
      DB_NAME: ${BD_NOMBRE:?define BD_NOMBRE en .env}
      DB_USER: ${BD_USUARIO:?define BD_USUARIO en .env}
      SPRING_CONFIG_IMPORT: "configtree:/run/secrets/"

      APP_STORAGE_TYPE: filesystem
      APP_STORAGE_PATH: /tmp/uploads
      APP_INSTANCE_NAME: tomcat-lab

    secrets:
      - source: bd_clave
        target: spring.datasource.password

    volumes:
      - ../aplicaciones/tomcat/escaparate.war:/usr/local/tomcat/webapps/escaparate.war:ro

    depends_on:
      bd:
        condition: service_healthy
```

!!! info "Reutilizamos el mismo contrato de configuración"
    No hemos vuelto a introducir una contraseña como variable de entorno.

    El Tomcat externo recibe la configuración de base de datos igual que las réplicas:

    ```text
    .env
    → nombre de BD y usuario

    secret bd_clave
    → spring.datasource.password
    ```

### 3.4. Levanta solo el laboratorio

Desde `practicas/compose/`:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  config --quiet
```

Después:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  up -d tomcat-lab
```

Observa:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  logs --tail=100 tomcat-lab
```

### 3.5. Mira la estructura de Tomcat

Ejecuta:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  exec tomcat-lab \
  sh -c 'ls -ld conf lib webapps && ls -1 conf | head'
```

Relaciona:

| Elemento | Función |
|---|---|
| `conf/` | configuración del servidor |
| `webapps/` | aplicaciones desplegadas |
| `lib/` | bibliotecas compartidas por el servidor |

No necesitas modificar `server.xml`, `context.xml` ni las bibliotecas.

El objetivo es **identificar físicamente** las piezas descritas en la teoría.

### 3.6. Comprueba el contexto creado por `escaparate.war`

Desde el contenedor `web`:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  exec web \
  wget -q -S -O /dev/null \
  http://tomcat-lab:8080/escaparate/api/salud/listo \
  2>&1 | grep 'HTTP/'
```

Después:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  exec web \
  wget -q -S -O /dev/null \
  http://tomcat-lab:8080/api/salud/listo \
  2>&1 | grep 'HTTP/'
```

Debes reconocer:

```text
escaparate.war
→ /escaparate
```

Por tanto, la primera ruta debe responder correctamente y la segunda no corresponde todavía al contexto desplegado.

### 3.7. Despliega el mismo WAR como aplicación raíz

Edita únicamente el montaje:

```yaml
volumes:
  - ../aplicaciones/tomcat/escaparate.war:/usr/local/tomcat/webapps/ROOT.war:ro
```

Recrea:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  up -d --force-recreate tomcat-lab
```

Comprueba:

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  exec web \
  wget -q -S -O /dev/null \
  http://tomcat-lab:8080/api/salud/listo \
  2>&1 | grep 'HTTP/'
```

Ahora:

```text
ROOT.war
→ /
```

!!! question "Reflexiona"
    Nginx acostumbra a reenviar:

    ```text
    /api/...
    ```

    ¿Por qué desplegar el WAR bajo `/escaparate` obligaría a ajustar las rutas entre Nginx y Tomcat?

**Evidencia 1:** una captura donde se reconozcan la estructura principal de Tomcat y el cambio:

```text
escaparate.war → /escaparate
ROOT.war       → /
```

### 3.8. Detén el laboratorio

```bash
docker compose \
  -f compose.yaml \
  -f compose.sesion10-lab.yaml \
  stop tomcat-lab
```

A partir de aquí vuelves a la arquitectura normal:

```text
Spring Boot + Tomcat embebido
```

## 🍪 Paso 4: reproduce una sesión que pertenece a una réplica

Ahora desplegarás la imagen `sesion-10-local` en las tres aplicaciones.

### 4.1. Cambia únicamente `IMAGEN_APP` en EC2

Edita:

```text
practicas/compose/.env
```

y establece:

```text
IMAGEN_APP=ghcr.io/<usuario>/escaparate:sesion-10-local
```

Desde `practicas/compose/`:

```bash
docker compose pull app-1 app-2 app-3
docker compose up -d app-1 app-2 app-3
docker compose ps
```

Comprueba individualmente las tres aplicaciones como hiciste en la Actividad 3.2 antes de continuar.

### 4.2. Haz visible temporalmente el backend utilizado

En el `location /api/` del bloque HTTPS de Nginx añade:

```nginx
add_header X-Destino $upstream_addr always;
```

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Recupera la URL pública:

```bash
read -r -p 'IPv4 pública actual de EC2: ' IP_PUBLICA
URL="https://escaparate.${IP_PUBLICA}.nip.io"
```

### 4.3. Crea una sesión

En EC2:

```bash
rm -f /tmp/sesion.txt

curl -i \
  -c /tmp/sesion.txt \
  -X POST \
  "$URL/api/sesion/abrir?usuario=demo"
```

Localiza:

```text
Set-Cookie: JSESSIONID=...
```

La cookie contiene un identificador de sesión.

### 4.4. Reutiliza la misma cookie

Ejecuta:

```bash
for i in $(seq 1 9); do
  echo "Petición $i"

  curl -s \
    -b /tmp/sesion.txt \
    -o /dev/null \
    -D - \
    "$URL/api/sesion/estado" \
    | grep -iE '^HTTP/|^X-Destino'

  echo
done
```

Completa en `actividad-3.5.md`:

| Petición | HTTP | `X-Destino` | ¿Sesión encontrada? |
|---|---:|---|---|
| 1 | | | |
| 2 | | | |
| 3 | | | |
| … | | | |

Deberías observar respuestas distintas al cambiar de backend.

!!! question "Formula la explicación"
    La cookie que envía el cliente es siempre la misma.

    Explica por qué una réplica puede responder `200` mientras otra responde `401`.

Relaciona este problema con la Actividad 3.2:

```text
imagen local
→ fichero en filesystem de una réplica

sesión local
→ dato en memoria de una JVM
```

En los dos casos el problema aparece cuando algo que necesita ser compartido **permanece dentro de una copia concreta**.

## 🗓️ Fin de la Sesión 10

Antes de terminar esta primera sesión, comprueba:

```bash
git status
```

El controlador y `compose.sesion10-lab.yaml` deben quedar versionados en la rama.

Registra el laboratorio si todavía no lo has hecho:

```bash
git add \
  escaparate \
  practicas/compose/compose.sesion10-lab.yaml

git commit -m "Compara Tomcat externo y sesión local"
git push
```

No fusiones todavía la rama.

La Actividad 3.5 continúa en la siguiente sesión.

## 🧠 Sesión 11: comparte el estado y mide

La segunda parte comienza desde el problema observado:

```text
sesión
→ memoria de una réplica
→ falla al cambiar de backend
```

Ahora moverás ese estado fuera de las JVM.

### 5.1. Recupera la rama en el equipo local

```bash
git switch sesion-10
git pull --ff-only
```

Entra en:

```bash
cd escaparate
```

### 5.2. Añade Spring Session Redis

En `pom.xml`, dentro de `<dependencies>`, añade:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-session-data-redis</artifactId>
</dependency>
```

No indiques versión: la gestiona Spring Boot.

El controlador no cambia:

```text
SesionDemoController
→ sigue utilizando HttpSession
```

Lo que cambia es quién guarda realmente el estado.

### 5.3. Comprueba y construye la imagen final

Ejecuta:

```bash
./mvnw test
```

Desde la raíz del repositorio:

```bash
docker build \
  -f practicas/docker/app/Dockerfile \
  -t ghcr.io/<usuario>/escaparate:sesion-10 \
  escaparate
```

Publica:

```bash
docker push \
  ghcr.io/<usuario>/escaparate:sesion-10
```

Registra el cambio:

```bash
git add escaparate/pom.xml
git commit -m "Añade soporte de sesiones compartidas"
git push
```

## 🟥 Paso 6: añade Redis al despliegue

Redis será un servicio interno. No necesita publicar ningún puerto hacia la EC2.

### 6.1. Añade el servicio

En `practicas/compose/compose.yaml`, añade dentro de `services:`:

```yaml
  redis:
    image: redis:8-alpine

    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 2s
      timeout: 2s
      retries: 15
```

No añadas:

```yaml
ports:
```

### 6.2. Amplía las tres réplicas sin sustituir su configuración anterior

En `app-1`, `app-2` y `app-3`, conserva:

- conexión a PostgreSQL;
- `SPRING_CONFIG_IMPORT`;
- secreto `bd_clave`;
- `APP_STORAGE_TYPE`;
- `APP_STORAGE_PATH`;
- `APP_INSTANCE_NAME`;
- volumen `imagenes-compartidas`.

Añade únicamente:

```yaml
environment:
  # ... configuración existente ...
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: "6379"
```

Y amplía `depends_on`:

```yaml
depends_on:
  bd:
    condition: service_healthy
  redis:
    condition: service_healthy
```

!!! warning "No regreses a contraseñas en variables"
    La incorporación de Redis no cambia cómo se conecta Escaparate a PostgreSQL.

    Debes seguir utilizando:

    ```yaml
    SPRING_CONFIG_IMPORT: "configtree:/run/secrets/"
    ```

    y:

    ```yaml
    secrets:
      - source: bd_clave
        target: spring.datasource.password
    ```

### 6.3. Conserva el almacenamiento compartido existente

Las tres aplicaciones siguen utilizando:

```yaml
volumes:
  - imagenes-compartidas:/data/uploads
```

y la declaración global existente:

```yaml
volumes:
  datos-bd:
  imagenes-compartidas:
```

No crees otro volumen con un nombre diferente.

### 6.4. Valida antes de publicar

Desde `practicas/compose/`:

```bash
docker compose config --quiet
```

Si no hay errores:

```bash
git add compose.yaml
git commit -m "Añade Redis al despliegue"
git push
```

## 🔄 Paso 7: despliega la sesión compartida en EC2

### 7.1. Actualiza código y configuración

En EC2:

```bash
git pull --ff-only
```

Edita el `.env` local y establece:

```text
IMAGEN_APP=ghcr.io/<usuario>/escaparate:sesion-10
```

Descarga y aplica:

```bash
docker compose pull app-1 app-2 app-3 redis
docker compose up -d
docker compose ps
```

Comprueba Redis:

```bash
docker compose exec redis redis-cli ping
```

Debes obtener:

```text
PONG
```

### 7.2. Crea una sesión nueva

Elimina la cookie anterior:

```bash
rm -f /tmp/sesion.txt
```

Crea una sesión:

```bash
curl -i \
  -c /tmp/sesion.txt \
  -X POST \
  "$URL/api/sesion/abrir?usuario=demo"
```

### 7.3. Comprueba que Redis contiene sesiones

Ejecuta:

```bash
docker compose exec redis \
  redis-cli --scan --pattern 'spring:session*'
```

Deben aparecer claves relacionadas con Spring Session.

### 7.4. Repite la prueba entre réplicas

Ejecuta:

```bash
for i in $(seq 1 9); do
  echo "Petición $i"

  curl -s \
    -b /tmp/sesion.txt \
    -o /dev/null \
    -D - \
    "$URL/api/sesion/estado" \
    | grep -iE '^HTTP/|^X-Destino'

  echo
done
```

Ahora deben cumplirse simultáneamente:

```text
X-Destino
→ cambia entre réplicas

HTTP
→ siempre 200
```

Completa:

| Estado de sesión | ¿Cambia `X-Destino`? | Resultado HTTP |
|---|---|---|
| memoria local | sí | mezcla de `200` y `401` |
| Redis | sí | `200` |

**Evidencia 2:** comparación entre el comportamiento de la sesión local y la sesión compartida, mostrando además claves `spring:session...` en Redis.

!!! question "Interpreta"
    1. ¿Qué pieza conoce ahora la sesión?
    2. ¿Por qué `app-2` puede utilizar una sesión creada originalmente mientras respondía `app-1`?
    3. Si un contenedor de aplicación se recrea, ¿qué ocurre con el estado guardado en Redis?

## 📈 Paso 8: compara una y tres réplicas

Ahora harás una prueba pequeña y controlada.

No es un estudio de capacidad de producción.

Compararás únicamente:

```text
misma EC2
mismo endpoint
mismo equipo generador
misma duración
misma carga

3 réplicas
vs.
1 réplica
```

### 8.1. Prepara el script en el equipo local

Crea:

```text
practicas/aplicaciones/prueba-carga.js
```

con:

```javascript
import http from 'k6/http';
import { check } from 'k6';

export default function () {
    const respuesta = http.get(
        `${__ENV.URL}/api/carga?ms=50`
    );

    check(respuesta, {
        'estado 200': (r) => r.status === 200,
    });
}
```

El endpoint provoca una carga pequeña y repetible.

Observarás:

```text
req/s
p95
errores
```

### 8.2. Mide primero las tres réplicas

Desde el **equipo local**:

```bash
read -r -p 'IPv4 pública actual de EC2: ' IP_PUBLICA
URL="https://escaparate.${IP_PUBLICA}.nip.io"
```

Ejecuta:

```bash
docker run --rm -i \
  -e URL="$URL" \
  grafana/k6 run \
  --vus 20 \
  --duration 30s \
  - < practicas/aplicaciones/prueba-carga.js
```

Anota:

| Configuración | req/s | p95 | errores |
|---|---:|---:|---:|
| 3 réplicas | | | |

### 8.3. Deja temporalmente una sola réplica

En EC2, desde `practicas/compose/`, guarda la configuración de Nginx:

```bash
cp \
  ../nginx/conf.d/sitios.conf \
  /tmp/sitios-tres-replicas.conf
```

En:

```nginx
upstream backend_pool {
```

deja temporalmente únicamente:

```nginx
server app-1:8080 resolve;
```

Detén:

```bash
docker compose stop app-2 app-3
```

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Comprueba:

```bash
for i in $(seq 1 3); do
  curl -s "$URL/api/instancia"
  echo
done
```

Solo debe aparecer:

```text
app-1
```

### 8.4. Repite exactamente la misma prueba

Desde el equipo local vuelve a ejecutar:

```bash
docker run --rm -i \
  -e URL="$URL" \
  grafana/k6 run \
  --vus 20 \
  --duration 30s \
  - < practicas/aplicaciones/prueba-carga.js
```

Completa:

| Configuración | req/s | p95 | errores |
|---|---:|---:|---:|
| 3 réplicas | | | |
| 1 réplica | | | |

!!! question "Interpreta los resultados"
    Responde:

    1. ¿ha aumentado el throughput al utilizar tres réplicas?;
    2. ¿qué ha ocurrido con el p95?;
    3. ¿se ha multiplicado la capacidad por tres?;
    4. ¿qué recursos físicos siguen compartiendo las tres JVM?;
    5. además del rendimiento, ¿qué utilidad aportan varias réplicas?

!!! warning "Qué demuestra y qué no demuestra esta prueba"
    k6 se ejecuta desde el equipo local para no competir directamente por CPU con la EC2.

    Aun así, este laboratorio solo permite comparar:

    ```text
    una configuración
    frente a otra
    ```

    No permite afirmar:

    ```text
    "Escaparate soporta X usuarios en producción"
    ```

**Evidencia 3:** resumen de ambas ejecuciones de k6 y tabla `1 réplica vs. 3 réplicas` con una conclusión breve.

## ♻️ Paso 9: restaura la arquitectura final

La actividad debe terminar con las tres réplicas disponibles.

### 9.1. Restaura Nginx

En EC2:

```bash
cp \
  /tmp/sitios-tres-replicas.conf \
  ../nginx/conf.d/sitios.conf
```

Arranca:

```bash
docker compose start app-2 app-3
```

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Comprueba:

```bash
for i in $(seq 1 9); do
  curl -s "$URL/api/instancia"
  echo
done
```

Deben volver a aparecer las tres.

### 9.2. Elimina la cabecera temporal

Quita de la configuración versionada:

```nginx
add_header X-Destino $upstream_addr always;
```

Valida y recarga de nuevo:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

!!! info "Por qué la retiramos"
    `X-Destino` ha sido útil como instrumento de diagnóstico.

    No necesitamos publicar permanentemente al cliente las direcciones internas de los backends.

### 9.3. Comprueba la arquitectura final

El resultado debe ser:

```text
Internet
   │ HTTPS
   ▼
Nginx
   │
   ├── app-1 ─┐
   ├── app-2 ─┼── Redis
   └── app-3 ─┘
        │
        ├── PostgreSQL
        └── imagenes-compartidas
```

Tomcat externo ya no forma parte de la arquitectura.

## 📝 Paso 10: completa la memoria del Tema 3

Abre:

```text
entregas/tema3/memoria-publicacion-ejecucion.md
```

La Actividad 3.4 ya creó:

```markdown
## Publicación, seguridad y observabilidad
```

Añade ahora:

```markdown
## Servidor de aplicaciones, estado y rendimiento
```

Con unas **200–250 palabras** y una tabla breve es suficiente.

### 10.1. Explica las decisiones principales

Debe quedar claro:

1. qué servidor ejecutan normalmente las tres aplicaciones;
2. qué diferencia observaste entre Tomcat embebido y externo;
3. por qué `escaparate.war` y `ROOT.war` producen rutas diferentes;
4. dónde vivía inicialmente `HttpSession`;
5. por qué la sesión fallaba al cambiar de réplica;
6. qué consiguió Spring Session + Redis;
7. qué mostró la comparación k6 y qué limitaciones tiene.

### 10.2. Incluye una tabla de síntesis

Por ejemplo:

| Aspecto | Resultado |
|---|---|
| Modelo habitual | |
| WAR en Tomcat externo | |
| Sesión local | |
| Sesión con Redis | |
| 1 vs. 3 réplicas | |

Actualiza también el esquema final:

```text
Internet
   │ HTTPS
   ▼
Nginx
   │
   ├── app-1 ─┐
   ├── app-2 ─┼── Redis
   └── app-3 ─┘
        │
        ├── PostgreSQL
        └── volumen compartido

logs
 ↓
Fluent Bit
 ↓
Elasticsearch
 ↓
Kibana
```

## 🔀 Paso 11: documenta e integra

### 11.1. Revisa `actividad-3.5.md`

Debe contener:

- interpretación WAR frente a imagen;
- reflexión sobre el contexto del WAR;
- tabla de sesión local;
- explicación del fallo al cambiar de réplica;
- comparación sesión local / Redis;
- tabla k6;
- interpretación del rendimiento;
- las tres evidencias;
- incidencias relevantes y cómo las resolviste.

### 11.2. Revisa el estado final

En EC2 comprueba:

```bash
docker compose ps
```

Deben estar disponibles:

```text
web
app-1
app-2
app-3
bd
redis
```

La pila de observabilidad puede permanecer activa.

Comprueba también:

```bash
curl -fsS "$URL/api/salud/listo"
```

y:

```bash
docker compose exec redis redis-cli ping
```

### 11.3. Publica la rama

En el equipo local:

```bash
git status
git add .
git status
```

Comprueba que no vas a registrar:

```text
.env
bd_clave.txt
certificados privados
.htpasswd
```

Registra:

```bash
git commit -m "Completa servidor de aplicaciones y sesiones"
git push
```

Abre:

```text
sesion-10 → main
```

Comprueba los checks y fusiona mediante:

```text
Create a merge commit
```

Finalmente:

```bash
git switch main
git pull --ff-only
```

## 🔧 Si algo falla

Utiliza el síntoma para decidir qué capa revisar.

| Síntoma | Causa probable | Qué comprobar |
|---|---|---|
| `./mvnw test` falla tras crear el controlador | ruta, paquete o imports incorrectos | ubicación exacta y código proporcionado |
| No aparece `/app/app.war` en la imagen | Dockerfile diferente al esperado | inspecciona la imagen y el Dockerfile de la Actividad 2.2 |
| `tomcat-lab` no conecta con PostgreSQL | se ha perdido configuración o secreto | `DB_HOST`, `BD_NOMBRE`, `BD_USUARIO`, `SPRING_CONFIG_IMPORT` y `bd_clave` |
| `/api/...` devuelve `404` con `escaparate.war` | aplicación desplegada en otro contexto | prueba `/escaparate/api/...` |
| `ROOT.war` continúa respondiendo bajo `/escaparate` | Tomcat conserva despliegue anterior | recrea el contenedor del laboratorio |
| Las nueve peticiones de sesión devuelven siempre `401` | la creación o cookie no se conservó | `/tmp/sesion.txt`, `Set-Cookie` y `curl -b` |
| Todas las peticiones devuelven `200` antes de Redis | las peticiones no están cambiando realmente de réplica | revisa `X-Destino` y las tres aplicaciones |
| Redis no responde `PONG` | servicio no está preparado | `docker compose ps` y logs de Redis |
| No aparecen claves `spring:session*` | imagen antigua o Redis no configurado | `IMAGEN_APP`, dependencia Maven y variables Redis |
| Con Redis siguen apareciendo `401` | alguna réplica usa imagen/configuración antigua | `docker compose ps`, `docker inspect` y `.env` |
| k6 produce muchos errores TLS | nombre/certificado no corresponde a la IP actual | URL pública y certificado de la Actividad 3.3 |
| La prueba con una réplica sigue mostrando otras | Nginx no se recargó o apps siguen en upstream | `nginx -t`, reload y `backend_pool` |
| Al final solo queda `app-1` | no se restauró el laboratorio | copia de seguridad de `sitios.conf` y `docker compose start app-2 app-3` |

!!! tip "Orden de diagnóstico para una sesión"
    Cuando la cookie existe pero el estado no aparece, piensa:

    ```text
    cliente envía cookie
            ↓
    Nginx elige backend
            ↓
    backend busca sesión
            ↓
    ¿memoria local o Redis?
    ```

## 📦 Qué se entrega

Antes de terminar, comprueba:

- [ ] `actividad-3.5.md` con resultados y reflexiones;
- [ ] exactamente tres evidencias;
- [ ] controlador docente de sesión versionado;
- [ ] laboratorio de Tomcat externo versionado;
- [ ] estructura básica de Tomcat identificada;
- [ ] `escaparate.war` probado bajo `/escaparate`;
- [ ] `ROOT.war` probado bajo `/`;
- [ ] sesión local reproducida con cambios de backend;
- [ ] Redis interno y sin puerto publicado;
- [ ] las tres réplicas conservan secretos y `imagenes-compartidas`;
- [ ] sesión compartida estable desde distintos backends;
- [ ] claves de Spring Session visibles en Redis;
- [ ] comparación k6 de una y tres réplicas;
- [ ] arquitectura final restaurada con tres réplicas;
- [ ] `X-Destino` temporal eliminado;
- [ ] `memoria-publicacion-ejecucion.md` completada;
- [ ] Pull Request `sesion-10 → main` fusionada.

## ✅ Cierre

Durante el Tema 3 has recorrido el sistema desde fuera hacia dentro.

Primero construiste la entrada:

```text
DNS
 ↓
Nginx
 ↓
HTTPS
 ↓
balanceo
```

Después hiciste observable su comportamiento:

```text
logs
 ↓
Fluent Bit
 ↓
Elasticsearch
 ↓
Kibana
```

Y finalmente has abierto el proceso que ejecuta la aplicación:

```text
Spring Boot
 ↓
Tomcat
 ↓
HttpSession
 ↓
Redis
```

La idea central de esta actividad no es aprender comandos de Redis ni convertir Tomcat externo en la arquitectura del proyecto.

Es reconocer dos decisiones profesionales:

```text
una aplicación necesita un entorno que la ejecute
```

y:

```text
si varias réplicas necesitan el mismo estado,
ese estado no puede depender de una única copia
```

El Tema 3 termina con una arquitectura publicable, protegida, observable y capaz de mantener estado de sesión entre réplicas.

En el siguiente tema empezarás a automatizar operaciones que hasta ahora has ejecutado manualmente: construir, comprobar, publicar y desplegar.
