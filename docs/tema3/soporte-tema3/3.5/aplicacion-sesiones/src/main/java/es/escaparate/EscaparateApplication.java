package es.escaparate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Punto de entrada de la aplicación Escaparate.
 *
 * <p>Puede ejecutarse como aplicación Spring Boot o desplegarse como WAR
 * en un contenedor de servlets externo compatible.</p>
 */
@SpringBootApplication
public class EscaparateApplication extends SpringBootServletInitializer {

    /**
     * Crea la aplicación. Spring utiliza este constructor sin argumentos al
     * inicializar el contexto y los contenedores externos pueden utilizarlo al
     * desplegar el WAR.
     */
    public EscaparateApplication() {
        // Constructor explícito para documentar la API pública.
    }

    /**
     * Arranca Escaparate con el servidor embebido.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(EscaparateApplication.class, args);
    }

    /**
     * Configura la aplicación cuando el WAR se despliega en un contenedor externo.
     *
     * @param application constructor de la aplicación Spring
     * @return constructor configurado con la clase principal de Escaparate
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(EscaparateApplication.class);
    }
}
