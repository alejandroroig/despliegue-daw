package es.escaparate.infraestructura;

import es.escaparate.infraestructura.dto.CargaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint técnico que genera carga de CPU durante un intervalo controlado.
 */
@RestController
@RequestMapping("/api/carga")
public class CargaController {

    private final IdentidadInstancia identidadInstancia;
    private final long maximoMs;

    // Evita que el JIT pueda considerar prescindible todo el cálculo del bucle.
    private volatile long sumidero;

    /**
     * Crea el generador de carga con un límite máximo configurable.
     *
     * @param identidadInstancia identidad de la réplica que atenderá la petición
     * @param maximoMs duración máxima permitida para una petición de carga
     */
    public CargaController(
            IdentidadInstancia identidadInstancia,
            @Value("${app.load.max-ms:5000}") long maximoMs) {
        this.identidadInstancia = identidadInstancia;
        this.maximoMs = Math.max(1, maximoMs);
    }

    /**
     * Consume CPU en un único hilo durante el tiempo solicitado, limitado por configuración.
     *
     * @param ms duración solicitada en milisegundos
     * @return réplica que atendió la petición, duración aplicada e iteraciones ejecutadas
     * @throws IllegalArgumentException si la duración no es positiva
     */
    @GetMapping
    public CargaResponse generarCarga(@RequestParam(defaultValue = "1000") long ms) {
        if (ms < 1) {
            throw new IllegalArgumentException("El tiempo de carga debe ser mayor que cero.");
        }

        long duracionMs = Math.min(ms, maximoMs);
        long fin = System.nanoTime() + duracionMs * 1_000_000L;
        long iteraciones = 0;
        long valor = 0x9E3779B97F4A7C15L;

        while (System.nanoTime() < fin) {
            valor ^= iteraciones + 0x9E3779B97F4A7C15L + (valor << 6) + (valor >>> 2);
            valor = Long.rotateLeft(valor, 13);
            iteraciones++;
        }

        sumidero = valor;
        return new CargaResponse(identidadInstancia.host(), duracionMs, iteraciones);
    }
}
