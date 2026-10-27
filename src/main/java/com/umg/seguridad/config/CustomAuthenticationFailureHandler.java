package com.umg.seguridad.config;

import com.umg.seguridad.entity.Usuario;
import com.umg.seguridad.security.SesionRegistroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Distingue los distintos motivos de fallo de login para dar el mensaje/redirect
 * correcto y dejar constancia en BITACORA_ACCESO (a traves de SesionRegistroService):
 *
 *  - CredentialsExpiredException: password vencida -> obliga a cambiarla (feature de "maje").
 *  - UsernameNotFoundException:   el usuario no existe.
 *  - DisabledException:           usuario inactivo o ya bloqueado.
 *  - Cualquier otro caso (normalmente BadCredentialsException): password incorrecta;
 *    si con este intento se llega al maximo permitido, se bloquea la cuenta (feature de "franco2").
 *
 * Requiere provider.setHideUserNotFoundExceptions(false) en SecurityConfig, si no
 * Spring "disfraza" el UsernameNotFoundException como BadCredentialsException y
 * nunca se distingue el evento "usuario no existe" del de "password incorrecta".
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final int MAX_INTENTOS_DEFAULT = 5;

    private final SesionRegistroService sesionRegistroService;

    public CustomAuthenticationFailureHandler(SesionRegistroService sesionRegistroService) {
        this.sesionRegistroService = sesionRegistroService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {

        String username = request.getParameter("username");

        if (exception instanceof CredentialsExpiredException) {
            request.getSession().setAttribute("usuarioExpirado", username);
            response.sendRedirect(request.getContextPath() + "/cambiar-password");
            return;
        }

        if (exception instanceof UsernameNotFoundException) {
            sesionRegistroService.registrarUsuarioNoExiste(username, request);
            response.sendRedirect(request.getContextPath() + "/login?error=usuario");
            return;
        }

        if (exception instanceof DisabledException) {
            sesionRegistroService.registrarUsuarioInactivo(username, request);
            response.sendRedirect(request.getContextPath() + "/login?error=inactivo");
            return;
        }

        // Password incorrecta (BadCredentialsException u otra causa no contemplada arriba).
        Usuario usuario = sesionRegistroService.registrarLoginFallido(username, request);
        if (usuario != null) {
            int maxIntentos = obtenerMaxIntentos(usuario);
            if (usuario.getIntentosDeAcceso() != null && usuario.getIntentosDeAcceso() >= maxIntentos) {
                sesionRegistroService.registrarBloqueo(usuario, request);
            }
        }

        response.sendRedirect(request.getContextPath() + "/login?error");
    }

    private int obtenerMaxIntentos(Usuario usuario) {
        try {
            Integer configurado = usuario.getSucursal().getEmpresa().getPasswordIntentosAntesDeBloquear();
            return (configurado != null && configurado > 0) ? configurado : MAX_INTENTOS_DEFAULT;
        } catch (Exception e) {
            return MAX_INTENTOS_DEFAULT;
        }
    }
}
