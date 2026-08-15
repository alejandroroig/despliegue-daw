# 🧪 Actividad 1.2: Tu repositorio como herramienta de despliegue

!!! warning "Descarga la plantilla"
    📄 [Plantilla 1.2 — Tu repositorio como herramienta de despliegue](plantillas/Actividad_1_2_DAW_Plantilla.docx){target="_blank" rel="noopener"}

## Contexto

La semana pasada dedicaste la sesión a mirar despliegues ajenos desde fuera. Hoy empieza el tuyo, y no por el servidor: por el sitio donde va a vivir todo lo demás.

El encargo es concreto. Tu responsable te pide que dejes montado el repositorio del proyecto **antes** de que llegue nadie más al equipo, con tres condiciones que ha repetido dos veces: que cualquiera pueda clonarlo y saber qué hacer con él, que no contenga ni una credencial, y que se pueda identificar sin ambigüedad qué versión es cada cosa. Lo que montes hoy lo vas a usar los dieciséis viernes del curso.

## Qué vas a practicar

- **Crear** un repositorio con una estructura pensada para el despliegue, no para el código.
- **Decidir** qué no entra nunca en el historial, y configurarlo antes del primer commit.
- **Recuperarte** de tres errores distintos eligiendo la operación adecuada a cada uno.
- **Trabajar con ramas** y proponer los cambios mediante una petición de fusión.
- **Etiquetar** una versión y entender por qué una etiqueta no es una rama.

## Requisitos previos

- Git instalado, con tu nombre y tu correo configurados.
- **Cuenta de GitHub con acceso resuelto**: clave SSH registrada o token de acceso personal. Esto es lo primero de la sesión, y no se puede aplazar: sin ello no puedes publicar nada.
- El documento de la actividad 1.1 terminado.
- El código de **Escaparate**, que se te entrega hoy.

!!! danger "Fuera de las carpetas sincronizadas"
    Trabaja en una carpeta que **no** esté dentro de OneDrive, iCloud ni Google Drive. Esos sistemas y Git se pelean por los mismos ficheros, y el resultado es un repositorio corrupto sin aviso previo.

!!! info "Reparto de tiempo orientativo"
    Pasos 1 a 4, unos 40 minutos. Paso 5, unos 25. Pasos 6 a 8, unos 25.

---

## Paso 1 — El esqueleto

Crea una carpeta `daw-despliegue` e inicialízala como repositorio. Dentro, monta esta estructura:

```
daw-despliegue/
├── README.md
├── docs/
├── entregas/tema1/
└── escaparate/     ← el código que se te ha entregado, tal cual
```

Antes de preparar nada, mira en qué estado ve Git lo que acabas de crear.

**Comprueba**: aparecen como no seguidos tanto tus carpetas como el proyecto entero de Escaparate.
**Captura**: el estado del repositorio antes de tu primer commit.

---

## Paso 2 — Lo que no entra

Abre la carpeta de Escaparate y busca lo que **no** debe acabar nunca en el historial. Hay dos familias: los ficheros que se generan al compilar y cualquier cosa que contenga una credencial.

Configura el repositorio para ignorarlos. Como mínimo tienen que quedar fuera la carpeta de compilación de Maven, los ficheros de clases, los ficheros de variables de entorno y la configuración de tu editor.

**Comprueba**: al volver a mirar el estado, ninguno de esos ficheros aparece ya como pendiente de añadir.
**Captura**: el fichero de exclusiones y el estado del repositorio después de crearlo.

!!! question "Reflexiona"
    Has hecho esto **antes** del primer commit, no después. Si lo hubieras hecho al revés, ¿bastaría con añadir las reglas más tarde para que esos ficheros desaparecieran del repositorio? Y si lo que se hubiera colado fuera la contraseña de la base de datos, ¿sería suficiente con borrarla en un commit posterior?

---

## Paso 3 — Publica

Haz el primer commit y crea en GitHub un repositorio **público** llamado `daw-despliegue`, vacío, sin ningún fichero inicial. Enlázalo y publica lo que tienes.

**Comprueba**: el repositorio se ve en tu perfil de GitHub y el contenido del `README` aparece en la portada.
**Captura**: la página del repositorio en GitHub.

!!! tip "Si te pide usuario y contraseña"
    GitHub dejó de aceptar la contraseña de la web hace años. Si te la pide, estás usando la dirección `https` sin token. Revisa qué dirección tiene configurada tu repositorio como remoto y qué método de autenticación resolviste en los requisitos previos.

---

## Paso 4 — Incorpora lo de la semana pasada

La actividad 1.1 la entregaste como documento suelto porque este repositorio todavía no existía. Ahora sí existe: colócalo en `entregas/tema1/` y regístralo con un commit propio, con su mensaje.

**Captura**: el fichero visible en GitHub dentro de su carpeta.

---

## Paso 5 — Tres maneras de estropearlo y tres de arreglarlo

Aquí está el núcleo de la sesión. Vas a provocar tres situaciones y a resolver cada una eligiendo tú la operación adecuada. **No te digo qué comando usar**: eso es exactamente lo que se corrige.

Después de cada una, anota **qué área de Git ha quedado afectada** —directorio de trabajo, preparación o historial— y por qué esa era la operación correcta y no otra.

**Situación 1.** Escribe tres párrafos en el `README` y déjalos a medias. No te convencen. Quieres volver al contenido que tenía en el último commit sin borrar el fichero ni deshacer nada a mano.

**Situación 2.** Modifica el `README` otra vez, esta vez con algo que sí quieres conservar, y prepáralo. Te das cuenta de que todavía no querías incluirlo en el próximo commit. Sácalo de la preparación **sin perder ni una línea de lo escrito**.

**Situación 3.** Añade al `README` una línea claramente equivocada, regístrala y **publícala**. Ya no es un error privado: está en el servidor y cualquiera puede haberlo descargado. Deshazlo de la forma que corresponde a un cambio publicado, y publica también la corrección.

**Comprueba**: tras la situación 3, el contenido erróneo ha desaparecido del fichero pero **ambos** commits siguen en el historial.
**Captura**: el estado del repositorio o el contenido del fichero antes y después de cada situación, y el historial al terminar la tercera.

!!! question "Reflexiona"
    En la situación 3 podrías haber hecho desaparecer ese commit del historial y forzar la publicación. Explica qué le habría ocurrido a un compañero que hubiera descargado tu rama entre las dos operaciones. Y una segunda: en un repositorio del que se despliega a producción, **¿qué más se pierde, aparte del trabajo ajeno, cuando se reescribe historial publicado?**

---

## Paso 6 — La tabla de decisión

Sin ejecutar nada más, rellena esta tabla razonando a partir de lo que acabas de practicar. La tercera columna es la que se corrige:

| Situación | Qué harías | Por qué esa y no otra |
|---|---|---|
| Has escrito algo que no te gusta y aún no lo has preparado | | |
| Has preparado un fichero de más y quieres conservar los cambios | | |
| El commit erróneo ya está publicado | | |

---

## Paso 7 — La rama de la sesión

Todo lo que falta va en una rama llamada `sesion-02`. Créala y trabaja en ella.

Escribe el `README` pensando en alguien que llega al repositorio sin haber estado en esta clase. Hoy todavía no hay nada que desplegar, así que lo que tiene que explicar es:

- Qué es este repositorio y qué contiene.
- Qué hay que tener instalado para trabajar en él.
- **Cómo se trabaja aquí**: una rama por sesión con el nombre `sesion-NN`, y las entregas llegan a la rama principal mediante petición de fusión.
- Un índice de las entregas. La primera línea ya la puedes escribir: la actividad 1.1, que colocaste en el paso 4.

Deja además creado el apartado «Puesta en marcha», aunque hoy solo contenga una línea diciendo que se completará cuando haya algo que arrancar. En la sesión 5 lo rellenarás de verdad.

Publica la rama y **abre una petición de fusión** hacia la rama principal, con una descripción que explique qué has hecho hoy y por qué. Esa descripción es parte de la entrega. Después ciérrala tú mismo integrando el trabajo.

**Comprueba**: en GitHub, la petición de fusión aparece como fusionada y el `README` se ve en la portada con su contenido.
**Captura**: la petición de fusión con su descripción, y el `README` renderizado.

!!! question "Reflexiona"
    Podrías haber fusionado con un comando y ahorrarte todo esto. Hoy no había nadie revisando ni nada que comprobar, así que ha sido pura ceremonia. **¿Qué tendría que aparecer en esa página para que dejara de serlo?**

---

## Paso 8 — La primera versión

Sobre la rama principal ya integrada, marca la versión `v0.1.0` con una etiqueta anotada que diga qué contiene, y publícala.

**Comprueba**: la etiqueta aparece en GitHub en el apartado de etiquetas, asociada al commit correcto.
**Captura**: la etiqueta en GitHub y la salida del comando que lista tus etiquetas con su mensaje.

!!! question "Reflexiona"
    La rama principal y la etiqueta `v0.1.0` apuntan ahora mismo al mismo commit. Haz un commit más y vuelve a mirarlas. **¿Cuál de las dos se ha movido?** Con eso contestado, explica por qué un procedimiento de despliegue reproducible debería indicar una **versión concreta** y no limitarse a decir «despliega lo que haya ahora mismo en la rama principal».

---

## Si te sobra tiempo

Nada de esto se entrega ni se corrige.

**Provoca un conflicto y resuélvelo.** Crea una rama, cambia en ella la primera línea del `README` y regístralo. Vuelve a la principal, cambia **esa misma línea** por algo distinto y regístralo también. Ahora intenta fusionar. Git no podrá decidir por ti: verás unas marcas en el fichero señalando las dos versiones. Quédate con una combinación de ambas, borra las marcas y cierra la fusión. Merece la pena verlo hoy con calma, porque el día que te pase será con prisa.

**Genera la documentación del código.** El proyecto de Escaparate está preparado para producir la documentación de sus clases a partir de los comentarios del código. Genérala, ábrela en tu navegador y anota en el `README` el comando exacto que la regenera. No la añadas al repositorio: es un artefacto, y en diciembre la producirá y la publicará el pipeline.

---

## Verificación

Para dar por válida la práctica se ejecutará, sustituyendo `<usuario>` por el tuyo:

```bash
git clone https://github.com/<usuario>/daw-despliegue.git verifica && cd verifica
git log --graph --oneline --all --decorate | head -30
git tag -n
cat .gitignore
git log --all --oneline -- '.env' 'target/*' '*.class' | wc -l
ls entregas/tema1/
```

Y debe observarse:

- Que el repositorio **se clona sin credenciales**: es público.
- Que la cuenta de commits que tocan ficheros de compilación o de variables de entorno es **cero**.
- Que existen tanto el commit que introduce el error del paso 5 **como** el que lo deshace, sin que el primero haya sido borrado.
- Que el trabajo del paso 7 llegó a la rama principal **a través de una petición de fusión**, y no por un envío directo.
- Que la etiqueta `v0.1.0` existe, está anotada y apunta al commit correcto.
- Que en `entregas/tema1/` está la entrega de la actividad 1.1.
- Que el `README` explica el convenio de ramas y contiene el índice de entregas.

---

## Qué se entrega

- [ ] El repositorio `daw-despliegue` público, con la estructura y el proyecto dentro.
- [ ] El fichero de exclusiones, creado **antes** del primer commit, con su reflexión.
- [ ] Las tres situaciones resueltas, con sus capturas y el área afectada en cada una.
- [ ] La tabla de decisión, con la columna del porqué completa.
- [ ] La petición de fusión de la rama `sesion-02`, con descripción, fusionada.
- [ ] La etiqueta `v0.1.0` publicada.
- [ ] El `README` con el convenio de ramas y el índice de entregas.
- [ ] El documento de hoy, colocado en `entregas/tema1/` y enlazado desde el índice.

---

## ✅ Cierre

Tienes un repositorio público con el proyecto dentro, sin una sola credencial en su historial y con una versión marcada. Y, sobre todo, tienes la costumbre de saber salir de tres errores distintos sin romper nada, que es lo que separa a quien usa Git de quien lo sufre.

Hoy buena parte de esto habrá parecido ceremonia. La rama para una sesión en la que trabajas solo, la petición de fusión que te apruebas a ti mismo, la etiqueta `v0.1.0` de un repositorio que todavía no despliega nada. Anótalo, porque en diciembre vas a volver aquí: esa rama tendrá un guardián que decidirá si tu código puede entrar, esa petición de fusión será el sitio donde se ejecuten las pruebas, y esa etiqueta será lo que te permita deshacer un despliegue roto en treinta segundos.

Antes de eso, la semana que viene empieza el trabajo de verdad. Escaparate ya está en tu repositorio, pero solo funciona si tienes instalado exactamente lo que hace falta y en la versión correcta, que es justo el problema que viste que sufren todos los despliegues. En la próxima sesión conocerás la forma estándar de resolverlo: empaquetar una aplicación junto con todo lo que necesita para funcionar, de modo que arranque igual en tu portátil, en el del compañero de al lado y en un servidor que no has visto nunca.