package es.escaparate.almacenamiento;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementación de {@link AlmacenamientoImagenes} basada en un directorio del sistema de ficheros.
 *
 * <p>La ruta puede apuntar a disco local, a un volumen de contenedor o a un sistema
 * compartido montado por el sistema operativo sin cambiar esta implementación.</p>
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "filesystem", matchIfMissing = true)
public class FileSystemStorage implements AlmacenamientoImagenes {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, MediaType> TIPOS = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "webp", MediaType.parseMediaType("image/webp"),
            "gif", MediaType.IMAGE_GIF
    );

    private final Path raiz;

    /**
     * Prepara el directorio raíz configurado para las imágenes.
     *
     * @param ruta ruta del sistema de ficheros donde se guardarán las imágenes
     * @throws AlmacenamientoException si el directorio no puede prepararse
     */
    public FileSystemStorage(@Value("${app.storage.path:./uploads}") String ruta) {
        raiz = Path.of(ruta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se ha podido preparar el directorio de imágenes.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("La imagen está vacía.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("La imagen no puede superar los 5 MB.");
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Formato de imagen no permitido. Usa JPG, PNG, WEBP o GIF.");
        }

        String clave = UUID.randomUUID() + "." + extension;
        Path destino = resolverSeguro(clave);

        try (var entrada = archivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            return clave;
        } catch (IOException e) {
            throw new AlmacenamientoException("No se ha podido guardar la imagen.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ImagenAlmacenada> cargar(String clave) {
        if (clave == null || clave.isBlank()) {
            return Optional.empty();
        }

        Path fichero = resolverSeguro(clave);
        if (!Files.isRegularFile(fichero)) {
            return Optional.empty();
        }

        String extension = obtenerExtension(clave);
        MediaType tipoContenido = TIPOS.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM);
        return Optional.of(new ImagenAlmacenada(new FileSystemResource(fichero), tipoContenido));
    }

    /** {@inheritDoc} */
    @Override
    public void eliminar(String clave) {
        if (clave == null || clave.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(resolverSeguro(clave));
        } catch (IOException e) {
            throw new AlmacenamientoException("No se ha podido eliminar la imagen.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String tipo() {
        return "filesystem";
    }

    private Path resolverSeguro(String clave) {
        Path resultado = raiz.resolve(clave).normalize();
        if (!resultado.startsWith(raiz)) {
            throw new IllegalArgumentException("La ruta de la imagen no es válida.");
        }
        return resultado;
    }

    private String obtenerExtension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return "";
        }
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
