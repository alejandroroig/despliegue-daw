# 🧪 Actividad 2.3: El conjunto entero con un comando

!!! warning "Descarga la plantilla"
    📄 [Plantilla 2.3 — El conjunto entero con un comando](plantillas/Actividad_2_3_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

Se incorpora gente nueva al equipo y el ritual de bienvenida es siempre el mismo: dos horas de «pues tienes que arrancar primero la base de datos, con estas variables, y crear una red, y luego…». Se ha acabado.

Tu encargo de hoy es dejar el despliegue de Escaparate escrito en un fichero, versionado junto al código, de forma que cualquiera que clone el repositorio tenga la aplicación entera funcionando con un comando. Y con una condición que viene de arriba: **de las tres piezas, solo una puede tener puerto abierto al exterior**.

## Qué vas a practicar

- **Describir** un despliegue completo en un fichero declarativo en vez de en comandos sueltos.
- **Comprobar** qué sobrevive y qué se destruye en cada forma de desmontar el conjunto.
- **Diagnosticar** un fallo de conexión y un fallo de arranque leyendo los logs.
- **Reducir** la superficie expuesta y demostrar que sigue funcionando.
- **Documentar** un procedimiento de despliegue que otra persona pueda seguir sin ayuda.

## Requisitos previos

- La actividad 2.2 terminada: tu imagen de Escaparate publicada en `ghcr.io`, y la de la base de datos de la 2.1.
- El paquete de la actividad, que incluye el `compose.yaml` de partida, el front de Escaparate y el fichero de configuración del servidor web.
- Tu rama de esta sesión, creada antes de empezar:

```bash
git switch main && git pull
git switch -c sesion-05
```

!!! info "Reparto de tiempo orientativo"
    Pasos 1 a 3, unos 30 minutos. Pasos 4 a 6, unos 40. Pasos 7 y 8, unos 20. Esta actividad **cierra el bloque de contenedores**: lo que entregues aquí es el resultado de tres sesiones.

---

## Paso 1 — Dos servicios que se conocen por su nombre

Antes de tocar Escaparate, un conjunto pequeño para entender el ciclo de vida.

Escribe un `compose.yaml` con:

- Un servicio llamado `bd`, a partir de tu imagen de base de datos de la actividad 2.1, con sus credenciales y un **volumen con nombre** para sus datos. **Sin ningún puerto publicado.**
- Un servicio llamado `gestor`, a partir de la imagen `adminer:4.8.1`, publicado en el puerto 8081 de tu equipo.

Levanta el conjunto y entra en `http://localhost:8081`. Cuando el gestor te pida a qué servidor quiere conectarse, tendrás que decírselo: usa **el nombre del servicio**.

**Comprueba**: entras en la base de datos desde el navegador y ves las tablas de Escaparate con sus productos.
**Captura**: el gestor conectado, con el listado de tablas.

---

## Paso 2 — Deja tu marca

Inserta desde el gestor un producto nuevo cuyo nombre incluya tu apellido, para que se identifique como tuyo.

**Captura**: la fila insertada.

---

## Paso 3 — Las tres formas de apagar

Haz esta secuencia en orden. **Antes de cada comprobación, escribe qué esperas que ocurra**; después ejecútalo y compara. Lo que se valora aquí no es acertar, sino que la explicación de tu error —si lo hay— sea correcta.

1. Detén el conjunto y vuelve a levantarlo. ¿Sigue tu producto?
2. Desmóntalo con `down` y vuelve a levantarlo. ¿Sigue tu producto?
3. Desmóntalo con `down -v` y vuelve a levantarlo. ¿Sigue tu producto?

**Captura**: el estado de la tabla después de cada uno de los tres pasos.

!!! question "Reflexiona"
    En dos de los tres casos los contenedores fueron **eliminados** y aun así los datos aparecieron intactos al volver. **¿Dónde estaban guardados mientras no existía ningún contenedor?** Y la pregunta que importa de verdad: si esto fuera la base de datos de una tienda real y alguien escribiera el tercer comando por costumbre, ¿qué se podría hacer para recuperarla?

---

## Paso 4 — Escaparate completo: levanta y falla

Abre ahora el `compose.yaml` que se entrega con la actividad. Describe los tres servicios de Escaparate: `front`, `api` y `bd`. **Tiene un error a propósito.** No lo busques leyendo: lo vas a encontrar como se encuentran en un proyecto real.

Levanta el conjunto y abre el catálogo en el navegador. No va a funcionar: la página carga pero no hay productos.

Diagnostica con el método de la sesión 3: mira si los tres servicios están vivos y lee los logs del que sospeches. **El mensaje de error dice exactamente qué nombre ha intentado resolver la aplicación.** Compáralo con los nombres de servicio declarados en el fichero, corrige y vuelve a levantar.

**Comprueba**: el catálogo muestra los productos.
**Captura**: la línea del log donde se ve el fallo, y el catálogo funcionando después.

!!! question "Reflexiona"
    Una vez corregido, la aplicación resuelve `bd` como si fuera un nombre de máquina de toda la vida. Pero si tú escribes ese mismo nombre en tu navegador, no existe. **¿Por qué el nombre del servicio resuelve dentro de la red y no en tu máquina? ¿Quién responde esa consulta, y a quién no se le ha ofrecido nunca ese listado?**

---

## Paso 5 — Cierra las puertas

Revisa el fichero y quita los puertos publicados de **todo lo que no reciba visitas del exterior**. Al terminar debe quedar exactamente una puerta abierta.

Ahora demuéstralo, y esta es la parte que se corrige:

- **Desde tu equipo**: intenta conectarte a la base de datos y a la API por sus puertos. No deben responder.
- **Desde dentro de la red**: comprueba por separado los dos servicios:
    - Desde el contenedor front, pide el endpoint de salud de la API usando su nombre de servicio.
    - Para comprobar PostgreSQL, utiliza un contenedor temporal que tenga las herramientas de PostgreSQL y conéctalo a la misma red de Compose. Desde él, comprueba que `bd:5432` está disponible.

```bash
docker run --rm --network <nombre-red> postgres:18-alpine \
  pg_isready -h bd -p 5432
```

El objetivo no es instalar herramientas dentro de tus contenedores de aplicación, sino demostrar que ambos servicios son accesibles **desde su red interna y no desde el anfitrión**.

**Comprueba**: el catálogo sigue funcionando con normalidad a pesar de que dos de los tres servicios son inalcanzables desde fuera.
**Captura**: los dos intentos fallidos desde tu equipo y los dos intentos correctos desde dentro.

---

## Paso 6 — Saca las credenciales del fichero

El `compose.yaml` va a ir al repositorio, así que las contraseñas no pueden estar escritas en él. Sustitúyelas por variables, crea el `.env` con los valores reales y un `.env.example` con las mismas claves y valores de mentira. Comprueba que el `.gitignore` que escribiste en la sesión 2 ya excluye el primero —debería— y añade el segundo al repositorio.

Comprueba con el comando que muestra el fichero ya interpolado que Compose está leyendo bien los valores.

**Comprueba**: el conjunto levanta igual y en el `compose.yaml` no queda ninguna credencial.
**Captura**: el `compose.yaml`, el `.env.example` y la salida del fichero interpolado.

---

## Paso 7 — El arranque en frío

Desmonta el conjunto por completo, **borrando también los volúmenes**, y vuelve a levantarlo. El entorno de la actividad está preparado para que, durante la inicialización desde cero, PostgreSQL tarde unos segundos adicionales en empezar a aceptar conexiones.

Observa los logs de la API. El contenedor de la base de datos ya existe, pero el servicio todavía no está listo. La API intenta conectarse demasiado pronto y falla.

Este retraso se ha introducido deliberadamente para que todos podáis observar el mismo problema. En un sistema real la carrera puede aparecer solo algunas veces, lo que precisamente la hace más difícil de diagnosticar.

Arréglalo declarando **cuándo se considera lista** la base de datos y haciendo que la API espere a esa condición, no solo a que el contenedor exista. Después repite el arranque en frío dos veces más.

**Comprueba**: tres arranques en frío consecutivos, los tres correctos, con el estado de salud visible en el listado de servicios.
**Captura**: la línea del log donde la API se queja antes del arreglo, el bloque del fichero que has añadido, y el listado mostrando la base de datos como sana.

!!! question "Reflexiona"
    «Que el contenedor exista» y «que el servicio esté listo» son dos cosas distintas, y acabas de ver la diferencia. **¿Quién decide, y cómo, que una pieza está lista?** Piensa qué tendría que comprobar el sistema en el caso de la base de datos, y qué comprobaría en el caso de la API.

---

## Paso 8 — El procedimiento

Escribe en el `README.md` del repositorio el procedimiento completo de despliegue, pensado para alguien que no ha estado en esta clase:

- Qué necesita tener instalado.
- Qué tiene que rellenar antes de empezar y de dónde saca los valores.
- Los comandos exactos, en orden.
- Cómo comprobar que ha funcionado.
- Cómo detenerlo, y cuál es la diferencia entre las dos formas de desmontarlo.

Cierra la sesión abriendo la petición de fusión de `sesion-05` hacia la rama principal.

**Captura**: el `README` renderizado en el repositorio.

!!! tip "La prueba del algodón"
    Si un compañero puede seguir tu procedimiento sin preguntarte nada, está bien escrito. Si tiene que preguntarte una sola cosa, esa cosa es justamente la que falta.

---

## Si te sobra tiempo

Añade al conjunto un **límite de memoria** para el servicio de la base de datos y comprueba en el listado de contenedores que se ha aplicado. Después bájalo a un valor absurdamente pequeño y vuelve a levantar: verás qué hace el motor cuando una pieza no cabe en lo que le has dado. Devuélvelo a un valor razonable antes de entregar.

---

## Verificación

Sobre un equipo limpio, y partiendo únicamente de tu repositorio:

```bash
git clone <tu-repositorio> && cd <tu-repositorio>
cp .env.example .env      # y se rellenarán los valores
docker compose up -d
docker compose ps
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080
curl -s --max-time 3 localhost:5432 ; echo "salida: $?"
docker compose exec front wget -qO- http://api:8080/api/salud
docker compose down -v && docker compose up -d && sleep 20 && docker compose ps
```

Y debe observarse:

- Que el conjunto levanta **sin más instrucciones que las del `README`**.
- Que los tres servicios aparecen en marcha y la base de datos, como sana.
- Que el catálogo responde en el puerto publicado, con sus productos.
- Que la base de datos **no responde** desde el equipo anfitrión.
- Que la API **sí responde** desde dentro de la red.
- Que tras un arranque en frío completo el conjunto vuelve a quedar sano, sin intervención.
- Que en el repositorio no hay ninguna credencial: ni en el `compose.yaml`, ni en el historial.
- Que la entrega llega a la rama principal **a través de una petición de fusión** desde `sesion-05`.

---

## Qué se entrega

- [ ] El conjunto de calentamiento: dos servicios, volumen con nombre y conexión por nombre de servicio.
- [ ] Las tres formas de apagar, con las predicciones, los resultados y la explicación de las diferencias.
- [ ] El diagnóstico del fallo de conexión a partir de los logs, con la corrección justificada.
- [ ] La reflexión sobre la resolución de nombres dentro y fuera de la red.
- [ ] Las cuatro comprobaciones de puertos: dos fallidas desde fuera y dos correctas desde dentro.
- [ ] El `compose.yaml` sin credenciales, el `.env.example` y la salida del fichero interpolado.
- [ ] La comprobación de salud, con el log del fallo previo y los tres arranques en frío correctos.
- [ ] El `README` con el procedimiento de despliegue completo y reproducible.
- [ ] La petición de fusión de `sesion-05`, fusionada.

---

## ✅ Cierre

Con esto se cierra el bloque de contenedores. Repasa lo que tienes ahora y compáralo con el primer día: una aplicación que solo funcionaba en el ordenador donde se escribió es hoy un conjunto de tres piezas empaquetadas, publicadas en un registro, descritas en un fichero versionado y levantables en cualquier máquina con Docker escribiendo un comando. Y de propina has cerrado dos de las tres puertas que estaban abiertas por descuido.

Queda lo que Compose no puede hacer. Tu aplicación responde en el puerto 8080 de una máquina concreta, no en el 80 ni en el 443 de un nombre que alguien pueda teclear; sirve los ficheros estáticos de cualquier manera y no sabe nada de compresión, de caché ni de tipos de contenido; y ese fichero de configuración del servidor web que hoy has usado sin abrir sigue siendo una caja negra.

En la próxima sesión empezamos por arriba: qué hace exactamente un servidor web, cómo sirve dos sitios distintos desde la misma máquina y cómo se llega hasta él escribiendo un nombre en lugar de una dirección. Y de paso publicarás los informes de pruebas y la documentación de Escaparate, que hasta ahora no han tenido dónde vivir.

La caja negra se abre una semana después, cuando ese mismo servidor deje de servir una aplicación para colocarse delante de varias y repartir el tráfico entre ellas. Entonces esas cuatro líneas que hoy has usado a ciegas serán lo más interesante del fichero.