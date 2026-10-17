package es.escaparate.infraestructura;

import es.escaparate.almacenamiento.AlmacenamientoImagenes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Centraliza la identidad técnica de la instancia que está atendiendo una petición.
 * Es útil para hacer visible el balanceo entre varias réplicas de la aplicación.
 */
@Component
public class IdentidadInstancia {

    private final String nombreConfigurado;
    private final String version;
    private final AlmacenamientoImagenes almacenamientoImagenes;

    /**
     * Construye la identidad con valores configurables por entorno.
     *
     * @param nombreConfigurado nombre opcional de la réplica
     * @param version versión lógica de la aplicación
     * @param almacenamientoImagenes almacenamiento activo de imágenes
     */
    public IdentidadInstancia(
            @Value("${app.instance.name:}") String nombreConfigurado,
            @Value("${app.version:desarrollo}") String version,
            AlmacenamientoImagenes almacenamientoImagenes) {
        this.nombreConfigurado = nombreConfigurado;
        this.version = version;
        this.almacenamientoImagenes = almacenamientoImagenes;
    }

    /**
     * Obtiene el nombre visible de la réplica.
     *
     * @return nombre configurado, hostname del entorno o hostname del sistema
     */
    public String host() {
        if (nombreConfigurado != null && !nombreConfigurado.isBlank()) {
            return nombreConfigurado.trim();
        }

        String hostnameEntorno = System.getenv("HOSTNAME");
        if (hostnameEntorno != null && !hostnameEntorno.isBlank()) {
            return hostnameEntorno;
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "desconocido";
        }
    }

    /**
     * Obtiene la versión visible de la aplicación.
     *
     * @return versión configurada
     */
    public String version() {
        return version;
    }

    /**
     * Obtiene el tipo de almacenamiento de imágenes activo.
     *
     * @return identificador del almacenamiento
     */
    public String almacenamiento() {
        return almacenamientoImagenes.tipo();
    }
}
