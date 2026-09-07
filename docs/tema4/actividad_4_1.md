# 🧪 Actividad 4.1: La puerta de entrada a `main`

## Contexto

En enero se incorporan dos personas al equipo de Escaparate. Hasta ahora el repositorio lo tocabas tú solo: abrías tu rama, hacías tu pull request y lo fusionabas cuando te parecía que la cosa iba bien. Con una sola persona eso funciona, porque la comprobación de calidad eres tú y te acuerdas de lo que has probado. Con tres personas fusionando en la misma semana, «yo ya lo he probado» deja de significar nada, y el acuerdo de que `main` es lo que está desplegado se convierte en una lotería.

Tu encargo de hoy es dejar el repositorio preparado antes de irte: que nada pueda entrar en `main` sin haberse construido, probado, medido y escaneado primero, y que eso no dependa de que nadie se acuerde de hacerlo. Cuando vuelvas en enero, la gente nueva se encontrará una puerta, no una costumbre.

## Qué vas a practicar

- Escribir un pipeline que se dispare en cada pull request y ejecutar sus jobs en paralelo.
- Convertir tres comprobaciones que hasta hoy solo informaban —tests, cobertura y vulnerabilidades— en comprobaciones que deciden.
- Manejar un secreto del repositorio y comprobar hasta dónde llega la protección que ofrece la plataforma.
- Proteger `main` para que la fusión dependa del resultado del pipeline y no del criterio de quien pulsa el botón.

## Requisitos previos

- Rama `sesion-11` fusionada y `main` al día, con el proyecto Escaparate y su `Dockerfile` multietapa de la sesión 4 en su sitio.
- Tu repositorio `daw-despliegue` **público**, como lo tienes desde septiembre. Las reglas de protección de rama que vas a usar hoy están disponibles sin coste en repositorios públicos; en privados dependen del plan.
- Acceso de administración a tu propio repositorio (lo tienes: es tuyo).

!!! tip "Hoy no hace falta arrancar el laboratorio"
    Todo lo de esta sesión ocurre en GitHub: los ejecutores los presta la plataforma y no se despliega nada en ningún servidor. Es la única sesión desde la 7 que no empieza rearrancando la instancia y actualizando el registro A. Aprovéchalo, porque son diez minutos que hoy valen mucho. La instancia vuelve el 18 de diciembre.

**Lo que entrego yo hoy:**

- El valor del **secreto desechable** que usarás en el paso 4, publicado en Moodle. Es un valor de juguete y no protege nada: puede aparecer en tu entrega sin ningún problema.
- Un fragmento resuelto del job de imagen y escaneo, que publicaré **solo a las 12:00** y solo si hace falta.

!!! info "Reparto de tiempo orientativo"
    Teoría hasta las 10:45. Descanso hasta las 11:00. Paso 1, hasta las 11:20. Paso 2, hasta las 11:40. Paso 3, hasta las 12:05. Paso 4, hasta las 12:20. Paso 5, hasta las 12:40, con la fusión hecha. `README` y evidencias, hasta las 12:45. A partir de ahí cerramos el proyecto y la documentación de la primera evaluación.

!!! danger "Hoy el pull request se fusiona antes de salir del aula"
    La semana que viene hay exámenes y la siguiente sesión es el 18 de diciembre. Lo que no quede fusionado hoy no se cierra. Ve a por la fusión aunque tengas que dejarte el último paso a medias, y documenta lo que falte.

---

## Paso 1 — Que cada pull request se construya y se pruebe solo

Abre la rama `sesion-12` y añade al repositorio un workflow que se dispare cuando se proponga un cambio hacia `main`, y que en una máquina nueva descargue el código, **fije la versión de Java que usa Escaparate**, compile el proyecto y ejecute sus tests. Guarda además el informe de cobertura como artefacto descargable, incluso cuando el job falle. Tienes el esqueleto en el apunte: adáptalo, no lo copies a ciegas.

Ponle al job un nombre corto y descriptivo, y anótalo: dentro de una hora lo vas a necesitar exactamente igual escrito.

Abre el pull request de la sesión hacia `main` en cuanto tengas el fichero, aunque el resto de la actividad esté por hacer. Es el pull request que fusionarás al final y el escenario donde ocurre todo lo demás.

**Comprueba**: en la pestaña de comprobaciones del pull request aparece tu job y termina en verde. En el registro de ejecución se ve cuántos tests se han ejecutado.
**Captura**: la vista del pull request con el check en verde, y la lista de artefactos de la ejecución.

!!! question "Reflexiona"
    El ejecutor no tiene tu `.env`, ni tu base de datos, ni tu Redis, ni nada de lo que llevas montando desde octubre. Y los tests han pasado igual. ¿Qué parte de Escaparate se ha probado de verdad ahí dentro, y cuál no se ha llegado a tocar?

## Paso 2 — Que la cobertura pueda decir que no

Activa la regla de cobertura que lleva desde septiembre escrita y desactivada en el `pom.xml`, con el umbral acordado del 60 %. Lanza el pipeline y comprueba que sigue en verde: no ha cambiado nada visible, y ese es justo el problema de una regla que nunca se ha probado.

Ahora demuestra que la regla decide de verdad. **Sube el umbral temporalmente a un valor deliberadamente inalcanzable** —99 % vale— y vuelve a lanzar. Busca en el registro la línea concreta en la que la comprobación de cobertura detiene la construcción, y compárala con un fallo de test: no dicen lo mismo ni fallan en el mismo sitio. Mientras el pipeline está en rojo, **fíjate en el botón de fusionar**. Después devuelve el umbral al 60 %.

**Comprueba**: el job falla por la regla de cobertura y el mensaje identifica el contador y el valor que no se alcanza. El informe sigue descargándose como artefacto pese al fallo.
**Captura**: la línea del registro donde la cobertura detiene la construcción, y el pull request en rojo **con el botón de fusión visible en la misma imagen**.

!!! question "Reflexiona"
    El pipeline dice que el proyecto no cumple y el botón de fusionar sigue estando ahí, disponible. Nombra exactamente qué es lo que falta: no es una herramienta, ni un paso del workflow. ¿Dónde vive esa pieza?

!!! tip "Punto de rescate — 11:35"
    Si a las 11:35 no consigues que la cobertura falle con el umbral al 99 %, avísame antes de seguir tocando el `pom.xml`. Lo habitual es que la ejecución que invoca la regla no esté enganchada a la fase correcta y la comprobación ni siquiera llegue a ejecutarse.

## Paso 3 — Que la imagen se construya y se mire

Añade al workflow un **segundo job**, independiente del primero, que construya la imagen del contenedor con el `Dockerfile` de la sesión 4 y después la analice en busca de vulnerabilidades conocidas. Los dos jobs se disparan con el mismo pull request, ninguno depende del otro y cada uno recibe su propia máquina, así que la plataforma puede ejecutarlos en paralelo. Anota también el nombre de este segundo job.

El escaneo tiene que **decidir**, no solo informar: configúralo para que detenga el job cuando encuentre vulnerabilidades de severidad crítica o alta **que tengan corrección disponible**, y para que deje pasar las demás dejando constancia de ellas. Guarda el informe completo como artefacto, y guárdalo también cuando el escaneo bloquee: es entonces cuando hace falta leerlo.

**Comprueba**: los dos jobs aparecen como comprobaciones independientes del mismo pull request y ninguno espera al otro. El informe del escáner es descargable.
**Captura**: la lista de comprobaciones con los dos jobs, y el informe del escáner. Si contiene hallazgos que no han bloqueado, señala uno y explica en una línea por qué no bloquea; si el informe sale limpio, entrega el informe limpio y dilo.

!!! question "Reflexiona"
    El escaneo va después de construir la imagen. Si lo hubieras puesto como primer paso del job, ¿qué habría analizado exactamente? Y una segunda: ¿por qué dejamos pasar una vulnerabilidad alta que todavía no tiene parche, si es igual de peligrosa que una que sí lo tiene?

!!! tip "Punto de rescate — 12:00"
    Este es el paso donde se atasca la sesión: entre el contexto de construcción, el nombre de la imagen y la configuración del escáner hay tres sitios donde equivocarse. Si a las 12:00 no lo tienes en verde, avísame y publico el fragmento resuelto. No pierdas aquí el paso 5, que es el que cierra la sesión.

## Paso 4 — Hasta dónde protege un secreto

Guarda en tu repositorio, como secreto, el valor desechable que he publicado en Moodle. Añade al pipeline un paso que lo use y haz dos intentos de que se vea, documentando el resultado de cada uno:

1. Sacarlo tal cual por el registro.
2. Sacarlo transformado de alguna manera antes de que salga, o por algún sitio que no sea el texto del registro.

**Comprueba**: el primer intento aparece enmascarado; el segundo no necesariamente.
**Captura**: los dos fragmentos de registro, uno al lado del otro.

!!! question "Reflexiona"
    La plataforma ha tapado uno de los dos casos. Explica qué tiene que reconocer exactamente para poder taparlo, y por qué eso no la convierte en una protección. Con eso en la mano: cuando en la sesión que viene el secreto sea la llave de tu servidor, ¿de quién dependerá que no se filtre?

## Paso 5 — Cerrar la puerta y fusionar

Crea sobre `main` una regla de protección que impida el envío directo a la rama y que **exija que las comprobaciones del pipeline estén en verde** para poder fusionar. Los dos jobs del paso 3 tienen que estar entre las comprobaciones exigidas, con el nombre exacto que anotaste. Y marca la opción que hace que la regla **no admita excepciones para quien administra el repositorio**: como el administrador eres tú, si dejas esa puerta abierta seguirás pudiendo fusionar en rojo y no habrás cerrado nada.

Con la regla activa, rompe uno de los tests del proyecto —cambia lo que comprueba, no lo borres—, empújalo y vuelve al pull request. Captura lo que ves. Después deshaz ese cambio con la misma herramienta que aprendiste en la sesión 2 para revertir algo ya publicado, espera al verde y **fusiona el pull request antes de salir del aula**.

**Comprueba**: con el check en rojo el botón de fusión está deshabilitado y la interfaz dice por qué. Con el check en verde vuelve a habilitarse.
**Captura**: la misma zona del pull request en los dos estados, una imagen al lado de la otra.

!!! question "Reflexiona"
    Llevas desde el 18 de septiembre abriendo una rama y un pull request por sesión, once veces, sin que nadie te revisara nunca nada. ¿Qué ha cambiado hoy en el pull request número doce? Contéstalo sin usar la palabra «automático».

!!! tip "Punto de rescate — 12:35"
    Si a las 12:35 la regla de protección no te deja fusionar y no ves por qué, avísame antes de tocar nada más. El error habitual es exigir un nombre de comprobación que no coincide con el del job, y desde fuera se ve en diez segundos.

---

## Si te sobra tiempo

Añade al `README` la insignia de estado del workflow, para que quien abra el repositorio vea de un vistazo si `main` está sano. No puntúa y no la necesita ninguna sesión posterior.

---

## Verificación

Para dar por válida la práctica se ejecutará, con `REPO=<usuario>/daw-despliegue`:

```bash
gh api repos/$REPO/contents/.github/workflows/ci.yml --jq .content | base64 -d
gh run list --repo $REPO --limit 15
gh pr list --repo $REPO --state merged --base main --limit 3
gh pr view <NUM> --repo $REPO --json title,mergedAt,statusCheckRollup
gh api repos/$REPO/rules/branches/main
```

Y debe observarse:

- El workflow se dispara con `pull_request` sobre `main`, declara la versión de Java y define **dos jobs**: uno de construcción y tests, otro de imagen y escaneo.
- La regla de cobertura del `pom.xml` está activa y al 60 %: la comprobación puede fallar, no solo medir.
- El escaneo está configurado con severidad crítica y alta y descartando lo que no tiene corrección disponible, y el informe se conserva también cuando el job falla.
- En el listado de ejecuciones hay **al menos dos fallidas**: la de cobertura del paso 2 y la del test roto del paso 5. Un historial sin ningún rojo significa que la puerta no se ha probado nunca.
- El pull request de la sesión 12 está **fusionado** y sus comprobaciones aparecen en verde.
- La consulta de reglas devuelve una regla sobre `main` con comprobaciones de estado exigidas y sin excepciones. Si has usado la protección clásica de rama en lugar de una regla, esta consulta saldrá vacía y valdrá la captura del paso 5, en la que debe verse que la restricción también te afecta a ti.

---

## Qué se entrega

- [ ] Rama `sesion-12` con pull request **fusionado** hacia `main`.
- [ ] `.github/workflows/ci.yml` en el repositorio, con los dos jobs.
- [ ] `pom.xml` con la regla de cobertura activada y el umbral devuelto al 60 %.
- [ ] Captura del paso 1: check en verde y artefactos de la ejecución.
- [ ] Captura del paso 2: la línea del registro donde la cobertura detiene la construcción, y el pull request en rojo con el botón de fusión todavía disponible.
- [ ] Captura del paso 3: los dos jobs y el informe del escáner, con el comentario de una línea sobre lo que contiene.
- [ ] Captura del paso 4: los dos intentos con el secreto, uno enmascarado y otro no.
- [ ] Captura doble del paso 5: botón bloqueado y botón habilitado.
- [ ] `README` actualizado con una sección que explique qué comprueba el pipeline y qué hacer cuando se pone en rojo.
- [ ] Todo lo anterior en `entregas/tema5/`, y la plantilla en Moodle con la URL del pull request fusionado.

---

## ✅ Cierre

Sales del aula con algo que no tenías al entrar: un repositorio en el que `main` ha dejado de ser una promesa. Cualquiera que llegue en enero —tú incluido, después de los exámenes— se encontrará con que proponer un cambio implica construirlo, probarlo, medir su cobertura y escanear su imagen, y que sin eso el botón de fusionar no está.

Lo que sigue sin ocurrir es lo obvio: que ese cambio llegue al servidor. Hoy la imagen se construye, se comprueba y se tira a la basura; la instancia sigue esperando a que alguien entre a mano un viernes por la mañana. El 18 de diciembre, en la **Actividad 5.2**, esa imagen dejará de tirarse: se publicará etiquetada y se desplegará sola en tu subdominio. Y como todo lo que se despliega solo puede desplegarse mal, ese mismo día aprenderás a volver atrás.

Ahora guarda esto y pasamos al cierre del proyecto y de la documentación de la primera evaluación.