package es.escaparate.infraestructura;

import es.escaparate.infraestructura.dto.InstanciaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la identidad de la réplica que atiende cada petición.
 */
@RestController
@RequestMapping("/api/instancia")
public class InstanciaController {

    private final IdentidadInstancia identidadInstancia;

    /**
     * Crea el controlador a partir de la identidad técnica de la réplica.
     *
     * @param identidadInstancia identidad técnica de la aplicación
     */
    public InstanciaController(IdentidadInstancia identidadInstancia) {
        this.identidadInstancia = identidadInstancia;
    }

    /**
     * Devuelve host, versión y tipo de almacenamiento de la réplica actual.
     *
     * @return información técnica de la instancia
     */
    @GetMapping
    public InstanciaResponse instancia() {
        return new InstanciaResponse(
                identidadInstancia.host(),
                identidadInstancia.version(),
                identidadInstancia.almacenamiento()
        );
    }
}
