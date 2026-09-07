# 🚀 Despliegue de Aplicaciones Web — 0614

Módulo de **Desarrollo de Aplicaciones Web (DAW)**

Escribir una aplicación es la mitad del trabajo. La otra mitad es conseguir que funcione fuera de tu ordenador: en un servidor, con su base de datos, servida por HTTPS, sin caerse cuando llega gente y sin que actualizar una versión sea una noche en vela. De eso va este módulo.

---

## 🎯 Qué vas a aprender

- 📦 Empaquetar una aplicación en contenedores y levantar el stack completo con un solo comando.
- 🌐 Publicar aplicaciones con un servidor web: sitios virtuales, proxy inverso, balanceo y HTTPS con certificados reales.
- ☕ Entender qué ejecuta realmente una aplicación Java, comparar servidor embebido y externo y resolver el estado cuando existen varias réplicas.
- 🔭 Ver qué está pasando por dentro mediante logs centralizados y datos que permitan diagnosticar incidencias.
- 📈 Medir rendimiento con latencia, percentiles, throughput y errores, evitando conclusiones basadas solo en medias.
- 🔁 Automatizar el camino desde la integración del código hasta el despliegue, incluyendo una estrategia de vuelta atrás.
- ☸️ Orquestar contenedores con Kubernetes y desplegar declarando el estado deseado en un repositorio.

---

## 📘 Temas del módulo

| Tema | Qué cubre | RA |
|------|-----------|-----|
| 🏁 [Tema 1 — Punto de partida](tema1/index.md) | Arquitecturas y proceso de despliegue, control de versiones y documentación aplicados al despliegue | RA1 · RA6 |
| 📦 [Tema 2 — Virtualización y contenedores](tema2/index.md) | Fundamentos de contenedores, imágenes multietapa y Docker Compose | RA1 |
| 🌐 [Tema 3 — Publicación y ejecución de aplicaciones web](tema3/index.md) | Nginx, sitios virtuales, proxy y balanceo, HTTPS, observabilidad, Tomcat, estado compartido y rendimiento | RA2 · RA3 |
| 🔁 [Tema 4 — Integración y despliegue continuos](tema5/index.md) | Integración continua, publicación de artefactos e imágenes, despliegue automatizado y vuelta atrás | RA6 |
| ☸️ [Tema 5 — Orquestación de contenedores](tema6/index.md) | Kubernetes, actualizaciones sin caída, clúster gestionado y GitOps | RA1 · RA2 · RA3 |

!!! info "Numeración y carpetas"
    La estructura didáctica ya utiliza **cinco temas**. De forma transitoria, el Tema 4 y el Tema 5 continúan almacenados en las carpetas `tema5/` y `tema6/` para no romper enlaces mientras esos bloques no se hayan revisado. Cuando lleguemos a ellos se renombrarán también las rutas físicas.

---

## 🛍️ El proyecto: Escaparate

Todo lo que ves aquí se practica sobre la misma aplicación: **Escaparate**, un catálogo de productos con imágenes. No la programas tú —te la damos hecha—: lo que construyes es todo lo que hay alrededor para que funcione en condiciones reales.

La vas a desplegar de cuatro formas distintas a lo largo del curso, cada una añadiendo una responsabilidad nueva:

| Forma | Cuándo | Qué añade |
|---|---|---|
| Stack local con Compose | Tema 2 | Aplicación y dependencias reproducibles en cualquier equipo |
| Servidor con Nginx, HTTPS y varias réplicas | Tema 3 | Publicación real, balanceo, observabilidad, estado compartido y análisis de rendimiento |
| Despliegue automático por pipeline | Tema 4 | Del cambio integrado a una versión desplegada con comprobaciones y vuelta atrás |
| Orquestado con Kubernetes y GitOps | Tema 5 | Estado deseado, réplicas, actualizaciones progresivas y el repositorio como fuente de verdad |

En la defensa final presentas las cuatro y respondes a la pregunta que resume el módulo: *dado este cliente concreto, ¿cuál le venderías y por qué?*

---

!!! tip "Cómo navegar"
    Haz clic en cualquier tema para empezar. Dentro de cada uno están los apuntes y las actividades en orden. Puedes usar las **flechas al pie de cada página** para avanzar o retroceder.

!!! info "Cómo son las actividades"
    Cada actividad tiene un enunciado con pasos numerados y está dimensionada para una sesión de trabajo. Al final encontrarás una sección **Verificación**, con las comprobaciones que permiten dar la práctica por válida, y la lista **Qué se entrega**, que sirve como referencia de corrección.

    Algunas actividades pueden terminar con un apartado **«Si te sobra tiempo»**. Es opcional, no puntúa y ninguna sesión posterior depende de él.

!!! note "Transferencia de archivos y servicios de red (RA4 y RA5)"
    Estos dos resultados de aprendizaje —servidores FTP/SFTP y servicios de nombres y directorio— se acreditan principalmente durante la **Formación en Empresa**. En el aula sí aparecen DNS y herramientas como `dig` cuando son necesarias para publicar sitios, diagnosticar resolución de nombres u obtener certificados.
