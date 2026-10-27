package com.umg.seguridad.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Al loguear con exito: deja constancia de la sesion activa en USUARIO
 * (sesionActual, ultimaFechaIngreso), reinicia el contador de intentos
 * fallidos y registra el evento en BITACORA_ACCESO.
 */
@Component
public class SesionAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SesionRegistroService sesionRegistroService;

    public SesionAuthenticationSuccessHandler(SesionRegistroService sesionRegistroService) {
        this.sesionRegistroService = sesionRegistroService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        String sesionId = request.getSession().getId();
        sesionRegistroService.registrarLogin(authentication.getName(), sesionId, request);
        response.sendRedirect(request.getContextPath() + "/consola");
    }
}
