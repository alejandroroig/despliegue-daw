# 🔄 Tema 5: Integración y despliegue continuos

> **RA6**: Elabora la documentación de la aplicación web evaluando y seleccionando herramientas de generación de documentación, control de versiones y de integración continua.

---

## 🎯 Criterios de evaluación

Los criterios de documentación y control de versiones —a), b), c), d), e) y g)— se acreditaron en el Tema 1. Aquí se completa el resultado de aprendizaje con la automatización:

✅ **h)** Se han utilizado herramientas para la integración continua del código.

✅ **f)** Se ha garantizado la accesibilidad y seguridad de la información y código almacenada por el sistema de control de versiones.

El criterio f) se abrió en el Tema 1 con los secretos fuera del repositorio y se completa aquí en su vertiente de automatización: protección de ramas con comprobaciones exigidas, credenciales temporales del pipeline, secretos por entorno y permisos mínimos de los workflows.

!!! info "Sobre el peso de este tema"
    El RA6 dedica siete criterios a la documentación y el control de versiones y uno solo a la integración continua. Este tema ocupa dos sesiones y es el más exigente del resultado de aprendizaje, así que su peso en la calificación se pondera por sesiones y no por número de criterios.

Los contenidos básicos del módulo incluyen además la **monitorización continua de las métricas de calidad de la aplicación**, que en este tema se concreta en el umbral de cobertura que bloquea la integración.

---

## 📘 Índice de contenidos

1. [Integración continua](integracion-continua.md)
2. [Despliegue continuo](despliegue-continuo.md)

**Actividades:**

- [Actividad 5.1 — La puerta de entrada a `main`](actividad_5_1.md)
- [Actividad 5.2 — Del commit a producción, y vuelta](actividad_5_2.md)

Con la Actividad 5.2 se cierra la unidad 5 y el RA6 completo.

---

!!! note "Cómo encaja este tema"
    Recupera el repositorio que montaste en septiembre y le da su función definitiva. Todo lo que allí parecía ceremonia —la rama, la pull request, la etiqueta, el secreto fuera del código— resulta ser el mecanismo con el que una máquina construye, comprueba y despliega Escaparate sin que nadie toque el servidor. Y de paso deja una lección que ninguna herramienta enseña sola: una comprobación automática vale exactamente lo que hayas puesto dentro, así que poder volver atrás forma parte del despliegue y no del plan B.