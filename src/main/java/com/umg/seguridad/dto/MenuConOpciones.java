package com.umg.seguridad.dto;

import java.util.List;

/** Un Menu (ej. "Parametros Generales") con las Opciones que el rol actual puede ver. */
public record MenuConOpciones(
        Integer idMenu,
        String nombre,
        List<OpcionMenuItem> opciones
) {
}
