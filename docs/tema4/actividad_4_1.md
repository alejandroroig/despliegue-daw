# 🧪 Actividad 4.1: El mismo artefacto, dos servidores, y una sesión que deja de perderse
 
!!! warning "Descarga la plantilla"
    📄 [Plantilla 4.1 — Servidor de aplicaciones](plantillas/Actividad_4_1_DAW_Plantilla.docx){target="_blank" rel="noopener"}
 
## Contexto
 
Escaparate lleva desde la sesión 7 publicado en internet con tres copias detrás de tu proxy, y desde la sesión 9 con todo lo que pasa por él registrado en Kibana. Funciona. Pero el equipo de atención al cliente ha abierto una incidencia con un texto que no ayuda nada: *«a veces la aplicación cierra la sesión sola, a otras personas no les pasa, y a la misma persona no le pasa siempre»*. Nadie ha conseguido reproducirlo de forma fiable y por eso lleva dos semanas sin cerrar.
 
Hoy vas a reproducirlo en menos de un minuto, vas a demostrarlo con evidencia que no dependa de lo que te parezca ver en el navegador, y vas a arreglarlo de la única forma que sobrevive a que mañana haya cinco copias en vez de tres. Y de camino vas a hacer algo que llevas pudiendo hacer desde octubre sin saberlo: coger el artefacto que despliegas cada semana y ejecutarlo en un servidor de aplicaciones instalado, para ver con tus manos qué cambia y qué no.
 
## Qué vas a practicar
 
- Reproducir y diagnosticar la pérdida de sesión al balancear entre varias copias.
- Externalizar el estado de sesión a un almacén compartido y demostrar que el problema desaparece.
- Desplegar el `war` de Escaparate en un Tomcat externo ejecutado en contenedor.
- Acceder al gestor de despliegue con el rol adecuado y el mínimo privilegio necesario.
- Comparar el modelo embebido y el modelo de servidor externo sobre tu propio despliegue.
## Requisitos previos
 
- Tu instancia del laboratorio, rearrancada hoy. **Para las sesiones 10 y 11 se usa el tipo de instancia ampliado que indico en el aula virtual**: el stack de hoy suma dos servicios más y en la máquina anterior no cabe.
- Tu repositorio `daw-despliegue` con `main` al día tras fusionar la sesión 9.
- El stack completo de la sesión 9: proxy con las tres copias, almacenamiento compartido, HTTPS y la pila de logs.
- El subdominio de tu equipo, delegado desde la sesión 8, y acceso a su registro de nombres.
- El fichero `compose.tema4.yaml` que publico hoy en el aula virtual. Trae dos servicios listos —el almacén de sesiones y el servidor de aplicaciones—, la ruta exacta donde la imagen de Escaparate guarda el artefacto, y el gestor de Tomcat ya configurado para admitir peticiones desde la red de tu stack.
!!! info "Reparto de tiempo orientativo"
    Teoría hasta las **11:15**. Paso 1, hasta las **11:25**. Paso 2, hasta las **11:45**. Paso 3, hasta las **12:05**. Paso 4, hasta las **12:35**. Pasos 5 y 6, hasta las **13:05**.
 
---
 
## Paso 1 — Recuperar el servicio y abrir la rama
 
El laboratorio ha caducado desde el viernes pasado: las credenciales se han renovado y la instancia tiene una IP nueva. **El nombre de tu servicio, en cambio, no cambia.** Ese fue el motivo de delegar un subdominio por equipo en la sesión 8: el certificado certifica el nombre, no la dirección, así que mientras el nombre siga siendo el mismo y el disco de la instancia siga ahí, el certificado sigue valiendo. Lo único que hay que rehacer cada semana es apuntar el registro del subdominio a la IP nueva.
 
Tu objetivo es volver a tener funcionando lo que dejaste la semana pasada, servido por HTTPS bajo el nombre de tu equipo, y con la rama de hoy abierta. Este trámite se repite cada viernes hasta final de curso, así que si el procedimiento está bien escrito en tu `README` te va a costar cinco minutos; si no lo está, hoy lo arreglas.
 
**Comprueba**: el nombre de tu equipo resuelve a la IP nueva, el catálogo se abre por HTTPS sin avisos del navegador, y tres peticiones seguidas a `/api/instancia` devuelven identificadores distintos.
 
**Captura**: la resolución del nombre y la salida de las tres peticiones a `/api/instancia`.
 
## Paso 2 — Reproducir la incidencia y demostrarla
 
Primero, velo con tus ojos. Entra en Escaparate desde el navegador con las credenciales de prueba, navega a la zona que exige haber iniciado sesión y recarga varias veces. Te va a expulsar, pero no siempre.
 
Eso es suficiente para creerlo y del todo insuficiente para demostrarlo, porque un navegador hace muchas cosas por su cuenta —caché, reintentos, precarga— y no sabes cuáles. Así que vas a repetir la prueba sin navegador.
 
Antes hace falta resolver un detalle, y es el que hace que la evidencia valga algo: **cada respuesta tiene que decirte qué copia la ha atendido**. Si lo averiguas lanzando después una petición a `/api/instancia` no demuestras nada, porque el reparto puede haber mandado esa segunda petición a una copia distinta de la que atendió la primera. Tu proxy ya tiene el dato —es el mismo que escribe en el registro de acceso desde la sesión 9—, así que haz que además lo devuelva en una cabecera de respuesta. Es una línea en la configuración de Nginx, del mismo tipo que las que añadiste en las sesiones 6 y 8, y a partir de ahí **una petición equivale a una fila de evidencia**.
 
Con eso resuelto: una petición de inicio de sesión que guarde la cookie en un fichero, y después una tanda de peticiones que reutilicen exactamente esa misma cookie, anotando de cada una la cabecera con la copia que la atendió y si seguías autenticado. Esa es la única evidencia que vale:
 
```text
misma cookie + copia distinta → sesión desconocida
misma cookie + copia que la creó → sesión intacta
```
 
**Comprueba**: en la tanda de peticiones con cookie fija, las respuestas en las que sigues autenticado llevan siempre la misma copia en la cabecera, y las que te expulsan llevan otras.
 
**Captura**: la tabla de peticiones con tres columnas —número de petición, copia que atendió según la cabecera, si seguías dentro—.
 
!!! question "Reflexiona"
    ¿Dónde vivía la sesión y por qué desapareció? Y la segunda, que es la que convierte esto en un diagnóstico: **¿por qué el fallo es intermitente y no constante?** Responde con el número de copias y el algoritmo de reparto en la mano, y di qué proporción de fallos predice tu explicación.
 
## Paso 3 — Sacar la sesión fuera de las copias
 
Ya sabes que el dato está en el sitio equivocado. En la sesión 7 tuviste este mismo problema con las imágenes de producto y lo resolviste sacándolas a un almacenamiento compartido; hoy toca aplicar la misma regla a algo que no se ve en ninguna carpeta.
 
El fichero que te he dado incluye un almacén de clave-valor en memoria, que es la pieza que se usa habitualmente para esto. Tu trabajo es integrarlo en tu stack y **hacer que las tres copias de la API dejen de guardar sesiones en su memoria y las guarden ahí**. La aplicación ya sabe hacerlo: no hay que compilar nada ni tocar código, solo indicárselo por configuración, igual que hiciste con la base de datos en la sesión 5.
 
Cuando lo tengas, repite exactamente la prueba con cookie fija del paso anterior.
 
**Comprueba**: tres cosas. La tanda de peticiones mantiene la sesión aunque la cabecera siga señalando copias distintas —el reparto no se ha desactivado, lo que ha cambiado es dónde vive el dato—. La cookie ha pasado a llamarse `SESSION`. Y en el almacén han aparecido entradas bajo el espacio de nombres `spring:session`.
 
**Captura**: la tanda de peticiones con su copia y su estado de sesión, y el listado de entradas del almacén.
 
!!! question "Reflexiona"
    En la sesión 7 mencionamos las sesiones pegajosas como forma de evitar este problema: bastaba con una línea en el proxy. ¿Por qué no las hemos usado hoy? Piensa en qué le pasa a tu sesión cuando la copia que te tocó se reinicia, cosa que va a ocurrir en cada despliegue a partir de diciembre.
 
!!! tip "Punto de rescate — 12:05"
    Este paso depende de que encajen tres cosas: el servicio nuevo dentro de tu red, la configuración llegando a las tres copias, y las copias recreadas después de cambiarla. Si a las 12:05 no te funciona, avísame y publico el fragmento resuelto. Esto no puede quedarse a medias: la sesión 11 y todo el tema 6 dan por hecho que tus copias son intercambiables.
 
## Paso 4 — El mismo artefacto en un servidor externo
 
Cambio de asunto. Hasta ahora, cada copia de Escaparate es un proceso que lleva su propio servidor dentro. Vas a ver la otra forma de hacerlo.
 
Primero necesitas el artefacto suelto. No lo vas a compilar —en el servidor no se compila— porque ya lo tienes: está dentro de la imagen que llevas desplegando desde octubre. **Extráelo de la imagen** y ábrelo, que es un fichero comprimido. Busca dentro el directorio que explica que el mismo fichero sirva para las dos formas de ejecutarlo: si lo encuentras, has entendido el truco.
 
Después levanta el servidor de aplicaciones del fichero que te he dado y **despliega el artefacto con el nombre que trae, `escaparate.war`**. Averigua en qué ruta ha quedado publicado y compáralo con la ruta que tu proxy está pidiendo: ahí tienes, en vivo, el motivo por el que esta sesión pierde tiempo todos los años.
 
Cuando lo hayas visto y sepas explicarlo, **renombra el artefacto para que la aplicación responda en la raíz**. Ese es el estado final con el que se queda tu despliegue y el que se va a corregir.
 
Este servidor es un backend más, así que **no publica puertos**, igual que las tres copias. Compruébalo desde dentro de la red, como aprendiste en la sesión 5.
 
**Comprueba**: con el nombre original, la aplicación responde bajo `/escaparate` y la ruta que usa tu proxy devuelve 404. Después de renombrar, el endpoint de salud responde 200 en la raíz. Y desde fuera de la instancia ese puerto no es accesible en ninguno de los dos casos.
 
**Captura**: el contenido del artefacto con el directorio que has localizado, y las dos respuestas del endpoint de salud —antes y después de renombrar— obtenidas desde dentro de la red.
 
!!! question "Reflexiona"
    ¿En qué ruta respondía la aplicación antes de que la renombraras? ¿Quién tomó esa decisión, si tú no la tomaste? Relaciónalo con lo que le pasaría a tu proxy si mañana desplegaras ahí una segunda aplicación.
 
!!! tip "Punto de rescate — 12:35"
    Aquí tienen que encajar la imagen, el volumen con el artefacto y la ruta de publicación. Si a las 12:35 el Tomcat externo no responde, avísame: te doy el fragmento con el volumen ya montado para que puedas hacer el paso 5, y el diagnóstico de por qué no te funcionaba lo cerramos entre todos en la puesta en común.
 
## Paso 5 — El gestor de despliegue y su rol
 
El servidor externo trae instalada una aplicación de gestión que permite consultar y manipular los despliegues sin tocar el disco de la máquina. Tiene dos candados. El primero es de procedencia: de fábrica solo atiende peticiones locales, y **eso ya te lo he dejado resuelto en el fichero de hoy** para que admita las que llegan desde la red de tu stack. El segundo es el rol, y ese lo pones tú.
 
Consulta desde dentro de tu red el listado de aplicaciones desplegadas que ofrece el gestor. Te va a rechazar. Tu objetivo es **conseguir entrar declarando el usuario y el rol que hacen falta**, y quedarte con las dos respuestas: la del intento sin credenciales y la del intento con ellas. Ten en cuenta que el gestor tiene dos interfaces —una gráfica y otra pensada para automatizar desde la línea de órdenes— y que cada una exige un rol distinto. Concede solo el de la que vas a usar.
 
El fichero donde declaras el usuario contiene una credencial administrativa, así que **no se versiona**. Sube al repositorio una plantilla sin contraseña y documenta en el `README` cómo se genera el fichero real, exactamente como hiciste con `.env` en la sesión 5.
 
**Comprueba**: sin credenciales el gestor responde con un código de error de acceso; con el usuario y el rol correctos devuelve la lista de aplicaciones desplegadas, y tu Escaparate aparece en ella. Y `git status` no muestra el fichero real como pendiente de subir.
 
**Captura**: las dos respuestas del gestor y la plantilla versionada.
 
!!! question "Reflexiona"
    En la sesión 8 protegiste la zona de informes con usuario y contraseña en Nginx. Hoy has protegido el gestor con usuario y contraseña en Tomcat. Si un compañero entra en el gestor con sus credenciales de Tomcat, ¿podría entrar también en la zona de informes? Explica por qué en una frase, nombrando quién decide en cada caso.
 
## Paso 6 — Comparar y documentar
 
Ya has ejecutado el mismo artefacto de las dos formas. Cierra la sesión con una **tabla comparativa** en el `README`, con al menos estas cuatro filas: tiempo desde que lanzas la orden hasta que la aplicación responde, dónde vive la configuración del servidor, qué hay que hacer para actualizar a una versión nueva, y qué se necesita tener instalado para desplegar. Rellénala con lo que has medido tú, no con lo que dice el apunte.
 
Debajo, **un párrafo de conclusión**: para un servicio que se despliega en contenedores varias veces al día, cuál de los dos modelos elegirías y por qué. Y revisa que el procedimiento de rearranque semanal del paso 1 esté escrito y actualizado, incluido el paso de apuntar el registro del subdominio.
 
Abre la rama `sesion-10` si no la tenías ya, deja en `entregas/tema4/` las capturas y la tabla, y lanza el pull request hacia `main`.
 
**Comprueba**: el pull request está fusionado y `main` levanta el stack completo —tres copias, almacén de sesiones y servidor externo— desde cero.
 
---
 
## Si te sobra tiempo
 
Copia el artefacto una segunda vez en el servidor externo con otro nombre distinto y comprueba que conviven **dos aplicaciones independientes en el mismo servidor**, cada una en su ruta y cada una con sus propias sesiones. Es la característica que justificaba todo el modelo clásico cuando los servidores eran caros: un proceso, varias aplicaciones. No entregues nada de esto y retíralo antes de irte.
 
---
 
## Verificación
 
Para dar por válida la práctica se ejecutará, desde la instancia y con `main` fusionado:
 
```bash
URL=https://<subdominio-del-equipo>
 
# 1. El servicio responde por su nombre y el reparto sigue activo
for i in 1 2 3; do curl -s $URL/api/instancia; echo; done
 
# 2. La sesión sobrevive al cambio de copia, con la copia leída en la MISMA respuesta
curl -s -c /tmp/ck.txt -X POST $URL/login -d 'username=demo&password=demo'
for i in $(seq 1 6); do
  curl -s -b /tmp/ck.txt -o /dev/null -D - $URL/api/perfil \
    | grep -iE '^HTTP/|^x-destino'
done
 
# 3. Las sesiones viven en el almacén externo, no en las copias
docker compose exec redis redis-cli --scan --pattern 'spring:session*' | head
 
# 4. El Tomcat externo sirve el mismo artefacto y no publica puertos
docker compose exec nginx curl -s -o /dev/null -w '%{http_code}\n' \
       http://tomcat:8080/api/salud
curl -s -m 5 -o /dev/null -w '%{http_code}\n' http://$IP:8080/ || echo "sin acceso"
 
# 5. El gestor exige el rol
docker compose exec nginx curl -s -o /dev/null -w '%{http_code}\n' \
       http://tomcat:8080/manager/text/list
docker compose exec nginx curl -s -u despliegue:CLAVE \
       http://tomcat:8080/manager/text/list
 
# 6. La credencial administrativa no está en el repositorio
git ls-files | grep -i tomcat-users
```
 
Y debe observarse:
 
- Tres identificadores de instancia distintos en la primera comprobación.
- Seis respuestas `200` en la segunda, **con la cabecera de destino variando entre copias**: la misma sesión la reconocen todas.
- Entradas bajo `spring:session` en el almacén.
- `200` desde dentro de la red al servidor externo y **ningún acceso** desde fuera.
- Rechazo sin credenciales en el gestor, y el listado con Escaparate dentro al presentarlas.
- En la última comprobación, **solo la plantilla**: si aparece el fichero real, la práctica no es válida.
*Si los nombres de tus servicios no coinciden con los del ejemplo, indícalo en el `README`.*
 
---
 
## Qué se entrega
 
- [ ] Rama `sesion-10` con pull request **fusionado** en `main`.
- [ ] Tabla de la incidencia reproducida con cookie fija: petición, copia que atendió según la cabecera y si seguías dentro.
- [ ] Respuesta escrita a por qué el fallo era intermitente, con la proporción de fallos que predice tu explicación.
- [ ] La misma tabla repetida con la sesión ya externalizada, más el listado de entradas del almacén.
- [ ] Respuesta escrita a por qué se descartaron las sesiones pegajosas.
- [ ] Contenido del artefacto con el directorio que explica el doble comportamiento, y las dos respuestas del endpoint de salud del servidor externo —bajo `/escaparate` y en la raíz— desde dentro de la red.
- [ ] Las dos respuestas del gestor y la **plantilla** de usuarios versionada, con el fichero real fuera del repositorio.
- [ ] Tabla comparativa de los dos modelos, con datos medidos, y el párrafo de conclusión.
- [ ] Procedimiento de rearranque semanal actualizado en el `README`, con el paso del registro de nombres.
- [ ] Plantilla `.docx` subida a Moodle con la URL del pull request fusionado.
---
 
## ✅ Cierre
 
Sales de hoy con un servicio en el que **ninguna copia guarda nada que un usuario necesite volver a encontrar**: ni las imágenes desde la sesión 7, ni la sesión desde hoy. Esa propiedad tiene consecuencias que vas a cobrar durante el resto del curso: es lo que permitirá en diciembre actualizar a una versión nueva sin echar a nadie, y lo que hará que en enero puedas pasar de tres copias a seis sin que el orquestador tenga que preguntar nada.
 
Y sabes algo más que la mayoría de quien despliega Java sin haberlo pensado: que el mismo artefacto vale para dos modelos operativos distintos, y por qué las empresas están migrando de uno al otro.
 
La semana que viene el servicio ya no se rompe porque el proxy te cambie de copia. Queda la otra pregunta, la que aún no te has hecho: **cuánta carga aguanta esto antes de ponerse lento, y qué pieza se rinde primero**. Trae el panel de Kibana preparado, porque parte de la respuesta lleva ahí desde la semana pasada.