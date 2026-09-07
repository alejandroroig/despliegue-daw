# 🏁 Cierre del módulo

Has recorrido el camino completo: desde una aplicación que solo funcionaba en tu ordenador hasta cuatro formas distintas de desplegarla y operarla.

## Lo que sabes hacer ahora

- Leer un despliegue ajeno desde fuera y deducir qué hay detrás con evidencias, no con intuiciones.
- Usar un repositorio como herramienta de despliegue: ramas de vida corta, revisión antes de fusionar, versiones etiquetadas y ningún secreto dentro.
- Documentar un despliegue de forma que otra persona pueda reproducirlo sin preguntarte cada paso.
- Empaquetar una aplicación en una imagen reproducible y levantar su stack completo con Docker Compose.
- Configurar Nginx con sitios virtuales, proxy inverso y balanceo entre varias réplicas.
- Publicar una aplicación mediante HTTPS con certificados reales y aplicar controles básicos de acceso y endurecimiento.
- Centralizar logs y utilizarlos para reconstruir qué ha ocurrido durante una incidencia.
- Distinguir el servidor web del proceso que ejecuta la aplicación, desplegar un WAR sobre Tomcat y entender las diferencias entre servidor embebido y externo.
- Detectar por qué el estado guardado dentro de una réplica falla al balancear y externalizar una sesión para que las copias sean intercambiables.
- Comparar rendimiento mediante throughput, errores y percentiles, entendiendo que aumentar procesos no equivale necesariamente a añadir recursos.
- Automatizar comprobaciones y despliegues mediante un pipeline y disponer de una estrategia de vuelta atrás.
- Declarar el estado deseado de una aplicación y dejar que un orquestador se encargue de mantenerlo.

## Una cosa que conviene que mires

Abre el historial de `daw-despliegue` y recórrelo de abajo arriba. La primera etiqueta que pusiste, `v0.1.0`, marcaba un repositorio con un `README` y poco más. La última representa una arquitectura mucho más completa.

Entre una y otra no hay un salto mágico: hay dieciséis sesiones en las que cada pieza aparece cuando existe un problema que la hace necesaria. Ese historial es probablemente la mejor descripción de lo que has aprendido.

## La pregunta final

En la defensa presentas Escaparate desplegado de cuatro maneras y respondes a una pregunta que resume todo el módulo:

> *Dado este cliente concreto, ¿qué forma de despliegue elegirías y por qué?*

La respuesta debe considerar, al menos:

- coste y recursos necesarios;
- esfuerzo de mantenimiento;
- automatización disponible;
- tolerancia a fallos;
- facilidad para crecer si aumenta el tráfico.

No hay una única respuesta correcta. Hay decisiones justificadas y decisiones que no lo están.

## Y a partir de aquí

Lo que has trabajado conecta directamente con ámbitos como **administración de sistemas, DevOps, SRE y platform engineering**. Guarda tu repositorio: además de contener las prácticas, muestra cómo ha evolucionado una arquitectura y qué decisiones has sido capaz de justificar.

!!! tip "Lo que queda por descubrir"
    Service mesh, políticas de seguridad de clúster, despliegues canary guiados por métricas, infraestructura como código más avanzada, arquitecturas multi-región... No cabía todo en el módulo, pero ya tienes la base para entender por qué existen esas herramientas cuando te las encuentres.
