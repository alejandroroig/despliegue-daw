package es.escaparate.producto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Producto persistente mostrado en el catálogo de Escaparate.
 */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "imagen_clave", length = 255)
    private String imagenClave;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private Instant fechaAlta;

    /**
     * Constructor requerido por JPA.
     */
    protected Producto() {
        // Constructor requerido por JPA.
    }

    /**
     * Crea un producto nuevo antes de persistirlo.
     *
     * @param nombre nombre visible del producto
     * @param descripcion descripción opcional
     * @param precio precio del producto
     * @param imagenClave clave opcional de la imagen asociada
     */
    public Producto(String nombre, String descripcion, BigDecimal precio, String imagenClave) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.imagenClave = imagenClave;
    }

    @PrePersist
    void asignarFechaAlta() {
        if (fechaAlta == null) {
            fechaAlta = Instant.now();
        }
    }

    /**
     * Devuelve el identificador asignado por la base de datos.
     *
     * @return identificador persistente del producto
     */
    public Long getId() {
        return id;
    }

    /**
     * Devuelve el nombre que se muestra en el catálogo.
     *
     * @return nombre visible del producto
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve la descripción del producto.
     *
     * @return descripción opcional del producto
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Devuelve el precio del producto.
     *
     * @return precio del producto
     */
    public BigDecimal getPrecio() {
        return precio;
    }

    /**
     * Devuelve la clave con la que se recupera la imagen del almacenamiento.
     *
     * @return clave de la imagen asociada, o {@code null} si no tiene
     */
    public String getImagenClave() {
        return imagenClave;
    }

    /**
     * Devuelve el instante en que el producto se dio de alta.
     *
     * @return instante en que se dio de alta el producto
     */
    public Instant getFechaAlta() {
        return fechaAlta;
    }
}
