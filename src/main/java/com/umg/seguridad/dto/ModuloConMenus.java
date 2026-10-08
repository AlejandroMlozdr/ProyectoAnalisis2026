package com.umg.seguridad.dto;

import java.util.List;

/**
 * Raiz del arbol del menu dinamico: un Modulo (ej. "Seguridad") con sus Menus,
 * y dentro de cada Menu las Opciones que el rol del usuario autenticado tiene
 * asignadas. Esto es exactamente lo que arma PermisoService.construirMenu()
 * y lo que consume layout.html para pintar el sidebar.
 */
public record ModuloConMenus(
        Integer idModulo,
        String nombre,
        List<MenuConOpciones> menus
) {
}
