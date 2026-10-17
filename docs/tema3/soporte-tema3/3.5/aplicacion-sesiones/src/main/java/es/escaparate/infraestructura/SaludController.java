package es.escaparate.infraestructura;

import es.escaparate.infraestructura.dto.SaludResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

/**
 * Expone comprobaciones separadas de liveness y readiness para orquestadores y balanceadores.
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Crea el controlador y configura el timeout de la comprobación de base de datos.
     *
     * @param dataSource origen de datos configurado por Spring
     * @param timeoutSegundos timeout máximo de la consulta de readiness
     */
    public SaludController(
            DataSource dataSource,
            @Value("${app.health.db-timeout-seconds:2}") int timeoutSegundos) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setQueryTimeout(Math.max(1, timeoutSegundos));
    }

    /**
     * Comprueba únicamente que el proceso de la aplicación está respondiendo.
     * No consulta dependencias externas.
     *
     * @return estado {@code ok} mientras el proceso pueda atender la petición
     */
    @GetMapping("/vivo")
    public SaludResponse vivo() {
        return new SaludResponse("ok");
    }

    /**
     * Comprueba que la aplicación está preparada para atender tráfico que requiera
     * acceso a PostgreSQL.
     *
     * @return 200 con estado {@code ok} o 503 con estado {@code degradado}
     */
    @GetMapping("/listo")
    public ResponseEntity<SaludResponse> listo() {
        try {
            Integer resultado = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (resultado != null && resultado == 1) {
                return ResponseEntity.ok(new SaludResponse("ok"));
            }
        } catch (DataAccessException ex) {
            // Una dependencia no disponible debe producir 503, no un error 500.
        }

        return ResponseEntity.status(503).body(new SaludResponse("degradado"));
    }
}
