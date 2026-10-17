package es.escaparate.infraestructura;

import es.escaparate.almacenamiento.AlmacenamientoImagenes;
import es.escaparate.almacenamiento.ImagenAlmacenada;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfraestructuraAdicionalTests {

    @Test
    void readinessDevuelve503SiLaBaseDeDatosNoEstaDisponible() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("BD no disponible"));
        SaludController controller = new SaludController(dataSource, 1);

        var respuesta = controller.listo();

        assertEquals(503, respuesta.getStatusCode().value());
        assertEquals("degradado", respuesta.getBody().estado());
    }

    @Test
    void instanciaUsaElNombreConfiguradoYExponeVersionYStorage() {
        IdentidadInstancia identidad = new IdentidadInstancia(" replica-a ", "v1.0.0", storage());
        InstanciaController controller = new InstanciaController(identidad);

        var respuesta = controller.instancia();

        assertEquals("replica-a", respuesta.host());
        assertEquals("v1.0.0", respuesta.version());
        assertEquals("filesystem", respuesta.almacenamiento());
    }

    @Test
    void cargaRechazaDuracionesNoPositivas() {
        CargaController controller = new CargaController(
                new IdentidadInstancia("test", "test", storage()), 100);

        assertThrows(IllegalArgumentException.class, () -> controller.generarCarga(0));
    }

    private AlmacenamientoImagenes storage() {
        return new AlmacenamientoImagenes() {
            @Override
            public String guardar(MultipartFile archivo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<ImagenAlmacenada> cargar(String clave) {
                return Optional.empty();
            }

            @Override
            public void eliminar(String clave) {
            }

            @Override
            public String tipo() {
                return "filesystem";
            }
        };
    }
}
