# Despliegue de Aplicaciones Web (0614) — Apuntes y actividades

Sitio de apuntes y actividades del módulo **Despliegue de Aplicaciones Web** (0614, ciclo DAW), construido con [MkDocs Material](https://squidfunk.github.io/mkdocs-material/). Teoría y actividades giran en torno a un proyecto compartido, **Escaparate** (catálogo de productos con Spring Boot), que se despliega de formas cada vez más profesionales a lo largo del curso. El mismo proyecto se trabaja desde la optativa **Introducción a la Nube Pública**, allí en su versión gestionada.

## Puesta en marcha

```bash
pip install -r requirements.txt
mkdocs serve
```

Abre la URL que te indique la terminal (por defecto `http://127.0.0.1:8000`).

## Temario

- **Tema 1 — Arquitecturas web y virtualización**: anatomía de un despliegue, fundamentos de contenedores, imágenes multietapa, Docker Compose.
- **Tema 2 — Documentación, versiones e integración continua**: control de versiones aplicado al despliegue, documentación generada, pipelines de integración y despliegue continuo.
- **Tema 3 — Administración de servidores web**: sitios virtuales y nombres, proxy inverso y balanceo, HTTPS con ACME y endurecimiento, observabilidad.
- **Tema 4 — Servidores de aplicaciones**: despliegue de artefactos, sesiones distribuidas, pruebas de carga y rendimiento.
- **Tema 5 — Orquestación de contenedores**: Kubernetes, configuración y actualizaciones sin caída, clúster gestionado y GitOps con Argo CD.

RA4 (transferencia de archivos) y RA5 (servicios de red) se acreditan durante la Formación en Empresa.
