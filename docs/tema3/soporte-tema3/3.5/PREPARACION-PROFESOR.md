# Preparación de las imágenes de sesiones

Realizar antes de clase en Amazon Linux x86_64 con Docker, Bash y Python 3. No hace falta Maven ni Java en el anfitrión. El preparador usa Docker con sudo y requiere Internet para las imágenes base y las dependencias Maven.

Desde la raíz del repositorio donde se extrajo el soporte:

```bash
bash soporte-tema3/3.5/preparar-imagenes.sh
sudo docker save escaparate:sesion-10-local escaparate:sesion-10-redis | gzip > imagenes-sesiones.tar.gz
```

Distribuye el archivo resultante mediante el aula virtual o cárgalo en las instancias antes de empezar:

```bash
sudo docker load -i imagenes-sesiones.tar.gz
```

Desde Mac se puede copiar a la instancia con:

```bash
scp -i labsuser.pem imagenes-sesiones.tar.gz ec2-user@IP_PUBLICA:~/
```

En EC2: `sudo docker load -i ~/imagenes-sesiones.tar.gz`. Sustituye IP_PUBLICA por la dirección actual. El paquete de actividades contiene fuentes y preparador, no estas imágenes binarias ni credenciales. No depende de etiquetas nuevas publicadas en GHCR.

El contexto de construcción procede del repositorio de clase, revisado en el commit 1fa354d. Incluye el controlador de sesión didáctico. El preparador construye primero la variante local, añade el starter de Spring Session Redis a una copia temporal y construye la segunda. No altera el proyecto del profesor ni el .env del alumnado. Se añade `instancia` a las respuestas para reconocer la réplica sin retocar cabeceras Nginx.

Las imágenes que ya se probaron en EC2 con esos nombres permiten demostrar 401/200 y Redis, pero no incluían el campo `instancia`. Para seguir exactamente este enunciado usa las construidas con este material. No reutilices por error una etiqueta antigua esperando que su contenido cambie solo.

Los dos despliegues comparten la lógica de la prueba realizada; esta automatización y el campo añadido no se han ejecutado de nuevo en EC2. Se han comprobado estáticamente los archivos y las combinaciones Compose. No se afirma una nueva prueba completa del paquete.

Construye en x86_64 para las instancias del curso; una imagen creada de forma nativa en un Mac ARM no equivale a la imagen de EC2. Reserva tiempo y disco antes de clase para las construcciones y la distribución. El TAR contiene capas grandes y no debe entrar en Git.
