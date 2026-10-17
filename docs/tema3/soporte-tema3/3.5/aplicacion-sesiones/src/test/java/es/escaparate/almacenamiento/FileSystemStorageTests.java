package es.escaparate.almacenamiento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemStorageTests {

    @TempDir
    Path temporal;

    @Test
    void guardaCargaYEliminaUnaImagen() throws Exception {
        FileSystemStorage storage = new FileSystemStorage(temporal.toString());
        byte[] contenido = "imagen-de-prueba".getBytes();
        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "producto.png", MediaType.IMAGE_PNG_VALUE, contenido);

        String clave = storage.guardar(archivo);

        assertTrue(clave.endsWith(".png"));
        assertTrue(Files.exists(temporal.resolve(clave)));
        var cargada = storage.cargar(clave).orElseThrow();
        assertEquals(MediaType.IMAGE_PNG, cargada.tipoContenido());
        assertArrayEquals(contenido, cargada.recurso().getInputStream().readAllBytes());

        storage.eliminar(clave);
        assertFalse(Files.exists(temporal.resolve(clave)));
        assertTrue(storage.cargar(clave).isEmpty());
        assertEquals("filesystem", storage.tipo());
    }

    @Test
    void rechazaFicherosVaciosFormatosNoPermitidosYRutasInseguras() {
        FileSystemStorage storage = new FileSystemStorage(temporal.toString());

        assertThrows(IllegalArgumentException.class,
                () -> storage.guardar(new MockMultipartFile("imagen", "vacia.png", "image/png", new byte[0])));
        assertThrows(IllegalArgumentException.class,
                () -> storage.guardar(new MockMultipartFile("imagen", "notas.txt", "text/plain", new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> storage.cargar("../fuera.png"));
        assertTrue(storage.cargar(null).isEmpty());
        assertTrue(storage.cargar(" ").isEmpty());
    }

    @Test
    void rechazaImagenesMayoresDeCincoMegabytes() {
        FileSystemStorage storage = new FileSystemStorage(temporal.toString());
        byte[] contenido = new byte[(5 * 1024 * 1024) + 1];
        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "grande.jpg", MediaType.IMAGE_JPEG_VALUE, contenido);

        assertThrows(IllegalArgumentException.class, () -> storage.guardar(archivo));
    }
}
