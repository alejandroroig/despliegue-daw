package es.escaparate.almacenamiento;

/**
 * Error no recuperable producido al guardar, cargar o eliminar una imagen.
 */
public class AlmacenamientoException extends RuntimeException {

    /**
     * Crea una excepción con un mensaje descriptivo.
     *
     * @param mensaje descripción del error
     */
    public AlmacenamientoException(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea una excepción conservando la causa original.
     *
     * @param mensaje descripción del error
     * @param causa excepción que originó el fallo
     */
    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
