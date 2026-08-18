# ☕ Tema 4: Servidores de aplicaciones

> **RA3**: Implanta aplicaciones web en servidores de aplicaciones, evaluando y aplicando criterios de configuración para su funcionamiento seguro.

---

## 🎯 Criterios de evaluación

✅ Se han descrito los componentes y el funcionamiento de los servicios proporcionados por el servidor de aplicaciones.

✅ Se han identificado los principales archivos de configuración y de bibliotecas compartidas.

✅ Se ha configurado el servidor de aplicaciones para cooperar con el servidor web.

✅ Se han configurado y activado los mecanismos de seguridad del servidor de aplicaciones.

✅ Se han configurado y utilizado los componentes web del servidor de aplicaciones.

✅ Se han realizado los ajustes necesarios para el despliegue de aplicaciones sobre el servidor.

✅ Se han realizado pruebas de funcionamiento y rendimiento de la aplicación web desplegada.

✅ Se ha elaborado documentación relativa a la administración y recomendaciones de uso del servidor de aplicaciones.

✅ Se han utilizado tecnologías de virtualización en el despliegue de servidores de aplicaciones en la nube y en contenedores.

---

## 📘 Índice de contenidos

1. [Servidor de aplicaciones](servidor-aplicaciones.md)
2. [Rendimiento y pruebas de carga](rendimiento-pruebas-carga.md)

**Actividades:**

- [Actividad 4.1 — El mismo artefacto, dos servidores, y una sesión que deja de perderse](actividad_4_1.md)
- [Actividad 4.2 — Cuánta carga aguanta y qué se rinde primero](actividad_4_2.md)

---

!!! note "Cómo encaja este tema"
    Es el tema más corto del módulo —dos sesiones— y responde a dos preguntas que el Tema 3 dejó abiertas.

    La primera: llevas cuatro sesiones administrando Nginx y Nginx no ha ejecutado nunca una línea de Escaparate. Recibe peticiones, decide a dónde van y devuelve lo que le contesten, pero quien ejecuta la aplicación es otra pieza que todavía no tiene nombre en el curso. La sesión 10 se lo pone, y de paso salda una deuda anunciada en la primera sesión: esa sesión de usuario que desaparece al recargar cuando hay un proxy repartiendo entre varias copias.

    La segunda: nadie ha comprobado si las tres copias que montaste en la sesión 7 rinden más que una. La sesión 11 lo mide con un criterio pactado de latencia y errores, y cierra el RA3 con la conclusión menos intuitiva del módulo: replicar en la misma máquina reparte el trabajo, pero no añade recursos.

    El RA3 reaparece en el Tema 6, cuando sea un orquestador quien decida cuántas copias hacen falta y sobre cuántas máquinas repartirlas.