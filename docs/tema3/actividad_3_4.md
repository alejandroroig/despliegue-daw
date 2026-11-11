# Actividad 3.4: Que te lo cuente el sistema

## 👁️ Contexto

Tu servicio ya está publicado por HTTPS, Nginx reparte las peticiones entre tres copias de Escaparate y la zona de informes está protegida.

Sin embargo, cuando quieres saber qué ha ocurrido todavía tienes que revisar registros separados o ejecutar comprobaciones manuales.

En esta actividad vas a **reunir esos registros, convertir el acceso de Nginx en datos consultables y utilizarlos para responder preguntas concretas**.

La pila técnica viene preparada. No vas a aprender a administrar Fluent Bit, Elasticsearch ni Kibana desde cero. Para esta sesión basta con entender:

```text
los servicios escriben
        ↓
Fluent Bit recoge
        ↓
Elasticsearch almacena
        ↓
Kibana permite consultar
```

El trabajo importante será decidir **qué información registrar, cómo consultarla y qué significa lo que observas**.

!!! abstract "Cómo vas a trabajar"
    **Recoger → estructurar → consultar → provocar → interpretar → decidir**

    La infraestructura de observabilidad viene preparada porque el objetivo no es memorizar la configuración interna de Elasticsearch, Fluent Bit o Kibana.

    Tu trabajo será comprobar que funciona, convertir los registros de Nginx en datos útiles y utilizarlos para responder preguntas sobre el servicio.

!!! info "Ficheros de soporte"
    [Descarga `actividad-3.4-soporte.zip`](descargas/actividad-3.4-soporte.zip){target="_blank" rel="noopener"}

    El comprimido contiene la configuración y los scripts auxiliares de la práctica.

    **No tienes que construir la pila de observabilidad desde cero.**

## 🎯 Qué vas a practicar

Al terminar la actividad deberías ser capaz de:

- centralizar registros de varios contenedores;
- distinguir texto libre de datos estructurados;
- convertir el access log de Nginx en campos consultables;
- comprobar qué réplica ha atendido cada petición;
- analizar qué rutas acumulan errores;
- comparar media y percentil 95 de latencia;
- acceder a una herramienta administrativa sin publicarla en Internet;
- provocar incidencias controladas y observar su efecto en los datos;
- diseñar condiciones de alerta con umbral, ventana, destinatario y acción;
- reconocer que la observabilidad también consume recursos.

## 🧰 Requisitos previos

Necesitas:

- Actividad 3.3 terminada y fusionada en `main`;
- la misma instancia EC2 utilizada en la actividad anterior;
- Escaparate funcionando por HTTPS;
- `app-1`, `app-2` y `app-3` detrás de Nginx;
- PostgreSQL funcionando;
- acceso SSH a la instancia;
- al menos **4 GiB de RAM** en EC2;
- `actividad-3.4-soporte.zip`.

!!! warning "Kibana no se publica en Internet"
    Kibana no dispone de autenticación en este laboratorio.

    Por tanto:

    ```text
    no se publica mediante Nginx
    no se abre el puerto 5601 en el Security Group
    no escucha en 0.0.0.0
    ```

    Accederás desde tu equipo mediante un **túnel SSH**.

!!! info "Evidencias"
    Solo se piden **tres evidencias**.

    No hagas una captura de cada paso. Las evidencias deben demostrar resultados significativos.

## ☁️ Paso 1: Comprueba el estado de la EC2

Antes de añadir observabilidad, comprueba que el despliegue de la sesión anterior continúa operativo.

### 1.1. Prepara la rama

Desde el repositorio:

```bash
git switch main
git pull --ff-only
git switch -c sesion-09
```

Crea:

```text
entregas/
└── tema3/
    └── actividad-3.4/
        ├── actividad-3.4.md
        └── img/
```

### 1.2. Recupera los nombres públicos

Si has abierto una terminal nueva:

```bash
read -r -p 'IPv4 pública actual de EC2: ' IP_PUBLICA

ESCAPARATE="escaparate.${IP_PUBLICA}.nip.io"
DOCS="docs.${IP_PUBLICA}.nip.io"
```

Comprueba:

```bash
printf 'Catálogo: %s\nDocs:     %s\n' "$ESCAPARATE" "$DOCS"
```

Y valida:

```bash
curl -fsS "https://${ESCAPARATE}/" > /dev/null \
  && echo "HTTPS OK"

curl -fsS "https://${ESCAPARATE}/api/salud/listo"
```

!!! warning "Si la IP pública ha cambiado"
    Si utilizas una IPv4 temporal y has detenido y vuelto a arrancar la instancia, puede haber cambiado.

    En ese caso también cambian:

    ```text
    escaparate.<IP>.nip.io
    docs.<IP>.nip.io
    ```

    y el certificado emitido para los nombres anteriores ya no coincidirá con los nuevos.

    Resuelve esa incidencia antes de continuar con observabilidad.

## 📦 Paso 2: Levanta la pila de observabilidad

La pila se añade al despliegue existente mediante un segundo fichero Compose.

### 2.1. Coloca los ficheros de soporte

Descomprime el paquete y deja:

```text
practicas/
├── compose/
│   └── compose.observabilidad.yaml
└── observabilidad/
    ├── fluent-bit/
    │   ├── fluent-bit.conf
    │   └── parsers.conf
    ├── genera-trafico.sh
    ├── prepara-host.sh
    └── kibana-init.sh
```

Haz ejecutables los scripts:

```bash
chmod +x practicas/observabilidad/*.sh
```

### 2.2. Prepara el host

La preparación del sistema operativo no es el objetivo de esta sesión, por lo que se automatiza:

```bash
./practicas/observabilidad/prepara-host.sh
```

El script configura los requisitos necesarios para Elasticsearch y prepara swap para absorber picos de memoria de esta pila de laboratorio.

!!! info "Observabilidad también consume recursos"
    Elasticsearch y Kibana son servicios relativamente pesados.

    Más adelante comprobarás su consumo con:

    ```bash
    docker stats --no-stream
    ```

### 2.3. Levanta los dos Compose conjuntamente

Desde:

```text
practicas/compose/
```

valida:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  config > /dev/null
```

Después:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  up -d
```

Comprueba:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  ps
```

Y verifica Elasticsearch desde la propia EC2:

```bash
curl -fsS http://127.0.0.1:9200/ > /dev/null \
  && echo "Elasticsearch OK"
```

La arquitectura añadida es:

```text
web + app-1 + app-2 + app-3
              │
              ▼
          Fluent Bit
              │
              ▼
        Elasticsearch
              │
              ▼
            Kibana
```

El servicio `kibana-init` prepara automáticamente el data view:

```text
Escaparate
```

sobre:

```text
escaparate-*
```

utilizando `@timestamp`.

No tienes que crear el data view manualmente.

## 🔐 Paso 3: Accede a Kibana sin publicarlo

Kibana debe estar disponible para administración, pero no necesita ser accesible desde cualquier equipo de Internet.

### 3.1. Comprueba qué puertos escucha el host

En EC2:

```bash
sudo ss -lntp | grep -E ':(24224|9200|5601)\b'
```

El estado esperado es:

```text
127.0.0.1:24224
127.0.0.1:9200
127.0.0.1:5601
```

Esto significa que los tres servicios solo aceptan conexiones desde la propia EC2.

!!! question "Predice"
    Desde tu equipo del aula, ¿debería funcionar?

    ```text
    http://<IP-PUBLICA>:5601
    ```

    Justifica tu respuesta antes de probarlo.

### 3.2. Crea un túnel SSH

Desde **tu equipo local**, abre una terminal nueva y ejecuta:

```bash
ssh -i <clave.pem> \
  -L 5601:127.0.0.1:5601 \
  ec2-user@<IP-PUBLICA>
```

Mantén esa conexión abierta.

El recorrido es:

```text
navegador local
localhost:5601
      │
      │ túnel SSH
      ▼
EC2 127.0.0.1:5601
      │
      ▼
    Kibana
```

Ahora abre en el navegador de tu equipo:

```text
http://localhost:5601
```

En Kibana entra en:

```text
Discover
```

y comprueba que aparece el data view:

```text
Escaparate
```

!!! question "Reflexiona"
    Explica la diferencia entre:

    ```text
    un proceso escucha en un puerto
    Docker publica ese puerto
    el Security Group permite entrar
    ```

    ¿Qué aporta el túnel SSH en este caso?

**Evidencia 1:** terminal mostrando `5601` ligado a `127.0.0.1` y Kibana accesible en tu equipo mediante `http://localhost:5601`.

## 🧾 Paso 4: Convierte el access log en datos estructurados

En Discover observarás inicialmente registros con pocos campos comunes, por ejemplo:

```text
@timestamp
container_name
log
source
```

El access log de Nginx sigue apareciendo dentro de `log` como texto.

Queremos convertirlo en campos:

```text
ip
metodo
ruta
estado
duracion
destino
```

### 4.1. Define el formato JSON

Abre:

```text
practicas/nginx/conf.d/sitios.conf
```

y añade, antes de los bloques `server`:

```nginx
log_format observabilidad escape=json
    '{'
    '"hora":"__________",'
    '"ip":"__________",'
    '"metodo":"__________",'
    '"ruta":"__________",'
    '"estado":__________,'
    '"duracion":__________,'
    '"destino":"__________"'
    '}';
```

Completa los huecos utilizando únicamente:

```text
$time_iso8601
$remote_addr
$request_method
$uri
$status
$request_time
$upstream_addr
```

!!! info "No memorices las variables"
    El objetivo es relacionar cada dato con la variable que lo representa.

    Por ejemplo:

    ```text
    ruta
    ↔
    $uri
    ```

    `estado` y `duracion` deben quedar como **números JSON**, no como cadenas entre comillas.

### 4.2. Activa el formato

Dentro de los bloques `server` que atienden tráfico añade:

```nginx
access_log /dev/stdout observabilidad;
```

Valida:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  exec web nginx -t
```

Y recarga:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  exec web nginx -s reload
```

### 4.3. Genera algunas peticiones conocidas

Desde tu equipo:

```bash
curl -fsS "https://${ESCAPARATE}/" > /dev/null

curl -s -o /dev/null \
  "https://${ESCAPARATE}/no-existe"

curl -s \
  "https://${ESCAPARATE}/api/instancia"
```

Actualiza Discover.

Debes empezar a encontrar campos como:

```text
ruta
estado
duracion
destino
```

!!! question "Reflexiona"
    Nginx ya conocía ruta, estado, duración y destino antes de este cambio.

    ¿Qué ventaja aporta convertir esos datos en campos estructurados?

## 🚦 Paso 5: Genera tráfico suficiente

Ahora necesitas un conjunto de datos que contenga respuestas correctas, errores y peticiones con duraciones diferentes.

### 5.1. Ejecuta el generador

Desde tu equipo:

```bash
./practicas/observabilidad/genera-trafico.sh \
  "https://${ESCAPARATE}"
```

No necesitas estudiar la implementación del script.

Genera:

- peticiones correctas;
- rutas inexistentes;
- peticiones rápidas;
- peticiones lentas.

!!! info "Por qué aparecen peticiones lentas"
    El guion utiliza `/api/carga` con distintas duraciones.

    Nginx registra:

    ```text
    /api/carga
    ```

    como una misma ruta, aunque unas peticiones tarden más que otras.

    Esto nos permitirá comparar **media y p95**.

### 5.2. Comprueba las tres réplicas desde los logs

En Discover filtra:

```text
ruta: "/api/instancia"
```

Añade como columna:

```text
destino
```

Debes observar **tres direcciones internas distintas**, correspondientes a las tres réplicas.

No necesitas crear una visualización para esta comprobación.

**Evidencia 2:** Discover mostrando los campos estructurados y peticiones a `/api/instancia` atendidas por tres destinos diferentes.

## 📊 Paso 6: Construye dos visualizaciones útiles

No se trata de llenar un dashboard de gráficos.

Cada visualización debe responder una pregunta concreta.

### 6.1. ¿Qué rutas están fallando?

Crea una visualización:

```text
Tipo:
Bar horizontal

Filtro:
estado >= 400

Categoría:
Top values of ruta.keyword

Métrica:
Count of records
```

Guárdala como:

```text
Errores por ruta
```

!!! note "Tráfico que no has generado tú"
    La EC2 está en Internet.

    Es posible que aparezcan peticiones automatizadas hacia rutas que tú no hayas utilizado.

    No significa necesariamente que haya un error en tu práctica.

### 6.2. ¿La media cuenta toda la historia?

Crea una segunda visualización:

```text
Tipo:
Table

Rows:
Top values of ruta.keyword

Métrica 1:
Average of duracion

Métrica 2:
Percentile of duracion
Percentil: 95
```

Guárdala como:

```text
Latencia por ruta
```

Interpreta:

```text
media
→ comportamiento promedio

p95
→ el 95 % de las peticiones
  tarda menos que ese valor
```

!!! question "Reflexiona"
    Observa especialmente:

    ```text
    /api/carga
    ```

    ¿Qué puede mostrar el p95 que una media relativamente buena podría ocultar?

    ¿Por qué tiene sentido consultar ambos valores?

## 💥 Paso 7: Provoca incidencias y observa los datos

Ahora vas a generar dos situaciones conocidas y comprobar cómo aparecen en los registros.

### 7.1. Genera accesos no autorizados

Ejecuta:

```bash
for i in $(seq 1 12); do
  curl -s -o /dev/null \
    "https://${DOCS}/informes/"
done
```

Actualiza:

```text
Errores por ruta
```

Debe aumentar:

```text
/informes/
```

con respuestas:

```text
401
```

### 7.2. Provoca un fallo de disponibilidad

En EC2, desde `practicas/compose/`:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  stop bd
```

Desde tu equipo genera varias comprobaciones:

```bash
for i in $(seq 1 12); do
  curl -s -o /dev/null \
    -w "%{http_code}\n" \
    "https://${ESCAPARATE}/api/salud/listo"
done
```

Debes observar respuestas:

```text
503
```

Recupera inmediatamente PostgreSQL:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  start bd
```

Espera hasta que:

```bash
curl -fsS \
  "https://${ESCAPARATE}/api/salud/listo"
```

vuelva a responder correctamente.

Actualiza de nuevo:

```text
Errores por ruta
```

Ahora debe crecer también:

```text
/api/salud/listo
```

### 7.3. Compara antes y después

Observa cómo las dos situaciones provocadas se convierten en datos consultables:

```text
401
→ intentos sin credenciales

503
→ dependencia no disponible
```

**Evidencia 3:** `Errores por ruta` mostrando el efecto de los `401` y `503`, junto con `Latencia por ruta` mostrando media y p95.

## 🚨 Paso 8: Diseña alertas que merezcan una acción

Un dashboard necesita que alguien lo mire.

Una alerta permite decidir cuándo una situación deja de ser puntual y merece avisar a una persona.

No configurarás notificaciones reales.

### 8.1. Recuerda las cuatro decisiones

Una alerta útil necesita:

| Elemento | Pregunta |
|---|---|
| Umbral | ¿a partir de qué valor preocupa? |
| Ventana | ¿durante cuánto tiempo? |
| Destinatario | ¿quién debe recibirla? |
| Acción | ¿qué debería comprobar o hacer? |

### 8.2. Diseña dos condiciones

Completa en `actividad-3.4.md`:

| Situación | Umbral | Ventana | Destinatario | Qué debería hacer |
|---|---:|---|---|---|
| respuestas `5xx` | | | | |
| muchos `401` desde una misma IP | | | | |

No existe una única respuesta correcta.

La propuesta debe permitir distinguir:

```text
evento aislado
        ≠
problema que merece atención
```

!!! question "Reflexiona"
    ¿Por qué una alerta necesita tanto un umbral como una ventana?

    ¿Qué ocurriría si el sistema avisara por cualquier `404`, `401` o `500` individual?

## 🧠 Paso 9: Comprueba el coste de observar

La observabilidad también consume recursos.

En EC2 ejecuta:

```bash
docker stats --no-stream
```

Identifica aproximadamente qué componentes consumen más memoria.

No necesitas hacer una captura específica.

En `actividad-3.4.md` responde brevemente:

1. ¿qué servicios de observabilidad son los que más memoria consumen?;
2. ¿por qué guardar y analizar más información también tiene un coste?;
3. ¿por qué no tendría sentido conservar todos los logs indefinidamente?

## 📝 Paso 10: Empieza la memoria conjunta del Tema 3

La Actividad 3.4 no cierra todavía todo el tema.

En la siguiente sesión abrirás el backend y trabajarás servidor de aplicaciones, sesiones y rendimiento.

Por tanto, ahora comenzarás una memoria que completarás en la Actividad 3.5.

### 10.1. Crea la memoria

Crea:

```text
entregas/
└── tema3/
    └── memoria-publicacion-ejecucion.md
```

Añade la sección:

```markdown
## Publicación, seguridad y observabilidad
```

Con unas **250–300 palabras** más un esquema es suficiente.

Debe explicar:

1. cómo llega una petición desde Internet hasta Nginx;
2. cómo distingue catálogo y documentación;
3. cómo reparte `/api/` entre tres réplicas;
4. cómo se protege `/informes/`;
5. dónde termina TLS;
6. qué datos registra Nginx;
7. cómo llegan esos registros hasta Kibana;
8. qué preguntas responden las dos visualizaciones.

Puedes utilizar como base:

```text
Internet
   │
   │ HTTPS
   ▼
Nginx :443
   │
   ├── estáticos
   │
   ├── docs
   │
   └── /api/
        │
        ├── app-1
        ├── app-2
        └── app-3
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

!!! info "La memoria se completa en la 3.5"
    En la siguiente actividad añadirás:

    ```markdown
    ## Servidor de aplicaciones, estado y rendimiento
    ```

    No intentes documentar todavía contenidos que no has trabajado.

## 🔀 Paso 11: Integra la rama

### 11.1. Revisa el repositorio

Ejecuta:

```bash
git status
```

Comprueba que estás versionando:

- configuración de observabilidad;
- scripts de apoyo;
- cambios de Nginx;
- documentación.

No deben aparecer secretos ni claves privadas.

### 11.2. Registra los cambios

```bash
git add .
git status
```

Revisa qué vas a registrar y crea el commit:

```bash
git commit -m "Añade observabilidad y análisis de registros"
git push -u origin sesion-09
```

Abre:

```text
sesion-09 → main
```

Comprueba los checks del repositorio y fusiona mediante:

```text
Create a merge commit
```

Finalmente:

```bash
git switch main
git pull --ff-only
```

## 🔧 Si algo falla

Diagnostica primero la capa afectada.

| Síntoma | Causa probable | Qué comprobar |
|---|---|---|
| Elasticsearch no responde en `127.0.0.1:9200` | servicio no levantado o falta de recursos | `docker compose ... ps`, logs y memoria disponible |
| Kibana no abre en `localhost:5601` | túnel SSH no activo | terminal SSH, puerto local y `127.0.0.1:5601` en EC2 |
| `http://<IP>:5601` funciona desde Internet | Kibana está publicado incorrectamente | Compose y Security Group |
| Kibana no muestra el data view | `kibana-init` no terminó correctamente | logs de `kibana-init` |
| Discover solo muestra `log` | Nginx aún usa formato no estructurado | `log_format`, `access_log`, `nginx -t` y recarga |
| `estado` o `duracion` aparecen como texto | se han puesto entre comillas en JSON | revisa el `log_format` |
| No aparece `destino` | la petición no pasó por un upstream o falta `$upstream_addr` | utiliza `/api/instancia` y revisa el formato |
| Solo aparece un destino | falta tráfico suficiente o una réplica no funciona | `docker compose ps` y `/api/instancia` |
| No aparecen los `401` | se está usando una ruta distinta | utiliza `/informes/` sin credenciales |
| No aparece `503` al detener PostgreSQL | la aplicación todavía no refleja la dependencia como no preparada | espera unos segundos y repite readiness |
| Tras arrancar PostgreSQL readiness sigue fallando | la BD aún no está lista | espera y consulta logs |
| Kibana/Elasticsearch consumen demasiada memoria | EC2 insuficiente | `docker stats`, swap y tamaño de instancia |

!!! tip "Orden de diagnóstico"
    Para esta sesión piensa:

    ```text
    aplicación genera petición
            ↓
    Nginx registra
            ↓
    Fluent Bit recoge
            ↓
    Elasticsearch almacena
            ↓
    Kibana consulta
    ```

    Si un dato no aparece, localiza primero en qué punto de ese recorrido se pierde.

## 📦 Qué se entrega

Antes de terminar, comprueba:

- [ ] `actividad-3.4.md` con resultados y reflexiones;
- [ ] solo tres evidencias;
- [ ] pila Fluent Bit → Elasticsearch → Kibana funcionando;
- [ ] Kibana ligado a `127.0.0.1:5601`;
- [ ] acceso a Kibana mediante túnel SSH;
- [ ] access log JSON de Nginx;
- [ ] campos `ruta`, `estado`, `duracion` y `destino`;
- [ ] `/api/instancia` atendida por tres destinos;
- [ ] visualización `Errores por ruta`;
- [ ] visualización `Latencia por ruta` con media y p95;
- [ ] efecto visible de `401` y `503`;
- [ ] dos condiciones de alerta justificadas;
- [ ] reflexión sobre el coste de observabilidad;
- [ ] `memoria-publicacion-ejecucion.md` iniciada;
- [ ] Pull Request `sesion-09 → main` fusionada.

## ✅ Cierre

La observabilidad no consiste en acumular gráficos.

En esta actividad has utilizado los datos del sistema para responder preguntas concretas:

```text
¿qué está fallando?
¿qué peticiones son lentas?
¿qué réplica ha atendido cada petición?
¿cuándo merece la pena avisar a alguien?
```

También has comprobado que las herramientas administrativas no necesitan estar expuestas públicamente para ser útiles:

```text
Kibana
→ solo localhost en EC2
→ acceso administrativo mediante SSH
```

Las herramientas pueden cambiar.

El patrón permanece:

```text
recoger
  ↓
estructurar
  ↓
consultar
  ↓
interpretar
  ↓
actuar
```

En la siguiente actividad abrirás la parte del sistema que hasta ahora has observado desde fuera: el servidor que ejecuta realmente Escaparate, su estado de sesión y su comportamiento bajo carga.
