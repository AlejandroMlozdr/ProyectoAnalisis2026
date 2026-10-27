package com.umg.seguridad.config;

import com.umg.seguridad.dto.ModuloConMenus;
import com.umg.seguridad.service.PermisoService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Agrega el atributo "menuLateral" al Model de TODAS las vistas (aplica a
 * cualquier @Controller), para que layout.html pueda pintar el sidebar
 * dinamico sin que cada controlador tenga que acordarse de inyectarlo a mano.
 *
 * Para el usuario no autenticado (ej. la pantalla de login, que no usa
 * layout.html) PermisoService.construirMenu() simplemente devuelve una lista
 * vacia, asi que no rompe nada.
 */
@ControllerAdvice
public class MenuGlobalAdvice {

    private final PermisoService permisoService;

    public MenuGlobalAdvice(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @ModelAttribute("menuLateral")
    public List<ModuloConMenus> menuLateral() {
        return permisoService.construirMenu();
    }
}
