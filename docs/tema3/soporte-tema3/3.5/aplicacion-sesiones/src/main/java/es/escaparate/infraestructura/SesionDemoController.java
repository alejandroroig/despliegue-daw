package es.escaparate.infraestructura;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Demostración de almacenamiento de sesión, sin autenticación real. */
@RestController
@RequestMapping("/api/sesion")
public class SesionDemoController {
    private String instancia() {
        return System.getenv().getOrDefault("APP_INSTANCE_NAME", "sin-identificador");
    }

    /** Crea una sesión didáctica para el usuario indicado. */
    @PostMapping("/abrir")
    public Map<String, String> abrir(@RequestParam(defaultValue = "demo") String usuario,
                                   HttpServletRequest request) {
        HttpSession sesion = request.getSession(true);
        sesion.setAttribute("usuario", usuario);
        return Map.of("usuario", usuario, "sesion", sesion.getId(), "instancia", instancia());
    }

    /** Consulta la sesión enviada en la cookie, sin crear una nueva. */
    @GetMapping("/estado")
    public ResponseEntity<Map<String, String>> estado(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            return ResponseEntity.status(401).body(Map.of(
                "estado", "sesion-no-encontrada", "instancia", instancia()));
        }
        return ResponseEntity.ok(Map.of("estado", "sesion-encontrada",
            "usuario", sesion.getAttribute("usuario").toString(),
            "sesion", sesion.getId(), "instancia", instancia()));
    }
}
