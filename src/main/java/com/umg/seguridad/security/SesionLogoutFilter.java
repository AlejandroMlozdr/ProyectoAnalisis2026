package com.umg.seguridad.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Se registra en SecurityConfig justo ANTES del LogoutFilter de Spring Security,
 * para leer y limpiar Usuario.sesionActual mientras la sesion HTTP todavia es
 * valida (el LogoutFilter la invalida despues). Asi, cuando SesionExpiradaListener
 * vea morir esa sesion, sesionActual ya esta en null y no la confunde con un
 * timeout por inactividad.
 *
 * IMPORTANTE: esta clase NO lleva @Transactional (ver SesionRegistroService) -
 * ponerlo directo en un Filter rompe su ciclo de vida con Tomcat porque
 * OncePerRequestFilter.doFilter()/GenericFilterBean.init() son metodos final
 * y Spring no puede crear un proxy de AOP sobre ellos.
 */
@Component
public class SesionLogoutFilter extends OncePerRequestFilter {

    private final RequestMatcher logoutMatcher = new AntPathRequestMatcher("/logout", "POST");
    private final SesionRegistroService sesionRegistroService;

    public SesionLogoutFilter(SesionRegistroService sesionRegistroService) {
        this.sesionRegistroService = sesionRegistroService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (logoutMatcher.matches(request)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            HttpSession session = request.getSession(false);
            if (authentication != null && session != null) {
                sesionRegistroService.registrarLogout(authentication.getName(), session.getId(), request);
            }
        }
        filterChain.doFilter(request, response);
    }
}
