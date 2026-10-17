package es.escaparate.infraestructura.dto;

/**
 * Respuesta mínima de los endpoints de salud.
 *
 * @param estado estado textual de la comprobación
 */
public record SaludResponse(String estado) {
}
