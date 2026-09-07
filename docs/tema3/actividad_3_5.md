# 🧪 Actividad 3.5: El backend por dentro

## Contexto

Escaparate ya está publicado por HTTPS y Nginx reparte `/api/` entre tres copias:

```text
Internet
   │
   ▼
Nginx
   │
   ├── app-1
   ├── app-2
   └── app-3
```

Hasta ahora esas tres copias han funcionado como una caja negra. Sabes que responden en `8080`, pero no hemos dedicado tiempo a observar qué servidor ejecuta realmente la aplicación ni qué consecuencias tiene guardar estado dentro de cada proceso.

Hoy vas a responder tres preguntas:

```text
¿qué ejecuta realmente cada app?
¿dónde debe vivir una sesión si hay varias copias?
¿tener tres copias significa tener tres veces más capacidad?
```

El Tomcat externo será únicamente un **laboratorio temporal**. La arquitectura final seguirá utilizando Spring Boot con Tomcat embebido.

!!! info "Tiempo orientativo"
    La actividad está pensada para unos **100 minutos**:

    ```text
    WAR y Tomcat externo       15–20 min
    incidencia de sesión       15 min
    sesión compartida          20 min
    comparación k6             20–25 min
    conclusiones + Git         15–20 min
    ```

## Qué vas a practicar

- **Comparar** el mismo WAR ejecutado con Tomcat embebido y desplegado en un Tomcat externo.
- **Comprobar** cómo el nombre del WAR afecta a la ruta de contexto.
- **Reproducir** una pérdida de sesión al balancear entre varias copias.
- **Externalizar** la sesión a Redis sin desactivar el reparto.
- **Comprobar** que una misma sesión funciona aunque cambie la réplica.
- **Comparar** una y tres copias con la misma prueba k6.
- **Interpretar** throughput, p95 y errores sin confundir el laboratorio con producción.

## Requisitos previos

- Actividad 3.4 terminada y fusionada en `main`.
- El despliegue de la sesión 9 funcionando en EC2.
- `app-1`, `app-2` y `app-3` detrás de Nginx.
- PostgreSQL operativo.
- Pila de observabilidad disponible.
- `memoria-publicacion-ejecucion.md` creada en la sesión anterior.
- Paquete `actividad-3.5-soporte.zip`.
- Imagen docente `ghcr.io/<usuario>/escaparate:sesion-10`, preparada para esta actividad.

El paquete incluye:

```text
practicas/
├── compose/
│   └── compose.aplicaciones.yaml
└── aplicaciones/
    ├── extrae-war.sh
    └── prueba-carga.js
```

`compose.aplicaciones.yaml` contiene:

- un servicio Redis preparado;
- un Tomcat 11 de laboratorio compatible con el WAR del proyecto;
- los montajes necesarios para el experimento;
- la referencia a la imagen docente de esta sesión.

No tienes que construir una imagen nueva ni modificar código Java. La imagen `sesion-10` añade únicamente el pequeño soporte necesario para experimentar con `HttpSession` y mantiene el resto de Escaparate igual que en las sesiones anteriores.

Prepara:

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

---

# Paso 1: Ejecuta el mismo WAR de otra forma

Hasta ahora cada `app-*` ejecuta el WAR de Escaparate de forma autónoma:

```text
java -jar app.war
       ↓
Spring Boot
       ↓
Tomcat embebido
```

Vamos a extraer **ese mismo artefacto** de la imagen y entregárselo a un Tomcat externo.

Desde el repositorio en EC2:

```bash
chmod +x practicas/aplicaciones/extrae-war.sh
./practicas/aplicaciones/extrae-war.sh
```

Comprueba que el script ha dejado:

```text
practicas/aplicaciones/tomcat/escaparate.war
```

!!! question "Antes de continuar"
    ¿Has extraído una imagen Docker o el artefacto Java que había dentro de ella?

## 1.1. Despliega `escaparate.war`

Desde `practicas/compose/` levanta solo el laboratorio:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  --profile lab \
  up -d tomcat-lab
```

`tomcat-lab` no publica `8080` hacia Internet.

Comprueba desde `web`:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  exec web \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://tomcat-lab:8080/escaparate/api/salud/listo
```

y:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  exec web \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://tomcat-lab:8080/api/salud/listo
```

Relaciona el resultado con:

```text
escaparate.war
→ /escaparate
```

## 1.2. Publícalo en la raíz

Cambia únicamente el nombre con el que el WAR se monta en Tomcat:

```text
ROOT.war
```

Recrea el laboratorio:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  --profile lab \
  up -d --force-recreate tomcat-lab
```

Repite:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  exec web \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://tomcat-lab:8080/api/salud/listo
```

Ahora debe responder en `/`.

**Captura 1:** las respuestas que demuestran `/escaparate` con el nombre original y `/` al desplegarlo como `ROOT.war`.

!!! question "Reflexiona"
    ¿Qué ha cambiado realmente: el código de la aplicación o la forma en que el servidor la ha desplegado?

Detén el laboratorio:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  --profile lab \
  stop tomcat-lab
```

A partir de aquí volvemos al despliegue habitual con Tomcat embebido.

Antes del siguiente experimento, recrea las tres réplicas con la imagen docente de esta sesión. El overlay cambia la imagen, pero **todavía no activa Redis**:

```bash
docker compose   -f compose.yaml   -f compose.aplicaciones.yaml   up -d app-1 app-2 app-3
```

Comprueba:

```bash
docker compose   -f compose.yaml   -f compose.aplicaciones.yaml   ps
```

y:

```bash
curl -s -o /dev/null -w "%{http_code}\n"   https://escaparate.<ip>.nip.io/api/sesion/estado
```

Sin cookie debe devolver:

```text
401
```

---

# Paso 2: Demuestra dónde vive una sesión

Escaparate incluye para esta actividad dos endpoints docentes:

```text
POST /api/sesion/abrir
GET  /api/sesion/estado
```

No representan un sistema de autenticación real. Solo nos permiten crear y consultar una `HttpSession`.

Primero necesitamos saber **qué réplica atendió la misma respuesta** que estamos comprobando.

En el bloque `location /api/` de Nginx añade:

```nginx
add_header X-Destino $upstream_addr always;
```

Valida y recarga:

```bash
docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Define:

```bash
URL=https://escaparate.<ip>.nip.io
```

Abre una sesión guardando la cookie:

```bash
rm -f /tmp/sesion.txt

curl -i -c /tmp/sesion.txt \
  -X POST "$URL/api/sesion/abrir?usuario=demo"
```

Localiza:

```text
Set-Cookie: JSESSIONID=...
```

Después reutiliza **esa misma cookie**:

```bash
for i in $(seq 1 9); do
  echo "Petición $i"
  curl -s -b /tmp/sesion.txt \
    -o /dev/null -D - \
    "$URL/api/sesion/estado" \
    | grep -iE '^HTTP/|^X-Destino'
  echo
done
```

Completa:

| Petición | Destino | ¿Sesión encontrada? |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |
| … | | |

Debes observar una relación:

```text
misma cookie
+
copia que creó la sesión
→ 200

misma cookie
+
otra copia
→ 401
```

!!! question "Reflexiona"
    ¿Por qué el fallo parece intermitente?

    Si el reparto fuese aproximadamente uniforme entre tres copias y solo una conociera la sesión, ¿qué proporción aproximada de peticiones esperarías que encontrara el estado?

---

# Paso 3: Saca la sesión fuera de las copias

La solución sigue la misma regla que aplicaste a los ficheros subidos:

> un dato que cualquier réplica necesita recuperar no debe vivir únicamente dentro de una de ellas.

El fichero:

```text
compose.aplicaciones.yaml
```

incluye un servicio `redis`.

Activa en `app-1`, `app-2` y `app-3` el perfil preparado para Spring Session siguiendo el fragmento incluido en el soporte. La configuración equivalente es:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: redis
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: 6379
```

Aplica:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  up -d
```

Comprueba que Redis responde:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  exec redis redis-cli ping
```

Debe devolver:

```text
PONG
```

Borra la cookie anterior:

```bash
rm -f /tmp/sesion.txt
```

Abre una sesión nueva:

```bash
curl -i -c /tmp/sesion.txt \
  -X POST "$URL/api/sesion/abrir?usuario=demo"
```

Comprueba que Redis contiene sesiones:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  exec redis \
  redis-cli --scan --pattern 'spring:session*' | head
```

Repite exactamente la tanda:

```bash
for i in $(seq 1 9); do
  echo "Petición $i"
  curl -s -b /tmp/sesion.txt \
    -o /dev/null -D - \
    "$URL/api/sesion/estado" \
    | grep -iE '^HTTP/|^X-Destino'
  echo
done
```

Ahora deben cumplirse simultáneamente:

```text
X-Destino
→ sigue cambiando

HTTP
→ siempre 200
```

**Captura 2:** varias respuestas atendidas por destinos diferentes manteniendo la sesión y alguna entrada `spring:session` en Redis.

!!! question "Reflexiona"
    Una sesión pegajosa habría hecho que el proxy intentara devolverte siempre a la misma copia.

    ¿Qué ventaja tiene el almacén externo cuando esa copia se reinicia o se sustituye?

---

# Paso 4: Compara una copia con tres

Ahora responderás una pregunta distinta:

```text
¿tres procesos en la misma EC2
equivalen a tres veces más capacidad?
```

El soporte incluye:

```text
practicas/aplicaciones/prueba-carga.js
```

y utiliza:

```text
/api/carga?ms=50
```

un endpoint preparado para generar una pequeña carga de CPU.

No buscamos un número universal de usuarios. Compararemos únicamente:

```text
peticiones/s
p95
porcentaje de errores
```

manteniendo iguales el generador, la duración y el número de VUs.

!!! info "Dónde ejecutar k6"
    Ejecuta k6 **desde tu equipo**, contra la URL pública de EC2. Así el generador no consume la CPU que estamos intentando comparar.

!!! warning "Una comparación necesita condiciones equivalentes"
    En el escenario de una sola copia **no basta con detener `app-2` y `app-3`** dejando esos destinos en el `upstream`: Nginx intentaría conectarse a copias caídas y añadiría reintentos a la medición.

    Para comparar de forma razonable dejaremos temporalmente el `upstream` con un único servidor.

## 4.1. Calentamiento

Desde tu equipo, situado en la raíz de tu copia del repositorio, define de nuevo la URL:

```bash
URL=https://escaparate.<ip>.nip.io
```

Asegúrate de que también tienes disponible localmente:

```text
practicas/aplicaciones/prueba-carga.js
```

Ejecuta:

```bash
docker run --rm \
  -e URL="$URL" \
  -v "$PWD/practicas/aplicaciones:/scripts:ro" \
  grafana/k6 run \
  --vus 10 --duration 15s \
  /scripts/prueba-carga.js
```

No anotes este resultado.

## 4.2. Tres copias

Ejecuta:

```bash
docker run --rm \
  -e URL="$URL" \
  -v "$PWD/practicas/aplicaciones:/scripts:ro" \
  grafana/k6 run \
  --vus 20 --duration 30s \
  /scripts/prueba-carga.js
```

Anota:

| Configuración | Peticiones/s | p95 | Errores |
|---|---:|---:|---:|
| 3 copias | | | |

## 4.3. Una copia

En EC2 guarda temporalmente la configuración:

```bash
cp ../nginx/conf.d/sitios.conf /tmp/sitios-tres.conf
```

En `backend_pool` deja únicamente:

```nginx
server app-1:8080 resolve;
```

Detén:

```bash
docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  stop app-2 app-3
```

Valida y recarga Nginx:

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

Solo debe aparecer `app-1`.

Desde tu equipo repite **exactamente**:

```bash
docker run --rm \
  -e URL="$URL" \
  -v "$PWD/practicas/aplicaciones:/scripts:ro" \
  grafana/k6 run \
  --vus 20 --duration 30s \
  /scripts/prueba-carga.js
```

Completa:

| Configuración | Peticiones/s | p95 | Errores |
|---|---:|---:|---:|
| 3 copias | | | |
| 1 copia | | | |

Restaura inmediatamente en EC2:

```bash
cp /tmp/sitios-tres.conf ../nginx/conf.d/sitios.conf

docker compose \
  -f compose.yaml \
  -f compose.aplicaciones.yaml \
  start app-2 app-3

docker compose exec web nginx -t
docker compose exec web nginx -s reload
```

Comprueba:

```bash
for i in $(seq 1 6); do
  curl -s "$URL/api/instancia"
  echo
done
```

**Captura 3:** resultados de las dos ejecuciones o una tabla donde se vean peticiones/s, p95 y errores.

!!! question "Reflexiona"
    Si las tres copias no multiplican por tres el throughput, ¿qué recursos siguen siendo compartidos?

    ¿Qué utilidad siguen teniendo varias réplicas aunque no tripliquen la capacidad?

!!! warning "Qué demuestra esta prueba"
    La comparación incluye la red entre tu equipo y EC2 y se ejecuta sobre una única máquina remota que comparte CPU, memoria y dependencias.

    Sirve para comparar **estas dos configuraciones bajo las mismas condiciones**, no para declarar la capacidad de producción de Escaparate.

---

# Paso 5: Completa la memoria del tema

Abre:

```text
entregas/tema3/memoria-publicacion-ejecucion.md
```

Añade una sección breve:

```markdown
## Ejecución, estado y rendimiento
```

Con unas **150–200 palabras** y una tabla pequeña es suficiente.

Debe explicar:

1. qué servidor ejecutan normalmente las tres aplicaciones;
2. qué cambió al desplegar el WAR en Tomcat externo;
3. por qué la sesión fallaba entre réplicas;
4. qué consiguió Redis;
5. qué conclusión obtuviste de la comparación k6.

Incluye:

| Aspecto | Embebido | Externo |
|---|---|---|
| Quién arranca el servidor | | |
| Cómo se despliega la aplicación | | |
| Modelo que conserva el proyecto | | |

Actualiza el diagrama final:

```text
Internet
   │
   ▼
Nginx :443
   │
   ├── app-1 ─┐
   ├── app-2 ─┼── Redis
   └── app-3 ─┘
        │
        └── PostgreSQL

logs
 ↓
Fluent Bit
 ↓
Elasticsearch
 ↓
Kibana
```

El Tomcat externo **no aparece**, porque era únicamente un laboratorio.

---

# Paso 6: Cierra la sesión

Revisa:

```bash
git status
```

No debe quedar la modificación temporal del `upstream`.

Después:

1. registra los cambios;
2. publica `sesion-10`;
3. abre Pull Request hacia `main`;
4. verifica los checks;
5. fusiona mediante **Create a merge commit**.

---

# Verificación

Al terminar debe cumplirse:

- Nginx sigue siendo la única puerta pública;
- `app-1`, `app-2` y `app-3` vuelven a estar activas;
- existe evidencia del WAR bajo `/escaparate` y después bajo `/`;
- el Tomcat externo ha quedado detenido;
- la misma cookie produce respuestas `200/401` según la réplica antes de Redis;
- después de Redis, `/api/sesion/estado` devuelve `200` aunque cambie `X-Destino`;
- existen entradas `spring:session` en Redis;
- el `upstream` final vuelve a contener las tres copias;
- existe una comparación k6 desde el mismo generador y con las mismas condiciones entre una y tres copias;
- la memoria conjunta está completada;
- `sesion-10` ha llegado a `main`.

---

# Qué se entrega

- [ ] `actividad-3.5.md`.
- [ ] Tres capturas.
- [ ] Evidencia del cambio de contexto `escaparate.war` → `ROOT.war`.
- [ ] Tabla de la incidencia de sesión antes de Redis.
- [ ] Evidencia de sesión estable entre distintas copias después de Redis.
- [ ] Entradas `spring:session`.
- [ ] Comparación k6 entre una y tres copias.
- [ ] Reflexiones pedidas en la actividad.
- [ ] `memoria-publicacion-ejecucion.md` completada.
- [ ] PR `sesion-10 → main` fusionada.

---

# ✅ Cierre

Has abierto la parte del sistema que hasta ahora estaba detrás del proxy.

Primero has comprobado que:

```text
mismo WAR
→ puede ejecutarse con Tomcat embebido
→ o desplegarse en un Tomcat externo
```

Después has aplicado una idea más importante para la arquitectura final:

```text
estado dentro de una réplica
→ copias no intercambiables

estado compartido
→ cualquier copia puede continuar
```

Y finalmente has comprobado que:

```text
más réplicas
≠
multiplicar automáticamente los recursos
```

El proyecto conserva el modelo embebido y queda preparado para el siguiente cambio de enfoque: **automatizar construcción, pruebas y publicación mediante integración continua**.
