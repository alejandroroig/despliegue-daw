package es.escaparate.api;

import es.escaparate.almacenamiento.AlmacenamientoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduce excepciones habituales de la aplicación a respuestas HTTP uniformes.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Constructor sin estado para el componente de tratamiento de errores.
     */
    public ApiExceptionHandler() {
        // Constructor explícito para documentar la API pública del componente.
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail solicitudNoValida(IllegalArgumentException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Solicitud no válida");
        return problema;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail imagenDemasiadoGrande(MaxUploadSizeExceededException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONTENT_TOO_LARGE,
                "La imagen no puede superar los 5 MB."
        );
        problema.setTitle("Imagen demasiado grande");
        return problema;
    }

    @ExceptionHandler(AlmacenamientoException.class)
    ProblemDetail errorDeAlmacenamiento(AlmacenamientoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se ha podido completar la operación con la imagen."
        );
        problema.setTitle("Error de almacenamiento");
        return problema;
    }
}
