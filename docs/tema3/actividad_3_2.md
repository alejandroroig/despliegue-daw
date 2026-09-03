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
Internet ──► web:Nginx ──► /api ├── app-2 ──┼──► bd
                              └── app-3 ──┘
```

Después desplegarás el mismo conjunto en una **instancia Amazon EC2 de AWS Academy Learner Lab**. Solo `web` quedará expuesto al exterior.

## Qué vas a practicar

- **Convertir** el reenvío mínimo de la sesión anterior en una configuración completa de proxy inverso.
- **Reenviar** la información necesaria sobre la petición original.
- **Balancear** peticiones entre tres copias de Escaparate.
- **Comprobar** que el servicio continúa respondiendo cuando una réplica falla.
- **Diagnosticar** un problema de estado local que solo aparece al replicar.
- **Compartir** almacenamiento entre varias copias ejecutadas en el mismo host.
- **Desplegar** el conjunto desde el repositorio y las imágenes públicas en una máquina remota.

## Requisitos previos

!!! info "Por qué esta actividad se despliega en Amazon EC2"
    Las actividades anteriores también podrían haberse ejecutado sobre una máquina remota, pero trabajar en local permitía centrarse en Docker, Compose y Nginx sin añadir todavía la gestión de infraestructura cloud.

    A partir de esta actividad interesa cambiar de escenario. Reutilizarás una **instancia Amazon EC2 de AWS Academy Learner Lab** preparada previamente en el módulo de **Infraestructura en la Nube (INU)**. En Despliegue no se evalúa crear la VPC, la instancia ni sus reglas de red: partimos de esa infraestructura para centrarnos en el proxy inverso, el balanceo y el despliegue reproducible.

    El momento también es intencionado. En INU trabajarás después **alta disponibilidad y escalado** con servicios de AWS. Aquí construirás antes una versión manual y visible del problema: tres copias de una aplicación detrás de Nginx, una única puerta de entrada y estado que debe compartirse. Así podrás comparar después qué partes sigues administrando tú y cuáles resuelve la infraestructura cloud.

    Esta práctica **no representa todavía una arquitectura cloud de alta disponibilidad real**: las tres copias se ejecutan dentro de una única instancia EC2. Su objetivo es entender los mecanismos antes de distribuirlos o sustituirlos por servicios gestionados.

    Para esta práctica solo necesitamos que la instancia EC2:

    - sea accesible por SSH;
    - tenga Docker y Docker Compose disponibles;
    - disponga de una IPv4 pública;
    - permita tráfico HTTP entrante por el puerto 80.

- La Actividad 3.1 terminada y fusionada en `main`.
- `practicas/compose/compose.yaml` con `web`, `app` y `bd`.
- `practicas/nginx/conf.d/sitios.conf` con los dos sitios de la sesión anterior.
- Frontend estático en `practicas/nginx/sitio-escaparate/escaparate/`.
- Documentación en `practicas/nginx/sitio-docs/`.
- Las imágenes públicas:
  - `ghcr.io/<usuario>/escaparate:sesion-04`
  - `ghcr.io/<usuario>/escaparate-db:1.0.0`
- Una **instancia Amazon EC2 de AWS Academy Learner Lab**, con Docker y Docker Compose instalados, acceso SSH, IPv4 pública y tráfico HTTP entrante permitido por el puerto 80.
- Acceso válido a tu repositorio privado desde la instancia. No escribas nunca un PAT dentro de una URL, un script o un `README`.

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

## Paso 1: Arranca Learner Lab y la instancia EC2

Pon en marcha **AWS Academy Learner Lab** y arranca la instancia EC2 que vas a utilizar.

Desde la instancia EC2 comprueba:

```bash
docker version
docker compose version
docker ps
```

Anota su **dirección IPv4 pública**.

Construye los dos nombres de esta sesión:

```text
escaparate.<ip-publica>.nip.io
docs.<ip-publica>.nip.io
```

Desde tu equipo:

```bash
dig +short escaparate.<ip-publica>.nip.io
```

Debe devolver la IP pública.

En la sesión anterior `server_name` contenía los nombres locales completos. No queremos editar Nginx cada vez que Learner Lab asigne otra dirección pública a la instancia.

Cambia:

```nginx
server_name escaparate.127.0.0.1.nip.io;
```

por:

```nginx
server_name escaparate.*;
```

y haz lo equivalente con `docs`:

```nginx
server_name docs.*;
```

El servidor `default_server` que devuelve `404` se mantiene.

!!! question "Reflexiona"
    `nip.io` resuelve el nombre hacia la IP y `server_name` decide qué sitio responde después. ¿Qué problema resuelve exactamente el comodín y qué problema sigue resolviendo DNS?

**Comprueba:** los nombres locales de la sesión anterior siguen funcionando y la configuración continúa siendo válida.

**Captura:** resolución del nombre público y fragmentos de los dos `server_name`.

---

## Paso 2: Sustituye una copia por tres y abre la caja negra

Trabaja **en tu repositorio local**, no editando una configuración aislada dentro de la instancia.

En `compose.yaml`, sustituye el servicio `app` por:

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

Ahora sustituye el bloque `/api/` dado en la Actividad 3.1.

### El grupo de aplicaciones

Declara fuera de los bloques `server`:

```nginx
upstream api_escaparate {
    zone api_escaparate 64k;
    resolver 127.0.0.11 valid=5s ipv6=off;

    server app-1:8080 resolve;
    server app-2:8080 resolve;
    server app-3:8080 resolve;
}
```

No necesitas memorizar estas líneas. Léelas así:

```text
upstream    → crea el grupo
resolver    → usa el DNS interno de Docker
resolve     → sigue el nombre si cambia su IP
zone        → permite mantener actualizado el grupo
```

### El reenvío de `/api/`

Completa tu `location /api/` para que utilice `api_escaparate`.

Añade también:

```nginx
proxy_connect_timeout 2s;
proxy_next_upstream error timeout;
proxy_next_upstream_tries 3;
```

Su objetivo es sencillo: si una copia no responde, Nginx no debe quedarse esperando durante mucho tiempo y debe poder probar otra.

Conserva íntegra la ruta `/api/...` y reenvía estas tres informaciones:

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Forwarded-For   $remote_addr;
proxy_set_header X-Forwarded-Proto $scheme;
```

Como Nginx es la primera puerta de confianza, la IP que reenvía es la que él mismo ha observado.

### Valídalo localmente antes de ir a la nube

Desde `practicas/compose/`:

```bash
docker compose config
docker compose up -d --remove-orphans
docker compose exec web nginx -t
docker compose ps
```

Comprueba:

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

**Comprueba:** aparecen `app-1`, `app-2` y `app-3` y el catálogo continúa mostrando productos.

**Captura:** `docker compose ps`, bloque `upstream`, `location /api/` y salida del bucle.

!!! tip "Diagnóstico rápido"
    - `502`: Nginx no alcanza los destinos. Revisa nombres de servicio, puertos y que las tres copias estén iniciadas.
    - `404` únicamente a través del proxy: revisa si `proxy_pass` está alterando `/api/...`.
    - Un único identificador: comprueba `APP_INSTANCE_NAME`, que las tres réplicas estén en marcha y que el `upstream` incluya `zone`, `resolver` y `resolve`.

---

## Paso 3: Publica la rama y despliega en Amazon EC2

Antes de desplegar en remoto, comprueba el estado del repositorio:

```bash
git status
```

Haz un commit coherente y publica la rama:

```bash
git push -u origin sesion-07
```

En la instancia, obtén **esa rama** desde tu repositorio privado utilizando el mecanismo de autenticación que tengas configurado.

No incluyas credenciales en el comando de clonación.

Tras clonar la rama, crea `.env` a partir de `.env.example` y completa sus valores locales.

No instales Java ni Maven y no construyas la aplicación en la instancia.

Desde `practicas/compose/`:

```bash
docker compose pull
docker compose up -d
docker compose ps
docker compose exec web nginx -t
```

Las imágenes de GHCR son públicas, por lo que Docker no necesita iniciar sesión en el registro.

Abre desde **tu equipo**, no desde la instancia:

```text
http://escaparate.<ip-publica>.nip.io/
http://docs.<ip-publica>.nip.io/
```

Y prueba:

```bash
curl -fsS http://escaparate.<ip-publica>.nip.io/api/salud/listo
```

Observa el reparto:

```bash
for i in $(seq 1 9); do
  curl -s http://escaparate.<ip-publica>.nip.io/api/instancia
  echo
done
```

### Comprueba la superficie pública

Desde fuera de la instancia, solo debe ser accesible el puerto de Nginx.

`docker compose ps` no debe mostrar publicaciones para:

```text
app-1
app-2
app-3
bd
```

Comprueba también en la configuración de red de la instancia que no has abierto 8080 ni 5432.

**Comprueba:** catálogo y documentación responden por sus nombres públicos, `/api/salud/listo` funciona y aparecen tres identificadores.

**Captura:** descarga de imágenes, `docker compose ps`, catálogo remoto y bucle de `/api/instancia`.

!!! question "Reflexiona"
    La instancia no tiene Java, Maven ni el código fuente de Escaparate y aun así ejecuta tres aplicaciones Java. ¿Qué artefacto está ejecutando realmente y en qué momento se construyó?

---

## Paso 4: Provoca la caída de una réplica

En la instancia:

```bash
docker compose stop app-2
docker compose ps
```

Lanza varias peticiones:

```bash
for i in $(seq 1 9); do
  curl -s -o /dev/null \
    -w "%{http_code} " \
    http://escaparate.<ip-publica>.nip.io/api/instancia
done
echo
```

Después observa qué identificadores siguen respondiendo:

```bash
for i in $(seq 1 9); do
  curl -s http://escaparate.<ip-publica>.nip.io/api/instancia
  echo
done
```

Consulta los registros de Nginx:

```bash
docker compose logs web --tail=100
```

Arranca de nuevo:

```bash
docker compose start app-2
```

Espera unos segundos y repite el bucle de identificadores.

**Comprueba:**

- el servicio sigue respondiendo con `app-2` parada;
- Nginx no se queda bloqueado durante un minuto intentando conectar con la copia caída;
- durante la parada las respuestas válidas proceden de `app-1` y `app-3`;
- en los logs aparece el fallo de conexión;
- al arrancar `app-2`, vuelve a aparecer automáticamente en el reparto.

!!! question "Reflexiona"
    Nginx no pregunta continuamente si `app-2` está sana. ¿Cómo descubre entonces que ha fallado? ¿Qué papel tienen el timeout corto y el reintento sobre otra copia?

**Captura:** `docker compose ps` con `app-2` parada, bucle durante el fallo y línea relevante del log.

---

## Paso 5: Provoca el problema del estado local y resuélvelo

Asegúrate primero de que las tres copias vuelven a estar activas.

Desde el catálogo crea un producto con una imagen.

Averigua su identificador y prueba repetidamente:

```bash
for i in $(seq 1 12); do
  curl -s -o /dev/null \
    -w "%{http_code} " \
    http://escaparate.<ip-publica>.nip.io/api/productos/<id>/imagen
done
echo
```

Deberías observar una mezcla de `200` y `404`: solo la réplica que guardó físicamente la imagen puede devolverla.

Antes de modificar nada, escribe en `actividad-3.2.md`:

1. tu hipótesis;
2. qué dato está compartido entre las tres copias;
3. qué dato sospechas que vive únicamente en una.

Compruébalo dentro de los contenedores:

```bash
docker compose exec app-1 sh -c 'ls -la /data/uploads'
docker compose exec app-2 sh -c 'ls -la /data/uploads'
docker compose exec app-3 sh -c 'ls -la /data/uploads'
```

### Resuelve el problema desde el repositorio

No edites el Compose únicamente en el servidor.

En tu equipo, añade un volumen nombrado:

```text
imagenes-compartidas
```

y móntalo en:

```text
/data/uploads
```

de **las tres** copias.

Registra y publica el cambio.

En la instancia:

```bash
git pull --ff-only
docker compose up -d
docker compose exec web nginx -t
```

No reinicies `web`: una de las cosas que estás comprobando es que Nginx puede seguir los nombres de las réplicas aunque sus direcciones internas cambien.

Sube **una imagen nueva** después del cambio. No utilices para demostrar la solución el fichero que se guardó antes de montar el volumen.

Repite el bucle de doce peticiones y comprueba el directorio desde las tres réplicas.

**Comprueba:** todas las peticiones de la nueva imagen devuelven `200` y las tres copias ven el mismo fichero.

**Captura:** secuencia de códigos antes del arreglo, contenido diferente de `/data/uploads`, fragmento del volumen compartido y secuencia estable después.

!!! question "Reflexiona"
    Formula en una sola frase la regla general que explica este fallo. ¿Por qué un volumen Docker compartido es suficiente hoy pero dejaría de ser una solución si `app-1`, `app-2` y `app-3` se ejecutaran en tres máquinas diferentes?

---

## Paso 6: Documenta y cierra la sesión

Actualiza `README.md` para que otra persona pueda reconstruir el despliegue con una IP pública diferente.

Debe explicar:

- cómo preparar `.env`;
- cómo obtener la IP pública y formar los nombres `nip.io`;
- por qué `server_name` utiliza `escaparate.*` y `docs.*`;
- qué tres réplicas existen;
- cómo comprobar `/api/salud/listo`;
- cómo observar el balanceo con `/api/instancia`;
- qué único puerto se publica;
- qué volumen comparten las réplicas;
- cómo actualizar el despliegue desde Git.

Incluye en `actividad-3.2.md` un diagrama como mínimo con:

```text
cliente
Nginx
app-1
app-2
app-3
PostgreSQL
volumen de imágenes
```

Revisa:

```bash
git status
```

Publica los últimos cambios, abre una Pull Request:

```text
sesion-07 → main
```

y fusiónala mediante **Create a merge commit**.

**Captura:** README renderizado, diagrama y Pull Request antes de fusionarla.

---

## Verificación

Sobre una instancia reconstruida desde un clon limpio del repositorio:

```bash
docker compose ps
docker compose exec web nginx -t

curl -fsS http://escaparate.<ip-publica>.nip.io/ > /dev/null
curl -fsS http://escaparate.<ip-publica>.nip.io/api/salud/listo
curl -fsS http://docs.<ip-publica>.nip.io/ > /dev/null

for i in $(seq 1 9); do
  curl -s http://escaparate.<ip-publica>.nip.io/api/instancia
  echo
done
```

Debe observarse:

- `web` es el único servicio con puerto publicado;
- `bd`, `app-1`, `app-2` y `app-3` solo son accesibles dentro de la red de Compose;
- catálogo y documentación responden bajo nombres distintos;
- `/api/salud/listo` conserva su ruta al atravesar Nginx;
- `/api/instancia` muestra las tres copias;
- con una réplica parada el servicio continúa respondiendo;
- las tres réplicas comparten `/data/uploads`;
- una imagen subida después de configurar el volumen responde de forma estable;
- la configuración puede desplegarse con otra IP sin modificar `server_name`;
- la entrega está en `entregas/tema3/actividad-3.2/`;
- la rama llegó a `main` mediante Pull Request.

---

## Qué se entrega

- [ ] `entregas/tema3/actividad-3.2/actividad-3.2.md` con resultados y reflexiones.
- [ ] `entregas/tema3/actividad-3.2/img/` con las capturas enlazadas mediante rutas relativas.
- [ ] `compose.yaml` con `web`, `bd` y tres réplicas de la aplicación.
- [ ] `sitios.conf` con `upstream`, proxy completo y nombres que soportan el cambio de IP.
- [ ] Demostración del reparto mediante `/api/instancia`.
- [ ] Demostración de continuidad con una réplica parada.
- [ ] Diagnóstico del problema de las imágenes antes de resolverlo.
- [ ] Volumen de imágenes compartido por las tres réplicas.
- [ ] Comprobación estable de una imagen después del cambio.
- [ ] `README.md` actualizado y diagrama del despliegue.
- [ ] Pull Request `sesion-07 → main` fusionada mediante merge commit.

---

## ✅ Cierre

Escaparate ya no depende de una única copia de la aplicación. Nginx recibe todas las peticiones públicas y reparte `/api/` entre tres réplicas que comparten PostgreSQL y el almacenamiento que necesitan recuperar.

También has comprobado dos ideas distintas de disponibilidad. Una réplica puede desaparecer sin derribar el servicio porque el proxy tiene alternativas. Pero replicar procesos no basta: cuando un dato necesario vive dentro de una sola copia, la propia replicación crea un fallo nuevo.

El siguiente paso será proteger esta única puerta pública. En la próxima sesión añadiremos control de acceso y HTTPS, dejando el cifrado concentrado precisamente en Nginx.
