# 🧩 Servidor de aplicaciones: ejecución, estado y rendimiento

Hasta ahora el foco ha estado en la entrada del sistema:

```text
cliente
   ↓
servidor web / proxy
   ↓
aplicación
```

El servidor web puede servir ficheros, seleccionar sitios, terminar TLS o reenviar peticiones. Pero cuando una petición necesita ejecutar lógica de negocio, debe llegar al proceso que contiene la aplicación.

En una aplicación Java moderna ese proceso puede llevar su propio servidor embebido. También existe el modelo clásico en el que varias aplicaciones se despliegan sobre un servidor instalado de forma independiente.

Esta sesión compara ambos modelos y después estudia dos problemas muy ligados a la ejecución de aplicaciones replicadas:

```text
¿dónde vive el estado de una sesión?
¿qué significa realmente tener varias copias?
```

---

## 1. Servidor web y servidor de aplicaciones

Las responsabilidades son distintas aunque cooperen.

| Pieza | Responsabilidad principal |
|---|---|
| **Servidor web** | recibir HTTP, servir estáticos, aplicar políticas de entrada y reenviar tráfico |
| **Servidor de aplicaciones** | proporcionar el entorno donde se ejecuta la aplicación dinámica |

Un esquema habitual es:

```text
Internet
   ↓
Nginx
servidor web / proxy
   ↓
Spring Boot + Tomcat
servidor de aplicaciones
   ↓
base de datos
```

Tomcat es, con más precisión, un **contenedor de servlets**. Entre otras tareas, recibe peticiones, gestiona el ciclo de vida de la aplicación, mantiene un conjunto de hilos y puede gestionar sesiones HTTP.

Otros productos, como WildFly o Payara, ofrecen además más servicios de la plataforma Jakarta EE. Para esta sesión basta con reconocer esa diferencia.

---

## 2. Qué aporta un contenedor de servlets

La palabra **contenedor** aquí no significa contenedor Docker.

Un contenedor de servlets rodea a la aplicación y administra su ejecución:

```text
desplegar aplicación
      ↓
crear contexto
      ↓
inicializar
      ↓
atender peticiones
      ↓
detener / retirar
```

### 2.1. Peticiones e hilos

Normalmente un controlador no crea un proceso nuevo por cada petición.

El servidor mantiene recursos preparados para atenderlas:

```text
petición 1 ──► hilo
petición 2 ──► hilo
petición 3 ──► hilo
...
```

Si todos están ocupados, nuevas peticiones tendrán que esperar.

Esta idea será importante al hablar de rendimiento:

```text
más peticiones concurrentes
≠
más CPU disponible
```

---

## 3. Un WAR, dos modelos

En temas anteriores distinguimos dos niveles:

```text
WAR
→ artefacto de aplicación Java

imagen de contenedor
→ unidad de despliegue que contiene runtime + artefacto
```

Una aplicación Spring Boot basada en servlets puede prepararse como WAR desplegable y, al mismo tiempo, seguir siendo ejecutable.

### 3.1. Modelo embebido

```text
java -jar aplicacion.war
        ↓
Spring Boot
        ↓
Tomcat embebido
```

La aplicación lleva consigo la versión del contenedor de servlets que necesita.

Es un modelo especialmente cómodo cuando cada aplicación se empaqueta y despliega como una unidad independiente.

### 3.2. Modelo externo

```text
Tomcat
├── aplicacion-a.war
├── aplicacion-b.war
└── aplicacion-c.war
```

El servidor existe independientemente de las aplicaciones y puede alojar varias.

Aquí la administración de:

```text
servidor
+
aplicaciones desplegadas
```

son responsabilidades separadas.

### 3.3. Comparación

| | Embebido | Externo |
|---|---|---|
| Unidad habitual | aplicación autónoma | servidor + aplicaciones |
| Versión del servidor | ligada a la aplicación | administrada por separado |
| Actualización | sustituir proceso o imagen | desplegar/reemplazar artefacto |
| Varias aplicaciones | normalmente procesos separados | pueden compartir servidor |
| Encaje con contenedores | muy natural | posible, pero no necesario para una única app |

No existe una regla universal de que uno sea siempre correcto y el otro incorrecto.

Para servicios nuevos desplegados como imágenes independientes, el modelo embebido suele encajar mejor. El modelo externo sigue siendo relevante para comprender y mantener sistemas empresariales existentes.

---

## 4. Qué conviene identificar en Tomcat

No necesitas memorizar su configuración.

Sí conviene saber reconocer:

| Elemento | Función |
|---|---|
| `conf/server.xml` | conectores, puertos y estructura principal |
| `conf/context.xml` | configuración común de contextos |
| `conf/tomcat-users.xml` | usuarios y roles en configuraciones sencillas |
| `lib/` | bibliotecas compartidas por el servidor |
| `webapps/` | aplicaciones desplegadas |

### Bibliotecas compartidas

Un servidor externo puede hacer visibles ciertas bibliotecas para varias aplicaciones.

Eso puede ahorrar duplicación, pero también introducir acoplamiento:

```text
aplicación A necesita v1
aplicación B necesita v2
        ↓
biblioteca compartida
        ↓
posible conflicto
```

En aplicaciones autónomas es habitual preferir que cada unidad declare y lleve sus propias dependencias.

---

## 5. La ruta de contexto

En Tomcat, el nombre del WAR puede determinar la ruta bajo la que se publica una aplicación.

Por ejemplo:

```text
ventas.war
→ /ventas

ROOT.war
→ /
```

Esto tiene una consecuencia inmediata cuando existe un proxy:

```text
proxy reenvía
/api/productos

servidor externo publica
/ventas/api/productos
```

Resultado:

```text
404
```

aunque ambas piezas estén funcionando.

El problema es simplemente:

```text
ruta esperada
≠
ruta desplegada
```

Cambiar el nombre a `ROOT.war` es una forma de desplegar esa aplicación en el contexto raíz.

---

## 6. Estado local y réplicas

Supón tres copias equivalentes detrás de un balanceador:

```text
          ┌── app-1
cliente ──┼── app-2
          └── app-3
```

Si cada petición puede llegar a cualquiera, una copia no debería guardar por sí sola información que la siguiente petición necesite recuperar.

Ya vimos esta regla con ficheros:

> si cualquier réplica debe poder recuperar un dato, ese dato no puede vivir únicamente dentro de una de ellas.

Las sesiones HTTP son otro ejemplo.

---

## 7. Cómo funciona una sesión

HTTP no recuerda automáticamente al usuario entre peticiones.

Una solución habitual es:

```text
login
  ↓
servidor crea sesión abc
  ↓
Set-Cookie: JSESSIONID=abc
  ↓
cliente reenvía la cookie
```

Si la sesión está en memoria:

```text
app-1
└── abc
```

y la siguiente petición llega a `app-2`, esa copia no conoce el identificador.

Con reparto entre tres copias puede aparecer:

```text
misma cookie
   │
   ├── app-1 → sesión encontrada
   ├── app-2 → sesión desconocida
   └── app-3 → sesión desconocida
```

El fallo parece aleatorio para el usuario, pero no lo es: depende del backend elegido.

---

## 8. Estrategias para compartir una sesión

Existen varias posibilidades.

| Estrategia | Idea | Limitación |
|---|---|---|
| **Sesión pegajosa** | el usuario vuelve a la misma copia | si esa copia desaparece, la sesión también |
| **Replicación** | las copias intercambian sesiones | aumenta complejidad y tráfico |
| **Almacén externo** | todas consultan un estado común | aparece una nueva dependencia |

En una arquitectura donde queremos copias intercambiables resulta especialmente útil:

```text
             ┌── app-1 ──┐
cliente ─► proxy ─ app-2 ─┼──► Redis
             └── app-3 ──┘
```

Con Spring Session, la aplicación puede guardar las sesiones en Redis en lugar de mantenerlas únicamente en memoria.

El aprendizaje importante no es Redis en sí:

```text
estado fuera de la réplica
        ↓
copias intercambiables
```

---

## 9. Rendimiento: tres palabras diferentes

### Latencia

Tiempo que tarda una petición.

```text
180 ms
```

### Throughput

Trabajo completado por unidad de tiempo.

```text
150 peticiones/s
```

### Concurrencia

Peticiones que permanecen dentro del sistema al mismo tiempo.

En condiciones estables puede utilizarse como intuición:

```text
concurrencia ≈ throughput × latencia
```

Cuando aumenta la latencia mientras sigue llegando trabajo, también puede crecer el número de peticiones acumuladas.

---

## 10. La media no cuenta toda la historia

Considera:

```text
50 ms
55 ms
60 ms
65 ms
800 ms
```

Una media resume los valores, pero puede ocultar una cola lenta.

Por eso utilizamos percentiles.

### p95

```text
p95
→ el 95 % de las peticiones
  tardó ese valor o menos
```

En esta sesión observaremos principalmente:

```text
peticiones/s
p95
errores
```

---

## 11. Varias copias no significan varias máquinas

Supón:

```text
app-1
app-2
app-3
   │
   ▼
mismo host
```

Las tres copias siguen compartiendo:

```text
CPU
memoria
red
disco
base de datos
almacén de sesiones
```

Por eso:

```text
3 procesos
≠
3 veces más recursos físicos
```

Tres copias pueden aportar:

- continuidad si una réplica concreta falla;
- posibilidad de reemplazar procesos progresivamente;
- reparto de trabajo entre procesos.

Pero no protegen frente a la caída del host completo ni garantizan multiplicar por tres la capacidad.

---

## 12. Una prueba pequeña y reproducible

Para comparar configuraciones hay que mantener constantes las condiciones:

```text
mismo endpoint
misma máquina
misma duración
misma carga
```

y cambiar solo lo que queremos estudiar.

En la actividad compararemos:

```text
1 copia
vs.
3 copias
```

y observaremos:

```text
peticiones/s
p95
errores
```

No fijaremos un umbral universal. El objetivo es comparar dos configuraciones bajo las mismas condiciones.

### Limitación del laboratorio

El generador k6 se ejecutará desde otro equipo y accederá a la EC2 por la URL pública. Así no consume la CPU de la máquina medida, pero la red también forma parte del tiempo observado.

Además, las aplicaciones siguen compartiendo:

```text
misma EC2
PostgreSQL
Redis
```

Por tanto, los resultados sirven para:

```text
comparar estas dos configuraciones
```

pero no para afirmar:

```text
"esta aplicación soporta X usuarios en producción"
```

---

## 🎯 Qué debes saber hacer al salir de esta sesión

- Diferenciar servidor web y servidor de aplicaciones.
- Describir qué aporta un contenedor de servlets.
- Explicar la diferencia entre Tomcat embebido y externo.
- Distinguir WAR e imagen de contenedor.
- Identificar los principales directorios y ficheros de Tomcat.
- Explicar la relación entre nombre del WAR y contexto.
- Explicar por qué una sesión local puede fallar al balancear entre réplicas.
- Comparar sesión pegajosa y almacén externo.
- Explicar por qué externalizar el estado hace las copias más intercambiables.
- Diferenciar latencia, throughput y concurrencia.
- Interpretar un p95.
- Explicar por qué tres réplicas en el mismo host no implican tres veces más capacidad.
- Interpretar una comparación sencilla de carga.

Lo que basta con **reconocer**:

- servidores Jakarta EE completos;
- detalles internos de `server.xml`;
- classloading y bibliotecas compartidas;
- replicación nativa de sesiones;
- p99;
- pruebas de estrés, pico y resistencia.

---

## ✅ Ideas clave

??? tip "Abrir resumen"

    - El servidor web publica y enruta; el servidor de aplicaciones ejecuta código.
    - Una aplicación Spring Boot puede llevar Tomcat embebido.
    - El mismo WAR puede prepararse para ejecución autónoma y despliegue tradicional.
    - El nombre del WAR puede determinar su contexto en Tomcat.
    - Una sesión guardada en memoria pertenece a una réplica.
    - Sacar el estado a un almacén compartido hace las copias más intercambiables.
    - Latencia, throughput y concurrencia no significan lo mismo.
    - p95 ayuda a observar una parte lenta que la media puede ocultar.
    - Varias copias dentro del mismo host siguen compartiendo recursos.
    - Una prueba de carga solo demuestra lo que permiten sus condiciones.

---

En la actividad abrirás brevemente el modelo clásico desplegando el mismo WAR en Tomcat, pero el proyecto conservará su arquitectura con servidor embebido. Después reproducirás una pérdida de sesión, externalizarás ese estado y compararás una y tres réplicas bajo la misma carga.
