package es.escaparate;

import es.escaparate.almacenamiento.AlmacenamientoImagenes;
import es.escaparate.almacenamiento.ImagenAlmacenada;
import es.escaparate.infraestructura.CargaController;
import es.escaparate.infraestructura.IdentidadInstancia;
import es.escaparate.infraestructura.SaludController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraestructuraTests {

    @Test
    void livenessYReadinessRespondenOkConBaseDisponible() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:salud;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        SaludController controller = new SaludController(dataSource, 2);

        assertEquals("ok", controller.vivo().estado());
        assertEquals(200, controller.listo().getStatusCode().value());
        assertEquals("ok", controller.listo().getBody().estado());
    }

    @Test
    void cargaRespetaElMaximoConfigurado() {
        AlmacenamientoImagenes almacenamiento = new AlmacenamientoImagenes() {
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

        IdentidadInstancia identidad = new IdentidadInstancia("instancia-test", "test", almacenamiento);
        CargaController controller = new CargaController(identidad, 20);

        var respuesta = controller.generarCarga(1000);

        assertEquals("instancia-test", respuesta.host());
        assertEquals(20, respuesta.ms());
        assertTrue(respuesta.iteraciones() > 0);
    }
}
