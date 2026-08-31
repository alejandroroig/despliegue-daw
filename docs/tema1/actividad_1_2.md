# 🧪 Actividad 1.2: Tu repositorio como herramienta de despliegue

!!! warning "Descarga el material"
    Para esta actividad necesitas la aplicación `escaparate-integrado.zip`.

## Contexto

La semana pasada dedicaste la sesión a observar despliegues ajenos desde fuera. Hoy empieza el tuyo, pero no por el servidor: por el lugar donde van a vivir el código, la documentación y el procedimiento de trabajo.

El encargo es concreto. Tu responsable te pide preparar el repositorio del proyecto antes de que llegue nadie más al equipo, con tres condiciones:

- que cualquier miembro del equipo con acceso pueda clonarlo y entender qué contiene;
- que no incorpore secretos ni ficheros locales que no deban versionarse;
- que permita identificar sin ambigüedad estados concretos del proyecto.


El repositorio que prepares hoy se seguirá utilizando durante el módulo.

!!! info "Sobre Escaparate"
    `escaparate-integrado.zip` es una aplicación ya preparada para las prácticas de despliegue. **No necesitas entender ni modificar todavía su código.** En esta actividad actúa simplemente como un proyecto real que debes incorporar correctamente a tu repositorio.

## Qué vas a practicar

- Crear un repositorio y organizarlo para trabajar durante el módulo.
- Interpretar y completar reglas de `.gitignore`.
- Distinguir **directorio de trabajo**, **área de preparación (*staging*)** e **historial**.
- Recuperarte de errores distintos utilizando la operación adecuada.
- Trabajar con ramas y Pull Requests.
- Crear una etiqueta para identificar un estado concreto del repositorio.

## Requisitos previos

- Git instalado, con tu nombre y tu correo configurados.
- **Cuenta de GitHub**. No necesitas traer un token preparado: configurarás la autenticación HTTPS en el paso 3.
- `escaparate-integrado.zip`.
- La carpeta completa `actividad-1.1/` de la sesión anterior (o el fichero comprimido subido a Aules).

!!! danger "Fuera de carpetas sincronizadas"
    Trabaja fuera de OneDrive, iCloud, Google Drive u otras carpetas sincronizadas.

!!! info "Tiempo orientativo"
    - Pasos 1 a 4: 40 min.
    - Pasos 5 y 6: 30 min.
    - Pasos 7 y 8: 25 min.

!!! info "Capturas durante los primeros pasos"
    Hasta que crees la rama `sesion-02` en el paso 7, guarda las capturas de esta actividad en una carpeta temporal **fuera del repositorio**. Así podrás practicar los distintos estados de Git sin que las imágenes pendientes aparezcan continuamente en `git status`. En el paso 7 las incorporarás al repositorio junto con el Markdown de la actividad.

---

## Paso 1: El esqueleto

Crea una carpeta `daw-despliegue` e inicialízala como repositorio Git haciendo que su rama principal se llame `main`. Dentro, prepara esta estructura:

```text
daw-despliegue/
├── README.md
├── docs/
│   └── README.md
├── entregas/
│   └── tema1/
├── escaparate/
│   └── ... aplicación integrada ...
└── practicas/
    └── README.md
```

Descomprime `escaparate-integrado.zip` y coloca su contenido dentro de `escaparate/`.

El `README.md` de la raíz puede contener inicialmente solo:

- nombre del módulo;
- una frase indicando que será el repositorio de trabajo de Despliegue.

En `docs/README.md` basta con explicar que esa carpeta contendrá documentación del proyecto. En `practicas/README.md` explica que esa carpeta contendrá los ficheros de configuración y despliegue creados durante las prácticas del módulo.

Tras editar los ficheros mencionados, ejecuta:

```bash
git status
```

!!! question "Observa"
    `entregas/tema1/` todavía está vacía. ¿Aparece en `git status`? ¿Qué te dice eso sobre cómo trata Git los directorios vacíos?

**Captura:** estado inicial del repositorio.

---

## Paso 2: Lo que no entra

Antes del primer commit debes revisar qué ficheros deberían quedar fuera del historial.

### 2.1 Inspecciona Escaparate

Dentro de `escaparate/` ya existe un fichero `.gitignore`. Ábrelo y explica brevemente qué pretende excluir cada regla.

No lo borres: forma parte del proyecto que has recibido.

### 2.2 Protege el repositorio completo

Crea además un `.gitignore` en la **raíz de `daw-despliegue/`** para establecer reglas aplicables a todo el repositorio.

Como mínimo deben quedar excluidos:

- directorios de compilación Maven;
- ficheros `.class`;
- ficheros `.env`;
- configuración local de los IDE habituales;
- datos locales generados al ejecutar Escaparate, como las imágenes subidas durante las pruebas.

Puedes comprobar qué regla afecta a una ruta con:

```bash
git check-ignore -v <ruta>
```

Prueba, por ejemplo:

```bash
git check-ignore -v escaparate/target/prueba.class
git check-ignore -v escaparate/.env
git check-ignore -v escaparate/uploads/prueba.png
```

!!! warning "No ignores demasiado"
    Un fichero de configuración no es automáticamente un secreto y un `.jar` no es automáticamente un artefacto prescindible. No utilices reglas generales sin comprobar qué eliminarían del proyecto.

**Comprueba:** las rutas anteriores quedan ignoradas y los ficheros necesarios de Escaparate siguen apareciendo como candidatos a ser versionados.

**Captura:** el `.gitignore` creado, las comprobaciones realizadas y `git status`.

!!! question "Reflexiona"
    Si un fichero ya hubiese entrado en un commit y después añadieras su nombre a `.gitignore`, ¿desaparecería del historial?

    Y si lo publicado hubiese sido una contraseña real, ¿bastaría con borrarla en un commit posterior?

---

## Paso 3: Publica

Añade el contenido de la carpeta `daw-despliegue` al área de preparación y crea el primer commit.

Después crea en GitHub un repositorio **privado** llamado `daw-despliegue`, vacío, sin ningún fichero inicial.

### 3.1 Crea el PAT del módulo

En este curso trabajaremos con GitHub mediante HTTPS. Para evitar gestionar varias credenciales durante las primeras sesiones utilizaremos, como simplificación didáctica, un único **Personal Access Token (classic)** que servirá tanto para Git como para GitHub Container Registry.

En GitHub ve a:

```text
GitHub
→ Settings
→ Developer settings
→ Personal access tokens
→ Tokens (classic)
```

Selecciona **Generate new token → Generate new token (classic)** y crea:

```text
Nombre: DAW - Curso
Caducidad: hasta el final del curso o la indicada por el profesor
Scopes:
    repo
    write:packages
```

Los dos permisos tienen finalidades distintas:

- `repo` permite trabajar desde Git con tu repositorio privado;
- `write:packages` se utilizará en la Actividad 2.1 para publicar imágenes en `ghcr.io`.

Cuando GitHub muestre el token, **cópialo en ese momento y guárdalo en un gestor de contraseñas**. No necesitas ni debes escribirlo dentro de `daw-despliegue`.

!!! danger "El token no aparece en la entrega"
    No incluyas el PAT en `actividad-1.2.md`, en `.env`, en el README ni en ninguna captura. Si se filtra accidentalmente, revócalo desde GitHub y crea otro.

!!! warning "Una simplificación para el aula"
    Un PAT classic con `repo` tiene permisos amplios. Lo utilizamos porque nos permite trabajar con Git ahora y reutilizar la misma credencial con GHCR en la próxima sesión, reduciendo la gestión de credenciales mientras aprendes las herramientas.

??? info "Alternativa recomendada: mínimo privilegio"
    Si prefieres aplicar desde ahora una política más estricta, puedes utilizar **dos credenciales separadas**:

    1. Para Git, crea un **fine-grained personal access token** limitado únicamente a `daw-despliegue`, con `Contents: Read and write`.
    2. En la Actividad 2.1 crearás un **PAT classic** independiente con `write:packages` para GHCR.

    La ruta para el primero es:

    ```text
    GitHub
    → Settings
    → Developer settings
    → Personal access tokens
    → Fine-grained tokens
    → Generate new token
    ```

    Selecciona tu cuenta como *Resource owner*, limita *Repository access* a `daw-despliegue` y concede únicamente `Contents: Read and write`.

    Esta opción sigue mejor el **principio de mínimo privilegio**: si una credencial se comprometiera, el daño posible quedaría limitado a la finalidad para la que fue creada. GitHub recomienda los PAT *fine-grained* para restringir el acceso a repositorios concretos, mientras que GitHub Packages requiere actualmente un PAT classic para la autenticación manual.

### 3.2 Enlaza y publica el repositorio

Enlaza ahora tu repositorio local con el remoto mediante la URL HTTPS y publica `main`, utilizando lo explicado en la teoría.

Si Git solicita credenciales:

```text
Username: <tu-usuario-de-GitHub>
Password: <tu-PAT>
```

En `Password` se introduce el PAT, **no la contraseña normal de tu cuenta de GitHub**. Si tu equipo utiliza un gestor de credenciales, es posible que lo recuerde y no vuelva a preguntarlo en cada `push`.

Añade después como **colaborador** al usuario de GitHub del profesor, de forma que pueda acceder al repositorio y corregir el trabajo sin que tengas que hacerlo público durante el curso.

**Comprueba:**

- la rama principal es `main`;
- el repositorio existe en GitHub y su visibilidad es privada;
- el profesor aparece con acceso al repositorio;
- el README aparece renderizado;
- `escaparate/` contiene el proyecto recibido;
- no hay secretos ni artefactos locales en el repositorio.

**Captura:** portada del repositorio en GitHub sin mostrar el PAT ni ninguna otra información sensible.

!!! info "Privado durante el curso, publicable al final"
    El repositorio se mantendrá privado mientras realizas las actividades para que el trabajo no quede expuesto al resto de la clase. Al final del módulo, después de revisar que no contiene secretos, credenciales ni datos personales, podrás cambiarlo a público y conservarlo como muestra de tu trabajo técnico.

---

## Paso 4: Incorpora lo de la semana pasada

La actividad 1.1 la entregaste comprimida en Aules porque este repositorio todavía no existía. Recupera la carpeta `actividad-1.1/`, colócala completa dentro de `entregas/tema1/` y regístrala con un commit propio.

La estructura resultante será la siguiente:

```text
daw-despliegue/
└── entregas/
    └── tema1/
        └── actividad-1.1/
            ├── actividad-1.1.md
            └── img/
```

**Captura:** fichero y commit que lo incorporó.

---

## Paso 5: Tres maneras de estropearlo y tres de arreglarlo

Aquí está el núcleo de la sesión. Vas a provocar tres situaciones y a resolver cada una eligiendo tú la operación adecuada. **No se indica qué comando usar**: eso debes decidirlo tú.

Después de cada situación anota:

- qué zona de Git estaba afectada;
- qué operación utilizaste;
- por qué esa operación era adecuada.

---

### Situación 1: Cambio que todavía no has preparado

Escribe tres párrafos en el `README.md` de la carpeta raíz y déjalos a medias. No te convencen. Decides descartarlos por completo y quieres volver al contenido que tenía el fichero en el último commit, sin editarlo manualmente.

**Comprueba:** el cambio desaparece y el repositorio vuelve a estar limpio.

---

### Situación 2: Cambio preparado que quieres conservar

Modifica otra vez el `README.md`, esta vez con contenido válido que sí quieres conservar. Prepáralo para el siguiente commit. Después decides que todavía no quieres incluir ese fichero en el próximo commit. Sácalo del área de preparación **sin perder lo escrito**.

**Comprueba:**

- el fichero sigue modificado;
- el contenido permanece;
- el cambio ya no está preparado.

!!! warning "Antes de seguir"

    El contenido era válido. Regístralo ahora en un commit normal y publícalo para que la siguiente situación empiece desde un estado limpio.

---

### Situación 3: El error ya está publicado

Añade al `README.md` una línea claramente equivocada, regístrala en un commit y **publícala**. Ahora el error ya forma parte del historial compartido: está en el servidor y cualquiera puede haberlo descargado. 

Deshaz su efecto de forma que:

- el contenido incorrecto desaparezca;
- el commit original siga existiendo;
- aparezca un nuevo commit que deshaga sus cambios.

Publica también la corrección.

**Captura:** historial final y README corregido.

!!! question "Reflexiona"
    Podrías haber hecho desaparecer el commit original reescribiendo el historial y forzando después el `push`.

    ¿Qué problema tendría alguien que hubiese descargado tu rama durante ese intervalo?

    ¿Qué información de trazabilidad puede perderse cuando se cambian commits que ya habían sido publicados?

---

## Paso 6: La tabla de decisión

Sin ejecutar nada más, completa esta tabla razonando a partir de lo que acabas de practicar. La incorporarás al Markdown de la actividad en el paso 7:

| Situación | Qué harías | Por qué esa y no otra |
|---|---|---|
| Has escrito algo que no te gusta y aún no lo has preparado | | |
| Has preparado un fichero de más y quieres conservar los cambios | | |
| El commit erróneo ya está publicado | | |

---

## Paso 7: La rama y la documentación de la sesión

El trabajo normal del módulo se realizará mediante ramas. Crea una rama llamada `sesion-02` para trabajar en ella.

### 7.1 Documenta la actividad

Ahora crea dentro del repositorio:

```text
entregas/
└── tema1/
    └── actividad-1.2/
        ├── actividad-1.2.md
        └── img/
```

Mueve a `img/` las capturas que has guardado durante los pasos anteriores e insértalas en `actividad-1.2.md` utilizando rutas relativas.

El Markdown debe recoger, de forma ordenada:

- las explicaciones sobre `.gitignore`;
- las respuestas y reflexiones solicitadas durante la actividad;
- las tres situaciones del paso 5, indicando qué zona de Git estaba afectada, qué operación utilizaste y por qué;
- la tabla de decisión del paso 6;
- las capturas solicitadas.

!!! tip "No dupliques los ficheros técnicos"
    `actividad-1.2.md` documenta lo que has hecho. Los ficheros reales del proyecto, como `.gitignore`, `README.md` o el contenido de `escaparate/`, permanecen en su ubicación normal dentro del repositorio.

### 7.2 Completa el README del repositorio

En esta misma rama, amplía el `README.md` de la raíz para que una persona que no haya estado en clase pueda entender:

- qué es el repositorio;
- qué contiene;
- qué herramientas básicas hacen falta;
- cómo se trabaja: ramas `sesion-NN`;
- que los cambios llegan a `main` mediante **Pull Request (PR)**;
- un índice de entregas que enlace las actividades 1.1 y 1.2;
- un apartado `Puesta en marcha`, que por ahora puede indicar que se completará más adelante.

Registra los cambios de la rama y publícala. **Abre en GitHub una Pull Request** hacia la rama principal con una descripción que indique:

1. qué has cambiado;
2. por qué;
3. cómo has comprobado que el repositorio queda en buen estado.

Abre la Pull Request con `Create pull request`.

Antes de fusionarla, haz una captura de la PR abierta y del README renderizado. Guarda ambas en `entregas/tema1/actividad-1.2/img/`, enlázalas desde `actividad-1.2.md` y registra y publica esos últimos cambios.

Comprueba que la Pull Request se actualiza automáticamente con el nuevo commit.

Cuando toda la documentación esté incluida, fusiónala utilizando **Create a merge commit**, de forma que la bifurcación y la posterior integración puedan verse en el historial. Después, actualiza tu `main` local.

**Comprueba:**

- la PR aparece fusionada;
- `entregas/tema1/actividad-1.2/` está en `main`;
- el README actualizado está en `main`;
- el grafo permite identificar la rama y su fusión.

!!! question "Reflexiona"
    Hoy has revisado tu propia PR y apenas existen comprobaciones automáticas. ¿Qué podría aparecer en esa página más adelante para convertirla en una auténtica puerta de entrada a `main`?

---

## Paso 8: La primera versión identificable

Sobre la rama principal ya integrada, marca la versión `v0.1.0` con una **etiqueta anotada**. Añade un mensaje que describa qué contiene este primer estado identificable de **tu repositorio del módulo** y publica la etiqueta.

!!! note "Dos cosas diferentes"
    Esta etiqueta identifica un estado de `daw-despliegue`. No necesitas relacionarla con ninguna posible versión interna de las herramientas o aplicaciones que contiene el repositorio.

**Comprueba:**

- `v0.1.0` aparece en GitHub;
- es una etiqueta anotada;
- apunta al mismo commit que `main` en este momento.

### Comprueba qué se mueve

Haz ahora un **commit vacío temporal solo en local**. No lo publiques.

Vuelve a observar `main`, `origin/main` y `v0.1.0`.

- ¿Cuál se ha movido?
- ¿Cuáles permanecen en el commit anterior?

Cuando termines la comprobación, elimina ese commit temporal y deja tu `main` local otra vez exactamente en `origin/main`.

!!! question "Reflexiona"
    ¿Por qué un procedimiento de despliegue reproducible debería señalar una **versión concreta y estable (por ejemplo, una etiqueta publicada)** y no limitarse a decir "despliega lo que haya ahora mismo en `main`"?

---

## Verificación

Para dar por válida la práctica, el profesor utilizará la cuenta añadida como colaboradora y clonará el repositorio desde un entorno autenticado, sustituyendo `<usuario>` por el tuyo:

```bash
git clone https://github.com/<usuario>/daw-despliegue.git verifica && cd verifica
```

Al ser un repositorio privado, este comando solo funcionará para una cuenta que tenga acceso y esté correctamente autenticada.

Y se comprobará:

```bash
git branch --show-current
git log --graph --oneline --all --decorate | head -30

git tag -n
git cat-file -t v0.1.0
git show v0.1.0 --no-patch

cat .gitignore
cat escaparate/.gitignore

git log --all --name-only --pretty=format: \
  | grep -E '(^|/)(\.env($|\.)|target/|[^/]+\.class$)' \
  | sed '/^$/d' \
  | wc -l

find escaparate -name .git -type d -print

ls entregas/tema1/
test -f escaparate/pom.xml && echo "pom.xml OK"
test -f escaparate/mvnw && echo "Maven Wrapper OK"
test -f practicas/README.md && echo "practicas/README.md OK"
```

Debe observarse:

- rama principal `main`;
- ningún `.env`, fichero de `target/` o `.class` registrado en el historial;
- ningún repositorio Git anidado dentro de `escaparate/`;
- el commit erróneo y el commit que lo deshace;
- la fusión de `sesion-02`;
- `v0.1.0` como etiqueta anotada;
- actividades 1.1 y 1.2 en `entregas/tema1/`;
- Escaparate con su `pom.xml` y Maven Wrapper.

La Pull Request se comprobará también en GitHub.

---

## Qué se entrega

- [ ] Repositorio privado `daw-despliegue` con el profesor añadido como colaborador.
- [ ] `escaparate-integrado` incorporado como carpeta `escaparate/`.
- [ ] `.gitignore` del repositorio y análisis del `.gitignore` recibido con Escaparate.
- [ ] Actividad 1.1 completa en `entregas/tema1/actividad-1.1/`.
- [ ] `entregas/tema1/actividad-1.2/actividad-1.2.md` con respuestas, reflexiones y tabla de decisión.
- [ ] `entregas/tema1/actividad-1.2/img/` con las capturas enlazadas mediante rutas relativas.
- [ ] Las tres situaciones de recuperación documentadas.
- [ ] Pull Request `sesion-02 → main` fusionada.
- [ ] README actualizado.
- [ ] Etiqueta anotada `v0.1.0`.

!!! info "Dónde queda la entrega"
    Desde esta actividad, la evidencia del trabajo queda versionada en el propio repositorio. No necesitas generar un documento Word ni convertir la actividad a PDF.

---

## ✅ Cierre

Ya tienes preparado el repositorio que utilizarás como base durante el módulo. Dentro conviven la aplicación, la documentación, tus entregas y el espacio donde irás incorporando las distintas configuraciones de despliegue. Además, el historial empieza limpio: has decidido qué debe versionarse, qué debe quedarse fuera y has marcado un primer estado identificable con `v0.1.0`.

Durante la práctica has comprobado que Git no guarda simplemente "versiones de ficheros". Un cambio puede encontrarse en el directorio de trabajo, en el área de preparación o formando ya parte del historial, y **la forma correcta de deshacerlo depende de dónde se encuentre y de si ese historial se ha compartido**. También has visto que una rama puede seguir avanzando mientras una etiqueta permanece señalando un punto concreto.

Parte del flujo de hoy (crear una rama trabajando solo, abrir una Pull Request que tú mismo fusionas o etiquetar un repositorio que todavía no despliega nada) puede parecer innecesario. Más adelante dejará de serlo: las Pull Requests incorporarán comprobaciones automáticas, las etiquetas y releases de la aplicación permitirán saber exactamente qué versión se está desplegando y el historial servirá para reconstruir qué ocurrió cuando algo falle.

A partir de la próxima sesión el repositorio deja de ser solo organización. Empezarás a utilizarlo para **construir, ejecutar y desplegar Escaparate de forma reproducible**, comenzando por aislar la aplicación y sus dependencias del equipo concreto en el que se ejecuta.