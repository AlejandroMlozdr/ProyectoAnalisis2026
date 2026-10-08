package com.umg.seguridad.security;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Detecta cuando una sesion HTTP muere por inactividad (timeout del contenedor,
 * ver server.servlet.session.timeout), a diferencia de un logout explicito
 * -que ya deja sesionActual en null antes de llegar aqui, ver SesionLogoutFilter-.
 * La decision de si fue "inactividad" o no la toma SesionRegistroService,
 * comparando el id de esta sesion contra Usuario.sesionActual.
 *
 * No hay HttpServletRequest disponible en este evento, por lo que direccionIp
 * y httpUserAgent quedan sin dato para este tipo de bitacora.
 */
@Component
public class SesionExpiradaListener implements HttpSessionListener {

    private final SesionRegistroService sesionRegistroService;

    public SesionExpiradaListener(SesionRegistroService sesionRegistroService) {
        this.sesionRegistroService = sesionRegistroService;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        Object atributo = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (!(atributo instanceof SecurityContext securityContext)) {
            return;
        }
        Authentication authentication = securityContext.getAuthentication();
        if (authentication == null) {
            return;
        }
        sesionRegistroService.registrarExpiracionSiSigueActiva(authentication.getName(), session.getId());
    }
}
