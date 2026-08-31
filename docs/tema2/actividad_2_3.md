# 🧪 Actividad 2.3: El conjunto entero con un comando

## Contexto

Se incorpora una persona nueva al equipo y el procedimiento de puesta en marcha sigue siendo una lista de instrucciones: crear una red, arrancar PostgreSQL con unas variables, comprobar que está preparado y después iniciar Escaparate con otras variables.

Tu encargo es convertir ese procedimiento en **infraestructura declarada en un fichero** y versionarla junto al resto del proyecto.

Escaparate es la misma aplicación integrada que has utilizado hasta ahora:

```text
Navegador
    │
    ▼
Escaparate (frontend + API)
    │
    ▼
PostgreSQL
```

Por tanto, el despliegue principal tendrá **dos servicios**, `app` y `bd`. Al finalizar, únicamente la aplicación debe publicar un puerto hacia el equipo anfitrión.

## Qué vas a practicar

- **Describir** un despliegue completo con Docker Compose.
- **Resolver** servicios por su nombre dentro de una red de Compose.
- **Comprobar** qué ocurre con los datos al detener contenedores, eliminarlos o borrar volúmenes.
- **Diagnosticar** fallos utilizando estado y logs.
- **Reducir** los puertos publicados al mínimo necesario.
- **Separar** configuración versionable de valores locales.
- **Esperar** a que una dependencia esté realmente preparada.
- **Documentar** un procedimiento reproducible.

## Requisitos previos

- Actividad 2.1 terminada y la imagen:

```text
ghcr.io/<usuario>/escaparate-db:1.0.0
```

publicada.

- Actividad 2.2 terminada y la imagen:

```text
ghcr.io/<usuario>/escaparate:sesion-04
```

publicada.

!!! info "No necesitas iniciar sesión en GHCR"
    Las imágenes publicadas en las actividades 2.1 y 2.2 son públicas, por lo que Compose podrá descargarlas sin autenticarse en `ghcr.io`.

    El repositorio `daw-despliegue` sí continúa siendo privado. Las operaciones Git siguen utilizando el método de autenticación configurado desde la actividad 1.2.

- El repositorio `daw-despliegue`.
- Tu rama de esta sesión:

```bash
git switch main
git pull --ff-only
git switch -c sesion-05
```

Crea el directorio de trabajo de esta práctica:

```text
practicas/
└── compose/
```

Durante la actividad crearás dentro de él `compose.yaml`, `.env`, `.env.example` y `00-delay.sql` cuando corresponda.

!!! info "Reparto de tiempo orientativo"
    - Pasos 1 a 3: unos 30 minutos.
    - Pasos 4 a 6: unos 40 minutos.
    - Pasos 7 y 8: unos 25 minutos.

!!! info "Documenta la actividad en el repositorio"
    Crea al comenzar la sesión:

    ```text
    entregas/
    └── tema2/
        └── actividad-2.3/
            ├── actividad-2.3.md
            └── img/
    ```

    Documenta en `actividad-2.3.md` las predicciones, resultados, respuestas y reflexiones de la actividad. Guarda las capturas en `img/` e insértalas en el Markdown mediante rutas relativas.

    Los ficheros técnicos de Compose permanecen en `practicas/compose/`; no los dupliques dentro de `entregas/`.

---

## Paso 1: Primer escenario con Compose

Antes de desplegar Escaparate, vas a trabajar con un primer escenario reducido formado por PostgreSQL y Adminer. El objetivo es aislar dos conceptos de Compose antes de añadir la aplicación: la resolución por nombre de servicio y la persistencia mediante volúmenes.

Escribe temporalmente en `practicas/compose/compose.yaml` dos servicios:

### `bd`

- imagen `ghcr.io/<usuario>/escaparate-db:1.0.0`;
- variables de PostgreSQL;
- volumen con nombre para `/var/lib/postgresql`;
- **ningún puerto publicado**.

!!! info "Ruta de persistencia en PostgreSQL 18"
    Esta actividad utiliza `postgres:18-alpine`. A partir de PostgreSQL 18, la imagen oficial cambió `PGDATA` a una ruta específica de versión y el volumen persistente debe montarse en `/var/lib/postgresql`. No utilices `/var/lib/postgresql/data`, que corresponde al esquema anterior de la imagen y provocaría que los datos no se reutilizaran al recrear el contenedor.

### `gestor`

- imagen `adminer:4.8.1`;
- puerto 8081 del anfitrión publicado hacia el contenedor.

Levanta el conjunto y entra en:

```text
http://localhost:8081
```

Cuando Adminer te pregunte por el servidor PostgreSQL, escribe:

```text
bd
```

No utilices `localhost`.

**Comprueba:** puedes entrar en la base de datos y ver la tabla `productos` con los datos iniciales de Escaparate.

**Captura:** Adminer conectado y listado de tablas.

!!! question "Reflexiona"
    Adminer utiliza el nombre `bd`, aunque tu equipo no conoce ninguna máquina con ese nombre. ¿Quién proporciona esa resolución dentro del conjunto?

---

## Paso 2: Deja tu marca

Desde Adminer añade un producto cuyo nombre incluya tu apellido.

**Captura:** fila insertada.

---

## Paso 3: Las tres formas de apagar

Haz esta secuencia en orden. **Antes de cada prueba escribe qué esperas que ocurra**.

1. Detén el conjunto y vuelve a iniciarlo.
2. Desmóntalo con `down` y vuelve a levantarlo.
3. Desmóntalo con `down -v` y vuelve a levantarlo.

Después de cada operación comprueba si sigue existiendo el producto añadido en el paso 2.

**Captura:** estado de la tabla después de cada caso.

!!! question "Reflexiona"
    En algunos casos los contenedores dejan de existir y, aun así, los datos vuelven a aparecer. **¿Dónde están guardados realmente?** ¿Qué diferencia introduce `-v`? Si esto fuera una base de datos real, ¿qué tendría que existir fuera de este procedimiento para poder recuperar los datos después de una pérdida del volumen?

---

## Paso 4: Pasa al despliegue de Escaparate

Ya has utilizado Compose para conectar dos servicios y comprobar la persistencia de un volumen. Antes de cambiar de escenario, desmonta el conjunto anterior:

```bash
docker compose down
```

Ahora sustituye el contenido de `practicas/compose/compose.yaml` por este punto de partida:

```yaml
services:
  bd:
    image: ghcr.io/<usuario>/escaparate-db:1.0.0
    environment:
      POSTGRES_USER: escaparate
      POSTGRES_PASSWORD: escaparate
      POSTGRES_DB: escaparate
    ports:
      - "5433:5432"
    volumes:
      - datos-bd:/var/lib/postgresql

  app:
    image: ghcr.io/<usuario>/escaparate:sesion-04
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: escaparate
      DB_USER: escaparate
      DB_PASSWORD: escaparate
    depends_on:
      - bd

volumes:
  datos-bd:
```

El conjunto principal tiene únicamente dos servicios:

```text
app
bd
```

No existe un servicio `front`: el frontend está integrado dentro de la propia aplicación Spring Boot.

Este punto de partida contiene **dos decisiones que tendrás que revisar durante la actividad**:

- un error deliberado en la configuración de conexión de `app` a PostgreSQL;
- un puerto de PostgreSQL publicado temporalmente en el anfitrión, que más adelante eliminarás.

No lo busques comparando línea por línea. Levanta el conjunto:

```bash
docker compose up -d
```

y diagnostica qué ocurre utilizando:

```bash
docker compose ps
docker compose logs
```

El contenedor de la aplicación debería dejar una pista clara sobre el nombre de host al que ha intentado conectarse.

Compara ese nombre con el servicio definido en Compose, corrige el problema y vuelve a levantar el conjunto.

**Comprueba:**

```text
http://localhost:8080/
```

muestra los productos y:

```text
http://localhost:8080/api/salud/listo
```

responde correctamente.

**Captura:** línea relevante de los logs antes del arreglo y catálogo funcionando después.

!!! question "Reflexiona"
    Una vez corregido, Escaparate puede utilizar `bd` como nombre de host. Sin embargo, `bd` no funciona si lo escribes en el navegador de tu equipo. **¿Por qué ese nombre existe dentro de la red de Compose y no fuera de ella?**

---

## Paso 5: Publica solo lo que necesita entrar desde fuera

Revisa `compose.yaml`.

Al finalizar:

- `app` debe publicar su puerto 8080;
- `bd` **no debe publicar ningún puerto**.

Comprueba primero desde el equipo anfitrión:

```bash
curl -fsS http://localhost:8080/api/salud/listo
```

Debe responder.

Después intenta acceder a PostgreSQL mediante un puerto del anfitrión. No debe existir ningún mapeo hacia 5432.

Compruébalo también con:

```bash
docker compose ps
```

### Comprueba la red desde dentro

Averigua el nombre de la red creada por Compose:

```bash
docker network ls
```

Utiliza después un contenedor temporal de PostgreSQL conectado a esa red:

```bash
docker run --rm --network <nombre-red> postgres:18-alpine \
  pg_isready -h bd -p 5432
```

Debe indicar que PostgreSQL acepta conexiones.

Haz una comprobación equivalente de la aplicación desde dentro de la red utilizando una imagen temporal con `curl`:

```bash
docker run --rm --network <nombre-red> curlimages/curl:8.21.0 \
  -fsS http://app:8080/api/salud/listo
```

Utilizamos una versión concreta para que la comprobación sea reproducible y no dependa de una etiqueta móvil.

**Comprueba:**

```text
Desde el anfitrión:
app:8080     accesible
bd:5432      no publicado

Desde la red de Compose:
app:8080     accesible
bd:5432      accesible
```

**Captura:** `docker compose ps`, petición correcta a la aplicación, ausencia de puerto publicado en `bd` y las dos comprobaciones internas.

!!! question "Reflexiona"
    Ocultar el puerto de PostgreSQL no impide que Escaparate se conecte. ¿Qué diferencia hay entre que un servicio sea accesible **dentro de una red Docker** y que tenga un puerto **publicado en el anfitrión**?

---

## Paso 6: Saca las credenciales del fichero

`compose.yaml` va a entrar en Git. Las contraseñas utilizadas en tu equipo no.

Sustituye los valores locales por variables y crea:

```text
practicas/
└── compose/
    ├── compose.yaml
    ├── .env
    ├── .env.example
    └── 00-delay.sql
```

### `.env`

Contiene los valores reales utilizados en tu equipo y **no se versiona**.

### `.env.example`

Contiene las mismas claves, pero valores de ejemplo que otra persona pueda sustituir.

Comprueba que las reglas creadas en la actividad 1.2 ignoran `.env`.

Antes de continuar:

```bash
git check-ignore -v practicas/compose/.env
```

Comprueba también el resultado que Compose interpreta:

```bash
docker compose config
```

!!! warning "Cuidado con la captura"
    `docker compose config` puede mostrar los valores ya interpolados. Si contiene tu contraseña real, no incluyas esa salida completa en una captura ni en la entrega.

**Comprueba:** el conjunto levanta igual, `compose.yaml` no contiene la contraseña real y `.env` no aparece como fichero versionable.

**Captura:** fragmento relevante de `compose.yaml`, `.env.example` y comprobación de que `.env` está ignorado.

---

## Paso 7: Contenedor iniciado no significa servicio preparado

Desmonta completamente el conjunto:

```bash
docker compose down -v
```

Crea ahora el fichero:

```text
practicas/compose/00-delay.sql
```

con este contenido:

```sql
SELECT pg_sleep(20);
```

Su única función es introducir una espera reproducible durante la **primera inicialización de un volumen vacío** de PostgreSQL. No representa una migración ni forma parte funcional de Escaparate: es un recurso didáctico para poder observar la diferencia entre "contenedor iniciado" y "servicio preparado".

Añade al servicio `bd` un montaje de **ese fichero concreto** en:

```text
/docker-entrypoint-initdb.d/00-delay.sql
```

De este modo se ejecutará antes de `01-schema.sql` y `02-data.sql`, que ya forman parte de tu imagen `escaparate-db:1.0.0`.

!!! warning "No montes el directorio completo"
    Si montaras una carpeta local completa sobre `/docker-entrypoint-initdb.d/`, ocultarías los scripts que ya contiene la imagen. Monta únicamente `00-delay.sql`.

Vuelve a levantar:

```bash
docker compose up -d
```

Observa:

```bash
docker compose ps
docker compose logs app
docker compose logs bd
```

La aplicación puede intentar conectarse cuando el contenedor `bd` ya existe pero PostgreSQL todavía no acepta conexiones de red.

### Haz que Compose espere a la condición correcta

Añade a `bd` una comprobación de salud basada en `pg_isready`.

La comprobación debe representar lo que realmente necesita `app`: que PostgreSQL acepte **conexiones TCP**. No te limites a comprobar únicamente el socket local del propio contenedor.

Después configura la dependencia de `app` para que espere a que `bd` esté **healthy**, no simplemente iniciado.

!!! tip "Esperar a la aplicación, no un número de segundos"
    Aunque Compose espere a que PostgreSQL esté preparado antes de iniciar `app`, Spring Boot todavía necesita unos segundos para arrancar.

    Para comprobar cuándo Escaparate está realmente preparado, no utilizaremos un `sleep` con una duración elegida a ojo. Utiliza este bucle ya preparado:

    ```bash
    for i in {1..30}; do
        if curl -fsS http://localhost:8080/api/salud/listo; then
            break
        fi
        sleep 2
    done
    ```

    El comando intenta consultar el endpoint de readiness cada 2 segundos y termina en cuanto responde correctamente.

    No necesitas memorizar ni saber construir todavía este bucle. Lo importante es entender la diferencia entre **esperar un tiempo fijo** y **comprobar una condición real**.

Repite tres veces el arranque desde cero:

```bash
docker compose down -v
docker compose up -d
```

Después de cada arranque, observa primero:

```bash
docker compose ps
```

y utiliza el bucle anterior para esperar hasta que Escaparate esté realmente preparado.

**Comprueba:**

- PostgreSQL pasa primero a estado `healthy`;
- `app` comienza a arrancar después;
- el bucle termina cuando `/api/salud/listo` responde correctamente;
- el catálogo funciona;
- los tres arranques en frío consecutivos terminan correctamente.

**Captura:** fallo previo, bloque `healthcheck`, dependencia condicionada y `docker compose ps` después del arreglo.

!!! question "Reflexiona"
    "El contenedor se ha iniciado" y "el servicio está preparado" no significan lo mismo. ¿Qué comprueba `pg_isready` en este caso? Escaparate dispone además de `/api/salud/vivo` y `/api/salud/listo`. ¿Qué diferencia conceptual hay entre ambas comprobaciones?

---

## Paso 8: Completa el procedimiento del repositorio

En la actividad 1.2 dejaste en el `README.md` principal un apartado:

```text
Puesta en marcha
```

que todavía estaba pendiente.

Complétalo ahora para que una persona que acaba de clonar el repositorio pueda levantar Escaparate sin preguntarte nada.

Debe indicar:

- qué necesita tener instalado;
- dónde está el fichero Compose;
- cómo crear `.env` a partir de `.env.example`;
- qué valores debe completar;
- comando para arrancar;
- URL para comprobar el catálogo;
- endpoint para comprobar readiness;
- cómo detener el conjunto sin borrar datos;
- cómo desmontarlo eliminando también los volúmenes;
- qué consecuencia tiene esta última operación.

Antes de cerrar la sesión, revisa `actividad-2.3.md` y comprueba que contiene las predicciones, resultados, reflexiones y capturas solicitadas.

Después sigue el flujo habitual:

1. registra los cambios;
2. publica `sesion-05`;
3. abre una Pull Request hacia `main`;
4. revisa los cambios.

Antes de fusionar la Pull Request, haz una captura de la PR abierta y de la sección `Puesta en marcha` renderizada. Guarda ambas en `entregas/tema2/actividad-2.3/img/`, enlázalas desde `actividad-2.3.md` y registra y publica esos últimos cambios.

Comprueba que la Pull Request se actualiza con el nuevo commit. Cuando toda la documentación esté incluida, fusiónala mediante **Create a merge commit** y actualiza tu `main` local.

!!! tip "La prueba del compañero"
    Un procedimiento es reproducible cuando otra persona puede ejecutarlo sin tener que preguntarte qué querías decir ni qué comando faltaba.

---

## Si te sobra tiempo

Añade un límite de memoria al servicio `bd` y comprueba que se ha aplicado.

Después reduce temporalmente el límite a un valor demasiado pequeño y observa qué ocurre al intentar arrancar PostgreSQL. Devuelve finalmente un valor razonable antes de entregar.

---

## Verificación

La práctica se comprobará desde un clon limpio utilizando una cuenta con acceso al repositorio privado.

```bash
git clone https://github.com/<usuario>/daw-despliegue.git verifica
cd verifica/practicas/compose

cp .env.example .env
```

Se completarán los valores de `.env` y después:

```bash
docker compose up -d
docker compose ps

curl -fsS http://localhost:8080/
curl -fsS http://localhost:8080/api/salud/vivo
curl -fsS http://localhost:8080/api/salud/listo
```

Debe comprobarse que PostgreSQL no publica puerto hacia el anfitrión.

Para verificar la red interna se utilizará la red creada por Compose y contenedores temporales:

```bash
docker run --rm --network <nombre-red> postgres:18-alpine \
  pg_isready -h bd -p 5432
```

y `curlimages/curl:8.21.0` para consultar:

```text
http://app:8080/api/salud/listo
```

Finalmente se probará un arranque completamente nuevo:

```bash
docker compose down -v
docker compose up -d
docker compose ps

for i in {1..30}; do
    if curl -fsS http://localhost:8080/api/salud/listo; then
        exit 0
    fi
    sleep 2
done

echo "Escaparate no ha alcanzado el estado ready"
exit 1
```

La comprobación no espera un número fijo de segundos: consulta periódicamente el endpoint de readiness y termina en cuanto la aplicación está realmente preparada. Si después de 30 intentos no responde, la verificación falla.

Y debe observarse:

- Los dos servicios están en ejecución.
- `bd` aparece como `healthy`.
- `app` se inicia después de que PostgreSQL alcance ese estado.
- El catálogo integrado responde en el puerto 8080.
- `/api/salud/vivo` y `/api/salud/listo` responden correctamente.
- La verificación espera a una condición real de readiness y no depende de un `sleep` fijo.
- PostgreSQL no tiene puerto publicado en el anfitrión.
- Tanto `app` como `bd` son accesibles por nombre desde la red de Compose.
- `compose.yaml` no contiene credenciales reales.
- `.env` no está registrado en Git.
- `.env.example` sí está versionado.
- La sección `Puesta en marcha` del `README.md` permite reproducir el despliegue.
- Los cambios han llegado a `main` mediante la Pull Request de `sesion-05`.

---

## Qué se entrega

- [ ] Prueba inicial con `bd` y Adminer, incluyendo conexión por nombre de servicio.
- [ ] Experimento de persistencia con `stop`, `down` y `down -v`.
- [ ] Diagnóstico del fallo deliberado de conexión de `app`.
- [ ] `practicas/compose/compose.yaml` final con `app` y `bd`.
- [ ] Comprobaciones de accesibilidad desde el anfitrión y desde la red interna.
- [ ] `.env.example` versionado y `.env` correctamente ignorado.
- [ ] `healthcheck` de PostgreSQL y dependencia de `app` condicionada a `service_healthy`.
- [ ] Tres arranques en frío consecutivos correctos.
- [ ] Sección `Puesta en marcha` completa en el `README.md`.
- [ ] `entregas/tema2/actividad-2.3/actividad-2.3.md` con predicciones, resultados y reflexiones.
- [ ] `entregas/tema2/actividad-2.3/img/` con las capturas enlazadas mediante rutas relativas.
- [ ] Pull Request `sesion-05 → main` fusionada.

!!! info "Dónde queda la entrega"
    La evidencia de la actividad queda versionada en el repositorio privado. Los ficheros técnicos permanecen en `practicas/compose/`.

---

## ✅ Cierre

Con esta sesión ya no necesitas recordar una secuencia de comandos para poner en marcha Escaparate. La aplicación, PostgreSQL, su red, la persistencia, la configuración y el orden de arranque están descritos en un fichero que forma parte del repositorio.

También has separado dos conceptos que suelen confundirse al empezar con Docker. Que un servicio sea accesible para otros contenedores no obliga a publicarlo en el equipo anfitrión, y que un contenedor se haya iniciado no significa necesariamente que la aplicación que contiene esté preparada para trabajar.

A partir de ahora podrás entregar el repositorio a otra persona y pedirle que levante el sistema siguiendo la sección `Puesta en marcha`. En el siguiente bloque empezarás a estudiar qué ocurre por delante de la aplicación: cómo un servidor web sirve contenido, responde a distintos nombres y, más adelante, se coloca como punto de entrada antes de Escaparate.
