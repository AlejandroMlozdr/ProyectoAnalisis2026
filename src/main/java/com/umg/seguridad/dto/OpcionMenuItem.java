package com.umg.seguridad.dto;

/**
 * Una Opcion (pantalla) ya resuelta para pintarla en el menu dinamico
 * (sidebar y tarjetas de la consola). Se arma dentro de PermisoService a
 * partir de RoleOpcion, dentro de la misma transaccion que hace el JOIN
 * FETCH, para no arrastrar entidades JPA "detached" fuera de la transaccion
 * (evita LazyInitializationException al leerlas despues, ya en la vista).
 */
public record OpcionMenuItem(
        Integer idOpcion,
        String nombre,
        String pagina,
        String icono
) {
}
