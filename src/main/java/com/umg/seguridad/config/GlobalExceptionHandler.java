package com.umg.seguridad.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Convierte errores tecnicos en mensajes amigables para el usuario, sin tener
 * que repetir try/catch en cada uno de los 10 Service.eliminar(id).
 *
 * El caso principal: eliminar un registro que todavia tiene dependencias
 * (ej. borrar una Empresa que tiene Sucursales, o un Modulo que tiene Menus)
 * hoy lanza DataIntegrityViolationException desde la base de datos (viola una
 * FK constraint). Sin este manejador, eso se traduce en un Whitelabel Error
 * Page (HTTP 500) que no le dice nada util al usuario. Con este manejador,
 * se redirige de vuelta al listado con un mensaje claro.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String manejarDependenciaAlEliminar(DataIntegrityViolationException ex,
                                                HttpServletRequest request,
                                                RedirectAttributes redirectAttributes) {
        log.warn("Intento de eliminar un registro con dependencias en {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        redirectAttributes.addFlashAttribute("error",
                "No se puede eliminar este registro porque todavía tiene información relacionada " +
                        "en otra pantalla del sistema (por ejemplo, sucursales, usuarios u opciones que " +
                        "dependen de él). Elimina primero esos registros dependientes, o deja de usar este " +
                        "en vez de borrarlo.");

        return "redirect:" + paginaDeOrigen(request);
    }

    /**
     * Misma convencion que PermisoInterceptor/PermisoService: la "pagina" es
     * el primer segmento de la ruta (ej. "/empresa/eliminar/3" -> "/empresa"),
     * para volver siempre al listado de la pantalla donde ocurrio el error.
     */
    private String paginaDeOrigen(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String[] partes = path.split("/");
        return partes.length > 1 ? "/" + partes[1] : "/consola";
    }
}
