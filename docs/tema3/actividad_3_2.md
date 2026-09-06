# 🧪 Actividad 3.2: Una puerta, tres copias


## Contexto

En la Actividad 3.1 Nginx se convirtió en la única puerta de entrada:

```text
                         ┌── frontend
Navegador ──► web:Nginx ├── documentación
                         │
                         └── /api/ ──► app ──► bd
```

Para que el catálogo siguiera funcionando utilizaste ya `proxy_pass`, pero el bloque `/api/` estaba dado y no debías modificarlo.

Hoy lo vas a escribir tú y cambiarás una propiedad mucho más importante: **ya no habrá una sola copia de `app`, sino tres**.

El resultado será:

```text
                              ┌── app-1 ──┐
Navegador ──► web:Nginx ──► /api ├── app-2 ──┼──► bd
                              └── app-3 ──┘
```

En esta sesión trabajarás en local para poder observar con claridad el comportamiento del proxy, las réplicas y el almacenamiento compartido. El despliegue sobre una máquina pública se realizará en la siguiente actividad, cuando tenga sentido disponer de nombres accesibles desde Internet para trabajar HTTPS y certificados.

## Qué vas a practicar

- **Convertir** el reenvío mínimo de la sesión anterior en una configuración completa de proxy inverso.
- **Reenviar** la información necesaria sobre la petición original.
- **Balancear** peticiones entre tres copias de la aplicación.
- **Comprobar** que el servicio continúa respondiendo cuando una réplica falla.
- **Diagnosticar** un problema de estado local que solo aparece al replicar.
- **Compartir** almacenamiento entre varias copias ejecutadas en el mismo host.
- **Interpretar** una configuración de Nginx con `upstream`, resolución dinámica y reintentos sin necesidad de memorizar todas sus directivas.

## Requisitos previos

- La Actividad 3.1 terminada y fusionada en `main`.
- `practicas/compose/compose.yaml` con `web`, `app` y `bd`.
- `practicas/nginx/conf.d/sitios.conf` con los dos sitios de la sesión anterior.
- Frontend estático en `practicas/nginx/sitio-escaparate/escaparate/`.
- Documentación en `practicas/nginx/sitio-docs/`.
- Las imágenes públicas:
  - `ghcr.io/<usuario>/escaparate:sesion-04`
  - `ghcr.io/<usuario>/escaparate-db:1.0.0`

Prepara la rama:

```bash
git switch main
git pull --ff-only
git switch -c sesion-07
```

Y crea:

```text
entregas/
└── tema3/
    └── actividad-3.2/
        ├── actividad-3.2.md
        └── img/
```

Los ficheros técnicos continúan en `practicas/`. `entregas/` contiene evidencias, resultados y reflexiones, no copias del Compose ni de la configuración de Nginx.

---

## Paso 1: Sustituye una copia por tres

Trabaja sobre tu repositorio local.

En `compose.yaml`, sustituye el servicio:

```text
app
```

por:

```text
app-1
app-2
app-3
```

Las tres copias deben:

- utilizar `ghcr.io/<usuario>/escaparate:sesion-04`;
- conectarse a `bd`;
- no publicar ningún puerto;
- esperar al `healthcheck` de `bd`;
- utilizar almacenamiento `filesystem`;
- usar `/data/uploads` como ruta de imágenes;
- tener un identificador distinto mediante `APP_INSTANCE_NAME`.

Utiliza:

```text
app-1
app-2
app-3
```

también como valores de `APP_INSTANCE_NAME`.

Todavía **no compartas ningún volumen de imágenes** entre ellas.

Actualiza `web` para que dependa de las tres copias en lugar de la antigua `app`.

---

## Paso 2: Convierte `/api/` en un proxy balanceado

En la sesión anterior utilizaste `proxy_pass` como una caja negra. Ahora vas a abrirla.

### 2.1. El grupo de aplicaciones

Añade fuera de los bloques `server` esta configuración:

```nginx
upstream backend_pool {
    zone backend_pool 64k;
    resolver 127.0.0.11 valid=5s ipv6=off;

    server app-1:8080 resolve;
    server app-2:8080 resolve;
    server app-3:8080 resolve;
}
```

No necesitas memorizar estas directivas. Interprétalas así:

| Directiva | Idea |
|---|---|
| `upstream` | crea un grupo de servidores |
| `server` | añade una réplica al grupo |
| `resolver 127.0.0.11` | utiliza el DNS interno de Docker |
| `resolve` | permite seguir el nombre aunque cambie su IP |
| `zone` | permite mantener actualizado el estado del grupo |

### 2.2. Reenvía `/api/` al grupo

Sustituye el bloque `/api/` de la sesión anterior por:

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

Lee el bloque antes de continuar:

```text
/api/...
   ↓
Nginx
   ↓
backend_pool
   ├── app-1
   ├── app-2
   └── app-3
```

Las tres directivas de timeout y reintento permiten que una copia que no responda no bloquee demasiado tiempo la petición.

Las cabeceras conservan información relevante de la petición original. Recuerda que el backend ya no recibe la conexión directamente desde el navegador: la recibe desde Nginx.

!!! warning "Conserva `/api/`"
    En esta aplicación los endpoints reales empiezan por `/api/`. Por eso:

    ```nginx
    proxy_pass http://backend_pool;
    ```

    no lleva `/` al final. Añadirla cambiaría la URI que recibe el backend.

### 2.3. Valida y comprueba el reparto

Desde `practicas/compose/`:

```bash
docker compose config
docker compose up -d --remove-orphans
docker compose exec web nginx -t
docker compose ps
```

Comprueba readiness:

```bash
curl -fsS http://escaparate.127.0.0.1.nip.io/api/salud/listo
```

Y observa el reparto:

```bash
for i in $(seq 1 9); do
  curl -s http://escaparate.127.0.0.1.nip.io/api/instancia
  echo
done
```

Deben aparecer:

```text
app-1
app-2
app-3
```

No importa que la secuencia exacta cambie entre ejecuciones.

**Captura 1:** `docker compose ps` y salida del bucle donde aparezcan las tres réplicas.

!!! question "Reflexiona"
    ¿Qué diferencia hay entre que Nginx reenvíe `/api/` a un único servidor y que lo haga a un `upstream`?

---

## Paso 3: Provoca la caída de una réplica

Detén una copia:

```bash
docker compose stop app-2
docker compose ps
```

Comprueba que el servicio sigue respondiendo:

```bash
for i in $(seq 1 9); do
  curl -s -o /dev/null \
    -w "%{http_code} " \
    http://escaparate.127.0.0.1.nip.io/api/instancia
done
echo
```

Después observa qué identificadores siguen apareciendo:

```bash
for i in $(seq 1 9); do
  curl -s http://escaparate.127.0.0.1.nip.io/api/instancia
  echo
done
```

Consulta los logs:

```bash
docker compose logs web --tail=50
```

Arranca de nuevo:

```bash
docker compose start app-2
```

Espera unos segundos y repite el bucle de identificadores.

**Comprueba:**

- el servicio sigue respondiendo con `app-2` detenida;
- durante la parada las respuestas válidas proceden de `app-1` y `app-3`;
- Nginx detecta el problema cuando intenta utilizar la copia;
- al arrancar `app-2`, vuelve a aparecer en el reparto.

**Captura 2:** `app-2` detenida y peticiones respondiendo correctamente desde las otras copias.

!!! question "Reflexiona"
    Nginx no comprueba continuamente `/api/salud/listo` en cada réplica. ¿Cómo descubre entonces que una copia ha dejado de responder? ¿Qué papel tienen el timeout corto y el reintento?

---

## Paso 4: Descubre el problema del estado local

Asegúrate de que las tres copias están activas:

```bash
docker compose start app-1 app-2 app-3
```

Desde el catálogo crea un producto con una imagen.

Averigua su identificador y prueba repetidamente:

```bash
for i in $(seq 1 12); do
  curl -s -o /dev/null \
    -w "%{http_code} " \
    http://escaparate.127.0.0.1.nip.io/api/productos/<id>/imagen
done
echo
```

Si las peticiones se reparten entre las tres copias, deberías observar una mezcla de respuestas correctas y fallidas.

Antes de modificar nada, escribe tu hipótesis en `actividad-3.2.md`:

1. ¿qué información sobre el producto comparten las tres copias?
2. ¿qué dato puede existir únicamente en una de ellas?
3. ¿por qué el balanceo hace visible el problema?

Comprueba los directorios:

```bash
docker compose exec app-1 sh -c 'ls -la /data/uploads'
docker compose exec app-2 sh -c 'ls -la /data/uploads'
docker compose exec app-3 sh -c 'ls -la /data/uploads'
```

La base de datos es común, pero el filesystem de cada contenedor no lo es.

---

## Paso 5: Comparte el estado que necesitan las réplicas

Añade a `compose.yaml` un volumen nombrado:

```text
imagenes-compartidas
```

y móntalo en:

```text
/data/uploads
```

de `app-1`, `app-2` y `app-3`.

Aplica el cambio:

```bash
docker compose up -d
docker compose exec web nginx -t
```

No reinicies manualmente `web`. Las réplicas pueden haberse recreado y recibir nuevas IP internas; Nginx debe seguir localizándolas por sus nombres.

Sube **una imagen nueva** después de añadir el volumen.

No utilices para demostrar la solución el fichero creado antes del cambio.

Repite:

```bash
for i in $(seq 1 12); do
  curl -s -o /dev/null \
    -w "%{http_code} " \
    http://escaparate.127.0.0.1.nip.io/api/productos/<id-nuevo>/imagen
done
echo
```

Comprueba también:

```bash
docker compose exec app-1 sh -c 'ls -la /data/uploads'
docker compose exec app-2 sh -c 'ls -la /data/uploads'
docker compose exec app-3 sh -c 'ls -la /data/uploads'
```

Ahora las tres copias deben ver el mismo fichero.

**Captura 3:** comparación entre la secuencia inestable antes del volumen compartido y la secuencia estable después.

!!! question "Reflexiona"
    Formula en una frase la regla general que explica este fallo.

    ¿Por qué un volumen Docker compartido resuelve el problema mientras las tres réplicas viven en el mismo host, pero no sería suficiente si cada réplica se ejecutara en una máquina diferente?

---

## Paso 6: Documenta lo nuevo y cierra la rama

No vuelvas a documentar todo el procedimiento de Compose.

Añade al `README.md` únicamente lo nuevo de esta sesión:

- que existen tres réplicas de la aplicación;
- que Nginx reparte `/api/` entre ellas;
- cómo comprobar el reparto con `/api/instancia`;
- que solo Nginx publica puerto;
- qué volumen comparten las réplicas para las imágenes.

Incluye también un diagrama sencillo:

```text
                     ┌── app-1 ──┐
cliente ──► Nginx ───┼── app-2 ──┼──► PostgreSQL
                     └── app-3 ──┘
                          │
                          ▼
                 volumen compartido
```

Revisa:

```bash
git status
```

Después:

1. registra los cambios;
2. publica `sesion-07`;
3. abre una Pull Request hacia `main`;
4. fusiona mediante **Create a merge commit**;
5. actualiza tu `main` local.

No necesitas una captura específica del README ni de la Pull Request.

---

## Verificación

Desde un clon limpio del repositorio:

```bash
cd practicas/compose
cp .env.example .env
# completa los valores necesarios

docker compose up -d
docker compose ps
docker compose exec web nginx -t
```

Se comprobará:

```bash
curl -fsS http://escaparate.127.0.0.1.nip.io/ > /dev/null
curl -fsS http://escaparate.127.0.0.1.nip.io/api/salud/listo

for i in $(seq 1 9); do
  curl -s http://escaparate.127.0.0.1.nip.io/api/instancia
  echo
done
```

Debe observarse:

- `web` es el único servicio con puerto publicado;
- `bd`, `app-1`, `app-2` y `app-3` solo son accesibles dentro de la red de Compose;
- `/api/salud/listo` conserva correctamente la ruta al atravesar Nginx;
- `/api/instancia` muestra las tres copias;
- con una réplica parada el servicio continúa respondiendo;
- las tres réplicas comparten `/data/uploads`;
- una imagen subida después de configurar el volumen responde de forma estable;
- la entrega está en `entregas/tema3/actividad-3.2/`;
- la rama llegó a `main` mediante Pull Request.

---

## Qué se entrega

- [ ] `entregas/tema3/actividad-3.2/actividad-3.2.md` con resultados y reflexiones.
- [ ] `entregas/tema3/actividad-3.2/img/` con **tres capturas**.
- [ ] `compose.yaml` con `web`, `bd` y tres réplicas de la aplicación.
- [ ] `sitios.conf` con `upstream` y proxy completo.
- [ ] Demostración del reparto mediante `/api/instancia`.
- [ ] Demostración de continuidad con una réplica parada.
- [ ] Diagnóstico del problema de estado local.
- [ ] Volumen de imágenes compartido por las tres réplicas.
- [ ] Comprobación estable de una imagen después del cambio.
- [ ] `README.md` actualizado con la arquitectura nueva.
- [ ] Pull Request `sesion-07 → main` fusionada mediante merge commit.

---

## ✅ Cierre

Escaparate ya no depende de una única copia de la aplicación. Nginx recibe todas las peticiones públicas y reparte `/api/` entre tres réplicas que comparten PostgreSQL y el almacenamiento que necesitan recuperar.

También has comprobado dos ideas distintas de disponibilidad. Una réplica puede desaparecer sin derribar el servicio porque el proxy tiene alternativas. Pero replicar procesos no basta: cuando un dato necesario vive dentro de una sola copia, la propia replicación crea un fallo nuevo.

El siguiente paso será llevar esta misma arquitectura a una máquina accesible desde Internet y proteger su única puerta pública. En la próxima sesión desplegarás el conjunto en una instancia remota y añadirás HTTPS y control de acceso, concentrando esas responsabilidades en Nginx.
