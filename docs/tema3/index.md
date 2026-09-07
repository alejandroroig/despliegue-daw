# 🌐 Tema 3: Publicación y ejecución de aplicaciones web

> **RA2**: Implanta aplicaciones web en servidores web, evaluando y aplicando criterios de configuración para su funcionamiento seguro.
>
> **RA3**: Implanta aplicaciones web en servidores de aplicaciones, evaluando y aplicando criterios de configuración para su funcionamiento seguro.

---

## 🎯 Criterios de evaluación

### RA2 · Servidor web

✅ Se han reconocido los parámetros de administración más importantes del servidor web.

✅ Se ha ampliado la funcionalidad del servidor mediante la activación y configuración de módulos.

✅ Se han creado y configurado sitios virtuales.

✅ Se han configurado los mecanismos de autenticación y control de acceso del servidor.

✅ Se han obtenido e instalado certificados digitales.

✅ Se han establecido mecanismos para asegurar las comunicaciones entre el cliente y el servidor.

✅ Se ha elaborado documentación relativa a la configuración, administración segura y recomendaciones de uso del servidor.

✅ Se han realizado los ajustes necesarios para la implantación de aplicaciones en el servidor web.

✅ Se han utilizado tecnologías de virtualización en el despliegue de servidores web en la nube y en contenedores.

✅ Se han instalado, configurado y utilizado conjuntos de herramientas de gestión de logs, permitiendo su monitorización, consolidación y análisis.

### RA3 · Servidor de aplicaciones

En este tema se trabajan especialmente los criterios que encajan de forma natural con la arquitectura del proyecto:

✅ Se han descrito los componentes y el funcionamiento de los servicios proporcionados por el servidor de aplicaciones.

✅ Se han identificado los principales archivos de configuración y de bibliotecas compartidas.

✅ Se han configurado y utilizado componentes web del servidor de aplicaciones mediante el despliegue de una aplicación Java.

✅ Se han realizado los ajustes necesarios para el despliegue de aplicaciones sobre el servidor.

✅ Se han realizado pruebas de funcionamiento y rendimiento de la aplicación web desplegada.

✅ Se han utilizado tecnologías de virtualización en el despliegue de servidores de aplicaciones en la nube y en contenedores.

!!! info "Sobre el RA3"
    El objetivo no es recorrer de forma artificial todos sus criterios. El proyecto utiliza **Spring Boot con Tomcat embebido** como arquitectura de referencia y dedica un laboratorio breve al despliegue tradicional sobre Tomcat externo. Los criterios de descripción e identificación se trabajan principalmente en teoría; la práctica se concentra en despliegue, estado compartido y rendimiento.

---

## 📘 Índice de contenidos

1. [Servidores web y DNS](servidores-web-dns.md)
2. [Proxy inverso y balanceo](proxy-inverso-balanceo.md)
3. [Seguridad web y HTTPS](seguridad-web-https.md)
4. [Observabilidad: logs, métricas y alertas](observabilidad.md)
5. [Servidor de aplicaciones: ejecución, estado y rendimiento](servidor-aplicaciones-rendimiento.md)

**Actividades:**

- [Actividad 3.1 — Dos sitios, un servidor y dos nombres](actividad_3_1.md)
- [Actividad 3.2 — Una puerta, tres copias](actividad_3_2.md)
- [Actividad 3.3 — Cierra la puerta y echa la llave](actividad_3_3.md)
- [Actividad 3.4 — Que te lo cuente el sistema](actividad_3_4.md)
- [Actividad 3.5 — El backend por dentro](actividad_3_5.md)

---

!!! note "Cómo encaja este tema"
    Es el bloque donde construyes la arquitectura completa de publicación y ejecución de Escaparate. Las cinco sesiones siguen el recorrido desde la entrada pública hasta el proceso que ejecuta realmente la aplicación:

    ```text
    sesión 6  → Nginx recibe, sirve y selecciona sitios
    sesión 7  → Nginx reenvía y reparte entre tres réplicas
    sesión 8  → el despliegue llega a Internet y se protege con HTTPS
    sesión 9  → los registros permiten observar y diagnosticar
    sesión 10 → abrimos el backend: Tomcat, estado de sesión y rendimiento
    ```

    RA2 y RA3 aparecen así como **dos responsabilidades de un mismo sistema**:

    ```text
    Nginx
    → publicación y entrada

    Spring Boot + Tomcat
    → ejecución de la aplicación
    ```

!!! warning "El cierre del tema es una memoria conjunta"
    En la Actividad 3.4 comienzas `memoria-publicacion-ejecucion.md` con la arquitectura de publicación, seguridad y observabilidad. En la Actividad 3.5 la completas con la ejecución del backend, el estado de sesión y la prueba de rendimiento.

    No se busca una memoria extensa: debe servir para explicar las decisiones principales de la arquitectura y dejar constancia de las comprobaciones realizadas.
