# 🧪 Actividad 1.2: Tu repositorio como herramienta de despliegue

!!! warning "Descarga el material"
    Para esta actividad necesitas la aplicación [`escaparate-integrado.zip`](descargas/escaparate-integrado.zip){target="_blank" rel="noopener"}.

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
- Interpretar el resultado de una comprobación automática asociada a una Pull Request.
- Crear una etiqueta para identificar un estado concreto del repositorio.

## Requisitos previos

- Git instalado, con tu nombre y tu correo configurados.
- **Cuenta de GitHub**. No necesitas traer un token preparado: configurarás la autenticación HTTPS en el paso 3.
- `escaparate-integrado.zip`.
- La carpeta completa `actividad-1.1/` de la sesión anterior (o el fichero comprimido subido a Aules).

!!! danger "Fuera de carpetas sincronizadas"
    Trabaja fuera de OneDrive, iCloud, Google Drive u otras carpetas sincronizadas.

!!! info "Evidencias de esta actividad"
    No necesitas capturar cada paso. Conserva únicamente las evidencias que se indican expresamente en el enunciado. Hasta que crees la rama `sesion-02`, guarda cualquier captura temporal **fuera del repositorio**.

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

**Evidencia 1:** una captura donde se vea `git check-ignore -v` funcionando sobre alguna de las rutas de prueba y el estado final del repositorio.

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


!!! info "Privado durante el curso, publicable al final"
    El repositorio se mantendrá privado mientras realizas las actividades para que el trabajo no quede expuesto al resto de la clase. Al final del módulo, después de revisar que no contiene secretos, credenciales ni datos personales, podrás cambiarlo a público y conservarlo como muestra de tu trabajo técnico.

### 3.3 Deja preparada la primera comprobación automática

A partir de esta sesión las Pull Requests tendrán una pequeña comprobación automática. Antes de pasar a los ejercicios de recuperación, déjala ya versionada en `main`. Crea:

```text
.github/
└── workflows/
    └── validar.yml
```

con este contenido:

```yaml
name: Validar repositorio

on:
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  estructura:
    name: Comprobar repositorio
    runs-on: ubuntu-latest

    steps:
      - name: Descargar repositorio
        uses: actions/checkout@v4

      - name: Comprobar estructura básica
        run: |
          test -f escaparate/pom.xml
          test -f escaparate/mvnw
          test -f practicas/README.md

      - name: Comprobar ficheros que no deben versionarse
        shell: bash
        run: |
          prohibidos="$(
            git ls-files \
              | grep -E '(^|/)\.env($|\.)|(^|/)target/|\.class$' \
              | grep -vE '(^|/)\.env\.example$' \
              || true
          )"

          if [ -n "$prohibidos" ]; then
            echo "Se han encontrado ficheros que no deberían estar versionados:"
            echo "$prohibidos"
            exit 1
          fi
```

No necesitas estudiar todavía la sintaxis del workflow. De momento quédate con su intención: GitHub comprobará en un equipo limpio que existe la estructura básica del repositorio y que no has registrado determinados ficheros que deberían permanecer fuera del historial.

Registra y publica este fichero en `main`. Todavía no verás la comprobación en acción: aparecerá cuando abras la Pull Request de `sesion-02` al final de la actividad.


---

## Paso 4: Empieza a trabajar como lo harás durante el módulo

Hasta este punto has preparado el repositorio base y lo has publicado en `main`. A partir de ahora el trabajo normal no se hará directamente sobre la rama principal.

Crea y publica la rama de esta sesión:

```bash
git switch -c sesion-02
git push -u origin sesion-02
```

Comprueba:

```bash
git branch --show-current
git status
```

Debe aparecer `sesion-02`.

!!! info "Por qué publicamos ya la rama"
    Más adelante vas a provocar un error que llegará al repositorio remoto. Queremos estudiar cómo corregir un historial **ya compartido**, pero sin ensuciar deliberadamente `main`.

---

## Paso 5: Recupera la actividad 1.1 y dale formato Markdown

La actividad 1.1 se escribió como texto sencillo porque todavía no habías trabajado Markdown. Ahora vas a darle un formato mínimo y a incorporarla al repositorio.

Recupera la carpeta completa `actividad-1.1/` y colócala en:

```text
entregas/
└── tema1/
    └── actividad-1.1/
        ├── actividad-1.1.md
        └── img/
```

Sin reescribir su contenido, mejora `actividad-1.1.md` utilizando únicamente lo que has visto en teoría:

- títulos y subtítulos;
- listas cuando ayuden a ordenar información;
- código en línea para comandos, nombres de ficheros o cabeceras;
- imágenes mediante rutas relativas.

Por ejemplo:

```markdown
## Cabeceras

### Web de apuntes

Código observado: `200`.

![Cabeceras de la web de apuntes](img/cabeceras-apuntes.png)
```

!!! warning "No cambies las conclusiones"
    El objetivo no es rehacer la Actividad 1.1, sino **presentar mejor la misma evidencia**. No modifiques tus resultados para que coincidan con los de otra persona.

Registra la incorporación de la actividad con un commit propio.

**Comprueba:** al abrir `actividad-1.1.md` desde GitHub, los títulos y las imágenes se renderizan correctamente.

---

## Paso 6: Tres maneras de estropearlo y tres de arreglarlo

Aquí está el núcleo de la sesión. Vas a provocar tres situaciones y a resolver cada una eligiendo tú la operación adecuada. **No se indica qué comando usar**: debes decidirlo a partir del estado en que se encuentra el cambio.

Después de cada situación anota en `actividad-1.2.md`:

```text
Zona de Git afectada:
Operación utilizada:
Por qué era adecuada:
```

### Situación 1: Cambio que todavía no has preparado

Escribe tres párrafos en el `README.md` de la carpeta raíz y déjalos a medias. No te convencen. Decides descartarlos por completo y quieres volver al contenido que tenía el fichero en el último commit, sin editarlo manualmente.

**Comprueba:** el cambio desaparece y el repositorio vuelve al estado anterior.

### Situación 2: Cambio preparado que quieres conservar

Modifica otra vez el `README.md`, esta vez con contenido válido que sí quieres conservar. Prepáralo para el siguiente commit. Después decides que todavía no quieres incluir ese fichero en el próximo commit.

Sácalo del área de preparación **sin perder lo escrito**.

**Comprueba:**

- el fichero sigue modificado;
- el contenido permanece;
- el cambio ya no está preparado.

El contenido era válido. Regístralo ahora en un commit normal y publícalo para que la siguiente situación empiece desde un estado limpio.

### Situación 3: El error ya está publicado

Añade al `README.md` una línea claramente equivocada, regístrala en un commit y **publícala en `sesion-02`**.

Ahora el error ya forma parte de un historial compartido: está en GitHub y otra persona podría haberlo descargado.

Deshaz su efecto de forma que:

- el contenido incorrecto desaparezca;
- el commit original siga existiendo;
- aparezca un nuevo commit que deshaga sus cambios.

Publica también la corrección.

**Evidencia 2:** una captura del historial donde se vean el commit erróneo y el commit posterior que lo corrige.

!!! question "Reflexiona"
    ¿Por qué en esta tercera situación interesa conservar el commit original en el historial? ¿Qué problema podría provocar reescribir una rama que otra persona ya hubiese descargado?

---

## Paso 7: Documenta la sesión y completa el README

Crea:

```text
entregas/
└── tema1/
    └── actividad-1.2/
        ├── actividad-1.2.md
        └── img/
```

Mueve a `img/` las evidencias de esta actividad e insértalas en `actividad-1.2.md` mediante rutas relativas.

El documento debe ser breve. Utiliza esta estructura:

```markdown
# Actividad 1.2

## Qué he decidido excluir del repositorio

Explica las decisiones principales de `.gitignore`.

## Recuperación de errores

### Cambio no preparado

Zona de Git afectada:
Operación utilizada:
Por qué era adecuada:

### Cambio preparado

Zona de Git afectada:
Operación utilizada:
Por qué era adecuada:

### Commit publicado

Zona de Git afectada:
Operación utilizada:
Por qué era adecuada:

## Reflexiones

Incluye las respuestas pedidas durante la actividad.

## Evidencias

Inserta aquí las capturas solicitadas.
```

!!! tip "No dupliques los ficheros técnicos"
    `actividad-1.2.md` documenta lo que has hecho. Los ficheros reales del proyecto, como `.gitignore`, `README.md` o el contenido de `escaparate/`, permanecen en su ubicación normal dentro del repositorio.

### Completa el README del repositorio

Amplía ahora el `README.md` de la raíz. Puedes partir de este esqueleto:

```markdown
# DAW - Despliegue de Aplicaciones Web

Breve descripción del repositorio.

## Estructura

Explica qué contienen las carpetas principales.

## Requisitos

Indica las herramientas básicas necesarias.

## Flujo de trabajo

Explica el uso de ramas `sesion-NN` y Pull Requests hacia `main`.

## Puesta en marcha

Se completará en sesiones posteriores.

## Entregas

Enlaza las actividades 1.1 y 1.2 mediante rutas relativas.
```

No conviertas el README en un diario de clase. Debe servir para que otra persona entienda el repositorio y pueda localizar la información importante.

---

## Paso 8: Revisa el trabajo mediante una Pull Request

Registra los cambios pendientes de `sesion-02` y publícalos.

Abre en GitHub una Pull Request:

```text
sesion-02 → main
```

La descripción debe responder brevemente a:

1. qué has cambiado;
2. por qué;
3. cómo has comprobado que el repositorio queda en buen estado.

En la PR aparecerá la comprobación:

```text
Comprobar repositorio  ✓
```

Si aparece en rojo, abre su detalle, identifica qué ha fallado, corrige el problema en `sesion-02`, crea otro commit y publícalo. La misma PR se actualizará automáticamente.

**No fusiones mientras haya una comprobación pendiente o fallida.**

**Evidencia 3:** captura de la Pull Request antes de fusionarla, con la comprobación automática en verde.

Cuando todo esté correcto, fusiónala mediante **Create a merge commit**.

Después actualiza tu rama principal local:

```bash
git switch main
git pull --ff-only
```

Comprueba:

```bash
git log --graph --oneline --all --decorate
```

Debe poder identificarse la rama de trabajo y su posterior integración en `main`.

!!! question "Reflexiona"
    Hoy la comprobación automática solo revisa aspectos básicos del repositorio. ¿Qué otras cosas tendría sentido comprobar antes de permitir que una aplicación llegara a desplegarse?

---

## Paso 9: Marca el primer estado identificable

Sobre `main` ya integrado, crea una **etiqueta anotada**:

```text
v0.1.0
```

Añade un mensaje que describa qué contiene este primer estado identificable del repositorio y publica la etiqueta.

!!! note "Dos cosas diferentes"
    `v0.1.0` identifica un estado concreto de `daw-despliegue`. No tiene por qué coincidir con ninguna versión interna de Escaparate.

Comprueba:

```bash
git tag -n
git show v0.1.0 --no-patch
git log --graph --oneline --all --decorate
```

En este momento `main` y `v0.1.0` apuntan al mismo estado. En próximas sesiones `main` seguirá avanzando, mientras que la etiqueta continuará identificando este punto concreto.

**Evidencia 4:** salida o vista de GitHub donde se vea `v0.1.0` asociada al estado actual.

!!! question "Reflexiona"
    ¿Por qué un procedimiento de despliegue reproducible debería poder señalar una versión concreta y estable, en lugar de limitarse a decir "despliega lo que haya ahora mismo en `main`"?

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
test -f .github/workflows/validar.yml && echo "workflow OK"
```

Debe observarse:

- rama principal `main`;
- ningún `.env`, fichero de `target/` o `.class` registrado en el historial;
- ningún repositorio Git anidado dentro de `escaparate/`;
- el commit erróneo de `sesion-02` y el commit posterior que lo deshace;
- la fusión de `sesion-02`;
- `v0.1.0` como etiqueta anotada;
- actividades 1.1 y 1.2 en `entregas/tema1/`;
- la Actividad 1.1 correctamente formateada en Markdown y con imágenes relativas;
- Escaparate con su `pom.xml` y Maven Wrapper.

La Pull Request se comprobará también en GitHub.

---

## Qué se entrega

- [ ] Repositorio privado `daw-despliegue` con el profesor añadido como colaborador.
- [ ] `escaparate-integrado` incorporado como carpeta `escaparate/`.
- [ ] `.gitignore` del repositorio y análisis del `.gitignore` recibido con Escaparate.
- [ ] Actividad 1.1 incorporada en `entregas/tema1/actividad-1.1/`, formateada en Markdown y con sus imágenes enlazadas mediante rutas relativas.
- [ ] `entregas/tema1/actividad-1.2/actividad-1.2.md` con las decisiones sobre `.gitignore`, las tres situaciones de recuperación y las reflexiones solicitadas.
- [ ] `entregas/tema1/actividad-1.2/img/` con las cuatro evidencias solicitadas.
- [ ] `.github/workflows/validar.yml` versionado y comprobación automática correcta en la PR.
- [ ] Pull Request `sesion-02 → main` fusionada.
- [ ] README actualizado.
- [ ] Etiqueta anotada `v0.1.0`.

!!! info "Dónde queda la entrega"
    Desde esta actividad, la evidencia del trabajo queda versionada en el propio repositorio. No necesitas generar un documento Word ni convertir la actividad a PDF.

---

## ✅ Cierre

Ya tienes preparado el repositorio que utilizarás como base durante el módulo. Dentro conviven la aplicación, la documentación, tus entregas y el espacio donde irás incorporando las distintas configuraciones de despliegue. Además, el historial empieza limpio: has decidido qué debe versionarse, qué debe quedarse fuera y has marcado un primer estado identificable con `v0.1.0`.

Durante la práctica has comprobado que Git no guarda simplemente "versiones de ficheros". Un cambio puede encontrarse en el directorio de trabajo, en el área de preparación o formando ya parte del historial, y **la forma correcta de deshacerlo depende de dónde se encuentre y de si ese historial se ha compartido**. También has convertido la evidencia de la sesión anterior en documentación Markdown integrada en el repositorio y has visto cómo una etiqueta identifica un estado concreto aunque `main` siga avanzando en sesiones posteriores.

Parte del flujo de hoy (crear una rama trabajando solo, abrir una Pull Request que tú mismo fusionas o etiquetar un repositorio que todavía no despliega nada) puede parecer innecesario. Más adelante dejará de serlo: la pequeña comprobación automática que has visto irá creciendo, las etiquetas y releases de la aplicación permitirán saber exactamente qué versión se está desplegando y el historial servirá para reconstruir qué ocurrió cuando algo falle.

A partir de la próxima sesión el repositorio deja de ser solo organización. Empezarás a utilizarlo para **construir, ejecutar y desplegar Escaparate de forma reproducible**, comenzando por aislar la aplicación y sus dependencias del equipo concreto en el que se ejecuta.