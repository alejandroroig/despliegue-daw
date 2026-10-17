package es.escaparate.producto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso JPA a los productos persistidos.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Recupera el catálogo mostrando primero los productos más recientes.
     *
     * @return lista ordenada por fecha de alta descendente
     */
    List<Producto> findAllByOrderByFechaAltaDesc();
}
