package es.escaparate.infraestructura.dto;

/**
 * Resultado de una petición de carga sintética.
 *
 * @param host réplica que ejecutó la carga
 * @param ms duración aplicada en milisegundos
 * @param iteraciones número de iteraciones ejecutadas
 */
public record CargaResponse(
        String host,
        long ms,
        long iteraciones
) {
}
