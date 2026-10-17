package es.escaparate.producto;

import es.escaparate.almacenamiento.AlmacenamientoImagenes;
import es.escaparate.almacenamiento.ImagenAlmacenada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de aplicación del catálogo de productos.
 */
@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AlmacenamientoImagenes almacenamientoImagenes;

    /**
     * Construye el servicio con persistencia y almacenamiento desacoplados.
     *
     * @param productoRepository repositorio de productos
     * @param almacenamientoImagenes almacenamiento de imágenes activo
     */
    public ProductoService(ProductoRepository productoRepository,
                           AlmacenamientoImagenes almacenamientoImagenes) {
        this.productoRepository = productoRepository;
        this.almacenamientoImagenes = almacenamientoImagenes;
    }

    /**
     * Lista el catálogo ordenado por fecha de alta descendente.
     *
     * @return productos del catálogo
     */
    public List<Producto> listar() {
        return productoRepository.findAllByOrderByFechaAltaDesc();
    }

    /**
     * Busca un producto por identificador.
     *
     * @param id identificador del producto
     * @return producto encontrado o vacío
     */
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    /**
     * Recupera la imagen de un producto si el producto y la imagen existen.
     *
     * @param id identificador del producto
     * @return imagen almacenada o vacío
     */
    public Optional<ImagenAlmacenada> cargarImagen(Long id) {
        return productoRepository.findById(id)
                .map(Producto::getImagenClave)
                .flatMap(almacenamientoImagenes::cargar);
    }

    /**
     * Valida y crea un producto, guardando previamente la imagen opcional.
     *
     * @param nombre nombre obligatorio
     * @param descripcion descripción opcional
     * @param precio precio no negativo; si es nulo se usa cero
     * @param imagen imagen opcional
     * @return producto persistido
     * @throws IllegalArgumentException si los datos no son válidos
     */
    @Transactional
    public Producto crear(String nombre,
                          String descripcion,
                          BigDecimal precio,
                          MultipartFile imagen) {
        String nombreLimpio = limpiarNombre(nombre);
        String descripcionLimpia = limpiarOpcional(descripcion);
        BigDecimal precioFinal = precio == null ? BigDecimal.ZERO : precio;

        if (precioFinal.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }

        String claveImagen = null;
        try {
            if (imagen != null && !imagen.isEmpty()) {
                claveImagen = almacenamientoImagenes.guardar(imagen);
            }

            Producto producto = new Producto(nombreLimpio, descripcionLimpia, precioFinal, claveImagen);
            return productoRepository.save(producto);
        } catch (RuntimeException e) {
            if (claveImagen != null) {
                try {
                    almacenamientoImagenes.eliminar(claveImagen);
                } catch (RuntimeException ignorada) {
                    // Conservamos el error original; la limpieza es de mejor esfuerzo.
                }
            }
            throw e;
        }
    }

    /**
     * Elimina un producto y, después de persistir el borrado, su imagen asociada.
     *
     * @param id identificador del producto
     * @return {@code true} si existía y se eliminó; {@code false} si no existía
     */
    @Transactional
    public boolean eliminar(Long id) {
        Optional<Producto> encontrado = productoRepository.findById(id);
        if (encontrado.isEmpty()) {
            return false;
        }

        Producto producto = encontrado.get();
        productoRepository.delete(producto);
        productoRepository.flush();
        almacenamientoImagenes.eliminar(producto.getImagenClave());
        return true;
    }

    private String limpiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > 120) {
            throw new IllegalArgumentException("El nombre no puede superar los 120 caracteres.");
        }
        return limpio;
    }

    private String limpiarOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > 500) {
            throw new IllegalArgumentException("La descripción no puede superar los 500 caracteres.");
        }
        return limpio;
    }
}
