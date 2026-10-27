package com.umg.seguridad.config;

import com.umg.seguridad.service.PermisoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

/**
 * Aplica del lado del servidor los mismos permisos (Consultar/Alta/Baja/Cambio/
 * Imprimir/Exportar) que PermisoService usa para mostrar u ocultar botones en
 * las vistas. Ocultar un boton en el HTML no es seguridad real: cualquiera
 * puede escribir la URL a mano o mandar el POST con curl/Postman sin pasar
 * por la pantalla. Este interceptor es el que de verdad bloquea eso, sin
 * tener que repetir la validacion adentro de cada Service.
 *
 * Convencion: la "pagina" de cada pantalla es el primer segmento de su ruta,
 * ej. "/empresa/nuevo" -> pagina "/empresa". Debe coincidir con lo que se
 * guarda en OPCION.Pagina para esa pantalla (ver PermisoService).
 *
 * El resto de la ruta despues de la pagina decide que accion se exige:
 *   .../nuevo              -> Alta   (ver formulario de alta)
 *   .../editar/**          -> Cambio (ver formulario de cambio)
 *   .../guardar   (POST)   -> Alta o Cambio (el mismo endpoint sirve para ambos)
 *   .../eliminar/** (POST) -> Baja
 *   .../exportar-pdf       -> Imprimir
 *   .../exportar-excel     -> Exportar
 *   .../procesar (POST)    -> Alta   (ejecutar un proceso, ej. Calcular Planilla)
 *   .../caducar-password/** (POST) -> Cambio (forzar vencimiento de password es una modificacion)
 *   cualquier otra cosa (el listado, o un endpoint auxiliar como el combo
 *   AJAX de sucursales-por-empresa) -> Consultar
 */
@Component
public class PermisoInterceptor implements HandlerInterceptor {

    // Rutas que no pertenecen al esquema de permisos por Opcion (login, consola,
    // recursos estaticos, pantallas publicas, etc). La bitacora de auditoria
    // tampoco esta modelada como Opcion; su proteccion sigue siendo el
    // hasRole("ADMINISTRADOR") de SecurityConfig.
    private static final Set<String> PAGINAS_EXCLUIDAS = Set.of(
            "/login", "/logout", "/consola", "/cambiar-password", "/perfil",
            "/recuperar-password", "/auditoria", "/acceso-denegado",
            "/css", "/js", "/error"
    );

    private final PermisoService permisoService;

    public PermisoInterceptor(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Solo nos interesan las peticiones que van a un @Controller; recursos
        // estaticos (css/js/imagenes) los maneja otro handler y no llegan aqui
        // como HandlerMethod.
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        String[] partes = path.split("/");
        if (partes.length < 2) {
            return true; // "/" (raiz)
        }
        String pagina = "/" + partes[1].toLowerCase();

        if (PAGINAS_EXCLUIDAS.contains(pagina)) {
            return true;
        }

        String resto = partes.length > 2
                ? String.join("/", Arrays.copyOfRange(partes, 2, partes.length)).toLowerCase()
                : "";

        boolean permitido = evaluarPermiso(pagina, resto, request.getMethod());

        if (!permitido) {
            response.sendRedirect(request.getContextPath() + "/acceso-denegado");
            return false;
        }
        return true;
    }

    private boolean evaluarPermiso(String pagina, String resto, String metodoHttp) {
        if (resto.startsWith("nuevo")) {
            return permisoService.puedeAlta(pagina);
        }
        if (resto.startsWith("editar")) {
            return permisoService.puedeCambio(pagina);
        }
        if (resto.startsWith("eliminar")) {
            return permisoService.puedeBaja(pagina);
        }
        if (resto.startsWith("procesar")) {
            return permisoService.puedeAlta(pagina);
        }
        if (resto.startsWith("caducar-password")) {
            return permisoService.puedeCambio(pagina);
        }
        if (resto.startsWith("exportar-pdf")) {
            return permisoService.puedeImprimir(pagina);
        }
        if (resto.startsWith("exportar-excel")) {
            return permisoService.puedeExportar(pagina);
        }
        if (resto.startsWith("guardar")) {
            // El mismo endpoint /guardar atiende Alta y Cambio (segun si el
            // formulario trae id o no); alcanza con tener al menos uno de
            // los dos permisos para poder llegar a el.
            return permisoService.puedeAlta(pagina) || permisoService.puedeCambio(pagina);
        }
        // Listado principal o endpoints auxiliares de solo lectura (ej. combos AJAX)
        if ("GET".equalsIgnoreCase(metodoHttp)) {
            return permisoService.puedeConsultar(pagina);
        }
        // Cualquier otro verbo no contemplado sobre una pagina protegida: denegar por defecto.
        return false;
    }
}
