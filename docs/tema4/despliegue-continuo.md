# 🧩 2. Despliegue continuo

!!!info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/despliegue-continuo.pptx){target="_blank" rel="noopener"}

---

Hace dos semanas dejaste una puerta montada delante de `main`. Después vinieron los exámenes, y es muy probable que ahora mismo no recuerdes ni qué disparaba tu workflow ni cómo se llamaban los jobs. No pasa nada: no vamos a repasarlo de memoria. Abre tu repositorio, entra en la última ejecución del pipeline —la del 4 de diciembre— y míralo. El pipeline es la documentación de sí mismo, y esa es una de las razones por las que existe.

Lo que veas ahí es el punto de partida de hoy. Y también su límite: esa ejecución construyó una imagen, la comprobó, la escaneó y **la tiró a la basura**. Tu instancia lleva desde el 4 de diciembre esperando a que alguien entre a mano un viernes por la mañana. Hoy dejamos de entrar.

---

## ⏸️ Reengancha: qué hace hoy tu pipeline

Antes de añadir nada, reconstruye lo que tienes mirándolo, no recordándolo. Abre `.github/workflows/ci.yml` y completa esta tabla en tu cuaderno:

| Pregunta | Dónde se responde |
|---|---|
| ¿Qué suceso lo dispara? | El bloque `on` del principio |
| ¿Cuántos jobs hay y qué hace cada uno? | Las claves bajo `jobs` |
| ¿En qué paso se decide si el código pasa o no? | El que ejecuta `verify` |
| ¿Qué hace que un hallazgo del escáner bloquee? | La configuración de severidad del segundo job |
| ¿Qué impide fusionar cuando algo está en rojo? | **Nada de lo que hay en este fichero**: está en la regla de protección de `main` |

Esa última fila conviene tenerla fresca. Tu pipeline no impide nada: solo produce un veredicto. Lo que impide es la regla del repositorio. Hoy añadimos un tercer actor —uno que sí toca cosas— y esa separación entre quien opina y quien actúa va a volver a aparecer.

---

## 🚦 Entrega continua y despliegue continuo no son lo mismo

Las dos expresiones se usan como sinónimos y no lo son. La diferencia está en un único punto: **si queda o no una decisión humana entre el código válido y la producción**.

| | Integración continua | Entrega continua | Despliegue continuo |
|---|---|---|---|
| Qué consigue | Que lo que entra en `main` **supere las puertas de calidad que hayas definido** | Que lo que está en `main` **pueda** desplegarse en cualquier momento | Que lo que está en `main` **esté** desplegado |
| Dónde acaba | En un veredicto | En un artefacto publicado y listo | En producción |
| Decisión humana | Fusionar | Fusionar y decidir cuándo sale | Solo fusionar |
| Qué tienes tú | ✅ desde el 4 de diciembre | Lo montas hoy | Lo dejas a un paso |

Que la tercera columna sea la mejor no es evidente. Desplegar cada fusión exige mucha confianza en las pruebas, y hay negocios donde la salida a producción depende de una campaña, de un aviso legal o de que haya alguien de guardia. **Elegir entrega continua no es quedarse corto: es reservarse el cuándo.** Lo que no es aceptable es que ese «cuándo» cueste una hora de trabajo manual, porque entonces la decisión ya no es cuándo, sino si compensa.

Fíjate además en que la primera columna dice «supere las puertas que hayas definido», no «esté sano». Dentro de un rato vas a ver una versión atravesar las cuatro comprobaciones y romper la aplicación al arrancar: un pipeline en verde significa exactamente lo que hayas puesto dentro, ni un gramo más.

### `main` deja de significar lo que significaba

Desde septiembre trabajas con una norma: **`main` es lo que está desplegado**. Era cierta mientras el despliegue lo hacías tú desde `main` un viernes por la mañana. A partir de hoy deja de serlo:

| | Hasta la sesión 12 | Desde hoy |
|---|---|---|
| `main` | Aproximadamente, lo desplegado | El último estado integrado que **ha superado las puertas de calidad**: un candidato a despliegue |
| Producción | Lo mismo, si nadie se ha despistado | La **etiqueta** que se desplegó por última vez |

Y sí, «candidato» es la palabra, no «versión desplegable»: dentro de un rato vas a desplegar una que superó todas las puertas y rompió el servicio.

Que `main` vaya por delante de producción no es un fallo: es lo normal en cuanto existen versiones. El fallo es no saber cuál está corriendo. Por eso, a partir de hoy, «¿qué hay en producción?» no se responde mirando el repositorio, sino mirando qué etiqueta se desplegó.

---

## 📦 Publicar la imagen: la etiqueta deja de ser decorativa

Desde la sesión 4 publicas imágenes en GHCR con su etiqueta, y desde la 2 usas versionado semántico. Hasta hoy las etiquetas las ponías a mano después de probar a mano, así que eran una anotación. A partir de hoy son **la referencia con la que el servidor decide qué arranca**, y eso las obliga a comportarse:

- **Una etiqueta de versión no se reescribe nunca.** Si `1.4.0` señala hoy a una imagen y mañana a otra, «volver a la 1.4.0» no significa nada y el rollback deja de existir.
- **`latest` no sirve para desplegar.** Es una etiqueta móvil: te dice «lo último», que es justo lo que no quieres saber cuando lo último está roto.

A partir de ahora hay **tres cosas distintas** que pueden ocurrirle a tu código, y cada una vive en su fichero en lugar de amontonarse en un workflow lleno de condiciones:

| Fichero | Se dispara con | Qué hace |
|---|---|---|
| `ci.yml` | Un pull request hacia `main` | Las puertas de calidad de la sesión 12. No toca producción |
| `release.yml` | Publicar una etiqueta `v*` | Comprueba y construye la imagen, la publica etiquetada en GHCR y pide que se despliegue |
| `deploy.yml` | Que otro workflow lo invoque, o una ejecución manual | Pone a correr **una versión que ya existe en el registro** |

La clave está en la última fila: `deploy.yml` **no construye nada**. Recibe un número de versión y despliega la imagen ya publicada con ese número. Eso es lo que hace posible el rollback, porque volver atrás no puede depender de reconstruir algo de hace tres semanas.

```yaml
# deploy.yml
on:
  workflow_call:
    inputs:
      version: { required: true, type: string }
  workflow_dispatch:
    inputs:
      version:
        description: 'Versión a desplegar (por ejemplo v1.0.0)'
        required: true
```

```yaml
# release.yml
on:
  push:
    tags: ['v*']

jobs:
  comprobar-y-publicar:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      # 1. descargar el código de la etiqueta
      # 2. mvnw verify  (tests + cobertura)
      # 3. construir la imagen
      # 4. escanearla
      # 5. solo si todo lo anterior pasa: subirla como
      #    ghcr.io/<usuario>/escaparate:${{ github.ref_name }}

  desplegar:
    needs: comprobar-y-publicar
    uses: ./.github/workflows/deploy.yml
    with:
      version: ${{ github.ref_name }}
```

- `workflow_call` convierte `deploy.yml` en una pieza reutilizable: existe **una sola** implementación del despliegue y todo lo que quiera desplegar la invoca. Cuando le añadas una comprobación posterior, protegerá igual a las versiones nuevas y a las vueltas atrás, sin escribirla dos veces.
- `workflow_dispatch` con la misma entrada permite lanzarlo a mano indicando la versión. Parece un añadido menor. Es el mecanismo de rollback.
- `github.ref_name` es aquí el nombre de la etiqueta recién publicada. La versión que se pone en la imagen y la que se despliega son la misma, sin teclearla dos veces.
- `permissions` sube a `packages: write` porque este job **sí** escribe. Compáralo con el `contents: read` del workflow de los pull requests: cada workflow pide lo que hace, no lo que podría hacer.
- **No hay `secrets: inherit`**, y no es un olvido. La llave del servidor es un secreto del entorno `produccion`, y `deploy.yml` la obtiene porque su propio job declara ese entorno. `release.yml` no la conoce ni la reenvía: sabe publicar, pero no tiene con qué entrar en tu máquina. Mínimo privilegio otra vez, ahora entre dos piezas de tu propio pipeline.

!!! warning "Lo que escaneaste en el pull request no es lo que vas a desplegar"
    Mira los pasos 2, 3 y 4 de `comprobar-y-publicar` y pregúntate por qué se repiten, si el pull request ya hizo eso mismo hace un rato. Porque **no es la misma imagen**: aquella se construyó, se comprobó y se destruyó, y esta es una construcción nueva. Y porque una etiqueta se puede publicar sobre cualquier estado del repositorio, no necesariamente sobre lo que fusionaste. La protección de `main` decide qué se integra; el workflow de publicación tiene que responder por **el artefacto que sale**. Una imagen no se da por buena porque una construcción anterior del mismo código pasara las pruebas.

!!! info "Para saber más"
    Hay una forma de no repetir el trabajo: construir la imagen una sola vez, identificarla por su huella digital en lugar de por su etiqueta, y **promover ese mismo artefacto** de una etapa a la siguiente sin reconstruirlo. Es lo que hacen los pipelines maduros y elimina de raíz la duda de si lo que despliegas es lo que probaste. Requiere maquinaria que no cabe hoy; quédate con la idea.

!!! warning "`github.ref_name` no es lo que has escrito en el formulario"
    Es el error silencioso de este montaje. En una ejecución manual, `github.ref_name` vale la rama o etiqueta **desde la que lanzaste** el workflow —casi siempre `main`—; el valor que tecleaste está en `inputs.version`. Si el job de despliegue lee `github.ref_name`, tu rollback desplegará algo que no habías pedido, sin dar ningún error. Dentro de `deploy.yml`, la versión se lee **siempre** de `inputs.version`.

!!! warning "Un job que despliega no puede dispararse con un pull request"
    Si lo hiciera, cualquiera que abriera una propuesta de cambio en tu repositorio público estaría ejecutando código con acceso a la llave de tu servidor. Los jobs que tocan producción se disparan con sucesos que solo puede provocar quien tiene permisos de escritura.

---

## 🔑 Cómo llega el pipeline hasta tu servidor

El ejecutor es una máquina de GitHub en internet y tu instancia está en el laboratorio. Para que una hable con la otra hacen falta tres cosas: saber a dónde ir, saber que quien contesta es tu servidor, y tener con qué entrar.

**A dónde ir: el nombre, no la dirección.** Tu laboratorio caduca cada semana y la instancia recibe una IP nueva cada viernes. Si el destino fuera una IP guardada en el repositorio, tu primera tarea de cada sesión sería editarla, y el pipeline automatizaría todo menos lo que se rompe. El destino es el **subdominio delegado de tu equipo**, el mismo desde la sesión 8. Y el nombre **no es un secreto** —está en tu certificado, que es público por diseño—, así que se guarda como variable.

**Saber que es tu servidor.** Cuando un cliente SSH se conecta por primera vez pregunta si te fías de la huella de la máquina. Un ejecutor no puede contestar, así que hay que dársela contestada. La forma cómoda es que pregunte por la clave del servidor justo antes de conectarse, y esa forma cómoda **no verifica nada**: si alguien se ha colado en medio, el ejecutor apuntará su clave como si fuera la buena. Lo correcto es obtener la clave pública del servidor una vez, verificándola, y guardarla como variable. Es el problema de identidad que resolviste con TLS en la sesión 8, en la otra dirección.

**Con qué entrar: una llave dedicada.** Aquí sí hay secreto, y es el más peligroso del curso: la clave privada de un par generado **solo para esto**, cuya pública autorizas en la instancia para un usuario dedicado. Ni tu clave personal de la sesión 2, ni el usuario con el que administras.

```yaml
# deploy.yml
jobs:
  desplegar:
    runs-on: ubuntu-latest
    environment: produccion
    steps:
      - name: Autorizar al ejecutor
        run: |
          install -m 600 -D /dev/null ~/.ssh/id_ed25519
          echo "${{ secrets.CLAVE_DESPLIEGUE }}" > ~/.ssh/id_ed25519
          echo "${{ vars.CLAVE_HOST }}" > ~/.ssh/known_hosts
      - name: Desplegar la versión
        env:
          VERSION: ${{ inputs.version }}
        run: |
          ssh despliegue@${{ vars.HOST_EQUIPO }} \
            "export VERSION=$VERSION; cd /opt/escaparate; docker compose pull; docker compose up -d"
```

- `environment: produccion` asocia el job a un entorno con secretos propios y permite exigir una aprobación manual antes de ejecutarlo. Ahí vive la diferencia entre entrega y despliegue continuo.
- El primer paso escribe la clave privada en el disco efímero del ejecutor y, como host conocido, **la clave del servidor que verificaste tú**.
- `VERSION` sale de `inputs.version`, igual si lo ha invocado `release.yml` que si lo has lanzado a mano. Y fíjate en los puntos y coma: si escribieras `VERSION=$VERSION docker compose pull && docker compose up -d`, la asignación valdría **solo para el primer comando** y el `up` recrearía los servicios sin saber qué versión toca. Es un fallo que no da error: simplemente despliega otra cosa.
- **En el servidor no se compila nada**, igual que desde la sesión 4: la instancia descarga una imagen pública ya construida y ni siquiera necesita credenciales del registro. La versión llega hasta el `compose.yaml`, que no cambia entre despliegues: cambia el valor.

!!! danger "Quien controle tu repositorio controla tu servidor"
    Has creado un camino nuevo: repositorio → ejecutor → instancia. Cualquiera que consiga fusionar código o modificar el workflow puede ejecutar lo que quiera en tu máquina. Se recorta como cualquier otro acceso: usuario dedicado, llave que sirve solo para eso, secreto asociado al entorno y no al repositorio entero, y ninguna acción de terceros que no sea imprescindible.

!!! warning "«Sin `sudo`» no es «sin privilegios»"
    El usuario de despliegue no administra el sistema, pero necesita hablar con el demonio de Docker para levantar los contenedores. Y quien puede hablar con ese demonio puede arrancar un contenedor que monte el disco entero: en la práctica, **equivale a ser administrador de la máquina**. Lo decimos claro porque es el tipo de detalle que se vende como seguridad y no lo es. En un sistema real se acota de otras formas —limitando la llave a ejecutar un único guion, o usando un demonio sin privilegios—; ninguna cabe hoy. Lo que sí cabe es no engañarse sobre lo construido.

!!! info "Para saber más"
    Existe una forma de evitar del todo la llave: que sea la instancia la que **tire** del repositorio en vez de que el pipeline **empuje** hacia ella. Ninguna credencial del servidor sale de él. Es el modelo de la sesión 16 con Argo CD, y la comparación con lo de hoy es la mitad de aquella sesión.

---

## 🔀 Cuatro maneras de sustituir lo que está corriendo

Desplegar es sustituir algo que está funcionando mientras hay gente usándolo. Hay cuatro formas clásicas:

| Estrategia | Cómo | ¿Corta? | Qué cuesta | Cuándo |
|---|---|---|---|---|
| **Recreate** | Para todo y arranca lo nuevo | Sí | Nada | Herramientas internas, ventanas de mantenimiento, o cambios en los que las dos versiones no pueden convivir |
| **Rolling** | Sustituye copia a copia, esperando a que cada nueva esté sana | No | Nada extra, pero conviven dos versiones un rato | El caso normal cuando tienes varias copias **y un orquestador** |
| **Blue-green** | Levanta el entorno nuevo entero al lado y conmuta el tráfico | No | El doble de infraestructura mientras dura | Cuando la vuelta atrás tiene que ser instantánea |
| **Canary** | Manda una fracción del tráfico a la nueva versión y va subiendo | No | Enrutado por peso y métricas separadas por versión | Cambios arriesgados que se quieren medir con tráfico real |

Ahora mira lo tuyo y sé honesto sobre cuál estás haciendo.

**Lo que haces hoy no es rolling.** Tu despliegue son dos órdenes: descargar la imagen y pedirle a Compose que deje el conjunto como dice el fichero. Compose recrea lo que ha cambiado, pero **no es un orquestador**: no secuencia la sustitución copia a copia, no espera a que la nueva esté sana antes de tocar la siguiente y no se detiene si algo va mal. Llamarlo *rolling* sería regalarte garantías que nadie te ha dado.

**Lo que sí tienes son sus requisitos previos**, y no por casualidad: tres copias detrás de Nginx e intercambiables desde la sesión 10, porque la sesión vive en Redis y las imágenes en almacenamiento compartido. Aquel viernes en que la sesión se perdía al balancear estabas pagando por adelantado la factura del Tema 6, que es cuando llega el mecanismo que aprovecha todo esto.

**Blue-green no resulta práctico hoy** porque exigiría dos entornos completos en una máquina donde ya vas justo, y un punto de entrada preparado para conmutar. Ojo: el nombre público y el certificado **no** son el obstáculo —podrían ser los mismos, y sería tu Nginx quien decidiera a dónde va el tráfico—; el obstáculo es duplicar la infraestructura. Por eso es una estrategia que se abarata cuando la infraestructura se pide por API. **Canary se puede insinuar**, porque Nginx reparte por pesos, pero sin métricas separadas por versión no tendrías con qué decidir si sigues o te vuelves.

!!! warning "Cualquier actualización sin corte exige que las dos versiones puedan convivir"
    Mientras se sustituyen las copias hay versiones viejas y nuevas atendiendo a la vez. Si la nueva necesita una columna que la vieja no conoce, o cambia el formato de lo que guarda en la sesión compartida, ese rato de convivencia es un rato de errores intermitentes. Cuando un cambio no admite convivencia, la estrategia correcta es `recreate` con su corte, no disimularlo.

---

## 💥 El fallo que atraviesa todas las puertas

Si el pipeline construye, prueba, mide la cobertura y escanea la imagen, **¿por qué hace falta poder volver atrás?**

Vuelve a la reflexión del paso 1 de la actividad anterior: los tests pasaron en una máquina limpia que no tenía tu base de datos, ni Redis, ni Nginx, ni tu `.env`, ni tu certificado. Todo lo que has montado desde octubre **no estaba allí**. De ahí sale una lista de cosas que ninguna puerta de calidad puede ver:

- Una variable de entorno que falta, o escrita con el valor de otro entorno.
- Una configuración que apunta a donde no debe: un nombre de servicio equivocado, un puerto cambiado, una dirección que en tu ordenador resolvía.
- Un secreto caducado o mal copiado.
- Una dependencia externa que se comporta distinto en producción.
- Un cambio en el esquema de datos que la versión anterior no entiende.

Ninguno rompe un test, porque el test no los mira. Todos rompen el arranque o el primer minuto de servicio real. Por eso la conclusión no es «con integración continua no hacen falta redes de seguridad», sino la contraria: **cuanto más automático es el despliegue, más rápido llega a producción un fallo de configuración, y más falta hace poder deshacerlo en un minuto.**

Esto justifica además separar la configuración de la imagen, decisión que arrastras desde la sesión 5: la misma imagen funciona o no según qué variables reciba. La imagen es la constante y el entorno es la variable, y por eso los secretos se guardan **por entorno**.

---

## ↩️ Volver atrás es desplegar una etiqueta anterior

Cuando algo va mal en producción hay dos instintos y solo uno sirve. El malo es **arreglarlo hacia delante**: encontrar el fallo, corregirlo, abrir un pull request, esperar al pipeline, fusionar, etiquetar y desplegar. Es lo correcto a medio plazo y carísimo a corto: entre veinte minutos y una hora con el servicio caído, con prisa y con gente mirando, que es exactamente la situación en la que se cometen los errores graves.

El bueno es **volver a lo que funcionaba** y luego investigar con calma. Como cada versión tiene su imagen inmutable en el registro, volver atrás no reconstruye nada: es lanzar `deploy.yml` a mano indicando la versión anterior. El tiempo hasta recuperar el servicio deja de depender de lo rápido que encuentres el fallo y pasa a depender de lo que tarde una descarga.

Aquí se cobra haber separado `deploy.yml` de `release.yml`. Si volver atrás implicara reconstruir, estarías compilando código de hace semanas con el servicio caído mientras tanto, y republicar arriesgaría a sobrescribir una etiqueta que ya existía, que es justo lo que rompe la posibilidad de volver atrás la próxima vez.

!!! tip "La velocidad de la vuelta atrás es lo que te deja desplegar a menudo"
    Los equipos que despliegan varias veces al día no lo hacen porque acierten más, sino porque equivocarse les cuesta dos minutos. Un rollback lento convierte cada despliegue en un acontecimiento y hace que se acumulen los cambios, que es lo que de verdad los vuelve peligrosos.

!!! warning "Volver la imagen atrás no vuelve atrás los datos"
    El rollback recupera el código, no lo que ese código hizo mientras corría. Si la versión defectuosa modificó el esquema de la base de datos o escribió datos con un formato nuevo, la anterior se encontrará algo que no espera. Por eso los cambios de esquema se diseñan compatibles hacia atrás y se despliegan por separado del código que los usa. Hoy no vas a tropezar con esto, pero es la primera pregunta antes de prometer que un rollback es seguro.

---

## 🎯 Qué debes saber hacer al salir de esta sesión

- Distinguir integración, entrega y despliegue continuos, y decir qué corre en producción sin mirar `main`.
- Separar el workflow que publica una versión del que la despliega, y explicar por qué el segundo no puede construir nada y el primero no puede fiarse de una comprobación anterior.
- Extender tu pipeline para que, al publicar una etiqueta, construya la imagen, la publique en GHCR y la despliegue en tu instancia.
- Configurar el acceso al servidor con un nombre estable, la clave del host verificada de antemano y una llave dedicada guardada como secreto de entorno.
- Ejecutar un rollback desplegando una etiqueta anterior sin reconstruirla, y medir cuánto tarda el servicio en recuperarse.
- Explicar por qué un pipeline en verde no garantiza que la versión arranque, y qué privilegios tiene de verdad el usuario que despliega.

**Lo que basta con reconocer**: la mecánica interna de blue-green y canary, cómo se implementa una actualización progresiva orquestada, las aprobaciones por entorno con revisores, y los cambios de esquema compatibles hacia atrás.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - Entrega y despliegue continuos se diferencian en una sola cosa: si queda o no una decisión humana entre el código válido y la producción. Reservarse el cuándo es legítimo; que ese cuándo cueste una hora de trabajo manual no lo es.
    - Un pipeline en verde significa «ha superado las puertas que definiste», no «está sano».
    - `main` pasa a ser el último estado integrado que ha superado las puertas: un candidato, no una garantía. Lo que corre en producción lo dice la etiqueta.
    - La imagen que publicas no es la que se escaneó en el pull request, y una etiqueta puede crearse sobre cualquier estado del repositorio: el workflow que publica responde por el artefacto que sale.
    - Una etiqueta de versión no se reescribe jamás, y `latest` no vale para desplegar: sin referencias inmutables no hay vuelta atrás.
    - Publicar y desplegar son workflows distintos: el que despliega recibe un número de versión y no construye nada.
    - En una ejecución manual, la versión que tecleaste está en `inputs`, no en la referencia desde la que lanzaste el workflow.
    - Cada workflow declara los permisos que necesita, y ninguno que toque producción se dispara con un pull request.
    - El destino es un nombre estable, no una IP: el nombre no es un secreto, la llave sí. Y recoger la huella del servidor justo antes de conectarse no verifica nada.
    - Un usuario sin `sudo` que puede hablar con el demonio de Docker tiene, en la práctica, privilegios de administrador sobre la máquina.
    - Compose recrea lo que ha cambiado, pero no secuencia la sustitución ni espera a que la copia nueva esté sana: eso no es un rolling update orquestado.
    - Ninguna puerta de calidad ve una variable que falta, un destino equivocado o un secreto caducado. Cuanto más automático es el despliegue, antes llega ese fallo a producción.
    - Volver atrás es desplegar una etiqueta anterior, no arreglar hacia delante con el servicio caído. Y devuelve el código, no los datos que ese código escribió.

Con esto ya tienes las piezas para la **Actividad 5.2**, donde vas a cerrar el círculo entero por primera vez en el curso: un cambio tuyo, con su etiqueta, construido, publicado y desplegado en tu servidor sin que toques la instancia. Y después lo romperás a propósito de la única forma que tu pipeline no puede detectar, para descubrir en directo por qué ninguna cantidad de comprobaciones sustituye a poder volver atrás. Con esa actividad se cierra la unidad 5 y el resultado de aprendizaje que abriste el 18 de septiembre.