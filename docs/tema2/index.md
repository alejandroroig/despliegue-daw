# 📦 Tema 2: Virtualización y contenedores

> **RA1**: Implanta arquitecturas web analizando y aplicando criterios de funcionalidad.

---

## 🎯 Criterios de evaluación

Los criterios de análisis de arquitecturas y de estructura de la aplicación se acreditaron en el Tema 1. Aquí se completa el resultado de aprendizaje con la parte práctica:

✅ Se ha realizado la instalación y configuración básica de tecnologías de virtualización de servidores en la nube y en contenedores.

✅ Se han realizado pruebas de funcionamiento de las tecnologías de virtualización.

✅ Se han descrito los requerimientos del proceso de implantación de una aplicación web.

✅ Se han documentado los procesos de instalación y configuración realizados.

---

## 📘 Índice de contenidos

1. [Fundamentos de contenedores](fundamentos-contenedores.md)
2. [Imágenes de contenedores](imagenes-contenedores.md)
3. [Docker Compose](docker-compose.md)

**Actividades:**

- [Actividad 2.1 — Ejecutar, inspeccionar y publicar](actividad_2_1.md)
- [Actividad 2.2 — Empaquetar Escaparate](actividad_2_2.md)
- [Actividad 2.3 — El conjunto entero con un comando](actividad_2_3.md)

---

!!! note "Cómo encaja este tema"
    Tres sesiones para convertir Escaparate en una unidad de despliegue reproducible. Al terminar tienes la aplicación empaquetada en una imagen propia, publicada en un registro y con el stack de esta etapa —Spring Boot con frontend, API y Tomcat embebido, más PostgreSQL— levantándose con un solo comando y documentado en tu repositorio.

    Este tema prepara directamente el bloque conjunto de **servidores web y servidores de aplicaciones**: primero empaquetas la pieza que ejecuta la aplicación; después colocarás Nginx delante como servidor web, proxy y puerta de entrada. No sustituirás el Tomcat embebido: ambas responsabilidades cooperarán dentro del mismo despliegue.