package es.escaparate.infraestructura.dto;

/**
 * Identidad técnica visible de una réplica de Escaparate.
 *
 * @param host nombre de la réplica
 * @param version versión desplegada
 * @param almacenamiento tipo de almacenamiento de imágenes activo
 */
public record InstanciaResponse(
        String host,
        String version,
        String almacenamiento
) {
}
