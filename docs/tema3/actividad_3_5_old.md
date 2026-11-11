# Actividad 3.5 — Tomcat y sesiones compartidas con Redis

## Contexto

El despliegue ya tiene Nginx, HTTPS, varias réplicas y registros centralizados. En esta sesión te centrarás en el servidor que ejecuta la aplicación Java y en cómo conserva información entre peticiones de un mismo cliente.

Primero desplegarás el archivo WAR de Escaparate en un Tomcat externo y comprobarás cómo su nombre determina la ruta de acceso. Después observarás qué sucede cuando una sesión se guarda en una sola réplica y el balanceador envía la siguiente petición a otra. Utilizarás Redis como almacén compartido de sesiones, con las imágenes que entrega el profesor y una explicación previa de los conceptos.

## Antes de comenzar

**Toda la actividad es obligatoria y se realiza en EC2.** El código Java y las configuraciones están preparados. El profesor proporciona las dos imágenes de sesiones antes de clase; las instrucciones para prepararlas están incluidas en el material. No se hacen pruebas de rendimiento.

Trabaja en el repositorio de EC2 con la 3.4 terminada. Desde su raíz:

```bash
git switch main
git pull --ff-only
git switch -c sesion-10
mkdir -p entregas/tema3/actividad-3.5/img
```

## 1. Reconoce el punto de partida

Las tres réplicas actuales ejecutan un WAR mediante `java -jar`. En ese caso, la aplicación inicia su Tomcat embebido. Vamos a extraer ese mismo WAR y entregárselo a un Tomcat externo temporal:

```text
Imagen de Escaparate
  └── /app/app.war
         ├── java -jar → Tomcat embebido
         └── webapps/  → Tomcat externo
```

Esta posibilidad depende de cómo se haya empaquetado la aplicación: el proyecto de clase genera un WAR desplegable y declara Tomcat como dependencia `provided`. No cualquier JAR ejecutable puede copiarse a Tomcat.

Desde la raíz del repositorio:

```bash
mkdir -p practicas/aplicaciones
cp soporte-tema3/3.5/compose.tomcat-lab.yaml practicas/compose/
cp soporte-tema3/3.5/tomcat-lab.sh soporte-tema3/3.5/extraer-war.sh practicas/aplicaciones/
printf '\n/practicas/aplicaciones/tomcat/*.war\n' >> .gitignore
bash practicas/aplicaciones/extraer-war.sh
```

El script utiliza la imagen de `app-1`, crea un contenedor sin arrancarlo, extrae `/app/app.war` y retira el contenedor temporal. Debe mostrar `escaparate.war` con un tamaño distinto de cero. No modifica la imagen.

## 2. Lee cuatro decisiones y arranca

Abre `practicas/compose/compose.tomcat-lab.yaml`. Identifica:

| Decisión | Motivo |
|---|---|
| Imagen `tomcat:11.0.25-jre21-temurin-noble` | proporciona un servidor compatible y Java |
| Montaje en `webapps/escaparate.war` | Tomcat descubre y despliega la aplicación |
| `SPRING_CONFIG_IMPORT` y `secrets` | reutilizan la contraseña de PostgreSQL como archivo |
| Sin `ports` | es un laboratorio interno, no otra puerta pública |

El archivo auxiliar se combina con el Compose base y el de observabilidad. El script `tomcat-lab.sh` escribe por ti esa lista de archivos; puedes abrirlo para comprobarlo.

```bash
cd practicas/compose
bash ../aplicaciones/tomcat-lab.sh config --quiet
bash ../aplicaciones/tomcat-lab.sh up -d tomcat-lab
bash ../aplicaciones/tomcat-lab.sh logs -f tomcat-lab
```

Espera a `Started EscaparateApplication`. Pulsa **Ctrl+C** para salir del seguimiento: no detiene Tomcat. Si aparecen errores de autenticación o de despliegue, consulta al profesor antes de continuar.

## 3. Comprueba el contexto

Desde el mismo directorio:

```bash
bash ../aplicaciones/tomcat-lab.sh exec -T web \
  wget -S -O /dev/null http://tomcat-lab:8080/escaparate/api/salud/listo \
  2>&1 | grep 'HTTP/'

bash ../aplicaciones/tomcat-lab.sh exec -T web \
  wget -S -O /dev/null http://tomcat-lab:8080/api/salud/listo \
  2>&1 | grep 'HTTP/'
```

Esperado: **200 con `/escaparate` y 404 sin él**. Aquí el 404 es el resultado buscado, no un fallo de instalación. Lanzamos las consultas desde `web` porque Tomcat solo es accesible en la red interna.

## 4. Cambia una única línea

En `compose.tomcat-lab.yaml`, sustituye el destino del montaje:

```yaml
      - ../aplicaciones/tomcat/escaparate.war:/usr/local/tomcat/webapps/ROOT.war:ro
```

El archivo del anfitrión sigue llamándose `escaparate.war`: solo cambia su nombre dentro de Tomcat. Recrea el contenedor para aplicar el montaje:

```bash
bash ../aplicaciones/tomcat-lab.sh up -d --force-recreate tomcat-lab
bash ../aplicaciones/tomcat-lab.sh logs -f tomcat-lab
```

Espera otra vez a `Started EscaparateApplication` y pulsa Ctrl+C. Repite la consulta sin contexto:

```bash
bash ../aplicaciones/tomcat-lab.sh exec -T web \
  wget -S -O /dev/null http://tomcat-lab:8080/api/salud/listo \
  2>&1 | grep 'HTTP/'
```

Ahora debe devolver **200**.

| Nombre dentro de Tomcat | URL de readiness |
|---|---|
| `escaparate.war` | `/escaparate/api/salud/listo` |
| `ROOT.war` | `/api/salud/listo` |

## 5. Retira el laboratorio y comprueba el servicio habitual

```bash
bash ../aplicaciones/tomcat-lab.sh stop tomcat-lab
bash ../aplicaciones/tomcat-lab.sh rm -f tomcat-lab
sudo docker compose ps
```

Las tres réplicas habituales siguen funcionando. Define tu dirección actual y comprueba:

```bash
read -r -p 'URL HTTPS del catálogo: ' URL
curl --max-time 60 -fsS "${URL%/}/api/salud/listo"
```

Esperado: `{"estado":"ok"}`. HTTPS, Secrets, imágenes compartidas y observabilidad se mantienen. No hemos cambiado el balanceador ni añadido otra aplicación al grupo.

## 6. Comprende qué vamos a comparar

No necesitas haber utilizado Redis ni Spring Session. Primero vamos a entender el problema que resuelven y después lo observaremos con dos imágenes que entrega el profesor **ya construidas**.

### 6.1. ¿Qué es una sesión?

Piensa en una tienda web: visitas una página, añades un producto al carrito y abres otra página. Cada acción produce una petición HTTP diferente. Por sí solo, HTTP no relaciona esas peticiones como acciones del mismo usuario.

Una **sesión** permite a la aplicación conservar información entre peticiones: por ejemplo, el usuario que ha iniciado sesión o un carrito. En el mecanismo que usaremos, los datos se guardan en el servidor y se les asigna un identificador. La sesión suele caducar tras un tiempo de inactividad.

Para simplificar, nuestro ejemplo solo guardará un dato: `usuario = demo`. No habrá contraseñas ni una pantalla de inicio de sesión. El nombre `demo` es ficticio y sirve para comprobar si la aplicación recuerda ese dato.

### 6.2. ¿Para qué sirve la cookie?

La **cookie de sesión** permite que el cliente conserve el identificador y lo envíe en las peticiones siguientes. El proceso será:

1. El cliente pide crear una sesión.
2. La aplicación guarda `usuario = demo` y genera un identificador, por ejemplo `abc123`.
3. La respuesta entrega ese identificador mediante la cabecera `Set-Cookie`.
4. El cliente lo guarda y lo devuelve mediante la cabecera `Cookie` en las siguientes peticiones.
5. La aplicación busca la sesión asociada a ese identificador y recupera el dato guardado.

En este ejercicio, **la cookie lleva el identificador; el dato `usuario = demo` permanece en el servidor**. Las cookies pueden tener otros usos, pero aquí solo necesitamos este.

Usaremos `curl` como cliente en lugar del navegador: `-c` guarda las cookies recibidas en un archivo y `-b` las envía. El script proporcionado hace ambas cosas por ti. Aunque no abras una página web, estás probando el mismo mecanismo HTTP.

### 6.3. ¿Qué ocurre cuando hay tres réplicas?

Cada réplica es un proceso Java independiente, con su propia memoria. Si `app-1` crea la sesión y la guarda en su memoria, `app-2` y `app-3` no reciben automáticamente una copia.

```text
Primera petición → Nginx → app-1: crea la sesión abc123 con usuario demo
Siguiente petición, con la cookie abc123:
    → app-1: encuentra la sesión
    → app-2: no la encuentra
    → app-3: no la encuentra
```

El cliente sigue enviando la misma cookie y Nginx sigue repartiendo peticiones correctamente. El problema es que el servidor elegido puede no tener los datos asociados a esa cookie. Reiniciar la réplica también hace perder las sesiones que solo estaban en su memoria.

En la 3.2 compartiste archivos mediante un volumen. Eso no comparte la memoria de los procesos: una sesión local no se convierte en un archivo del volumen por montarlo en los tres contenedores.

### 6.4. ¿Qué aportan Redis y Spring Session?

**Redis** es un servicio que permite almacenar y consultar datos mediante claves. Trabaja principalmente en memoria y admite distintas estructuras de datos. En esta práctica lo usaremos como un almacén común para las sesiones.

Redis tendrá **su propio contenedor**. Las tres réplicas accederán a él por la red interna de Compose. PostgreSQL seguirá almacenando los datos habituales de Escaparate; Redis tendrá una función diferente: guardar las sesiones de esta prueba.

**Spring Session** es una biblioteca que se incorpora a la aplicación Java y permite gestionar las sesiones utilizando un almacenamiento como Redis. Su integración con Redis se encarga de guardar y recuperar los datos: no tendrás que programar esas operaciones.

```text
Cliente con cookie → Nginx → app-1 ─┐
                         → app-2 ─┼→ Redis: sesión abc123 → usuario demo
                         → app-3 ─┘
```

Ahora cualquier réplica puede recuperar la misma sesión. El cliente continúa enviando una cookie; no se conecta directamente a Redis. Compartir las sesiones no convierte Redis en un servicio infalible: en este laboratorio usamos un único contenedor, sin configurar alta disponibilidad ni persistencia.

### 6.5. ¿Qué contiene cada imagen que entrega el profesor?

Las dos imágenes contienen Escaparate, su entorno de ejecución Java y el mismo código didáctico para crear y consultar sesiones. En este bloque se ejecutan con Tomcat embebido, igual que las réplicas habituales.

| Imagen de aplicación | Diferencia | Almacenamiento de sesión |
|---|---|---|
| `escaparate:sesion-10-local` | Utiliza la sesión local del contenedor web | Memoria de cada réplica |
| `escaparate:sesion-10-redis` | Añade la integración de Spring Session con Redis | Servicio Redis compartido |

**La segunda imagen no contiene el servidor Redis.** Compose lo arranca por separado a partir de la imagen `redis:8-alpine` y proporciona a las aplicaciones su nombre y puerto. Cambiar únicamente una variable no añade la biblioteca: por eso el profesor entrega dos variantes preparadas.

El código didáctico incorpora estas dos rutas:

| Petición | Qué hace | Respuesta esperada |
|---|---|---|
| `POST /api/sesion/abrir?usuario=demo` | Crea una sesión y guarda el dato ficticio | 200 y cookie de sesión |
| `GET /api/sesion/estado` | Busca la sesión identificada por la cookie recibida | 200 si la encuentra; 401 si no |

Las respuestas incluyen `instancia` para reconocer qué réplica atendió la petición. Aquí usamos 401 para hacer visible la ausencia de sesión; **esto no implementa un sistema de autenticación real**.

Antes de ejecutar los comandos, formula una predicción: si conservas la misma cookie y cambia la réplica, ¿en qué variante esperas que siempre se recuerde al usuario `demo`?

### 6.6. Carga el material preparado

El profesor proporciona `imagenes-sesiones.tar.gz`. Copia ese archivo a tu instancia EC2 y, desde el directorio que lo contiene, importa las imágenes:

```bash
sudo docker load -i imagenes-sesiones.tar.gz
sudo docker image inspect escaparate:sesion-10-local escaparate:sesion-10-redis --format '{{.RepoTags}}'
```

Deben aparecer ambas etiquetas. `docker load` importa imágenes ya construidas; todavía no arranca los contenedores. Si el profesor ya las ha cargado en la instancia, basta con comprobar las etiquetas con el segundo comando. No necesitas construir imágenes, modificar Java ni instalar Maven.

Desde la raíz del repositorio, copia el soporte:

```bash
cp soporte-tema3/3.5/compose.sesiones-*.yaml practicas/compose/
cp soporte-tema3/3.5/sesiones.sh soporte-tema3/3.5/probar-sesion.sh practicas/aplicaciones/
cp soporte-tema3/comprobar-replicas.sh practicas/aplicaciones/
```

Los archivos de sesiones se añaden al Compose base y al de observabilidad. El script `sesiones.sh` selecciona esos tres archivos, sin cambiar `.env`. Mantiene Secrets, los archivos compartidos y los logs. Durante esta comparación usa los comandos indicados: un `docker compose up` sin el archivo de sesiones seleccionaría otra vez las imágenes anteriores.

## 7. Observa el problema con una sesión local

Desde `practicas/compose`:

```bash
bash ../aplicaciones/sesiones.sh local up -d --no-deps app-1 app-2 app-3
bash ../aplicaciones/comprobar-replicas.sh
read -r -p 'URL HTTPS del catálogo: ' URL
bash ../aplicaciones/probar-sesion.sh "$URL"
```

Espera a que la comprobación indique **preparado: si** para las tres réplicas. Si alguna falla, no interpretes todavía el resultado de sesiones; consulta sus logs.

El script crea una sesión nueva, guarda su cookie y hace doce consultas con esa misma cookie. Es breve y puedes abrirlo: `curl -c` guarda la cookie y `curl -b` la envía. El archivo temporal se elimina al terminar.

**Esperado:** alternan 200 y 401. El cuerpo de cada respuesta muestra `instancia`, por ejemplo `app-1`. La réplica que creó la sesión responde 200; las otras no la encuentran. Con reparto uniforme y sin otro tráfico, verás aproximadamente cuatro 200 y ocho 401. No es necesario que comiencen en un orden concreto.

No cambies el balanceador para ocultar el problema: lo que falta es compartir el estado de sesión.

## 8. Comparte la sesión con Redis

Abre `compose.sesiones-redis.yaml` y localiza estas decisiones:

| Configuración | Función |
|---|---|
| Imagen con el soporte Spring Session para Redis | Cambia el almacenamiento de sesiones |
| `SPRING_DATA_REDIS_HOST: redis` | Localiza Redis por su nombre de servicio |
| `SPRING_DATA_REDIS_PORT: "6379"` | Puerto interno de Redis |
| `depends_on` con `service_healthy` | Espera a que Redis responda antes de arrancar las réplicas |
| Redis sin `ports` | Evita publicarlo en la instancia |

Arranca Redis y después aplica la imagen preparada a las tres réplicas:

```bash
bash ../aplicaciones/sesiones.sh redis up -d redis
bash ../aplicaciones/sesiones.sh redis up -d app-1 app-2 app-3
bash ../aplicaciones/comprobar-replicas.sh
bash ../aplicaciones/probar-sesion.sh "$URL"
bash ../aplicaciones/sesiones.sh redis exec -T redis \
  redis-cli --scan --pattern 'spring:session*'
```

**Esperado:** doce respuestas 200, atendidas por diferentes instancias, y alguna clave `spring:session...` en Redis. El último comando usa `redis-cli`, el cliente de Redis: `--scan` recorre los nombres de claves y `--pattern` muestra solo los que empiezan por `spring:session`. Una clave es el nombre con el que se localizan datos; este comando muestra su nombre, no todo su contenido. La sesión tiene el mismo identificador en las respuestas. El script crea una cookie nueva: no se ha migrado la sesión anterior de la memoria a Redis.

Ahora cada réplica consulta el mismo estado, aunque Nginx cambie el destino. El volumen de imágenes compartidas no interviene en este proceso. Redis se usa aquí como servicio interno de laboratorio; no se está configurando persistencia ni alta disponibilidad de Redis.

## 9. Deja Redis integrado en el despliegue

La comparación ha terminado. Mantendremos las sesiones compartidas como parte del proyecto. Hasta ahora el script añadía un archivo de sesiones de forma explícita; vamos a incorporar esa configuración al archivo automático de Compose para que un `docker compose up -d` normal conserve Redis.

Desde `practicas/compose`, abre `compose.override.yaml`. Conserva los servicios y las opciones de logging de la 3.4. Para cada `app-1`, `app-2` y `app-3`, incorpora las propiedades de `compose.sesiones-redis.yaml`: `image`, `pull_policy`, `environment` y `depends_on`. Copia también el servicio `redis` al bloque `services` existente. **No crees dos entradas con el mismo nombre ni un segundo bloque `services`.**

El servicio de cada aplicación quedará así en el archivo de observabilidad (cambia el nombre para las otras dos):

```yaml
  app-1:
    image: escaparate:sesion-10-redis
    pull_policy: never
    logging: *fluent-logging
    environment:
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: "6379"
    depends_on:
      redis:
        condition: service_healthy
```

El Compose base sigue aportando PostgreSQL, Secrets, el resto de variables y el volumen de imágenes. Comprueba el resultado combinado antes de aplicarlo:

```bash
sudo docker compose config --quiet
sudo docker compose config --images
sudo docker compose up -d redis app-1 app-2 app-3
bash ../aplicaciones/comprobar-replicas.sh
bash ../aplicaciones/probar-sesion.sh "$URL"
sudo docker compose ps
```

La lista de imágenes debe incluir `escaparate:sesion-10-redis`. Esperamos las tres réplicas listas y doce respuestas 200 con la misma sesión sobre distintas instancias. Comprueba también que siguen llegando peticiones nuevas a Discover. Tomcat externo ya está retirado; Redis queda activo sin publicar puertos.

Al levantar el proyecto en otra instancia habrá que importar primero las imágenes proporcionadas por el profesor. Versionar el Compose no incluye las imágenes Docker.

## Entrega breve

En `entregas/tema3/actividad-3.5/actividad-3.5.md`, entrega tres evidencias (si son capturas, guárdalas en `img/`): el contexto de Tomcat **200 / 404 / 200**, la mezcla de **200 y 401** con sesiones locales y los **200 sobre distintas réplicas junto a una clave de Redis**. Pueden ser salidas de texto, no hace falta capturar cada petición.

Responde brevemente:

1. ¿Qué diferencia hay entre imagen Docker, WAR y servidor Tomcat? ¿Por qué cambió la URL al usar `ROOT.war`?
2. ¿Qué guarda la cookie y dónde se guarda el usuario `demo` en cada variante?
3. ¿Por qué el volumen compartido de la 3.2 no soluciona la sesión local? ¿Qué aporta Redis?

Conserva las configuraciones y scripts en Git. No subas WAR, imágenes Docker exportadas ni cookies. No hay ampliaciones ni pruebas de rendimiento.

## Documenta e integra

Completa el diagrama de `entregas/tema3/memoria-publicacion-ejecucion.md` con Redis y su conexión a las tres aplicaciones. Distingue los tres tipos de datos: productos en PostgreSQL, archivos en el volumen compartido y sesiones en Redis. Indica que las réplicas usan Tomcat embebido y que el Tomcat externo fue un laboratorio temporal.

Actualiza el README con la importación de las imágenes y la comprobación de sesiones. Revisa `git status`, registra los cambios de configuración, scripts y evidencias, publica `sesion-10` y abre la PR hacia `main`. Fusiona mediante **Create a merge commit** cuando los checks sean correctos. No incorpores las imágenes exportadas ni el WAR al repositorio.
