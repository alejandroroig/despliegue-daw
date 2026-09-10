# 🧪 Actividad 2.1: Ejecutar, inspeccionar y publicar

## Contexto

A partir de ahora las piezas que despliegues viajarán empaquetadas en **imágenes de contenedor**. Antes de construir la imagen completa de Escaparate necesitas dominar el ciclo básico: ejecutar, observar, modificar, eliminar y volver a crear.

Después aplicarás ese conocimiento a una pieza real del proyecto: construirás una imagen de PostgreSQL capaz de inicializar automáticamente la base de datos de Escaparate.

No recibirás una nueva versión de la aplicación. Utilizarás los scripts que ya existen en tu repositorio.

!!! abstract "Cómo vas a trabajar"
    **Ejecutar → observar → comparar → construir → configurar → publicar → comprobar**

    La actividad tiene dos objetivos distintos: primero entenderás la diferencia entre **imagen y contenedor**; después comprobarás que una imagen propia puede construirse en tu equipo y ejecutarse desde un registro.

---

## Qué vas a practicar

- Ejecutar contenedores con nombres y puertos publicados.
- Utilizar logs y una shell para diagnosticar.
- Distinguir imagen y contenedor mediante su ciclo de vida.
- Pasar configuración mediante variables de entorno.
- Construir una imagen sencilla con `FROM` y `COPY`.
- Publicar una imagen en GHCR y volver a descargarla.

---

## Requisitos previos

Comprueba que Docker responde:

```bash
docker version
```

Necesitas además:

- el repositorio `daw-despliegue` de la actividad anterior;
- los scripts:

```text
escaparate/
└── db/
    ├── 01-schema.sql
    └── 02-data.sql
```

- el PAT preparado durante la Actividad 1.2.

Prepara tu rama:

```bash
git switch main
git pull --ff-only
git switch -c sesion-03
```

Crea desde el principio:

```text
entregas/
└── tema2/
    └── actividad-2.1/
        ├── actividad-2.1.md
        └── img/
```

!!! info "Evidencias"
    Solo se piden **cuatro evidencias**. Documenta las respuestas y reflexiones indicadas, no cada comando que ejecutes.

---

## Paso 1: Ejecuta tu primer contenedor

Tu primera imagen será:

```text
nginx:1.30.4-alpine
```

Arranca un contenedor con estas decisiones:

```text
segundo plano
nombre: web-1
puerto del anfitrión: 8080
puerto del contenedor: 80
```

Construye el comando a partir del patrón:

```text
docker run -d --name <nombre> -p <host>:<contenedor> <imagen>
```

Después comprueba:

```bash
docker ps
```

y abre:

```text
http://localhost:8080
```

Debe aparecer la página inicial de Nginx.

!!! tip "Fíjate en la imagen"
    Utilizamos una etiqueta concreta (`1.30.4-alpine`) en lugar de depender de una etiqueta flotante como `latest`.

---

## Paso 2: Obsérvalo desde fuera y desde dentro

Recarga varias veces la página y consulta los logs:

```bash
docker logs web-1
```

Solicita también:

```text
http://localhost:8080/no-existe
```

y vuelve a mirar los logs.

**Comprueba:** puedes distinguir peticiones correctas y fallidas.

Entra después en el contenedor:

```bash
docker exec -it web-1 sh
```

Averigua:

1. qué sistema operativo utiliza;
2. desde qué directorio sirve Nginx sus ficheros.

Puedes ayudarte de:

```bash
cat /etc/os-release
nginx -T 2>/dev/null | grep -E 'root|index'
```

Modifica la página de bienvenida dentro del contenedor para que muestre un texto reconocible. Sal de la shell y recarga el navegador.

**Comprueba:** el navegador muestra tu modificación.

**Evidencia 1:** captura donde se vea `web-1` en ejecución y los logs con al menos una petición correcta y otra fallida.

---

## Paso 3: Comprueba qué pertenece a la imagen y qué al contenedor

Aquí está el experimento principal de esta primera parte.

### 3.1. Detener no es eliminar

Detén y vuelve a arrancar `web-1`:

```bash
docker stop web-1
docker start web-1
```

Abre de nuevo:

```text
http://localhost:8080
```

**Comprueba:** tu modificación continúa allí.

### 3.2. Dos contenedores, una misma imagen

Crea un segundo contenedor desde **la misma imagen**:

```text
nombre: web-2
puerto del anfitrión: 8081
puerto del contenedor: 80
```

Compara:

```text
http://localhost:8080
http://localhost:8081
```

El primer contenedor debe mostrar tu modificación; el segundo, la página original.

Modifica ahora `web-2` con un texto diferente.

**Evidencia 2:** captura donde se vea que `web-1` y `web-2`, creados desde la misma imagen, muestran contenido distinto.

### 3.3. Eliminar sí cambia el resultado

Elimina `web-1` y créalo de nuevo con:

```text
mismo nombre
mismo puerto
misma imagen
```

**Comprueba:** la modificación que hiciste originalmente en `web-1` ha desaparecido.

En `actividad-2.1.md` responde brevemente:

1. ¿Por qué `stop` y `start` conservaron el cambio?
2. ¿Por qué `web-1` y `web-2` podían contener ficheros diferentes?
3. ¿Por qué eliminar y recrear hizo desaparecer la modificación?
4. ¿Qué problema tendría este comportamiento si el fichero modificado contuviera datos importantes?

!!! question "La idea que debes conservar"
    ¿Dónde estaba realmente escrito tu cambio: en la imagen o en la capa de escritura del contenedor?

---

## Paso 4: Limpia el experimento

Detén y elimina los contenedores de Nginx.

Comprueba:

```bash
docker ps -a
```

No debe quedar ningún contenedor de esta primera parte.

??? info "Si te sobra tiempo"
    Ejecuta `docker system df` antes y después de limpiar y observa qué recursos ocupan espacio en el equipo.

---

## Paso 5: Construye la imagen de base de datos

Ahora utilizarás una imagen oficial como punto de partida para crear una imagen propia.

Los scripts de Escaparate están en:

```text
escaparate/db/
├── 01-schema.sql
└── 02-data.sql
```

La imagen oficial de PostgreSQL ejecuta los scripts de `/docker-entrypoint-initdb.d/` cuando inicializa un directorio de datos vacío.

Crea:

```text
practicas/
└── docker/
    └── db/
        └── Dockerfile
```

El Dockerfile debe:

1. partir de `postgres:18-alpine`;
2. copiar `01-schema.sql` y `02-data.sql` a `/docker-entrypoint-initdb.d/`.

!!! info "La decisión importante: el contexto"
    El Dockerfile está en `practicas/docker/db/`, pero los scripts están en `escaparate/db/`.

    Los caminos utilizados por `COPY` se interpretan respecto al **contexto de construcción**, no respecto a la carpeta donde está el Dockerfile.

Construye desde la raíz de `daw-despliegue` utilizando:

```text
Dockerfile: practicas/docker/db/Dockerfile
imagen: escaparate-db:1.0.0
contexto: .
```

Puedes partir de esta forma:

```bash
docker build -f <Dockerfile> -t <imagen> <contexto>
```

Comprueba:

```bash
docker image ls escaparate-db:1.0.0
```

!!! info "Inicializar no es migrar"
    Este mecanismo es útil para aprender a construir la imagen y crear una base inicial. La evolución de un esquema real suele gestionarse mediante herramientas de migración como Flyway o Liquibase.

---

## Paso 6: Ejecuta la misma imagen con configuración distinta

Arranca un contenedor de `escaparate-db:1.0.0` con:

```text
nombre: bd-1
puerto publicado: 5433:5432
POSTGRES_USER: alumno
POSTGRES_PASSWORD: una contraseña de prueba
POSTGRES_DB: escaparate
```

Utiliza `-e NOMBRE=valor` para cada variable.

Espera a que PostgreSQL esté preparado. Puedes comprobarlo con:

```bash
docker logs bd-1
```

Cuando esté listo, ejecuta dentro del contenedor:

```bash
docker exec bd-1 \
  psql -U alumno -d escaparate \
  -c "SELECT count(*) FROM productos;"
```

El resultado debe ser:

```text
8
```

Elimina `bd-1` y crea `bd-2` desde **la misma imagen**, pero con un usuario y una contraseña de prueba diferentes.

Vuelve a ejecutar la consulta utilizando el nuevo usuario.

**Comprueba:** la imagen sigue creando el esquema de Escaparate y aparecen los mismos **8 productos**.

**Evidencia 3:** captura de la consulta realizada sobre `bd-2` con resultado `8`. No muestres ninguna credencial real.

!!! question "Reflexiona"
    La imagen ha funcionado con dos configuraciones distintas. ¿Dónde estaban esos valores si no estaban dentro de la imagen? ¿Qué problema tendría escribir una contraseña real en el Dockerfile?

---

## Paso 7: Publica la imagen en GHCR

Hasta ahora la imagen existe únicamente en tu equipo. Ahora vas a convertirla en una pieza que pueda descargarse desde otra máquina.

### 7.1. Inicia sesión

```bash
docker login ghcr.io -u <tu-usuario>
```

Cuando Docker solicite la contraseña, utiliza el PAT preparado para GHCR.

!!! danger "No pegues el token en comandos que vayas a documentar"
    El PAT no debe aparecer en capturas, Markdown ni historial del repositorio.

### 7.2. Etiqueta y publica

Añade la referencia:

```text
ghcr.io/<tu-usuario>/escaparate-db:1.0.0
```

utilizando `docker tag`, y publícala con `docker push`.

Después localiza el paquete en GitHub y configúralo como **público**.

### 7.3. Demuestra que la copia local ya no es necesaria

Cierra la sesión:

```bash
docker logout ghcr.io
```

Elimina primero los contenedores de PostgreSQL que queden y después las dos referencias locales:

```bash
docker image rm ghcr.io/<tu-usuario>/escaparate-db:1.0.0
docker image rm escaparate-db:1.0.0
```

Comprueba que ya no aparece:

```bash
docker image ls escaparate-db:1.0.0
```

Descárgala de nuevo **sin iniciar sesión**:

```bash
docker pull ghcr.io/<tu-usuario>/escaparate-db:1.0.0
```

Arráncala con unas nuevas credenciales de prueba y verifica de nuevo que:

```sql
SELECT count(*) FROM productos;
```

devuelve `8`.

**Evidencia 4:** captura donde se vea el paquete público en GitHub y la descarga posterior desde GHCR después de haber eliminado las referencias locales.

---

## Paso 8: Documenta e integra

Revisa `actividad-2.1.md`. Debe contener únicamente:

- las respuestas del Paso 3;
- la reflexión sobre configuración del Paso 6;
- las cuatro evidencias;
- cualquier incidencia relevante y cómo la resolviste.

!!! tip "No conviertas la entrega en una transcripción"
    Los comandos reales, el Dockerfile y el historial ya están en el repositorio. Documenta **decisiones, resultados y evidencias**, no cada pulsación de teclado.

Registra los cambios de `sesion-03`, publícalos y abre una Pull Request:

```text
sesion-03 → main
```

Espera a que terminen las comprobaciones. Si son correctas, revisa los cambios y fusiona mediante **Create a merge commit**.

Actualiza después tu rama principal:

```bash
git switch main
git pull --ff-only
```

Comprueba:

```bash
git log --graph --oneline --all --decorate
```

---

## Qué se entrega

Antes de terminar, comprueba:

- [ ] `actividad-2.1.md` con respuestas, reflexiones y cuatro evidencias;
- [ ] `practicas/docker/db/Dockerfile`;
- [ ] imagen pública `ghcr.io/<usuario>/escaparate-db:1.0.0`;
- [ ] prueba de que la imagen funciona después de volver a descargarla;
- [ ] Pull Request `sesion-03 → main` fusionada.

!!! info "Dónde queda la entrega"
    La documentación y el Dockerfile quedan versionados en el repositorio privado. La imagen de GHCR es pública únicamente para permitir su descarga y comprobación.

??? info "Cómo se comprobará"
    El profesor podrá eliminar cualquier copia local, descargar la imagen pública y arrancarla con credenciales de prueba distintas.

    Una comprobación equivalente sería:

    ```bash
    docker logout ghcr.io 2>/dev/null || true
    docker rm -f verifica 2>/dev/null || true
    docker image rm ghcr.io/<usuario>/escaparate-db:1.0.0 2>/dev/null || true

    docker pull ghcr.io/<usuario>/escaparate-db:1.0.0

    docker run -d --name verifica -p 5434:5432 \
      -e POSTGRES_USER=profesor \
      -e POSTGRES_PASSWORD=prueba-verificacion \
      -e POSTGRES_DB=escaparate \
      ghcr.io/<usuario>/escaparate-db:1.0.0

    until docker exec verifica \
      pg_isready -U profesor -d escaparate >/dev/null 2>&1; do
      sleep 1
    done

    docker exec verifica \
      psql -U profesor -d escaparate \
      -c "SELECT count(*) FROM productos;"

    docker rm -f verifica
    ```

    La consulta debe devolver **8**.

---

## ✅ Cierre

Has comprobado de forma práctica que **imagen y contenedor no son lo mismo**: varios contenedores pueden partir del mismo contenido y mantener estados independientes, y eliminar un contenedor elimina también su capa de escritura.

Después has construido una imagen propia de PostgreSQL, has mantenido la configuración fuera del paquete y la has publicado en un registro.

En la próxima sesión aplicarás el mismo proceso a la aplicación completa de Escaparate.
