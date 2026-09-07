# 🧪 Actividad 5.1: El catálogo, declarado

## Contexto

Tu equipo lleva desde octubre manteniendo el catálogo a mano sobre una instancia. Funciona, está en internet con su nombre y su certificado, y desde diciembre se despliega solo cuando alguien publica una etiqueta. Pero cada viernes lo levantas tú, y estas tres semanas ha estado apagado porque no había nadie para encenderlo.

La dirección técnica quiere evaluar si la plataforma de contenedores que usa el resto de la empresa —Kubernetes— aporta algo a este servicio. Tu encargo de hoy no es migrar nada a producción: es **construir la misma aplicación sobre un clúster de pruebas, en la misma máquina, y traer datos**. Al final de la sesión tienes que poder decir con criterio qué se ha vuelto más fácil, qué se ha vuelto más caro y qué sigue exactamente igual de roto que antes.

## Qué vas a practicar

- Levantar un clúster local sobre tu instancia y leer lo que trae dentro sin haberlo pedido.
- Declarar la base de datos y el catálogo como Deployment y Service, partiendo de tu propio `compose.yaml`.
- Comprobar en directo la reposición automática de una copia y el escalado por declaración.
- Traducir cada entrada de tu fichero de composición al objeto equivalente, y localizar las que no tienen traducción.

## Requisitos previos

- Tu `README` con la rutina de arranque semanal de la actividad 4.1. Hoy se usa entera y se comprueba si está bien escrita.
- `main` desplegable, tal como quedó al cerrar la S13, con la imagen `v1.0.0` publicada y pública en GHCR.
- Rama `sesion-14` abierta desde `main` al empezar.
- **VS Code conectado a la instancia por SSH remoto.** Hoy escribes manifiestos de cuarenta líneas con sangría significativa: hacerlo en una terminal es pedir errores. Si no lo tienes montado, la extensión de conexión remota y el bloque en tu fichero de configuración SSH son cinco minutos bien invertidos.

Ficheros que se entregan hoy (carpeta `entregados/tema6/`):

- `arrancar-cluster.sh` — instala `minikube` y `kubectl` si faltan y levanta el clúster con los puertos que hará falta publicar hoy **y las próximas dos semanas**. Se ejecuta tal cual. **No lo modifiques**: esos puertos solo se pueden declarar al crear el clúster, así que si lo cambias, la S15 empieza borrándolo y recreándolo.
- `postgres.yaml` — la base de datos declarada, con su volumen y su Service. Es andamiaje: hoy no es objeto de estudio.
- `fragmentos-6-1.yaml` — el trozo de volumen para las imágenes subidas y el Service con el puerto de nodo, para pegar en tu manifiesto.

!!! info "Reparto de tiempo orientativo"
    **10:00** Paso 1, primera mitad (arrancas el laboratorio y lo dejas encendiendo). **10:10 a 10:55**, teoría. **10:55** se retoma el Paso 1, hasta las 11:10. Paso 2, hasta las 11:25. Paso 3, hasta las 11:45. Paso 4, hasta las 12:20. Paso 5, hasta las 12:40. Paso 6, hasta las 13:00. **A las 13:00 se deja de tocar el clúster** y se dedica el resto a la rama y el pull request.

---

## Paso 1 — Devolver diciembre a la vida, y relevarlo

Arranca el laboratorio y la instancia lo primero, antes de la teoría, y actualiza el registro A de tu subdominio a la IP nueva. Cuando volvamos, la máquina tiene que estar en pie.

Después de la teoría, levanta el stack desde `main` y comprueba que el catálogo responde por su nombre y por HTTPS, igual que el 18 de diciembre. Solo entonces, **párala sin destruir nada**: los contenedores se apagan, los volúmenes se quedan.

!!! warning "El orden importa"
    El clúster que vas a crear necesita los puertos 80 y 443 del anfitrión, y los reserva **en el momento de crearse**. Si intentas arrancarlo con el stack de Compose todavía en marcha, fallará y tendrás que borrarlo y empezar de nuevo. Primero paras Compose, después arrancas el clúster. Sin excepciones.

Antes de seguir, añade al `README` una sección corta titulada «Volver a Compose» con los pasos exactos para revertir esto. La vas a necesitar en febrero.

**Comprueba**: el catálogo cargaba con datos antes de parar; después, ningún contenedor de la aplicación en marcha y ningún volumen borrado.
**Captura**: `entregas/tema6/6-1-antes-de-parar.png` con la portada funcionando, y la salida del listado de volúmenes después de parar.

!!! question "Reflexiona"
    Has tardado un rato en devolver a la vida algo que dejaste funcionando. ¿Qué paso de tu `README` estaba mal escrito o faltaba? Corrígelo hoy, no en junio.

!!! warning "Lo que acabas de apagar también renovaba el certificado"
    La renovación automática que montaste en la S8 vive en ese stack. Con el stack parado, deja de correr. Anótalo en el `README` como riesgo conocido: la semana que viene necesitas ese certificado.

---

## Paso 2 — El clúster, y lo que trae dentro

Ejecuta el script entregado y espera. La primera vez descarga la imagen base del nodo, así que tarda un par de minutos. Cuando termine, confirma que el nodo aparece listo.

Después mira tres cosas que no has pedido tú: los pods que ya están corriendo en los espacios de nombres del sistema, la clase de almacenamiento que el clúster trae configurada por defecto, y —desde la propia instancia— **qué contenedor de Docker ha aparecido**.

**Comprueba**: un nodo en estado listo; varios pods del sistema en marcha; una clase de almacenamiento marcada como predeterminada; un contenedor llamado `minikube` con puertos publicados.
**Captura**: `entregas/tema6/6-1-cluster.txt` con la salida del listado de nodos, de los pods de todos los espacios de nombres y del listado de contenedores de Docker.

!!! question "Reflexiona"
    **(a)** El clúster entero es un contenedor dentro de tu Docker, y dentro de él hay otro motor de contenedores ejecutando los pods. Explica en dos líneas por qué eso es una simplificación de aula y qué habría en su lugar en un clúster de verdad. **(b)** Localiza **CoreDNS** y uno de los componentes de red del nodo. ¿Qué síntoma esperarías si fallase cada uno: resolución de nombres o comunicación entre servicios?

---

## Paso 3 — La base de datos, dentro

Aplica el manifiesto de Postgres que se te ha entregado, sin modificarlo. Léelo antes: verás una reclamación de volumen, un Deployment de **una sola réplica** y un Service que no publica nada hacia fuera.

**Comprueba**: la reclamación de volumen enlazada y el pod de la base de datos listo, con productos ya cargados.
**Captura**: la salida del listado de reclamaciones de volumen y de pods.

!!! question "Reflexiona"
    Este volumen está declarado como accesible **por un solo nodo a la vez**, que no es lo mismo que «por un solo pod»: hoy, con un único nodo, tres réplicas podrían llegar a montar el mismo directorio. Aun así, poner `replicas: 3` aquí sería un desastre. ¿Por qué es mala idea arrancar tres procesos de PostgreSQL independientes contra el mismo directorio de datos, y qué tendría que ofrecer una solución real de alta disponibilidad para una base de datos?

---

## Paso 4 — El catálogo, con tres copias

Escribe tú el manifiesto de la API en `k8s/base/escaparate-api.yaml`, tomando como punto de partida el ejemplo del apunte. Tiene que quedar declarado:

- Tres réplicas de la imagen `v1.0.0` que ya tienes publicada en GHCR.
- Las variables de conexión a la base de datos apuntando al **nombre del Service** del paso anterior, no a una IP.
- El volumen para las imágenes que suben los usuarios, montado en las tres copias (usa el fragmento entregado).
- Un Service que exponga la API dentro del clúster **y** un puerto de nodo. Usa el **30080**: es el que el clúster tiene publicado y el que espera la corrección.

Cuando esté en marcha, pide varias veces el punto de información de instancia y observa qué devuelve.

**Comprueba**: tres pods listos; el punto de información de instancia devuelve identificadores distintos entre peticiones; el listado de productos trae datos.
**Captura**: `entregas/tema6/6-1-tres-copias.txt` con seis peticiones seguidas y sus respuestas.

!!! question "Reflexiona"
    En la S7 tuviste que escribir tres líneas en el `upstream` de Nginx para que el reparto existiera. Hoy no has escrito ninguna lista de destinos. ¿De dónde ha salido la lista, y qué pasa con ella si mañana escalas a diez copias?

!!! tip "Punto de rescate — 12:10"
    Si a las 12:10 tus pods no arrancan, avísame: publico el manifiesto resuelto y sigues desde ahí. Los tres fallos habituales son que las etiquetas del selector no coinciden con las del molde, que la variable de conexión apunta al nombre del contenedor de Compose en vez de al del Service, y que el volumen de imágenes se declaró sin permitir que el directorio se cree solo.

---

## Paso 5 — Rómpelo, y luego duplícalo

Tres comprobaciones seguidas, con un minuto de observación cada una.

Primero, borra uno de los tres pods de la API a mano y observa qué ocurre en los siguientes segundos. Fíjate en el nombre del que aparece.

Después, escala a seis réplicas y vuelve a tres. Hazlo **modificando el manifiesto y volviendo a aplicarlo**, no con una orden de escalado directa: quieres que el fichero del repositorio siga siendo verdad.

Por último, una comprobación pequeña sobre el volumen de imágenes: crea un fichero dentro de ese directorio desde uno de los pods y compruébalo desde otro.

**Comprueba**: vuelve a haber tres pods, con un nombre nuevo entre ellos; seis pods listos y luego tres; el fichero creado desde un pod se ve desde otro.
**Captura**: `entregas/tema6/6-1-reposicion.txt` con el listado de pods antes y después del borrado, y `6-1-escalado.txt` con el listado durante el escalado a seis.

!!! question "Reflexiona"
    Dos preguntas separadas. **(a)** ¿Quién ha creado el pod nuevo, y por qué no se llama igual que el que borraste? **(b)** El fichero se ve desde las tres copias. ¿Por qué funciona eso hoy, y qué tendría que cambiar en el clúster para que dejara de funcionar? Esta segunda respuesta se cobra en la S16.

---

## Paso 6 — La traducción, por escrito

Abre tu `compose.yaml` de la S5 —el de verdad, el tuyo— y escribe en `docs/kubernetes-traduccion.md` una tabla con una fila por cada clave que aparezca en él: qué objeto o campo de Kubernetes ocupa su lugar y, en una frase, qué cambia. Incluye **las dos claves que no tienen traducción directa** y explica qué haces en su lugar.

Cierra con un párrafo de tres o cuatro líneas: qué ha resultado más fácil hoy que con Compose, qué ha resultado más laborioso, y si a día de hoy migrarías el catálogo a esto. No hay respuesta correcta; hay respuesta justificada.

**Comprueba**: la tabla cubre todas las claves de tu fichero, no las del ejemplo del apunte.
**Captura**: el propio fichero, enlazado desde el `README`.

---

## Si te sobra tiempo

Crea un Pod suelto, sin Deployment por encima, con cualquier imagen ligera. Bórralo y observa qué ocurre. Después borra un pod del catálogo y compara. En dos líneas: qué objeto es el que aporta la reposición, y por qué el apunte insiste en que casi nunca escribirás un Pod a mano.

---

## Verificación

Para dar por válida la práctica se ejecutará, **en la instancia del equipo**:

```bash
kubectl get nodes
kubectl get deploy,svc,pvc
kubectl get pods -l app=escaparate-api
for i in 1 2 3 4 5 6; do curl -s http://localhost:30080/api/instancia; echo; done
kubectl delete pod "$(kubectl get pods -l app=escaparate-api -o name | head -1)"
sleep 25; kubectl get pods -l app=escaparate-api
docker compose -f /opt/escaparate/compose.yaml ps
```

Y **sobre el repositorio**:

```bash
git log --oneline -3 origin/main -- k8s/
```

Debe observarse:

- Un nodo en estado `Ready`.
- Un Deployment de la API con `3/3` disponibles, un Deployment de la base de datos con `1/1`, una reclamación de volumen en estado `Bound` y un Service con el puerto de nodo `30080`.
- Seis peticiones consecutivas que **no** devuelven todas el mismo identificador de copia.
- Tras el borrado, tres pods de nuevo, uno de ellos con nombre y antigüedad distintos.
- Ningún contenedor de la aplicación en marcha bajo Compose, y los volúmenes de Compose intactos.
- Los manifiestos bajo `k8s/base/` en `main`, llegados por un pull request fusionado.

---

## Qué se entrega

- [ ] Rama `sesion-14` fusionada en `main` mediante pull request, con su URL en la plantilla de Moodle.
- [ ] `k8s/base/escaparate-api.yaml` escrito por el equipo, con tres réplicas, variables de conexión por nombre de Service, volumen de imágenes y Service con puerto `30080`.
- [ ] `k8s/base/postgres.yaml` aplicado sin modificar.
- [ ] `docs/kubernetes-traduccion.md` con una fila por clave del `compose.yaml` propio, las dos sin traducción identificadas, y el párrafo de valoración final.
- [ ] Sección «Volver a Compose» en el `README`, con el riesgo del certificado anotado.
- [ ] Evidencias en `entregas/tema6/`: portada antes de parar, estado del clúster, seis peticiones al punto de instancia, reposición y escalado.
- [ ] Respuestas a las cinco reflexiones en la plantilla, incluidas las dos partes de las del Paso 2 y el Paso 5.

---

## ✅ Cierre

Tienes el catálogo corriendo sobre un clúster, con tres copias que se reponen solas y un número que las multiplica. También tienes tres cosas incómodas apuntadas: la base de datos no puede replicarse como está, el volumen de imágenes funciona por casualidad, y todavía no hay ni nombre ni HTTPS delante del servicio, así que lo que has montado hoy es menos publicable que lo que tenías en diciembre.

La semana que viene se arreglan las dos últimas. Sacarás la configuración y las credenciales fuera de los manifiestos, pondrás un Ingress delante para recuperar el nombre y el certificado, y —por fin— harás la actualización progresiva que en diciembre quedó anunciada y sin hacer: desplegar una versión nueva midiendo cuánto se pierde por el camino.