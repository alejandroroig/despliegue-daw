# Proyecto: apuntes de DAW e INU — conocimiento base
 
Documento de contexto para generar material de los módulos **Despliegue de Aplicaciones Web (0614)** e **Introducción a la Nube Pública**. Súbelo al proyecto de Claude junto con la temporalización en Excel.
 
---
 
## 1 · Quién y qué
 
Profesor de FP del ciclo **DAW** (2.º curso). Imparte los dos módulos: **INU los miércoles** y **DAW los viernes**, 3 h por sesión, 16 sesiones cada uno, curso 2026/27. El mismo alumnado suele cursar ambos, pero **no siempre**: la optativa también la cogen alumnos de DAM, así que INU debe sostenerse sola.
 
Ambos sitios son MkDocs Material, en español, publicados con licencia CC BY-NC-SA 4.0.
 
---
 
## 2 · El proyecto que atraviesa los dos módulos: Escaparate
 
**Escaparate** es un catálogo de productos con imágenes: Spring Boot 3 (Java 21) empaquetado como `war` que arranca embebido *y* se despliega en Tomcat externo, front estático puro (HTML/CSS/JS con un `config.js` de una línea), PostgreSQL, e imágenes guardadas en disco.
 
El alumnado **no la programa**: se le entrega hecha. Lo que construye es todo lo que hay alrededor.
 
Cuatro decisiones de diseño que hay que respetar al escribir material, porque cada una existe para desbloquear sesiones concretas:
 
| Decisión | Qué desbloquea |
|---|---|
| Front estático separado del back | Servirlo desde Nginx, GitHub Pages, S3 y CDN |
| `war` que vale para embebido y para Tomcat externo | La sesión de servidor de aplicaciones (RA3) |
| Imágenes en disco local — **mal a propósito** | El fallo de «una réplica tiene la foto y la otra no», que aparece al balancear y se resuelve con almacenamiento compartido y luego con objetos |
| Endpoints `/api/instancia`, `/api/carga`, `/api/salud` | Ver el balanceo, generar carga para el autoescalado, y alimentar health checks y sondas |
 
Existe también una **versión 2.0.0** visualmente distinta (otro color de cabecera) para hacer visibles las actualizaciones progresivas y los rollback.
 
El `pom.xml` de Escaparate se congela **antes de la S2** y ya incorpora `maven-javadoc-plugin` (lo usa la actividad 1.2) y `jacoco-maven-plugin` con la regla de cobertura presente pero desactivada (la activan en la 5.1). Es dependencia de las sesiones 2, 4, 12 y 13: tocarlo a mitad de curso arrastra material escrito.
 
### El repositorio del alumnado (DAW)
 
Se llama **`daw-despliegue`**, es público, se crea en la **actividad 1.2** y acompaña las dieciséis sesiones. Convenios que todo el material debe respetar:
 
```
daw-despliegue/
├── README.md                 manual de despliegue; crece cada viernes
├── compose.yaml              despliegue de producción
├── compose.override.yaml     añadidos de desarrollo
├── .env.example
├── .gitignore
├── docs/                     documentación publicada como sitio del repositorio
├── entregas/temaN/
└── escaparate/               código entregado; no se programa, se despliega
```
 
- **Una rama por sesión**, `sesion-NN`, integrada en `main` mediante pull request. `main` significa «lo que está desplegado». Modelo de tronco con ramas cortas: nada de git-flow ni de rama por entorno.
- **Registros**: la imagen de la base de datos en Docker Hub (actividad 2.1), la de la aplicación en **GHCR** (actividad 2.2). Ambas **públicas**, porque la S13 las descarga desde la instancia sin credenciales.
- **Etiquetas**: inmutable para lo que se despliega, móvil solo para desarrollo. Versionado semántico introducido en la S2, aplicado a imágenes en la S4 y cobrado en el rollback de la S13.
- **Credenciales**: clave SSH o token personal, creados en la S2. El token de GHCR de la S4 es otro distinto. El pipeline de la S13 no necesita ninguno de los dos: usa el temporal que presta la plataforma.
---
 
## 3 · Estructura de los dos sitios
 
```
<modulo>/
├── mkdocs.yml
├── requirements.txt        mkdocs, mkdocs-material, mkdocs-pdf
├── .gitignore              excluye soluciones del profesor y site/
├── .claude/launch.json     mkdocs serve en 127.0.0.1:8765
├── overrides/partials/copyright.html
└── docs/
    ├── index.md            portada del módulo
    ├── curriculum.md       RA y CE completos, literales del currículo
    ├── cierre.md           página final
    ├── css/extra.css
    └── temaN/
        ├── index.md        RA, CE, índice de contenidos, actividades
        ├── <apunte>.md     un apunte por sesión
        ├── actividad_N_M.md
        ├── diapositivas/   .pptx y .pdf con el mismo nombre que el apunte
        ├── plantillas/     Actividad_N_M_<MOD>_Plantilla.docx
        └── img/<tema>/     capturas
```
 
**Paletas:** DAW `primary: indigo`, `accent: light blue`. INU `primary: teal`, `accent: deep orange`. (AD usa red y PSP brown — los cuatro sitios se distinguen de un vistazo.)
 
---
 
## 4 · Correspondencia tema ↔ unidad ↔ sesión
 
### DAW — 6 temas, 16 sesiones
 
| Tema | RA | Sesiones | Actividades | Apuntes |
|---|---|---|---|---|
| 1 · Punto de partida: la aplicación y su repositorio | RA1 + RA6 | S1, S2 | 1.1, 1.2 | arquitecturas-despliegue, control-versiones-documentacion |
| 2 · Virtualización y contenedores | RA1 | S3, S4, S5 | 2.1 – 2.3 | fundamentos-contenedores, imagenes-contenedores, docker-compose |
| 3 · Administración de servidores web | RA2 | S6, S7, S8, S9 | 3.1 – 3.4 | servidores-web-dns, proxy-inverso-balanceo, seguridad-web-https, observabilidad |
| 4 · Servidores de aplicaciones | RA3 | S10, S11 | 4.1, 4.2 | servidor-aplicaciones, rendimiento-pruebas-carga |
| 5 · Integración y despliegue continuos | RA6 | S12, S13 | 5.1, 5.2 | integracion-continua, despliegue-continuo |
| 6 · Orquestación de contenedores | RA1 (+RA2, RA3) | S14, S15, S16 | 6.1 – 6.3 | kubernetes-arquitectura, kubernetes-configuracion, eks-gitops |
 
**RA4 y RA5 se acreditan en la Formación en Empresa** y no tienen material en el sitio.
 
**Los seis temas van en orden de sesión, sin saltos ni inversiones.** El orden de lectura del sitio es el orden del aula.
 
**Dos resultados de aprendizaje están repartidos entre temas**, y cada `index.md` recoge solo los criterios que ese tema acredita:
 
| RA | Reparto |
|---|---|
| RA1 | Tema 1: arquitecturas, protocolos, estructura de una aplicación web y requerimientos de implantación. Tema 2: virtualización en contenedores, pruebas y documentación de lo instalado. Tema 6 vuelve sobre él con orquestación |
| RA6 | Tema 1: criterios a) a g) —documentación y control de versiones—. Tema 5: criterio h) —integración continua— y la parte de f) que corresponde a la seguridad del pipeline |
 
El RA6 tiene siete criterios de documentación y control de versiones frente a uno solo de integración continua. **El peso evaluador se pondera por sesiones, no por número de criterios**, y así debe quedar justificado en la programación didáctica.
 
**Distancia entre los dos temas de RA6**: tres meses y once sesiones. Los apuntes del Tema 5 deben **reactivar** lo del Tema 1 con una tabla corta al principio (qué dejaste en septiembre, dónde lo has usado, qué le pasa hoy), nunca reexplicarlo.
 
### INU — 7 temas, 16 sesiones
 
| Tema | RA | Sesiones | Apuntes |
|---|---|---|---|
| 1 · Introducción a la nube pública | RA1 + RA2 | S1 | primer-contacto-aws |
| 2 · Redes virtuales y cómputo | RA3 | S2, S3, S4 | vpc-diseno, seguridad-red, maquinas-virtuales |
| 3 · Almacenamiento, datos y primera arquitectura | RA4 (+RA3) | S5, S6, S7 | almacenamiento, bases-datos-gestionadas, arquitectura-completa |
| 4 · Alta disponibilidad y entrega de contenido | RA3 | S8, S9 | alta-disponibilidad-escalado, dns-https-cdn |
| 5 · Gobierno de la nube | RA2 (+RA1) | S10, S11, S12 | monitorizacion-operacion, iam-aplicado, economia-nube |
| 6 · Automatización y modelos de ejecución | RA3 (+RA4) | S13, S14, S15 | infraestructura-como-codigo, serverless, contenedores-gestionados |
| 7 · Arquitectura bien diseñada | RA4 | S16 | well-architected |
 
---
 
## 5 · Principios pedagógicos que rigen todo el material
 
**Manual antes que gestionado.** DAW enseña el mecanismo montándolo a mano; INU da la versión gestionada del mismo problema días después. La segunda vez no se repite teoría: se compara. Parejas:
 
| Concepto | Manual (DAW) | Gestionado (INU) | Separación |
|---|---|---|---|
| Base de datos | Postgres en Compose, S5 | Servicio gestionado, S6 | 5 días |
| Balanceo | Nginx upstream, S7 | Balanceador + autoescalado, S8 | 5 días |
| HTTPS | Let's Encrypt a mano, S8 | Certificado gestionado, S9 | 5 días |
| Observabilidad | Pila EFK propia, S9 | Servicio gestionado, S10 | 5 días |
| Contenedores | Compose y Dockerfile, S3–S5 | Registro y servicio gestionado, S15 | meses |
| Kubernetes | Clúster sobre instancia, S14–S15 | Clúster gestionado, S16 | días |
 
**Actividades A y B.** Cada actividad tiene una parte **A** guiada, que debe terminar todo el mundo, y una **B** escalada sobre la anterior, con bonus explícito en la rúbrica. El listón de aprobado está en A.
 
**Reflexiones después de la acción, nunca antes.** El enunciado describe qué conseguir sin dar los comandos; ejecutan, ven el efecto, y una pregunta les obliga a nombrar lo que acaba de pasar. Ejemplos que ya están decididos:
 
- Compose: *«¿por qué el nombre del servicio resuelve dentro de la red y no en tu máquina?»*
- Proxy: *«¿quién decidió no mandar tráfico a la réplica caída y cómo lo supo?»*
- Sesiones: *«¿dónde vivía la sesión y por qué desapareció?»*
- VPC: *«la instancia privada no sale a internet: ¿falta una ruta, una regla o una pasarela? Nómbralo antes de tocar nada.»*
**Fallos provocados a propósito.** Son la mejor actividad de cada módulo y hay que preservarlos: las cuatro averías de red de INU S3, la pérdida de sesión al balancear en DAW S10, las imágenes que no se ven al escalar en DAW S7, la incidencia a diagnosticar en INU S10.
 
**Ritual de costes (solo INU).** Cinco minutos al cierre de cada sesión: revisar crédito, apagar todo, anotar el recurso más caro. Al llegar a S12 hay quince datos propios sobre los que razonar.
 
---
 
## 6 · Formato de un apunte
 
```markdown
<a id="slug"></a>
 
# 🧩 N. Título del apunte
 
![Título](diapositivas/slug.pdf){ type=application/pdf style="width:100%;min-height:80vh" }
 
!!!info "Descarga de diapositivas"
    [Descarga las diapositivas](diapositivas/slug.pptx){target="_blank" rel="noopener"}
 
---
 
<párrafo de enganche: qué sabe ya el alumnado, qué pregunta abierta
queda del apartado anterior, y qué responde este>
 
---
 
## 🧩 Sección con emoji
 
<prosa explicativa, tablas comparativas, bloques de código comentados,
diagramas mermaid, admoniciones !!! tip / !!! warning / !!! example>
 
---
 
## 🎯 Qué debes saber hacer al salir de esta sesión
 
<4-6 viñetas, en infinitivo, con lo EXIGIBLE. Nada más. Todo lo demás
del apunte es contexto, ampliación o referencia para consultar.>
 
---
 
## ✅ Ideas clave
 
??? tip "Abrir resumen"
 
    - <una línea por idea, autocontenida, sin depender del texto de arriba>
```
 
**Longitud objetivo: 2.000–3.000 palabras.** Los apuntes de referencia de AD/PSP están en esa horquilla.
 
**Los tres niveles.** «Esto está en el apunte» ≠ «esto lo explico en clase» ≠ «esto tienen que saber hacerlo». El bloque «Qué debes saber hacer» marca el tercero, y es lo único que la actividad puede exigir. Las secciones que van más allá se señalan con `!!! info "Para saber más"` para que quede claro que son ampliación.
 
**Reglas de estilo:**
 
- Segunda persona del singular, cercano pero no coloquial.
- Cada concepto nuevo se justifica con el problema que resuelve, nunca «esto es X y sirve para Y» a secas.
- Las tablas comparativas son el recurso preferido para «cuándo usar cada cosa».
- Los bloques de código llevan explicación línea a línea después, no antes.
- El último párrafo enlaza con la actividad que viene: *«Con esto ya tienes las piezas para la Actividad N.M…»*.
- Admoniciones: `!!! tip` para matices útiles, `!!! warning` para errores frecuentes, `!!! example` para analogías, `!!! info` para contexto lateral, `!!! danger` solo para lo que cuesta dinero o rompe cosas.
- Emojis en los `##`, con moderación y coherentes dentro del tema.
---
 
## 7 · Formato de una actividad
 
```markdown
# 🧪 Actividad N.M: Título
 
!!! warning "Descarga la plantilla"
    📄 [Plantilla N.M — Título](plantillas/Actividad_N_M_<MOD>_Plantilla.docx){target="_blank" rel="noopener"}
 
## Contexto
<el encargo: escenario profesional con una necesidad concreta, dos párrafos>
 
## Qué vas a practicar
<3-5 viñetas con verbos de acción>
 
## Requisitos previos
<qué debe estar funcionando de la sesión anterior, incluida la rama de la sesión>
 
!!! info "Reparto de tiempo orientativo"
    <dos o tres marcas: «Pasos 1 a 3, unos 30 minutos», etc.>
 
---
 
## Paso 1 — …
<describe el objetivo y el resultado esperado, no los comandos>
 
**Comprueba**: <qué debe verse>
**Captura**: <qué evidencia se entrega>
 
!!! question "Reflexiona"
    <pregunta que obliga a nombrar lo que acaba de pasar; SIEMPRE después de la acción>
 
## Paso 2 — …
…
 
---
 
## Si te sobra tiempo
<opcional y no evaluable. Solo si aporta algo de verdad: si no, se omite
la sección entera. Nunca produce nada que otra sesión necesite.>
 
---
 
## Verificación
 
Para dar por válida la práctica se ejecutará:
 
```bash
<comandos exactos>
```
 
Y debe observarse: <lista de comprobaciones>
 
---
 
## Qué se entrega
 
- [ ] <un elemento por línea, comprobable de un vistazo>
- [ ] Documentación en el repositorio
---
 
## ✅ Cierre
<qué tiene el alumnado al terminar y qué llega en la siguiente sesión>
```
**Longitud objetivo: 1.500–2.500 palabras.**
 
**Principio rector: todo lo que está en el enunciado es exigible.** No hay parte guiada y parte de reto. Si algo no es exigible, va a «Si te sobra tiempo» o no va. Esto obliga a recortar en la redacción en lugar de esconder lo que sobra en una parte opcional, y evita el fallo más grave posible: que una sesión posterior dependa de algo que la mitad de la clase no hizo.
 
**Nada de rúbricas numéricas.** La lista «Qué se entrega» es la única referencia de corrección: elementos comprobables de un vistazo, marcables como hecho o no hecho. Si el material acaba necesitando nota numérica, la lista ya sirve de base.
 
**La sección de verificación es obligatoria** en toda actividad evaluable: el alumnado sabe exactamente qué debe funcionar y la corrección baja de leer configuraciones a ejecutar tres comandos.
 
**Calibrado de carga.** El alumnado es de 2.º de DAW y la sesión dura tres horas **con la teoría delante**: quedan unos noventa minutos reales de práctica, con incidencias. Ante la duda, menos pasos y mejor entendidos. Una actividad que solo termina el tercio de arriba de la clase está mal dimensionada, aunque el material sea bueno.
 
---
 
## 8 · Restricciones técnicas que condicionan el material
 
**AWS Academy Learner Lab (INU y las sesiones de DAW que usan AWS):**
 
- La sesión caduca a las 4 h y el crédito es finito. Toda práctica arranca y termina dentro de la sesión.
- **No se pueden crear usuarios ni roles IAM.** Existe un rol preasignado. La sesión de identidad se resuelve con lectura y corrección de políticas, el simulador de políticas y el uso del rol existente — no creando la estructura desde cero.
- Regiones y servicios restringidos: comprobar disponibilidad antes de cada bloque.
- EKS funciona para lo básico, pero **sin proveedor OIDC**: nada de controlador de balanceadores para Ingress (usar Service de tipo LoadBalancer), nada de volúmenes persistentes sobre EBS/EFS (la base de datos vive fuera, en el servicio gestionado), nada de permisos por pod.
- **El plano de control de EKS factura aunque el laboratorio esté cerrado.** Norma innegociable de aula: destruir el clúster antes de salir.
- Terraform: las credenciales caducan cada sesión; el estado vive en local dentro del repositorio y se explica por qué en una empresa iría remoto.
**Preparativos del profesor** que el material debe dar por hechos:
 
| Antes de | Qué |
|---|---|
| **DAW S2** | **Escaparate congelado**, con `pom.xml` estable, `maven-javadoc-plugin` y `jacoco-maven-plugin` (regla de cobertura presente y desactivada). Sin esto no hay conflicto sobre fichero real ni documentación que generar en la actividad 1.2 |
| DAW S4 | Imagen de Escaparate publicada en registro público, como referencia del profesor |
| INU S2 | Módulo Terraform de red probado en el laboratorio |
| **DAW S8** | **Dominio propio con subdominio por equipo, delegado** — sin esto no hay ACME y la sesión se cae |
| DAW S9 | Pila de observabilidad en `compose`, lista para levantar |
| DAW S12 | `pom.xml` con la regla de cobertura lista para activar |
| DAW S13 | Escaparate 2.0.0 visualmente distinto; instancia EC2 con `user data`, cuya IP se pega en el secreto del pipeline al empezar la sesión |
| DAW S14 | Script de clúster local sobre instancia, probado con 4 GB |
| INU S3, S10 | Entornos con averías preparadas |
 
---
 
## 9 · Fuentes externas admitidas
 
- **Catálogo de workshops de AWS** (abierto): filtrar por nivel 100–200. El workshop *AWS 101* es el esqueleto de las sesiones 2 a 8 de INU.
- **Well-Architected Labs** (abierto): el laboratorio de nivel 100 de estimación de costes es casi copiable tal cual para INU S12.
- **Builder Labs de Skill Builder** (suscripción individual del profesor): son cantera para él, **no actividad del alumnado**.
**Regla:** de un laboratorio ajeno se extrae la arquitectura y las decisiones de diseño, y se reescribe el enunciado. No se copian sus plantillas —ni por licencia ni por practicidad, porque vienen llenas de andamiaje que no funciona en el laboratorio del aula—. Lo único que sí se copia literalmente es el `user data` de las instancias.
 
---
 
## 10 · Cómo pedir material en las sesiones siguientes
 
Formato de petición que funciona bien:
 
> «Escribe el apunte de DAW Tema 3, `proxy-inverso-balanceo.md`.»
> «Escribe la actividad 2.2 de INU (arregla esta red), con las cuatro averías concretas.»
> «Revisa el apunte X y añade una sección sobre Y.»
 
Ojo con la numeración de DAW: **las actividades van por tema, no por sesión.** El Tema 1 cubre las sesiones 1 y 2, así que a partir del Tema 2 el número de actividad va desfasado respecto al de sesión: la actividad 2.2 es la de la S4 y la 5.1 es la de la S12.
 
Claude debe, sin que se le recuerde: respetar el formato de la sección 6 o 7, usar Escaparate como hilo, mantener el orden manual→gestionado, incluir la reflexión y la verificación, y no inventar servicios que el Learner Lab no permite.
 
**Lo que hay que preguntar antes de escribir**, si no está claro: qué se ha visto exactamente en la sesión anterior, si la actividad es evaluable o de aula, y si el grupo cursa los dos módulos o solo uno.