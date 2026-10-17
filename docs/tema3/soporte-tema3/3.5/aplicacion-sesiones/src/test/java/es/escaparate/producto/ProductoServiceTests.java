package es.escaparate.producto;

import es.escaparate.almacenamiento.AlmacenamientoImagenes;
import es.escaparate.almacenamiento.ImagenAlmacenada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTests {

    @Mock
    ProductoRepository repository;

    @Mock
    AlmacenamientoImagenes almacenamiento;

    @Test
    void listaYBuscaProductosDelegandoEnRepositorio() {
        Producto producto = new Producto("Teclado", null, BigDecimal.TEN, null);
        when(repository.findAllByOrderByFechaAltaDesc()).thenReturn(List.of(producto));
        when(repository.findById(7L)).thenReturn(Optional.of(producto));

        ProductoService service = new ProductoService(repository, almacenamiento);

        assertEquals(List.of(producto), service.listar());
        assertEquals(producto, service.buscarPorId(7L).orElseThrow());
    }

    @Test
    void creaProductoNormalizandoDatosYSinImagen() {
        when(repository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ProductoService service = new ProductoService(repository, almacenamiento);

        Producto creado = service.crear("  Teclado  ", "  Mecánico  ", null, null);

        assertEquals("Teclado", creado.getNombre());
        assertEquals("Mecánico", creado.getDescripcion());
        assertEquals(BigDecimal.ZERO, creado.getPrecio());
        assertEquals(null, creado.getImagenClave());
        verify(almacenamiento, never()).guardar(any());
    }

    @Test
    void creaProductoConImagenYEliminaLaImagenSiFallaLaPersistencia() {
        MockMultipartFile imagen = new MockMultipartFile("imagen", "foto.png", "image/png", new byte[]{1, 2});
        when(almacenamiento.guardar(imagen)).thenReturn("clave.png");
        when(repository.save(any(Producto.class))).thenThrow(new RuntimeException("BD caída"));
        ProductoService service = new ProductoService(repository, almacenamiento);

        assertThrows(RuntimeException.class,
                () -> service.crear("Producto", null, BigDecimal.ONE, imagen));

        verify(almacenamiento).eliminar("clave.png");
    }

    @Test
    void creaProductoConImagenCuandoLaPersistenciaFunciona() {
        MockMultipartFile imagen = new MockMultipartFile("imagen", "foto.webp", "image/webp", new byte[]{1});
        when(almacenamiento.guardar(imagen)).thenReturn("clave.webp");
        when(repository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ProductoService service = new ProductoService(repository, almacenamiento);

        Producto creado = service.crear("Producto", " ", new BigDecimal("12.50"), imagen);

        assertEquals("clave.webp", creado.getImagenClave());
        assertEquals(null, creado.getDescripcion());
    }

    @Test
    void validaNombreDescripcionYPrecio() {
        ProductoService service = new ProductoService(repository, almacenamiento);

        assertThrows(IllegalArgumentException.class,
                () -> service.crear(" ", null, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear("x".repeat(121), null, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear("Producto", "x".repeat(501), BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear("Producto", null, new BigDecimal("-0.01"), null));
    }

    @Test
    void cargaImagenSoloCuandoProductoTieneClave() {
        Producto conImagen = new Producto("Con imagen", null, BigDecimal.ONE, "foto.png");
        Producto sinImagen = new Producto("Sin imagen", null, BigDecimal.ONE, null);
        ImagenAlmacenada imagen = new ImagenAlmacenada(new ByteArrayResource(new byte[]{1}), MediaType.IMAGE_PNG);
        when(repository.findById(1L)).thenReturn(Optional.of(conImagen));
        when(repository.findById(2L)).thenReturn(Optional.of(sinImagen));
        when(repository.findById(3L)).thenReturn(Optional.empty());
        when(almacenamiento.cargar("foto.png")).thenReturn(Optional.of(imagen));
        ProductoService service = new ProductoService(repository, almacenamiento);

        assertEquals(imagen, service.cargarImagen(1L).orElseThrow());
        assertTrue(service.cargarImagen(2L).isEmpty());
        assertTrue(service.cargarImagen(3L).isEmpty());
    }

    @Test
    void eliminaProductoYSuImagenYDevuelveFalseSiNoExiste() {
        Producto producto = new Producto("Producto", null, BigDecimal.ONE, "foto.jpg");
        when(repository.findById(10L)).thenReturn(Optional.of(producto));
        when(repository.findById(11L)).thenReturn(Optional.empty());
        ProductoService service = new ProductoService(repository, almacenamiento);

        assertTrue(service.eliminar(10L));
        verify(repository).delete(producto);
        verify(repository).flush();
        verify(almacenamiento).eliminar("foto.jpg");

        assertFalse(service.eliminar(11L));
    }
}
