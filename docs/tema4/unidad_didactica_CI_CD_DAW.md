# Unidad didáctica: Integración continua y despliegue continuo (CI/CD)

## 1. Datos generales

**Módulo:** Desarrollo de Aplicaciones Web (DAW)\
**Unidad:** Integración continua y despliegue continuo\
**Duración recomendada:** 6--8 horas\
**Modalidad:** Teoría + demostración + práctica guiada + actividad
individual\
**Nivel:** Ciclo Formativo de Grado Superior\
**Herramientas:** Git, GitHub, GitHub Actions y GitHub Pages

------------------------------------------------------------------------

## 2. Introducción

En el desarrollo tradicional de aplicaciones, un desarrollador modifica
el código, comprueba manualmente que funciona y posteriormente copia o
publica la aplicación en el servidor.

Este proceso puede funcionar cuando el proyecto es pequeño, pero
presenta numerosos problemas cuando intervienen varios desarrolladores o
cuando se realizan cambios con frecuencia.

Algunos problemas habituales son:

-   Los cambios pueden romper funcionalidades existentes.
-   Los errores pueden descubrirse varios días después de introducirse.
-   El despliegue puede depender de una persona concreta.
-   Los procesos manuales son propensos a errores.
-   No siempre se sabe exactamente qué versión está desplegada.
-   Integrar los cambios de diferentes desarrolladores puede resultar
    complicado.

Las prácticas de **Integración Continua (Continuous Integration, CI)** y
**Entrega/Despliegue Continuos (Continuous Delivery/Deployment, CD)**
buscan automatizar buena parte de este proceso.

El objetivo de esta unidad es que el alumno no solamente conozca estos
conceptos de forma teórica, sino que pueda observar el proceso completo:

``` text
Modificar código
      ↓
    commit
      ↓
     push
      ↓
Integración continua
      ↓
 Tests / comprobaciones
      ↓
    Build
      ↓
 Despliegue automático
      ↓
 Aplicación publicada
```

------------------------------------------------------------------------

## 3. Objetivos didácticos

Al finalizar la unidad, el alumno será capaz de:

1.  Explicar qué es la integración continua.
2.  Explicar qué es la entrega continua.
3.  Explicar qué es el despliegue continuo.
4.  Diferenciar CI, Continuous Delivery y Continuous Deployment.
5.  Comprender qué es un pipeline.
6.  Identificar las diferentes fases de un pipeline.
7.  Comprender el papel de Git dentro de un proceso CI/CD.
8.  Utilizar GitHub Actions para automatizar tareas.
9.  Configurar un workflow básico.
10. Ejecutar pruebas automáticamente ante cambios en el código.
11. Automatizar el despliegue de una aplicación web.
12. Analizar el resultado de una ejecución de CI/CD.
13. Identificar errores producidos durante un pipeline.
14. Comprender las ventajas y limitaciones de la automatización.
15. Aplicar un flujo básico de trabajo basado en CI/CD.

------------------------------------------------------------------------

## 4. Conceptos previos

Se recomienda que el alumnado tenga conocimientos básicos de:

-   HTML.
-   CSS.
-   JavaScript.
-   Git.
-   Repositorios.
-   `commit`.
-   `push`.
-   `pull`.
-   Ramas.

No es necesario que conozcan previamente GitHub Actions.

------------------------------------------------------------------------

## 5. Contenidos

### 5.1. Desarrollo tradicional

Imaginemos una pequeña empresa que desarrolla una aplicación web.

El proceso podría ser:

``` text
Desarrollador
     ↓
Modifica código
     ↓
Prueba manualmente
     ↓
Comprime archivos
     ↓
Accede al servidor
     ↓
Copia archivos
     ↓
Comprueba la aplicación
```

Este sistema tiene varios inconvenientes.

#### Problema 1: procesos manuales

Cada despliegue requiere repetir una serie de pasos.

#### Problema 2: errores humanos

Un archivo puede no copiarse correctamente, una configuración puede
quedar modificada o puede desplegarse una versión incorrecta.

#### Problema 3: integración tardía

Si varios desarrolladores trabajan durante varios días en ramas
diferentes, integrar sus cambios puede resultar complicado.

#### Problema 4: pruebas insuficientes

Un desarrollador puede comprobar únicamente la funcionalidad que acaba
de modificar.

Puede no darse cuenta de que ha roto otra parte de la aplicación.

------------------------------------------------------------------------

## 6. Integración continua

La **Integración Continua (Continuous Integration o CI)** consiste en
integrar frecuentemente los cambios realizados por los desarrolladores
en un repositorio común y ejecutar automáticamente una serie de
comprobaciones.

Una simplificación sería:

``` text
Desarrollador
      ↓
  git push
      ↓
┌───────────────┐
│ CI            │
│               │
│ Tests         │
│ Análisis      │
│ Build         │
└───────┬───────┘
        ↓
    Resultado
```

La idea fundamental es:

> Integrar los cambios frecuentemente y detectar los problemas lo antes
> posible.

------------------------------------------------------------------------

## 7. ¿Qué puede hacer una herramienta de CI?

Cuando un desarrollador realiza un `push`, un sistema de integración
continua puede:

1.  Descargar el código.
2.  Instalar dependencias.
3.  Ejecutar pruebas.
4.  Analizar el código.
5.  Construir la aplicación.
6.  Generar artefactos.
7.  Informar del resultado.

Por ejemplo:

``` text
git push
   ↓
Descargar código
   ↓
npm install
   ↓
npm test
   ↓
npm run build
   ↓
¿Correcto?
```

Si alguna fase falla, el pipeline puede detenerse.

------------------------------------------------------------------------

## 8. ¿Qué es un pipeline?

Un **pipeline** es una secuencia automatizada de tareas que se ejecutan
para comprobar, construir y/o desplegar una aplicación.

Por ejemplo:

``` text
┌──────────────┐
│   Checkout   │
└──────┬───────┘
       ↓
┌──────────────┐
│ Dependencias │
└──────┬───────┘
       ↓
┌──────────────┐
│    Tests     │
└──────┬───────┘
       ↓
┌──────────────┐
│     Build    │
└──────┬───────┘
       ↓
┌──────────────┐
│    Deploy    │
└──────────────┘
```

Cada una de estas fases puede estar formada por múltiples pasos.

------------------------------------------------------------------------

## 9. Continuous Delivery

La **Entrega Continua (Continuous Delivery)** lleva un paso más allá la
integración continua.

La aplicación se mantiene en un estado en el que está preparada para ser
desplegada.

Por ejemplo:

``` text
Código
  ↓
Tests
  ↓
Build
  ↓
Aplicación preparada
  ↓
[APROBACIÓN]
  ↓
Producción
```

El despliegue puede requerir una acción humana.

------------------------------------------------------------------------

## 10. Continuous Deployment

En el **Despliegue Continuo (Continuous Deployment)**, el despliegue se
produce automáticamente cuando todas las comprobaciones necesarias han
terminado correctamente.

``` text
Código
  ↓
Tests
  ↓
Build
  ↓
Deploy automático
  ↓
Producción
```

Por ejemplo:

``` text
Alumno hace push
       ↓
Tests automáticos
       ↓
      OK
       ↓
Build
       ↓
      OK
       ↓
Deploy
       ↓
Página web actualizada
```

------------------------------------------------------------------------

## 11. Diferencia entre CI, Continuous Delivery y Continuous Deployment

  -----------------------------------------------------------------------
  Concepto                            Objetivo
  ----------------------------------- -----------------------------------
  Continuous Integration              Integrar y comprobar cambios
                                      frecuentemente

  Continuous Delivery                 Mantener el software preparado para
                                      desplegar

  Continuous Deployment               Desplegar automáticamente los
                                      cambios válidos
  -----------------------------------------------------------------------

Una forma sencilla de recordarlo:

``` text
CI
↓
¿El cambio funciona?

Continuous Delivery
↓
¿Está preparado para publicar?

Continuous Deployment
↓
Publícalo automáticamente
```

------------------------------------------------------------------------

## 12. DevOps

CI/CD está estrechamente relacionado con **DevOps**.

DevOps busca mejorar la colaboración entre las actividades
tradicionalmente asociadas al desarrollo y a las operaciones.

Una representación sencilla:

``` text
Plan
 ↓
Code
 ↓
Build
 ↓
Test
 ↓
Release
 ↓
Deploy
 ↓
Operate
 ↓
Monitor
 ↓
      ↺
```

El proceso es continuo.

La automatización es uno de los elementos fundamentales.

------------------------------------------------------------------------

## 13. Ventajas de CI/CD

### Detección temprana de errores

Los errores se detectan poco después de introducirse.

### Automatización

Se reducen tareas manuales.

### Repetibilidad

El mismo proceso se ejecuta de la misma manera.

### Rapidez

Los cambios pueden llegar a los usuarios con mayor rapidez.

### Trazabilidad

Es posible relacionar un despliegue con un commit concreto.

### Confianza

Si el pipeline contiene pruebas adecuadas, el equipo puede tener mayor
confianza en los cambios.

------------------------------------------------------------------------

## 14. CI/CD no significa que todo sea automático

Es importante transmitir al alumno que:

> Automatizar un proceso no significa que el proceso sea necesariamente
> correcto.

Un pipeline mal diseñado puede automatizar errores.

Por ejemplo:

``` text
Código incorrecto
      ↓
Pipeline automático
      ↓
Deploy automático
      ↓
Aplicación incorrecta
```

Por eso son importantes:

-   las pruebas,
-   las revisiones de código,
-   los entornos separados,
-   las políticas de protección,
-   la monitorización,
-   la posibilidad de volver a una versión anterior.

------------------------------------------------------------------------

## 15. Herramientas habituales

Existen muchas herramientas para implementar CI/CD.

Algunos ejemplos son:

-   GitHub Actions.
-   GitLab CI/CD.
-   Jenkins.
-   Azure Pipelines.
-   CircleCI.
-   Travis CI.
-   Bitbucket Pipelines.

En esta práctica utilizaremos **GitHub Actions**.

La elección se debe principalmente a que permite realizar una práctica
completa con una infraestructura relativamente sencilla.

------------------------------------------------------------------------

## 16. GitHub Actions

GitHub Actions permite automatizar tareas relacionadas con los
repositorios de GitHub.

Un workflow puede ejecutarse como consecuencia de diferentes eventos.

Por ejemplo:

``` text
push
pull request
creación de una release
ejecución manual
programación temporal
```

En nuestra práctica utilizaremos principalmente:

``` text
push
```

Es decir:

> Cada vez que se realice un `push`, GitHub ejecutará nuestro proceso
> automatizado.

------------------------------------------------------------------------

## 17. Workflow

Un workflow describe qué debe hacer GitHub Actions.

Los workflows se almacenan normalmente dentro de:

``` text
.github/workflows/
```

Por ejemplo:

``` text
mi-web/
│
├── index.html
├── style.css
│
└── .github/
    └── workflows/
        └── deploy.yml
```

El archivo `deploy.yml` describe el proceso.

------------------------------------------------------------------------

## 18. Jobs y steps

Un workflow puede contener uno o varios **jobs**.

Cada job contiene diferentes **steps**.

Ejemplo conceptual:

``` text
WORKFLOW
│
└── JOB
    │
    ├── STEP 1
    ├── STEP 2
    ├── STEP 3
    └── STEP 4
```

Por ejemplo:

``` text
Workflow
 └── deploy
      ├── Descargar código
      ├── Configurar entorno
      ├── Ejecutar tests
      └── Desplegar
```

------------------------------------------------------------------------

# 19. Práctica: nuestra primera aplicación CI/CD

## Objetivo

Construiremos una pequeña aplicación web y configuraremos un proceso que
permita:

1.  Almacenar el código en GitHub.
2.  Ejecutar comprobaciones automáticamente.
3.  Publicar la aplicación.
4.  Modificar la aplicación.
5.  Hacer `push`.
6.  Comprobar cómo el cambio llega automáticamente a la web publicada.

------------------------------------------------------------------------

## 20. Aplicación inicial

Crearemos una web muy sencilla.

### Estructura

``` text
daw-cicd-demo/
│
├── index.html
├── style.css
└── script.js
```

### index.html

``` html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DAW CI/CD</title>
    <link rel="stylesheet" href="style.css">
</head>

<body>

    <main class="container">

        <h1>Mi primera aplicación CI/CD</h1>

        <p>
            Esta página se despliega automáticamente.
        </p>

        <button id="button">
            Probar aplicación
        </button>

        <p id="message"></p>

    </main>

    <script src="script.js"></script>

</body>
</html>
```

------------------------------------------------------------------------

## 21. CSS

### style.css

``` css
* {
    box-sizing: border-box;
}

body {
    margin: 0;
    font-family: Arial, sans-serif;
    background: #f4f4f4;
}

.container {
    max-width: 800px;
    margin: 100px auto;
    padding: 40px;
    text-align: center;
    background: white;
    border-radius: 12px;
}

button {
    padding: 12px 20px;
    cursor: pointer;
}
```

------------------------------------------------------------------------

## 22. JavaScript

### script.js

``` javascript
const button = document.getElementById("button");
const message = document.getElementById("message");

button.addEventListener("click", () => {
    message.textContent = "¡La aplicación funciona correctamente!";
});
```

------------------------------------------------------------------------

## 23. Primera publicación

Cada alumno debe crear un repositorio.

Por ejemplo:

``` text
daw-cicd-nombre
```

La estructura inicial será:

``` text
daw-cicd-nombre/
│
├── index.html
├── style.css
└── script.js
```

Después deberá realizar:

``` bash
git init
git add .
git commit -m "Versión inicial"
git branch -M main
git remote add origin <repositorio>
git push -u origin main
```

------------------------------------------------------------------------

## 24. Activación de GitHub Pages

A continuación se configurará GitHub Pages para publicar la aplicación.

La idea que queremos conseguir es:

``` text
Repositorio GitHub
        ↓
     GitHub Pages
        ↓
    Aplicación web
```

En este punto los alumnos deben comprobar que la aplicación funciona
públicamente.

------------------------------------------------------------------------

## 25. Primera reflexión

Antes de automatizar nada, plantearemos al alumnado:

> Si ahora modificamos `index.html`, ¿qué tenemos que hacer para que la
> modificación llegue a la web?

La respuesta inicial será aproximadamente:

``` text
Modificar
↓
commit
↓
push
↓
publicar
```

Aquí introducimos la pregunta fundamental:

> ¿Podemos automatizar la última parte?

------------------------------------------------------------------------

# 26. Nuestro primer workflow

Crearemos:

``` text
.github/workflows/deploy.yml
```

El workflow será el encargado de automatizar el proceso de publicación.

Un ejemplo de workflow para una web estática puede ser:

``` yaml
name: Deploy web

on:
  push:
    branches:
      - main

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  deploy:
    environment:
      name: github-pages

    runs-on: ubuntu-latest

    steps:
      - name: Descargar código
        uses: actions/checkout@v4

      - name: Configurar Pages
        uses: actions/configure-pages@v5

      - name: Subir artefacto
        uses: actions/upload-pages-artifact@v3
        with:
          path: .

      - name: Desplegar
        uses: actions/deploy-pages@v4
```

El profesor puede explicar el workflow progresivamente, no entregarlo
todo de golpe.

------------------------------------------------------------------------

## 27. Explicación del workflow

### name

``` yaml
name: Deploy web
```

Es el nombre que veremos en GitHub Actions.

### on

``` yaml
on:
  push:
    branches:
      - main
```

Indica cuándo se ejecutará.

En este caso:

> Cada vez que se haga `push` sobre `main`.

### permissions

``` yaml
permissions:
  contents: read
  pages: write
  id-token: write
```

Indica los permisos necesarios para que el workflow pueda realizar las
operaciones correspondientes.

Es importante explicar que los permisos deben concederse de forma
controlada y que no conviene otorgar más permisos de los necesarios.

### runs-on

``` yaml
runs-on: ubuntu-latest
```

Indica el entorno en el que se ejecutará el job.

### checkout

``` yaml
uses: actions/checkout@v4
```

Descarga el contenido del repositorio para que el workflow pueda
trabajar con él.

### upload-pages-artifact

``` yaml
uses: actions/upload-pages-artifact@v3
```

Prepara los archivos que se van a publicar.

### deploy-pages

``` yaml
uses: actions/deploy-pages@v4
```

Realiza el despliegue en GitHub Pages.

------------------------------------------------------------------------

# 28. El pipeline completo

Ahora tenemos:

``` text
Alumno
  │
  │ modifica código
  ↓
git commit
  │
  ↓
git push
  │
  ↓
GitHub
  │
  ↓
GitHub Actions
  │
  ├── Checkout
  │
  ├── Configuración
  │
  ├── Preparación
  │
  └── Deploy
  │
  ↓
GitHub Pages
  │
  ↓
🌍 Aplicación web
```

------------------------------------------------------------------------

# 29. La demostración que debe hacer el profesor

Esta es una de las partes más importantes de la unidad.

Con la web abierta en una pestaña del navegador:

### Paso 1

Modificar:

``` html
<h1>Mi primera aplicación CI/CD</h1>
```

por:

``` html
<h1>Mi primera aplicación desplegada automáticamente</h1>
```

### Paso 2

Ejecutar:

``` bash
git add .
git commit -m "Cambio título"
git push
```

### Paso 3

Entrar en la sección **Actions** del repositorio.

Los alumnos observarán que aparece una nueva ejecución.

### Paso 4

Esperar a que termine correctamente.

### Paso 5

Recargar la aplicación publicada.

El nuevo título aparecerá automáticamente.

------------------------------------------------------------------------

# 30. Momento pedagógico clave

En este punto conviene detener la explicación.

Preguntar a los alumnos:

> ¿Qué hemos hecho nosotros después de modificar el código?

La respuesta:

``` text
commit
push
```

Preguntar:

> ¿Hemos entrado en el servidor?

No.

> ¿Hemos copiado los archivos manualmente?

No.

> ¿Hemos ejecutado el despliegue?

No.

Entonces:

> ¿Quién lo ha hecho?

**El pipeline.**

Esta es la idea fundamental que debe quedar clara.

------------------------------------------------------------------------

# 31. Ejercicio 1 --- Modificación sencilla

Cada alumno deberá modificar:

-   título,
-   subtítulo,
-   texto del botón.

Después realizará:

``` bash
git add .
git commit -m "Personalizo la aplicación"
git push
```

Deberá comprobar:

1.  Que aparece una nueva ejecución.
2.  Que el pipeline termina correctamente.
3.  Que la aplicación publicada se actualiza.

------------------------------------------------------------------------

# 32. Ejercicio 2 --- Modificación visual

Cada alumno deberá personalizar el CSS.

Debe modificar como mínimo:

-   fondo,
-   tamaño del título,
-   aspecto del botón,
-   espaciado.

Por ejemplo:

``` css
body {
    font-family: Arial, sans-serif;
    background: #e8f0fe;
}
```

Después:

``` bash
git add .
git commit -m "Personalizo estilos"
git push
```

El alumno deberá documentar el proceso.

------------------------------------------------------------------------

# 33. Ejercicio 3 --- Nueva funcionalidad

Añadir una funcionalidad JavaScript.

Por ejemplo:

> Al pulsar el botón debe aparecer el nombre de la asignatura.

El resultado puede ser:

``` text
Desarrollo de Aplicaciones Web
```

El alumno deberá:

1.  Modificar HTML.
2.  Modificar JavaScript.
3.  Hacer commit.
4.  Hacer push.
5.  Esperar al pipeline.
6.  Comprobar el resultado publicado.

------------------------------------------------------------------------

# 34. Ejercicio 4 --- Provocar un error

Este ejercicio es especialmente importante.

Los alumnos deberán introducir deliberadamente un error.

Por ejemplo, modificar:

``` javascript
const button = document.getElementById("button");
```

por:

``` javascript
const button = document.getElementById("boton-inexistente");
```

La aplicación puede dejar de comportarse correctamente.

Aquí plantearemos una cuestión:

> ¿El pipeline ha detectado el problema?

En una web estática sencilla, probablemente **no**, porque todavía no
tenemos una prueba automática que compruebe este comportamiento.

Y esta observación es fundamental:

> El hecho de tener CI/CD no significa que automáticamente sepamos si
> todo funciona.

------------------------------------------------------------------------

# 35. Introducción de tests

Para demostrar realmente el valor de CI, añadiremos una prueba.

Podemos crear una pequeña validación automatizada que compruebe que
determinados elementos existen en la aplicación.

Por ejemplo, podemos utilizar Node.js y una herramienta de testing.

El objetivo didáctico no es aprender Jest en profundidad, sino entender:

``` text
Código
 ↓
Test
 ↓
¿Pasa?
 ├── NO → pipeline detenido
 └── SÍ → continúa
```

------------------------------------------------------------------------

# 36. Pipeline con tests

Conceptualmente tendremos:

``` text
           git push
               ↓
        ┌──────────────┐
        │ Checkout     │
        └──────┬───────┘
               ↓
        ┌──────────────┐
        │ Instalar     │
        │ dependencias │
        └──────┬───────┘
               ↓
        ┌──────────────┐
        │ Tests        │
        └──────┬───────┘
               ↓
          ¿Correcto?
          /        \
        NO          SÍ
        ↓            ↓
     ERROR          Build
                     ↓
                   Deploy
```

------------------------------------------------------------------------

# 37. Ejercicio 5 --- Pipeline roto

El profesor puede pedir a los alumnos:

> Modificad el proyecto para que una prueba falle.

Los alumnos harán un `push`.

Deberán observar:

``` text
❌ Test
```

y comprobar que el proceso posterior no se ejecuta.

La reflexión será:

> ¿Por qué es positivo que el despliegue no se produzca cuando las
> pruebas fallan?

Respuesta esperada:

Porque evita publicar automáticamente una versión que no cumple las
condiciones establecidas.

------------------------------------------------------------------------

# 38. Ejercicio 6 --- Recuperación

Una vez provocado el error, el alumno deberá:

1.  Identificar el fallo.
2.  Corregirlo.
3.  Hacer un nuevo commit.
4.  Hacer `push`.
5.  Comprobar que el pipeline vuelve a ejecutarse.
6.  Comprobar que termina correctamente.
7.  Comprobar que la aplicación vuelve a estar disponible correctamente.

------------------------------------------------------------------------

# 39. Actividad final

## Reto: "Mi aplicación CI/CD"

Cada alumno deberá crear una versión personalizada de la aplicación.

Debe contener:

### HTML

-   Un título.
-   Una descripción.
-   Una imagen.
-   Un botón.
-   Una sección de información.

### CSS

-   Diseño personalizado.
-   Colores.
-   Espaciado.
-   Adaptación básica a dispositivos móviles.

### JavaScript

Al menos una interacción.

### Git

Al menos:

``` text
5 commits
```

Cada commit debe representar un cambio coherente.

Ejemplo:

``` text
feat: añado estructura principal
style: personalizo la página
feat: añado interacción
fix: corrijo botón
docs: actualizo README
```

### CI/CD

La aplicación debe:

1.  Estar almacenada en GitHub.
2.  Tener un workflow.
3.  Ejecutarse automáticamente después de un `push`.
4.  Realizar las comprobaciones definidas.
5.  Desplegar automáticamente la aplicación.

------------------------------------------------------------------------

# 40. README obligatorio

Cada alumno deberá incluir un `README.md`.

Debe explicar:

``` text
# Mi aplicación CI/CD

## Descripción

Descripción de la aplicación.

## Tecnologías

- HTML
- CSS
- JavaScript
- Git
- GitHub Actions

## CI/CD

Explicación del pipeline.

## URL

Dirección de la aplicación publicada.
```

------------------------------------------------------------------------

# 41. Preguntas de comprensión

Al finalizar la práctica, el alumno debe responder:

1.  ¿Qué significa CI?
2.  ¿Qué significa CD?
3.  ¿Cuál es la diferencia entre Continuous Delivery y Continuous
    Deployment?
4.  ¿Qué es un pipeline?
5.  ¿Qué ocurre cuando realizamos un `push`?
6.  ¿Qué función tiene un test dentro del pipeline?
7.  ¿Por qué no interesa desplegar una aplicación si las pruebas fallan?
8.  ¿Qué ventajas tiene automatizar el despliegue?
9.  ¿Qué inconvenientes puede tener un despliegue automático?
10. ¿En qué momento se produce el despliegue en nuestra práctica?

------------------------------------------------------------------------

# 42. Actividad de reflexión

Plantear al alumnado el siguiente escenario:

> Una empresa tiene 20 desarrolladores trabajando en una aplicación web.
>
> Cada viernes una persona descarga manualmente el proyecto, copia los
> archivos al servidor y reinicia la aplicación.
>
> Un viernes se publica una versión que contiene un error y la
> aplicación deja de funcionar.

Preguntas:

1.  ¿Qué problemas existen?
2.  ¿Qué tareas automatizaríais?
3.  ¿Qué pruebas añadiríais?
4.  ¿En qué momento realizaríais el despliegue?
5.  ¿Permitiríais que todos los cambios fueran directamente a
    producción?
6.  ¿Qué mecanismos utilizaríais para evitar errores?

------------------------------------------------------------------------

# 43. Ampliación: entornos

Una vez entendido el concepto básico, podemos introducir:

``` text
Desarrollo
    ↓
    CI
    ↓
Staging / Preproducción
    ↓
Validación
    ↓
Producción
```

Explicar que en proyectos profesionales normalmente no se recomienda que
cualquier cambio vaya directamente a producción sin controles.

------------------------------------------------------------------------

# 44. Ampliación: Pull Requests

Un flujo más profesional puede ser:

``` text
main
 │
 ├───────────────┐
 │               │
 ↓               ↓
feature/login    feature/menu
 │               │
 ↓               ↓
Pull Request     Pull Request
 │               │
 └───────┬───────┘
         ↓
       Tests
         ↓
      Revisión
         ↓
       Merge
         ↓
        main
         ↓
       Deploy
```

Esto permite introducir otro concepto importante:

> El pipeline puede ejecutarse antes incluso de aceptar un cambio en la
> rama principal.

------------------------------------------------------------------------

# 45. CI en Pull Requests

Podemos configurar workflows para que se ejecuten cuando se crea o
actualiza un Pull Request.

Por ejemplo:

``` yaml
on:
  pull_request:
    branches:
      - main
```

El proceso podría ser:

``` text
Alumno
  ↓
Crea rama
  ↓
Modifica aplicación
  ↓
Pull Request
  ↓
Tests automáticos
  ↓
Revisión
  ↓
Merge
```

Esto representa un flujo de trabajo mucho más cercano al utilizado en
equipos profesionales.

------------------------------------------------------------------------

# 46. Buenas prácticas

Los alumnos deberían conocer las siguientes recomendaciones:

### Commits pequeños

Es preferible:

``` text
Añadir menú
```

que:

``` text
Cambios varios
```

### Automatizar las comprobaciones

Los tests deberían ejecutarse automáticamente.

### No almacenar secretos en el repositorio

Nunca se deben guardar contraseñas, tokens o claves privadas
directamente en el código.

### Revisar los logs

Cuando un pipeline falla, los logs permiten localizar el problema.

### Mantener pipelines sencillos

Un pipeline excesivamente complejo puede resultar difícil de mantener.

### Proteger producción

En proyectos reales pueden utilizarse revisiones, aprobaciones y
diferentes entornos.

------------------------------------------------------------------------

# 47. Errores habituales de los alumnos

## "He hecho push pero no se actualiza la página"

Comprobar:

1.  Que el `push` se ha realizado.
2.  Que se ha modificado la rama correcta.
3.  Que el workflow se ha ejecutado.
4.  Que el workflow ha terminado correctamente.
5.  Que GitHub Pages está correctamente configurado.
6.  Que el navegador no está mostrando una versión almacenada en caché.

## "El workflow aparece en rojo"

Ir a:

``` text
Actions
→ Workflow
→ Job
→ Step que ha fallado
```

Leer el mensaje de error.

No intentar solucionar el problema sin leer previamente el log.

## "Mi código funciona localmente pero falla en CI"

Posibles causas:

-   Dependencias no instaladas.
-   Diferencias de versiones.
-   Archivos que no se han incluido.
-   Rutas incorrectas.
-   Diferencias entre sistemas operativos.
-   Variables de entorno.
-   Dependencias que solamente existen en el ordenador local.

Esta situación permite explicar uno de los valores principales de CI:

> El código debe funcionar en un entorno reproducible, no únicamente en
> el ordenador del desarrollador.

------------------------------------------------------------------------

# 48. Evaluación

Se propone la siguiente rúbrica:

  Criterio                          Peso
  ------------------------------- ------
  Comprensión de CI/CD              20 %
  Uso correcto de Git               15 %
  Aplicación web                    20 %
  Workflow de CI/CD                 20 %
  Automatización del despliegue     15 %
  Documentación                     10 %

------------------------------------------------------------------------

# 49. Rúbrica detallada

## Excelente

El alumno:

-   explica correctamente CI/CD;
-   diferencia CI, Delivery y Deployment;
-   utiliza Git correctamente;
-   mantiene commits coherentes;
-   configura correctamente el workflow;
-   interpreta los logs;
-   consigue el despliegue automático;
-   documenta adecuadamente el proceso.

## Adecuado

El alumno:

-   comprende los conceptos fundamentales;
-   utiliza Git correctamente con algún error puntual;
-   consigue ejecutar el pipeline;
-   consigue publicar la aplicación;
-   documenta suficientemente el trabajo.

## En proceso

El alumno:

-   confunde algunos conceptos;
-   necesita ayuda para utilizar Git;
-   tiene dificultades interpretando el pipeline;
-   no consigue completar alguna parte del despliegue.

## No alcanzado

El alumno:

-   no diferencia CI y CD;
-   no comprende el funcionamiento del pipeline;
-   no consigue realizar el flujo básico Git → CI → Deploy;
-   no documenta el trabajo.

------------------------------------------------------------------------

# 50. Temporalización propuesta

## Sesión 1 --- Introducción

**60--90 minutos**

Contenidos:

-   Problemas del despliegue manual.
-   Integración continua.
-   Continuous Delivery.
-   Continuous Deployment.
-   Pipeline.
-   DevOps.

Actividad:

> Diseñar en la pizarra un proceso de despliegue manual y transformarlo
> en uno automatizado.

## Sesión 2 --- GitHub Actions

**60--90 minutos**

Contenidos:

-   GitHub Actions.
-   Workflows.
-   Jobs.
-   Steps.
-   Eventos.
-   YAML.

Actividad:

> Analizar un workflow existente.

## Sesión 3 --- Primera aplicación

**90 minutos**

Los alumnos:

1.  Crean repositorio.
2.  Suben la aplicación.
3.  Configuran GitHub Pages.
4.  Comprueban la publicación.

## Sesión 4 --- Despliegue automático

**90 minutos**

Los alumnos:

1.  Crean el workflow.
2.  Hacen `push`.
3.  Observan Actions.
4.  Modifican la web.
5.  Comprueban el despliegue.

## Sesión 5 --- Tests y errores

**60--90 minutos**

Los alumnos:

1.  Ejecutan comprobaciones.
2.  Provocan un error.
3.  Analizan los logs.
4.  Corrigen el problema.
5.  Vuelven a desplegar.

## Sesión 6 --- Reto final

**90--120 minutos**

Cada alumno crea su versión de la aplicación.

------------------------------------------------------------------------

# 51. Guion del profesor

Una explicación especialmente efectiva puede seguir este orden.

### Pregunta inicial

> "¿Qué ocurre cuando modificáis una web que está publicada?"

Dejar que los alumnos expliquen su experiencia.

Después:

> "¿Y si tenéis que hacer esto 50 veces al día?"

Introducir la automatización.

Después:

> "¿Y si además queremos comprobar automáticamente que el cambio no
> rompe nada?"

Introducir CI.

Finalmente:

> "¿Y si queremos que, si todo está correcto, la web se publique sola?"

Introducir CD.

La secuencia conceptual queda:

``` text
¿Podemos integrar automáticamente?
             ↓
             CI
             ↓
¿Podemos preparar automáticamente?
             ↓
      Continuous Delivery
             ↓
¿Podemos desplegar automáticamente?
             ↓
     Continuous Deployment
```

------------------------------------------------------------------------

# 52. Esquema final para la pizarra

Al terminar la unidad, debería quedar algo parecido a esto:

``` text
                    GIT
                     │
                     │ push
                     ▼
              ┌──────────────┐
              │ CI/CD         │
              │ Pipeline      │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │   Checkout   │
              └──────┬───────┘
                     ▼
              ┌──────────────┐
              │    Tests     │
              └──────┬───────┘
                     │
                ¿Todo OK?
                 /       \
               NO         SÍ
               ↓           ↓
            ❌ Error      Build
                           │
                           ▼
                        Deploy
                           │
                           ▼
                     🌍 PRODUCCIÓN
```

------------------------------------------------------------------------

# 53. Idea fundamental que debe recordar el alumno

La unidad puede terminar con esta definición:

> **CI/CD es un conjunto de prácticas y automatizaciones que permiten
> integrar, comprobar, preparar y desplegar cambios de software de forma
> rápida, repetible y controlada.**

Y, sobre todo:

``` text
El desarrollador
      ↓
   modifica
      ↓
    commit
      ↓
     push
      ↓
  AUTOMATIZACIÓN
      ↓
   comprobar
      ↓
   construir
      ↓
   desplegar
      ↓
   PRODUCCIÓN
```

El objetivo no es simplemente "subir una web automáticamente".

El objetivo es conseguir que **el proceso que lleva desde un cambio en
el código hasta una versión publicada sea reproducible, verificable y
automatizado**.

------------------------------------------------------------------------

# 54. Entrega final del alumno

El alumno deberá entregar:

``` text
Repositorio GitHub
│
├── index.html
├── style.css
├── script.js
├── README.md
│
└── .github/
    └── workflows/
        └── deploy.yml
```

Además deberá proporcionar:

1.  URL del repositorio.
2.  URL de la aplicación publicada.
3.  Explicación breve del pipeline.
4.  Captura o evidencia de una ejecución correcta.
5.  Captura o evidencia de una ejecución fallida.
6.  Explicación de cómo solucionó el error.

------------------------------------------------------------------------

# 55. Checklist del alumno

Antes de entregar, comprobar:

-   [ ] Tengo un repositorio GitHub.
-   [ ] La aplicación funciona.
-   [ ] He realizado varios commits.
-   [ ] Tengo un workflow.
-   [ ] El workflow se ejecuta automáticamente.
-   [ ] Sé interpretar el resultado de Actions.
-   [ ] He realizado una modificación de la web.
-   [ ] He hecho `push`.
-   [ ] La modificación se ha desplegado automáticamente.
-   [ ] He provocado y solucionado un error.
-   [ ] La aplicación está publicada.
-   [ ] Tengo un README.
-   [ ] He incluido las URLs solicitadas.

------------------------------------------------------------------------

# 56. Pregunta final de examen

Una posible pregunta para evaluar la comprensión sería:

> Un desarrollador modifica una aplicación web y realiza un `push` al
> repositorio. Automáticamente se descargan las dependencias, se
> ejecutan las pruebas y se construye la aplicación. Si todas las
> pruebas son correctas, la aplicación se despliega automáticamente en
> producción.
>
> Explica qué conceptos de CI/CD aparecen en este proceso y describe el
> flujo completo.

### Respuesta esperada

El proceso utiliza integración continua porque los cambios se integran
en el repositorio y se comprueban automáticamente. También utiliza
despliegue continuo porque, si las comprobaciones son correctas, la
aplicación se publica automáticamente sin intervención manual.

El flujo sería:

``` text
push
 ↓
checkout
 ↓
instalación de dependencias
 ↓
tests
 ↓
build
 ↓
deploy
 ↓
producción
```

Si las pruebas fallan, el proceso debería detenerse y no realizar el
despliegue.

------------------------------------------------------------------------

# 57. Extensiones para cursos posteriores

Una vez dominado este ejercicio, se puede ampliar progresivamente hacia:

``` text
Git
 ↓
GitHub
 ↓
GitHub Actions
 ↓
Tests
 ↓
Build
 ↓
Docker
 ↓
Registro de imágenes
 ↓
Staging
 ↓
Producción
 ↓
Monitorización
```

También se pueden introducir:

-   Docker.
-   Docker Compose.
-   Variables de entorno.
-   Secrets.
-   Pull Requests.
-   Branch protection.
-   Code review.
-   SonarQube.
-   Tests unitarios.
-   Tests de integración.
-   Tests end-to-end.
-   Rollback.
-   Versionado.
-   Releases.
-   Infraestructura como código.
-   Kubernetes.

De esta manera, la práctica inicial puede convertirse en la primera
pieza de un itinerario de **DevOps** mucho más amplio.
