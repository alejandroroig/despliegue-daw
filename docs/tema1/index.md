# 🧭 Tema 1: Punto de partida — la aplicación y su repositorio

> **RA1**: Implanta arquitecturas web analizando y aplicando criterios de funcionalidad.
>
> **RA6**: Elabora la documentación de la aplicación web evaluando y seleccionando herramientas de generación de documentación, control de versiones y de integración continua.

---

## 🎯 Criterios de evaluación

**Del RA1** — el resto se acredita en el Tema 2:

✅ Se han analizado aspectos generales de arquitecturas web, sus características, ventajas e inconvenientes.

✅ Se han descrito los fundamentos y protocolos en los que se basa el funcionamiento de un servidor web.

✅ Se ha analizado la estructura y recursos que componen una aplicación web.

✅ Se han descrito los requerimientos del proceso de implantación de una aplicación web.

**Del RA6** — el criterio de integración continua se acredita en el Tema 5:

✅ Se han identificado diferentes herramientas de generación de documentación.

✅ Se han documentado los componentes software utilizando los generadores específicos de las plataformas.

✅ Se han utilizado diferentes formatos para la documentación.

✅ Se han utilizado herramientas colaborativas para la elaboración y mantenimiento de la documentación.

✅ Se ha instalado, configurado y utilizado un sistema de control de versiones.

✅ Se ha garantizado la accesibilidad y seguridad de la información y del código almacenado por el sistema de control de versiones.

✅ Se ha documentado la instalación, configuración y uso del sistema de control de versiones utilizado.

---

## 📘 Índice de contenidos

1. [Arquitecturas y proceso de despliegue](arquitecturas-despliegue.md)
2. [Control de versiones y documentación](control-versiones-documentacion.md)

**Actividades:**

- [Actividad 1.1 — Qué se puede saber de un despliegue desde fuera](actividad_1_1.md)
- [Actividad 1.2 — Tu repositorio como herramienta de despliegue](actividad_1_2.md)

---

!!! note "Cómo encaja este tema"
    Dos sesiones para responder a dos preguntas previas a todo lo demás: qué piezas necesita una aplicación web para llegar a usuarios y dónde versionamos la información necesaria para reconstruir ese despliegue. Al terminar sabes leer un sistema desde fuera y tienes montado `daw-despliegue`, el repositorio que te acompañará durante el curso.

    Más adelante esas piezas aparecerán juntas en un mismo bloque de publicación: **el servidor web gestionará la entrada y el servidor de aplicaciones o runtime ejecutará la lógica dinámica**. Esta primera unidad solo introduce esa separación; la configuración práctica llegará en los temas posteriores.