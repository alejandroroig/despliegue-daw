package es.escaparate.producto.dto;

import es.escaparate.producto.Producto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representación HTTP de un producto sin exponer detalles internos de persistencia.
 *
 * @param id identificador del producto
 * @param nombre nombre visible
 * @param descripcion descripción opcional
 * @param precio precio del producto
 * @param imagenUrl URL de la imagen o {@code null} si no existe
 * @param fechaAlta instante de alta
 */
public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        String imagenUrl,
        Instant fechaAlta
) {
    /**
     * Convierte una entidad en la representación expuesta por la API.
     *
     * @param producto entidad persistente
     * @return DTO listo para serializar
     */
    public static ProductoResponse from(Producto producto) {
        String imagenUrl = producto.getImagenClave() == null
                ? null
                : "/productos/" + producto.getId() + "/imagen";

        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                imagenUrl,
                producto.getFechaAlta()
        );
    }
}
