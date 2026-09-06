# 🧪 Actividad 3.4: Que te lo cuente el sistema

## Contexto

Tu servicio ya está desplegado por HTTPS en una instancia EC2. Nginx reparte `/api/` entre tres copias y actúa como única puerta pública.

El problema ahora es otro:

```text
el servicio responde
        ↓
pero ocurre algo extraño
        ↓
¿cómo averiguamos qué está pasando?
```

En esta sesión vas a utilizar una pila de observabilidad ya preparada para **reunir registros, convertirlos en datos consultables y responder preguntas concretas**.

```text
servicios
   ↓
Fluent Bit
   ↓
Elasticsearch
   ↓
Kibana
```

No vas a aprender a administrar Fluent Bit, Elasticsearch o Kibana desde cero. La pila es el medio; el objetivo es aprender a **registrar, consultar e interpretar**.

!!! info "Tiempo orientativo"
    La actividad está diseñada para una sesión de aproximadamente **100 minutos**.

    Para ajustarnos a ese tiempo:

    - la configuración base de la pila está proporcionada;
    - Kibana será accesible mediante túnel SSH desde el principio;
    - solo crearás **dos visualizaciones**;
    - no configurarás notificaciones reales;
    - la memoria final del tema será breve.

## Qué vas a practicar

- **Centralizar** registros procedentes de varios contenedores.
- **Convertir** el access log de Nginx en campos estructurados.
- **Filtrar y agrupar** registros por ruta, estado y backend.
- **Comprobar** qué réplicas han atendido las peticiones.
- **Comparar** latencia media y percentil 95.
- **Observar** cómo cambian los registros al provocar incidencias controladas.
- **Diseñar** una condición de alerta con umbral, ventana, destinatario y acción.
- **Administrar** Kibana sin publicar su puerto hacia Internet.

## Requisitos previos

- Actividad 3.3 terminada y fusionada en `main`.
- La misma instancia EC2, con al menos **4 GiB de RAM**.
- Aplicación funcionando por HTTPS.
- `app-1`, `app-2` y `app-3` detrás de Nginx.
- PostgreSQL operativo.
- Acceso SSH.
- Paquete `actividad-3.4-soporte.zip`.

Prepara:

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

---

# Paso 1: Levanta la pila de observabilidad

Descomprime el paquete de apoyo para obtener:

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

La preparación específica del host para Elasticsearch está automatizada:

```bash
./practicas/observabilidad/prepara-host.sh
```

Desde `practicas/compose/`:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  config > /dev/null

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

y:

```bash
curl -fsS http://127.0.0.1:9200/ > /dev/null \
  && echo "Elasticsearch OK"
```

La pila debe quedar conceptualmente así:

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

!!! note "Puertos de administración"
    Elasticsearch y Kibana no necesitan estar expuestos públicamente. En esta práctica se accede a ellos desde la propia instancia o mediante SSH.

---

# Paso 2: Accede a Kibana mediante un túnel SSH

Kibana debe quedar ligado únicamente a la interfaz local de EC2:

```yaml
- "127.0.0.1:5601:5601"
```

Comprueba:

```bash
sudo ss -lntp | grep -E ':(9200|5601)\b'
```

Debes observar ambos servicios en `127.0.0.1`.

Desde **tu equipo** abre el túnel:

```bash
ssh -i <clave.pem> \
  -L 5601:127.0.0.1:5601 \
  ec2-user@<ip-publica>
```

Mantén la sesión abierta y entra desde el navegador en:

```text
http://localhost:5601
```

El recorrido es:

```text
navegador
localhost:5601
      │
      │ túnel SSH
      ▼
EC2 127.0.0.1:5601
      │
      ▼
    Kibana
```

En **Discover** debe aparecer el data view preparado por el paquete de soporte.

!!! question "Reflexiona"
    ¿Por qué podemos administrar Kibana desde nuestro equipo aunque el puerto 5601 no esté publicado hacia Internet?

---

# Paso 3: Convierte el access log en datos estructurados

Inicialmente puedes encontrar registros donde gran parte de la información aparece dentro de un único campo de texto.

Queremos que una petición produzca campos como:

```text
ip
metodo
ruta
estado
duracion
destino
```

En:

```text
practicas/nginx/conf.d/sitios.conf
```

añade antes de los bloques `server`:

```nginx
log_format observabilidad escape=json
    '{'
      '"hora":"$time_iso8601",'
      '"ip":"$remote_addr",'
      '"metodo":"$request_method",'
      '"ruta":"$uri",'
      '"estado":$status,'
      '"duracion":$request_time,'
      '"destino":"$upstream_addr"'
    '}';
```

Lee el formato:

| Campo | Qué representa |
|---|---|
| `hora` | momento de la petición |
| `ip` | cliente observado por Nginx |
| `metodo` | método HTTP |
| `ruta` | URI solicitada |
| `estado` | código de respuesta |
| `duracion` | tiempo total de petición |
| `destino` | backend utilizado por el proxy |

`escape=json` evita que ciertos caracteres rompan el JSON.

En los bloques `server` que atienden el tráfico añade:

```nginx
access_log /dev/stdout observabilidad;
```

Valida y recarga:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  exec web nginx -t

docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  exec web nginx -s reload
```

Genera algunas peticiones:

```bash
curl -fsS https://escaparate.<ip>.nip.io/ > /dev/null

curl -s -o /dev/null \
  https://escaparate.<ip>.nip.io/no-existe

curl -s \
  https://escaparate.<ip>.nip.io/api/instancia
```

Actualiza Discover.

Ahora `estado`, `ruta`, `duracion` y `destino` deben poder consultarse como campos separados.

**Captura 1:** Discover mostrando una entrada con los campos estructurados.

!!! question "Reflexiona"
    Nginx ya conocía todos esos datos antes. ¿Qué ventaja obtenemos al convertirlos en campos con nombre?

---

# Paso 4: Genera tráfico y comprueba el reparto

Desde tu equipo ejecuta:

```bash
./practicas/observabilidad/genera-trafico.sh \
  https://escaparate.<ip>.nip.io
```

El script genera:

- peticiones correctas;
- rutas inexistentes;
- peticiones rápidas y lentas.

No necesitas estudiar su código.

En Discover filtra:

```text
ruta: "/api/instancia"
```

y añade:

```text
destino
```

como columna.

Debes observar **tres destinos internos distintos**.

Esto permite demostrar el reparto sin entrar en los contenedores:

```text
peticiones
    ↓
 Nginx
 ┌──┼──┐
 ▼  ▼  ▼
A1 A2 A3
```

**Captura 2:** `/api/instancia` atendida por tres destinos diferentes.

---

# Paso 5: Construye dos visualizaciones

No vamos a explorar todo Kibana. Solo crearás dos visualizaciones porque responden a preguntas concretas.

## 5.1. ¿Qué rutas acumulan errores?

Crea:

```text
Tipo:
Barra horizontal

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

La pregunta que responde es:

```text
¿dónde se concentran los errores?
```

## 5.2. ¿La media cuenta toda la historia?

Crea una tabla:

```text
Filas:
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
  ha tardado ese valor o menos
```

Observa especialmente la ruta utilizada por el generador para producir peticiones con distintas duraciones.

!!! question "Reflexiona"
    Si la media parece aceptable pero el p95 es mucho mayor, ¿qué experiencia puede estar teniendo una parte de los usuarios?

---

# Paso 6: Provoca dos incidencias y observa el efecto

## 6.1. Accesos no autorizados

Genera varios accesos sin credenciales:

```bash
for i in {1..8}; do
  curl -s -o /dev/null \
    https://docs.<ip>.nip.io/informes/
done
```

Actualiza **Errores por ruta**.

Debe crecer:

```text
/informes/
→ 401
```

## 6.2. Dependencia no disponible

Detén PostgreSQL:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  stop bd
```

Genera varias peticiones:

```bash
for i in {1..8}; do
  curl -s -o /dev/null -w "%{http_code} " \
    https://escaparate.<ip>.nip.io/api/salud/listo
done
echo
```

Recupera inmediatamente la base:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observabilidad.yaml \
  start bd
```

Espera hasta que:

```bash
curl -fsS https://escaparate.<ip>.nip.io/api/salud/listo
```

vuelva a responder correctamente.

Actualiza **Errores por ruta**.

Debe observarse también el aumento de errores de disponibilidad.

**Captura 3:** `Errores por ruta` después de provocar los `401` y los errores de disponibilidad.

---

# Paso 7: Diseña alertas que merezcan una acción

No configurarás notificaciones reales.

Para cada caso decide:

- **umbral**;
- **ventana temporal**;
- **destinatario**;
- **acción**.

Completa:

| Situación | Umbral | Ventana | Destinatario | Acción |
|---|---:|---|---|---|
| respuestas `5xx` | | | | |
| muchos `401` desde una misma IP | | | | |

No existe una única respuesta correcta. Debes justificar que la condición distingue un evento puntual de un problema que merece atención.

!!! question "Reflexiona"
    ¿Por qué alertar por cualquier error individual produciría probablemente una mala alerta?

---

# Paso 8: Cierra el tema

Crea:

```text
entregas/tema3/memoria-servidor-web.md
```

La memoria debe ser **muy breve**: aproximadamente 250–350 palabras más un diagrama.

Incluye:

1. arquitectura final;
2. papel de Nginx;
3. HTTPS y control de acceso;
4. tres réplicas y almacenamiento compartido;
5. observabilidad: qué se recoge y qué preguntas permiten responder las dos visualizaciones.

Puedes utilizar un esquema como:

```text
Internet
   │
   ▼
Nginx :443
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

Después:

1. revisa `git status`;
2. publica `sesion-09`;
3. abre Pull Request hacia `main`;
4. verifica los checks del repositorio;
5. fusiona mediante **Create a merge commit**.

---

# Verificación

Al finalizar debe cumplirse:

- la aplicación continúa respondiendo por HTTPS;
- Kibana y Elasticsearch no están publicados hacia Internet;
- Kibana es accesible mediante túnel SSH;
- Nginx genera un access log estructurado;
- los campos `ruta`, `estado`, `duracion` y `destino` pueden consultarse;
- `/api/instancia` muestra tres destinos;
- existe `Errores por ruta`;
- existe `Latencia por ruta` con media y p95;
- las incidencias provocadas aparecen en los datos;
- se han definido dos condiciones de alerta justificadas;
- existe `memoria-servidor-web.md`;
- la rama `sesion-09` ha llegado a `main`.

---

# Qué se entrega

- [ ] `actividad-3.4.md` con respuestas y resultados.
- [ ] Tres capturas.
- [ ] Configuración de observabilidad versionada.
- [ ] Access log JSON de Nginx.
- [ ] Comprobación de tres destinos.
- [ ] Visualización `Errores por ruta`.
- [ ] Visualización `Latencia por ruta`.
- [ ] Dos condiciones de alerta justificadas.
- [ ] `memoria-servidor-web.md`.
- [ ] PR `sesion-09 → main` fusionada.

---

# ✅ Cierre

La observabilidad no consiste en acumular gráficos.

En esta actividad has utilizado los datos del sistema para responder preguntas:

```text
¿qué está fallando?
¿qué rutas son más lentas?
¿qué réplica atendió una petición?
¿cuándo merece la pena avisar?
```

Las herramientas concretas pueden cambiar. El patrón permanece:

```text
recoger
→ estructurar
→ consultar
→ interpretar
→ actuar
```
