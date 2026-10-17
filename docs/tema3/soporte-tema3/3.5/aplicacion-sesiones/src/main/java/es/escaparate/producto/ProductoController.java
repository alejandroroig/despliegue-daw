package es.escaparate.producto;

import es.escaparate.almacenamiento.ImagenAlmacenada;
import es.escaparate.producto.dto.ProductoResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * API REST del catálogo de productos.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Crea el controlador del catálogo.
     *
     * @param productoService servicio de aplicación de productos
     */
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Lista todos los productos ordenados por fecha de alta descendente.
     *
     * @return productos expuestos por la API
     */
    @GetMapping
    public List<ProductoResponse> listar() {
        return productoService.listar().stream()
                .map(ProductoResponse::from)
                .toList();
    }

    /**
     * Busca un producto por identificador.
     *
     * @param id identificador del producto
     * @return producto encontrado o respuesta 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id)
                .map(ProductoResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crea un producto a partir de un formulario multipart.
     *
     * @param nombre nombre obligatorio
     * @param descripcion descripción opcional
     * @param precio precio; por defecto cero
     * @param imagen imagen opcional
     * @return respuesta 201 con el producto creado y cabecera Location
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoResponse> crear(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(defaultValue = "0") BigDecimal precio,
            @RequestParam(required = false) MultipartFile imagen) {

        Producto creado = productoService.crear(nombre, descripcion, precio, imagen);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getId())
                .toUri();

        return ResponseEntity.created(location).body(ProductoResponse.from(creado));
    }

    /**
     * Elimina un producto y su imagen asociada si existe.
     *
     * @param id identificador del producto
     * @return 204 si se elimina o 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return productoService.eliminar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Devuelve la imagen asociada a un producto.
     *
     * @param id identificador del producto
     * @return recurso de imagen o 404 si no existe
     */
    @GetMapping("/{id}/imagen")
    public ResponseEntity<?> cargarImagen(@PathVariable Long id) {
        var imagen = productoService.cargarImagen(id);
        if (imagen.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return respuestaImagen(imagen.get());
    }

    private ResponseEntity<?> respuestaImagen(ImagenAlmacenada imagen) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(imagen.tipoContenido())
                .body(imagen.recurso());
    }
}
