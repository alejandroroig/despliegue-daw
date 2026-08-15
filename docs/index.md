# 🚀 Despliegue de Aplicaciones Web — 0614

Módulo de **Desarrollo de Aplicaciones Web (DAW)**

Escribir una aplicación es la mitad del trabajo. La otra mitad es conseguir que funcione fuera de tu portátil: en un servidor, con su base de datos, servida por HTTPS, sin caerse cuando llega gente y sin que actualizar una versión sea una noche en vela. De eso va este módulo.

---

## 🎯 Qué vas a aprender

- 📦 Empaquetar una aplicación en contenedores y levantar el stack completo con un solo comando.
- 🌐 Administrar un servidor web: sitios virtuales, módulos, proxy inverso, balanceo de carga y HTTPS con certificados reales.
- ☕ Desplegar sobre un servidor de aplicaciones y medir su rendimiento bajo carga.
- 🔭 Ver qué está pasando por dentro: logs centralizados, métricas y alertas que avisan antes que el usuario.
- 🔁 Automatizar el camino del `git push` a producción, con vuelta atrás cuando algo sale mal.
- ☸️ Orquestar contenedores con Kubernetes y desplegar declarando el estado deseado en un repositorio.

---

## 📘 Temas del módulo

| Tema | Qué cubre | RA |
|------|-----------|-----|
| 🏁 [Tema 1 — Punto de partida](tema1/index.md) | Arquitecturas y proceso de despliegue, control de versiones y documentación aplicados al despliegue | RA1 · RA6 |
| 📦 [Tema 2 — Virtualización y contenedores](tema2/index.md) | Fundamentos de contenedores, imágenes multietapa y Docker Compose | RA1 |
| 🌐 [Tema 3 — Administración de servidores web](tema3/index.md) | Sitios virtuales, DNS, proxy inverso y balanceo, HTTPS con ACME, observabilidad | RA2 |
| ☕ [Tema 4 — Servidores de aplicaciones](tema4/index.md) | Despliegue de artefactos, sesiones, pruebas de carga y rendimiento | RA3 |
| 🔁 [Tema 5 — Integración y despliegue continuos](tema5/index.md) | Pipelines de integración continua y despliegue continuo con vuelta atrás | RA6 |
| ☸️ [Tema 6 — Orquestación de contenedores](tema6/index.md) | Kubernetes, actualizaciones sin caída, clúster gestionado y GitOps | RA1 · RA2 · RA3 |

---

## 🛍️ El proyecto: Escaparate

Todo lo que ves aquí se practica sobre la misma aplicación: **Escaparate**, un catálogo de productos con imágenes. No la programas tú —te la damos hecha—: lo que construyes es todo lo que hay alrededor para que funcione en condiciones reales.

La vas a desplegar de cuatro formas distintas a lo largo del curso, cada una más profesional que la anterior:

| Forma | Cuándo | Qué añade |
|---|---|---|
| Stack local con Compose | Tema 2 | Todo junto, reproducible en cualquier equipo |
| Servidor con proxy y HTTPS | Tema 3 | Balanceo entre réplicas, certificado real, logs centralizados |
| Despliegue automático por pipeline | Tema 5 | Del `merge` a producción sin tocar el servidor |
| Orquestado con Kubernetes y GitOps | Tema 6 | El repositorio como única fuente de verdad |

En la defensa final presentas las cuatro y respondes a la pregunta que resume el módulo: *dado este cliente concreto, ¿cuál le venderías y por qué?*

---

!!! tip "Cómo navegar"
    Haz clic en cualquier tema para empezar. Dentro de cada uno están los apuntes y las actividades en orden. Puedes usar las **flechas al pie de cada página** para avanzar o retroceder.

!!! info "Actividades A y B"
    Cada actividad tiene una parte **A**, guiada, que debe terminar todo el mundo, y una parte **B**, un reto que va más allá y suma en la calificación. Si vas justo de tiempo, asegúrate primero de la A.

!!! note "Transferencia de archivos y servicios de red (RA4 y RA5)"
    Estos dos resultados de aprendizaje —servidores FTP/SFTP y servicios de nombres y directorio— no se trabajan en estos apuntes: se acreditan durante la **Formación en Empresa**.