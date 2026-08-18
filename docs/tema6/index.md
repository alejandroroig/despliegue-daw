# ☸️ Tema 6: Orquestación de contenedores

> **RA1**, con evidencia complementaria de **RA2** y **RA3**.
> Sesiones **S14, S15 y S16** — viernes 8, 15 y 22 de enero.

---

## 🎯 Criterios de evaluación

Este tema no abre criterios nuevos: **cierra la mitad pendiente de tres que ya conocías**. El Tema 2 los acreditó sobre contenedores en una máquina; el enunciado oficial dice «en la nube **y** en contenedores», y esa segunda mitad se cubre aquí, cuando el mismo catálogo pasa a ejecutarse sobre un orquestador propio y después sobre uno gestionado.

**Del RA1 — Implanta arquitecturas web analizando y aplicando criterios de funcionalidad:**

✅ e) Se ha realizado la instalación y configuración básica de tecnologías de virtualización de servidores en la nube y en contenedores.

✅ f) Se han realizado pruebas de funcionamiento de los servidores web y de aplicaciones. y de tecnologías de virtualización en la nube y en contenedores.

✅ i) Se han documentado los procesos de instalación y configuración realizados sobre los servidores web, de aplicaciones. y sobre tecnologías de virtualización en la nube y en contenedores.

**Del RA2 — Implanta aplicaciones web en servidores web, evaluando y aplicando criterios de configuración para su funcionamiento seguro:**

✅ i) Se han utilizado tecnologías de virtualización en el despliegue de servidores web en la nube y en contenedores.

**Del RA3 — Implanta aplicaciones web en servidores de aplicaciones, evaluando y aplicando criterios de configuración para su funcionamiento seguro:**

✅ i) Se han utilizado tecnologías de virtualización en el despliegue de servidores de aplicaciones en la nube y en contenedores.

---

## 📘 Índice de contenidos

1. [Arquitectura de Kubernetes: el estado deseado](kubernetes-arquitectura.md) — S14
2. [Configuración, sondas y actualizaciones progresivas](kubernetes-configuracion.md) — S15
3. [Clúster gestionado y GitOps](eks-gitops.md) — S16

**Actividades:**

- [Actividad 6.1 — El catálogo, declarado](actividad_6_1.md)
- [Actividad 6.2 — Publicar, vigilar y actualizar sin cortar](actividad_6_2.md)
- [Actividad 6.3 — El repositorio manda](actividad_6_3.md)

---

## 🧭 El hilo del tema

Once sesiones montando a mano un servicio que funciona. Este tema no añade una pieza más al montaje: **pregunta quién lo mantiene cuando tú no estás delante**.

La **S14** responde con el modelo declarativo —tú describes el estado deseado y algo se ocupa de que la realidad se le parezca— y traduce tu propio `compose.yaml` de septiembre a los objetos equivalentes, línea a línea. Termina con la pregunta incómoda: en qué casos todo esto es sobreingeniería y el Compose de diciembre era la respuesta correcta.

La **S15** declara tres cosas que ya sabías hacer a mano: la configuración fuera de la imagen, el proxy inverso y la salud del servicio. Y con ellas paga la deuda que quedó abierta el 18 de diciembre: una actualización progresiva de verdad, medida con tráfico encima. Después enseña dónde termina esa protección, desplegando la versión rota que ya conoces y comprobando que pasa igualmente.

La **S16** cambia dos cosas a la vez. El plano de control deja de ser tuyo y empieza a facturar por horas. Y el despliegue cambia de sentido: en vez de un pipeline que empuja hacia el servidor con una llave —con todo lo que eso implicaba, como descubriste en la actividad 5.2—, un agente dentro del clúster tira del repositorio y ninguna credencial vive fuera.

!!! note "Cómo encaja en el módulo"
    Es el cierre del temario. El mismo catálogo, la misma imagen, se habrá ejecutado de cuatro maneras a lo largo del curso: contenedores sobre una instancia que administras, contenedor como servicio gestionado (INU), clúster propio y clúster gestionado con despliegue por extracción. La semana del 22 de enero coincide con el **HITO C**, de entrega conjunta con INU, y a partir de ahí empiezan las tres semanas del proyecto integrador. La pregunta que se defiende en febrero es una sola: **a este cliente, ¿cuál de las cuatro le vendes, y qué le dices que va a perder?**

!!! danger "Norma de aula para la S16"
    El plano de control de un clúster gestionado **factura aunque el laboratorio esté cerrado**. Destruirlo antes de salir del aula no es una recomendación: forma parte de la entrega, y un clúster vivo al terminar invalida la práctica.