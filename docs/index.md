# 🚀 Despliegue de Aplicaciones Web — 0614

Módulo de **Desarrollo de Aplicaciones Web (DAW)**

!!! info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/presentacion_modulo_v2.pdf){target="_blank" rel="noopener"}

Escribir una aplicación es la mitad del trabajo. La otra mitad es conseguir que funcione fuera de tu ordenador: en un servidor, con su base de datos, servida por HTTPS, sin caerse cuando llega gente y sin que actualizar una versión sea una noche en vela. De eso va este módulo.

---

## 🎯 Qué vas a aprender

- 📦 Empaquetar una aplicación en contenedores y levantar el stack completo con un solo comando.
- 🌐 Publicar aplicaciones con un servidor web: sitios virtuales, proxy inverso, balanceo y HTTPS con certificados reales.
- ☕ Entender qué ejecuta una aplicación Java, comparar servidor embebido y externo y resolver el estado cuando existen varias réplicas.
- 🔭 Observar y diagnosticar el sistema mediante logs centralizados.
- 📈 Medir rendimiento con throughput, percentiles y errores.
- 🔁 Automatizar integración y despliegue, incluyendo una estrategia de vuelta atrás.
- ☸️ Orquestar contenedores con Kubernetes y declarar el estado deseado desde un repositorio.

---

## 📘 Temas del módulo

| Tema | Qué cubre | RA |
|------|-----------|-----|
| 🏁 [Tema 1 — Punto de partida](tema1/index.md) | Arquitecturas, proceso de despliegue, control de versiones y documentación | RA1 · RA6 |
| 📦 <!-- [Tema 2 — Virtualización y contenedores](tema2/index.md) -->Tema 2 — Virtualización y contenedores | Contenedores, imágenes multietapa y Docker Compose | RA1 |
| 🌐 <!-- [Tema 3 — Publicación y ejecución de aplicaciones web](tema3/index.md) -->Tema 3 - Publicación y ejecución de aplicaciones web | Nginx, proxy, HTTPS, observabilidad, Tomcat, estado compartido y rendimiento | RA2 · RA3 |
| 🔁 <!-- [Tema 4 — Integración y despliegue continuos](tema4/index.md) -->Tema 4 — Integración y despliegue continuos | Integración continua, despliegue automatizado y vuelta atrás | RA6 |
| ☸️ <!-- [Tema 5 — Orquestación de contenedores](tema5/index.md) -->Tema 5 — Orquestación de contenedores | Kubernetes, actualizaciones progresivas, clúster gestionado y GitOps | RA1 · RA2 · RA3 |

---

## 🛍️ El proyecto: Escaparate

Todo lo que ves aquí se practica sobre la misma aplicación: **Escaparate**, un catálogo de productos con imágenes. No la programas tú —te la damos hecha—: lo que construyes es todo lo que hay alrededor para que funcione en condiciones reales.

La vas a desplegar de cuatro formas distintas:

| Forma | Cuándo | Qué añade |
|---|---|---|
| Stack local con Compose | Tema 2 | Aplicación y dependencias reproducibles |
| Servidor con Nginx, HTTPS y varias réplicas | Tema 3 | Publicación, balanceo, observabilidad y estado compartido |
| Despliegue automático por pipeline | Tema 4 | Del cambio integrado al despliegue y vuelta atrás |
| Orquestado con Kubernetes y GitOps | Tema 5 | Estado deseado, réplicas y actualizaciones progresivas |

En la defensa final presentas las cuatro y respondes a la pregunta que resume el módulo: *dado este cliente concreto, ¿cuál le venderías y por qué?*

---

!!! tip "Cómo navegar"
    Haz clic en cualquier tema para empezar. Dentro de cada uno están los apuntes y las actividades en orden.

!!! info "Cómo son las actividades"
    Cada actividad tiene un enunciado con pasos numerados y está dimensionada para una sesión de trabajo. La sección **Verificación** recoge las comprobaciones que permiten dar la práctica por válida y **Qué se entrega** sirve como referencia de corrección.

!!! note "Transferencia de archivos y servicios de red (RA4 y RA5)"
    Estos resultados se acreditan principalmente durante la **Formación en Empresa**. En el aula aparecen DNS y herramientas como `dig` cuando son necesarias para publicar y diagnosticar aplicaciones.
