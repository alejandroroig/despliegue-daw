# 🧪 Actividad 1.2: Tu repositorio como herramienta de despliegue

!!! warning "Descarga el material"
    Para esta actividad necesitas la aplicación [`escaparate-integrado.zip`](descargas/escaparate-integrado.zip){target="_blank" rel="noopener"}.

## Contexto

En la sesión anterior observaste despliegues ajenos desde fuera. Hoy empieza el tuyo, pero no por el servidor: por el lugar donde van a vivir **el código, la documentación y el procedimiento de trabajo**.

Tu objetivo es preparar el repositorio que utilizarás durante el resto del módulo. Debe cumplir tres condiciones:

- cualquier miembro del equipo con acceso debe poder clonarlo y entender qué contiene;
- no debe incorporar secretos ni ficheros locales que no deban versionarse;
- debe permitir identificar sin ambigüedad estados concretos del proyecto.

!!! info "Sobre Escaparate"
    `escaparate-integrado.zip` contiene la aplicación que utilizaremos como hilo conductor del módulo. **Todavía no necesitas entender ni modificar su código**: en esta actividad actúa simplemente como un proyecto real que debes incorporar correctamente al repositorio.

!!! abstract "Cómo vas a trabajar"
    **Preparar → proteger → publicar → trabajar en rama → corregir → revisar → etiquetar**

    La actividad no pretende que memorices comandos. Lo importante es que entiendas **qué estado tiene el repositorio**, qué cambios deben quedar fuera y qué operación corresponde cuando algo sale mal.

---

## Qué vas a practicar

- Crear y organizar el repositorio que utilizarás durante el módulo.
- Interpretar y completar reglas de `.gitignore`.
- Distinguir **directorio de trabajo**, **staging** e **historial**.
- Recuperarte de errores eligiendo la operación adecuada.
- Trabajar con ramas y Pull Requests.
- Interpretar una comprobación automática asociada a una PR.
- Documentar el trabajo en Markdown.
- Identificar un estado concreto mediante una etiqueta.

---

## Requisitos previos

- Git instalado y configurado con tu nombre y correo.
- Cuenta de GitHub.
- `escaparate-integrado.zip`.
- La carpeta completa `actividad-1.1/` de la sesión anterior, o el ZIP que entregaste en Aules.

!!! danger "Trabaja fuera de carpetas sincronizadas"
    No utilices OneDrive, iCloud, Google Drive ni otras carpetas sincronizadas para guardar el repositorio.

!!! info "Evidencias"
    Durante la actividad solo se pedirán **cuatro evidencias**. Hasta que crees la rama `sesion-02`, guarda las capturas temporalmente fuera del repositorio.

---

## Paso 1: Prepara el repositorio

Crea el repositorio haciendo que su rama principal se llame `main`:

```bash
mkdir daw-despliegue
cd daw-despliegue
git init -b main
```

Prepara esta estructura:

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

Puedes crear las carpetas y ficheros iniciales con:

```bash
mkdir -p docs entregas/tema1 escaparate practicas
touch README.md docs/README.md practicas/README.md
```

Descomprime `escaparate-integrado.zip` y coloca su contenido dentro de `escaparate/`.

El `README.md` de la raíz puede contener inicialmente:

```markdown
# DAW - Despliegue de Aplicaciones Web

Repositorio de trabajo del módulo.
```

En `docs/README.md` indica que la carpeta contendrá documentación general. En `practicas/README.md`, que contendrá los ficheros técnicos de configuración y despliegue creados durante el módulo.

Comprueba el estado:

```bash
git status
```

!!! question "Observa"
    `entregas/tema1/` todavía está vacía. ¿Aparece en `git status`? ¿Qué te indica eso sobre cómo trata Git los directorios vacíos?

---

## Paso 2: Decide qué no debe entrar

Antes del primer commit, revisa qué ficheros deben quedar fuera del historial.

### 2.1. Inspecciona Escaparate

Dentro de `escaparate/` ya existe un `.gitignore`. Ábrelo e identifica brevemente qué pretende excluir cada regla.

No lo borres: forma parte del proyecto recibido.

### 2.2. Protege el repositorio completo

Crea también un `.gitignore` en la **raíz de `daw-despliegue/`**.

Como mínimo deben quedar fuera:

- directorios de compilación Maven;
- ficheros `.class`;
- ficheros `.env`;
- configuración local de los IDE habituales;
- datos generados localmente, como las imágenes subidas durante las pruebas.

Utiliza lo visto en teoría para escribir las reglas. Después comprueba qué regla afecta a distintas rutas:

```bash
git check-ignore -v escaparate/target/prueba.class
git check-ignore -v escaparate/.env
git check-ignore -v escaparate/uploads/prueba.png
```

!!! warning "No ignores demasiado"
    Un fichero de configuración no es automáticamente un secreto y un `.jar` no es automáticamente prescindible. Excluye aquello que sabes que es local, sensible o reconstruible.

**Comprueba:** las rutas de prueba quedan ignoradas y los ficheros necesarios de Escaparate continúan apareciendo como candidatos a ser versionados.

**Evidencia 1:** captura donde se vea al menos una comprobación con `git check-ignore -v` y el resultado obtenido.

!!! question "Reflexiona"
    Si un fichero ya hubiese entrado en un commit y después lo añadieras a `.gitignore`, ¿desaparecería del historial?

    ¿Y si se hubiese publicado una contraseña real? ¿Bastaría con borrarla en un commit posterior?

---

## Paso 3: Publica la base del repositorio

Añade el contenido que sí debe versionarse y crea el primer commit:

```bash
git add .
git status
git commit -m "chore: crea la estructura inicial del repositorio"
```

Después crea en GitHub un repositorio **privado** llamado `daw-despliegue`, vacío y sin ficheros iniciales.

### 3.1. Crea el PAT del módulo

En este curso utilizaremos GitHub mediante HTTPS. Para simplificar las primeras sesiones crearás un único **Personal Access Token (classic)** que servirá para trabajar con el repositorio privado y, más adelante, con GitHub Container Registry.

En GitHub ve a:

```text
Settings
→ Developer settings
→ Personal access tokens
→ Tokens (classic)
```

Crea un token con:

```text
Nombre: DAW - Curso
Caducidad: hasta el final del curso

Scopes:
repo
workflow
write:packages
```

- `repo` permite trabajar con el repositorio privado mediante Git.
- `workflow` permite añadir y modificar workflows de GitHub Actions en `.github/workflows/`.
- `write:packages` se utilizará posteriormente para publicar imágenes en `ghcr.io`.

Cuando GitHub muestre el token, **guárdalo en un gestor de contraseñas**. No lo escribas dentro del repositorio.

!!! danger "El PAT es un secreto"
    No debe aparecer en el README, en `.env`, en `actividad-1.2.md` ni en ninguna captura. Si se filtra, revócalo y crea otro.

??? info "Alternativa: mínimo privilegio"
    En un entorno profesional es preferible separar credenciales por finalidad.

    Puedes utilizar un **fine-grained PAT** limitado a `daw-despliegue` para Git y crear posteriormente un PAT classic independiente con `write:packages` para GHCR.

    La opción utilizada en el aula simplifica la gestión de credenciales, pero concede permisos más amplios.

### 3.2. Enlaza y publica

Configura el remoto:

```bash
git remote add origin https://github.com/<usuario>/daw-despliegue.git
git push -u origin main
```

Si Git solicita credenciales:

```text
Username: <tu-usuario-de-GitHub>
Password: <tu-PAT>
```

En `Password` se introduce el PAT, **no la contraseña normal de GitHub**.

Añade al usuario de GitHub del profesor como **colaborador** del repositorio.

Comprueba que:

- la rama principal es `main`;
- el repositorio es privado;
- el profesor tiene acceso;
- el README aparece correctamente;
- `escaparate/` contiene el proyecto recibido;
- no has publicado secretos ni artefactos locales.

### 3.3. Añade la primera comprobación automática

Antes de empezar a trabajar con ramas vamos a dejar preparada una pequeña validación de las futuras Pull Requests.

!!! note "Una excepción de puesta en marcha"
    Este workflow se añade todavía directamente a `main` porque forma parte de la configuración inicial del repositorio. **A partir del paso 4, el trabajo normal se hará en ramas.**

Crea:

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
        uses: actions/checkout@v7

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

No necesitas estudiar todavía la sintaxis del workflow. Quédate con su intención: GitHub comprobará en un entorno limpio que existe la estructura básica y que no has registrado determinados ficheros que deberían permanecer fuera.

Regístralo y publícalo:

```bash
git add .github/workflows/validar.yml
git commit -m "chore: añade la validación básica del repositorio"
git push
```

La comprobación aparecerá cuando abras una Pull Request hacia `main`.

---

## Paso 4: Empieza a trabajar en una rama

La preparación inicial ha terminado. A partir de ahora no trabajarás directamente sobre `main`.

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

Debe aparecer:

```text
sesion-02
```

!!! info "Por qué publicamos ya la rama"
    Más adelante provocarás deliberadamente un error y lo publicarás. Queremos estudiar cómo corregir un historial **ya compartido**, pero sin ensuciar `main`.

---

## Paso 5: Recupera la Actividad 1.1 y dale formato

La Actividad 1.1 se escribió como texto sencillo porque todavía no habías trabajado Markdown. Ahora vas a integrarla correctamente en el repositorio.

Coloca la carpeta completa en:

```text
entregas/
└── tema1/
    └── actividad-1.1/
        ├── actividad-1.1.md
        └── img/
```

Sin cambiar tus conclusiones, mejora `actividad-1.1.md` utilizando:

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

!!! warning "No rehagas la actividad"
    El objetivo es **presentar mejor la misma evidencia**, no modificar los resultados para que coincidan con los de otra persona.

Registra la incorporación de esta actividad en un commit propio.

**Comprueba:** al abrir `actividad-1.1.md` desde GitHub, los títulos y las imágenes se renderizan correctamente.

---

## Paso 6: Tres errores, tres decisiones

Este es el núcleo de la actividad. Vas a provocar tres situaciones y resolverlas **sin que el enunciado te diga qué comando utilizar**.

Antes de empezar, crea:

```text
entregas/
└── tema1/
    └── actividad-1.2/
        ├── actividad-1.2.md
        └── img/
```

En `actividad-1.2.md` escribe inicialmente:

```markdown
# Actividad 1.2

## Recuperación de errores
```

Registra este pequeño esqueleto para empezar los ejercicios con el repositorio limpio.

Después de cada situación documenta:

```text
Zona de Git afectada:
Operación utilizada:
Por qué era adecuada:
```

### Situación 1: Cambio que todavía no has preparado

Escribe varios párrafos provisionales en el `README.md` de la raíz. No te convencen y quieres recuperar exactamente el contenido del último commit, **sin editar el fichero manualmente**.

**Comprueba:** el cambio desaparece y el repositorio vuelve al estado anterior.

### Situación 2: Cambio preparado que quieres conservar

Modifica de nuevo `README.md`, esta vez con contenido válido que sí quieres conservar, y prepáralo para el siguiente commit.

Después decides que todavía no quieres incluirlo. Sácalo del área de preparación **sin perder lo escrito**.

**Comprueba:**

- el fichero sigue modificado;
- el contenido permanece;
- el cambio ya no está preparado.

El contenido es válido. Regístralo y publícalo antes de continuar.

### Situación 3: El error ya está publicado

Añade al `README.md` una línea claramente equivocada, regístrala en un commit y **publícala en `sesion-02`**.

Ahora el error forma parte de un historial compartido.

Corrige su efecto de forma que:

- el contenido incorrecto desaparezca;
- el commit original siga existiendo;
- aparezca un nuevo commit que deshaga sus cambios.

Publica también la corrección.

**Evidencia 2:** captura del historial donde se vean el commit erróneo y el commit posterior que lo corrige.

!!! question "Reflexiona"
    ¿Por qué interesa conservar el commit original cuando el error ya se ha publicado? ¿Qué problema podría provocar reescribir una rama que otra persona ya hubiese descargado?

---

## Paso 7: Documenta la sesión y completa el README

Completa ahora `entregas/tema1/actividad-1.2/actividad-1.2.md`.

Debe ser un documento breve y contener:

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

Inserta las capturas solicitadas mediante rutas relativas.
```

Mueve a `img/` las evidencias que hayas guardado temporalmente y enlázalas desde el documento.

!!! tip "No dupliques los ficheros técnicos"
    `actividad-1.2.md` documenta lo realizado. Los ficheros reales —`.gitignore`, `README.md`, workflows o contenido de `escaparate/`— permanecen en su ubicación normal.

Después amplía el `README.md` de la raíz. Puedes utilizar este esqueleto:

```markdown
# DAW - Despliegue de Aplicaciones Web

Breve descripción del repositorio.

## Estructura

Qué contienen las carpetas principales.

## Requisitos

Herramientas básicas necesarias.

## Flujo de trabajo

Uso de ramas `sesion-NN` y Pull Requests hacia `main`.

## Puesta en marcha

Se completará en sesiones posteriores.

## Entregas

Enlaces relativos a las actividades 1.1 y 1.2.
```

El README **no es un diario de clase**: debe permitir a otra persona entender el repositorio y localizar la información importante.

Registra y publica los cambios pendientes de `sesion-02`.

---

## Paso 8: Revisa el trabajo mediante una Pull Request

Abre en GitHub una Pull Request:

```text
sesion-02 → main
```

La descripción debe responder brevemente a:

1. ¿Qué has cambiado?
2. ¿Por qué?
3. ¿Cómo has comprobado que queda en buen estado?

En la PR deberá aparecer la comprobación automática del repositorio.

Si falla:

```text
abrir detalle
→ identificar el problema
→ corregir en sesion-02
→ commit
→ push
→ revisar de nuevo la misma PR
```

!!! warning "No fusiones una comprobación fallida"
    Espera a que las comprobaciones hayan terminado y estén correctas antes de integrar los cambios.

**Evidencia 3:** captura de la Pull Request antes de fusionarla, con la comprobación automática correcta.

Cuando todo esté bien, fusiona mediante **Create a merge commit**.

Después actualiza tu `main` local:

```bash
git switch main
git pull --ff-only
```

Comprueba el historial:

```bash
git log --graph --oneline --all --decorate
```

Debe poder identificarse el trabajo realizado en `sesion-02` y su posterior integración en `main`.

!!! question "Reflexiona"
    La comprobación de hoy solo revisa aspectos básicos. ¿Qué otras comprobaciones tendría sentido realizar antes de desplegar una aplicación?

---

## Paso 9: Identifica este estado con una etiqueta

Ya tienes un nuevo estado integrado en `main`. Crea una **etiqueta anotada** llamada:

```text
v0.1.0
```

Utiliza los comandos vistos en teoría para crearla y publicarla.

Comprueba:

```bash
git tag -n
git show v0.1.0 --no-patch
git log --graph --oneline --all --decorate
```

En este momento `main` y `v0.1.0` apuntan al mismo commit. En próximas sesiones `main` seguirá avanzando, mientras que la etiqueta permanecerá identificando este estado.

!!! note "No es la versión de Escaparate"
    `v0.1.0` identifica un estado concreto del repositorio `daw-despliegue`. No tiene por qué coincidir con la versión interna de la aplicación.

**Evidencia 4:** captura o vista de GitHub donde se vea `v0.1.0` asociada al estado actual.

!!! question "Reflexiona"
    ¿Por qué un despliegue reproducible debería poder señalar una versión concreta, en lugar de limitarse a decir «despliega lo que haya ahora mismo en `main`»?

---

## Qué se entrega

Todo queda versionado en el propio repositorio. Antes de terminar, comprueba:

- [ ] repositorio privado `daw-despliegue` con el profesor como colaborador;
- [ ] Escaparate incorporado y `.gitignore` correctamente configurado;
- [ ] Actividad 1.1 formateada en Markdown con sus imágenes relativas;
- [ ] Actividad 1.2 con las tres situaciones, reflexiones y **cuatro evidencias**;
- [ ] workflow `validar.yml` y comprobación correcta en la Pull Request;
- [ ] Pull Request `sesion-02 → main` fusionada mediante merge commit;
- [ ] README actualizado;
- [ ] etiqueta anotada `v0.1.0` publicada.

!!! info "Dónde queda la entrega"
    Desde esta actividad, la evidencia del trabajo queda en el propio repositorio. **No necesitas generar un Word ni convertir la actividad a PDF.**

??? info "Cómo se comprobará"
    El profesor podrá clonar el repositorio utilizando la cuenta añadida como colaboradora y comprobará, entre otras cosas:

    - que la rama principal es `main`;
    - que no hay `.env`, `target/` ni `.class` registrados;
    - que Escaparate no contiene un repositorio Git anidado;
    - que el error publicado y su corrección siguen visibles en el historial;
    - que `sesion-02` se integró mediante una Pull Request;
    - que la comprobación automática terminó correctamente;
    - que `v0.1.0` es una etiqueta anotada;
    - que las actividades 1.1 y 1.2 están correctamente documentadas.

---

## ✅ Cierre

Has preparado el repositorio que utilizarás durante el módulo y has empezado a tratarlo como algo más que un lugar donde guardar código: contiene **historial, documentación, decisiones de configuración y estados identificables**.

También has practicado una regla que será importante durante todo el curso: **para corregir un problema primero debes saber dónde está el cambio y si el historial ya se ha compartido**.

En la próxima sesión ese repositorio empezará a utilizarse para construir y ejecutar Escaparate de forma reproducible.
