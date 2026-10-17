package es.escaparate.almacenamiento;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * Imagen recuperada del almacenamiento junto con su tipo MIME.
 *
 * @param recurso recurso que contiene los bytes de la imagen
 * @param tipoContenido tipo MIME que debe enviarse al cliente
 */
public record ImagenAlmacenada(Resource recurso, MediaType tipoContenido) {
}
