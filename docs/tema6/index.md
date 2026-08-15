# ☸️ Tema 6: Orquestación de contenedores

> **RA1** (virtualización en contenedores), con evidencia complementaria de **RA2** y **RA3**.

---

## 🎯 Criterios de evaluación

Este tema no acredita criterios nuevos: **consolida sobre una plataforma de orquestación** criterios ya trabajados en los temas 2, 3 y 4, y aporta la evidencia final de que el alumnado sabe aplicarlos en un entorno distinto del inicial.

✅ Se ha realizado la instalación y configuración básica de tecnologías de virtualización en contenedores.

✅ Se han realizado pruebas de funcionamiento de las tecnologías de virtualización.

✅ Se han realizado los ajustes necesarios para la implantación de aplicaciones.

✅ Se han utilizado tecnologías de virtualización en el despliegue de servidores web y de aplicaciones.

✅ Se han documentado los procesos realizados.

---

## 📘 Índice de contenidos

1. [Kubernetes: arquitectura y objetos](kubernetes-arquitectura.md)
2. [Configuración y actualizaciones sin caída](kubernetes-configuracion.md)
3. [Clúster gestionado y GitOps](eks-gitops.md)

**Actividades:**

- [Actividad 6.1 — De Compose a manifiestos](actividad_6_1.md)
- [Actividad 6.2 — Actualizar sin cortar el servicio](actividad_6_2.md)
- [Actividad 6.3 — El repositorio manda](actividad_6_3.md)

---

!!! note "Cómo encaja este tema"
    El cierre del módulo. Todo lo que has hecho a mano en los temas anteriores —levantar réplicas, repartir tráfico, actualizar sin cortar— aquí se declara en un fichero y lo ejecuta un orquestador. Y en la última sesión se invierte la dirección del despliegue: en lugar de que el pipeline empuje hacia el servidor, es el clúster el que va a buscar al repositorio lo que debe estar corriendo.