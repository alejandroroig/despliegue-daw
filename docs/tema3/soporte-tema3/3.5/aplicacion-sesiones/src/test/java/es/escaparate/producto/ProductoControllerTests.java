package es.escaparate.producto;

import es.escaparate.api.ApiExceptionHandler;
import es.escaparate.almacenamiento.ImagenAlmacenada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductoControllerTests {

    ProductoService service;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(ProductoService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new ProductoController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listaYBuscaProductos() throws Exception {
        Producto producto = producto(4L, "Ratón", "raton.png");
        when(service.listar()).thenReturn(List.of(producto));
        when(service.buscarPorId(4L)).thenReturn(Optional.of(producto));
        when(service.buscarPorId(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ratón"))
                .andExpect(jsonPath("$[0].imagenUrl").value("/productos/4/imagen"));

        mvc.perform(get("/api/productos/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));

        mvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creaProductoMultipartYDevuelveLocation() throws Exception {
        Producto creado = producto(21L, "Webcam", null);
        when(service.crear(eq("Webcam"), eq("Full HD"), eq(new BigDecimal("34.90")), any()))
                .thenReturn(creado);

        mvc.perform(multipart("/api/productos")
                        .file("imagen", new byte[]{1, 2, 3})
                        .param("nombre", "Webcam")
                        .param("descripcion", "Full HD")
                        .param("precio", "34.90"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/productos/21"))
                .andExpect(jsonPath("$.nombre").value("Webcam"));
    }

    @Test
    void eliminaYSirveImagen() throws Exception {
        when(service.eliminar(2L)).thenReturn(true);
        when(service.eliminar(3L)).thenReturn(false);
        when(service.cargarImagen(2L)).thenReturn(Optional.of(
                new ImagenAlmacenada(new ByteArrayResource(new byte[]{7, 8}), MediaType.IMAGE_PNG)));
        when(service.cargarImagen(3L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/productos/2")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/productos/3")).andExpect(status().isNotFound());
        mvc.perform(get("/api/productos/2/imagen"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
        mvc.perform(get("/api/productos/3/imagen")).andExpect(status().isNotFound());
    }

    @Test
    void traduceErroresDeValidacionA400() throws Exception {
        when(service.crear(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("El nombre es obligatorio."));

        mvc.perform(multipart("/api/productos").param("nombre", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud no válida"));
    }

    private Producto producto(Long id, String nombre, String imagenClave) {
        Producto producto = new Producto(nombre, "Descripción", new BigDecimal("10.00"), imagenClave);
        ReflectionTestUtils.setField(producto, "id", id);
        ReflectionTestUtils.setField(producto, "fechaAlta", Instant.parse("2026-01-01T10:00:00Z"));
        return producto;
    }
}
