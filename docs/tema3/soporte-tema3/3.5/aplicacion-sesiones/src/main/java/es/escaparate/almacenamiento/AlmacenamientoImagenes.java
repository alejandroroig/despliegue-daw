package es.escaparate.almacenamiento;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Abstracción del lugar donde se guardan las imágenes de los productos.
 * La lógica del catálogo no necesita saber si el almacenamiento es local,
 * compartido o, más adelante, un servicio de objetos.
 */
public interface AlmacenamientoImagenes {

    /**
     * Guarda una imagen y devuelve la clave con la que podrá recuperarse.
     *
     * @param archivo imagen recibida en la petición
     * @return clave persistible asociada a la imagen
     * @throws IllegalArgumentException si el fichero no cumple las reglas admitidas
     * @throws AlmacenamientoException si no puede escribirse en el soporte de almacenamiento
     */
    String guardar(MultipartFile archivo);

    /**
     * Recupera una imagen a partir de su clave.
     *
     * @param clave clave almacenada junto al producto
     * @return imagen y tipo de contenido, o vacío si no existe
     */
    Optional<ImagenAlmacenada> cargar(String clave);

    /**
     * Elimina una imagen si existe.
     *
     * @param clave clave almacenada junto al producto
     * @throws AlmacenamientoException si el soporte no permite completar el borrado
     */
    void eliminar(String clave);

    /**
     * Identifica el tipo de almacenamiento activo.
     *
     * @return nombre corto del almacenamiento
     */
    String tipo();
}
