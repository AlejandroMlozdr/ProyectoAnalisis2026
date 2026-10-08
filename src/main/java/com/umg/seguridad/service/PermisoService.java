package com.umg.seguridad.service;

import com.umg.seguridad.dto.MenuConOpciones;
import com.umg.seguridad.dto.ModuloConMenus;
import com.umg.seguridad.dto.OpcionMenuItem;
import com.umg.seguridad.entity.RoleOpcion;
import com.umg.seguridad.repository.RoleOpcionRepository;
import com.umg.seguridad.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Corazon del RBAC del sistema.
 *
 * Responsabilidades:
 *  1. Construir el menu dinamico (Modulo -> Menu -> Opcion) a partir de las
 *     RoleOpcion del rol del usuario autenticado, para pintar el sidebar y
 *     la consola.
 *  2. Responder, para una "pagina" dada (el mismo valor guardado en
 *     OPCION.Pagina, ej. "/empresa"), si el rol actual puede Consultar,
 *     Alta, Baja, Cambio, Imprimir o Exportar. Estos metodos se usan tanto
 *     desde las vistas Thymeleaf (@{@permisoService.puedeAlta('/empresa')})
 *     como desde PermisoInterceptor para la validacion del lado del servidor.
 *
 * IMPORTANTE - convencion de OPCION.Pagina: debe contener la misma ruta base
 * que usa el @RequestMapping del controlador de esa pantalla (ej. "/empresa",
 * "/sucursal", "/status-usuario", "/role-opcion"). Se normaliza (trim, agrega
 * "/" inicial si falta, quita "/" final, minusculas) asi que no importa si en
 * la base de datos quedo con mayusculas o sin la barra inicial.
 *
 * Cache: los permisos de un rol casi no cambian (solo cuando el administrador
 * guarda la pantalla "Asignar Opciones a un Rol"), asi que se cachean en
 * memoria por idRole y se invalidan explicitamente desde RoleOpcionService
 * cuando se guardan cambios para ese rol.
 */
@Service
public class PermisoService {

    private static final Logger log = LoggerFactory.getLogger(PermisoService.class);

    private final UsuarioRepository usuarioRepository;
    private final RoleOpcionRepository roleOpcionRepository;

    private final Map<Integer, Map<String, PermisoPagina>> cachePermisosPorRole = new ConcurrentHashMap<>();
    private final Map<Integer, List<ModuloConMenus>> cacheMenuPorRole = new ConcurrentHashMap<>();

    public PermisoService(UsuarioRepository usuarioRepository, RoleOpcionRepository roleOpcionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.roleOpcionRepository = roleOpcionRepository;
    }

    // ---------- API publica: consultas de permiso ----------

    public boolean puedeConsultar(String pagina) {
        return existeEnMapa(pagina);
    }

    public boolean puedeAlta(String pagina) {
        return chequear(pagina, PermisoPagina::alta);
    }

    public boolean puedeBaja(String pagina) {
        return chequear(pagina, PermisoPagina::baja);
    }

    public boolean puedeCambio(String pagina) {
        return chequear(pagina, PermisoPagina::cambio);
    }

    public boolean puedeImprimir(String pagina) {
        return chequear(pagina, PermisoPagina::imprimir);
    }

    public boolean puedeExportar(String pagina) {
        return chequear(pagina, PermisoPagina::exportar);
    }

    /** Menu dinamico (Modulo -> Menu -> Opcion) del rol del usuario autenticado. */
    public List<ModuloConMenus> construirMenu() {
        Integer idRole = idRoleActual();
        if (idRole == null) {
            return List.of();
        }
        return cacheMenuPorRole.computeIfAbsent(idRole, this::cargarMenuDesdeBD);
    }

    /** Invalida el cache de un rol. Se llama desde RoleOpcionService al guardar permisos. */
    public void invalidarCache(Integer idRole) {
        if (idRole == null) return;
        cachePermisosPorRole.remove(idRole);
        cacheMenuPorRole.remove(idRole);
    }

    // ---------- Internos ----------

    private boolean existeEnMapa(String pagina) {
        Integer idRole = idRoleActual();
        if (idRole == null) return false;
        return permisosDelRole(idRole).containsKey(normalizar(pagina));
    }

    private boolean chequear(String pagina, Predicate<PermisoPagina> condicion) {
        Integer idRole = idRoleActual();
        if (idRole == null) return false;
        PermisoPagina permiso = permisosDelRole(idRole).get(normalizar(pagina));
        return permiso != null && condicion.test(permiso);
    }

    private Map<String, PermisoPagina> permisosDelRole(Integer idRole) {
        return cachePermisosPorRole.computeIfAbsent(idRole, this::cargarPermisosDesdeBD);
    }

    // Nota: no se marca @Transactional aqui porque, al invocarse por auto-referencia
    // (this::cargarPermisosDesdeBD dentro de computeIfAbsent), el proxy de Spring no
    // intercepta la llamada y la anotacion no tendria efecto. No hace falta de todas
    // formas: findConDetallePorRole ya trae Opcion/Menu/Modulo con JOIN FETCH en una
    // sola consulta (transaccional por si sola, via Spring Data), asi que no hay
    // acceso perezoso pendiente despues de que el metodo retorna.
    private Map<String, PermisoPagina> cargarPermisosDesdeBD(Integer idRole) {
        Map<String, PermisoPagina> mapa = new LinkedHashMap<>();
        for (RoleOpcion ro : roleOpcionRepository.findConDetallePorRole(idRole)) {
            String pagina = normalizar(ro.getOpcion().getPagina());
            if (pagina == null) {
                log.warn("Opcion '{}' (id={}) no tiene Pagina configurada en la BD: no participara en la autorizacion ni en el menu hasta que se corrija.",
                        ro.getOpcion().getNombre(), ro.getOpcion().getIdOpcion());
                continue; // Opcion sin pagina asociada: no participa en la autorizacion
            }
            mapa.put(pagina, new PermisoPagina(
                    ro.puedeAlta(), ro.puedeBaja(), ro.puedeCambio(), ro.puedeImprimir(), ro.puedeExportar()));
        }
        return mapa;
    }

    /**
     * Agrupa las RoleOpcion del rol en el arbol Modulo -> Menu -> Opcion,
     * ordenado por OrdenMenu. Se excluye la opcion de pagina "/consola"
     * porque esa se muestra siempre, fija, arriba del sidebar (no depende
     * de permisos).
     */
    // Misma nota que cargarPermisosDesdeBD: sin @Transactional por auto-invocacion,
    // pero no se necesita porque el JOIN FETCH ya trae todo en una sola consulta.
    private List<ModuloConMenus> cargarMenuDesdeBD(Integer idRole) {
        // LinkedHashMap para conservar el orden que ya viene de la consulta (ORDER BY ordenMenu)
        Map<Integer, String> nombreModuloPorId = new LinkedHashMap<>();
        Map<Integer, Integer> moduloDeMenu = new LinkedHashMap<>();
        Map<Integer, String> nombreMenuPorId = new LinkedHashMap<>();
        Map<Integer, List<OpcionMenuItem>> opcionesPorMenu = new LinkedHashMap<>();

        for (RoleOpcion ro : roleOpcionRepository.findConDetallePorRole(idRole)) {
            var opcion = ro.getOpcion();
            var menu = opcion.getMenu();
            var modulo = menu.getModulo();

            String pagina = normalizar(opcion.getPagina());
            if (pagina == null) {
                continue; // Opcion sin Pagina definida en la BD: no se puede enlazar, se omite del menu
            }
            if ("/consola".equals(pagina)) {
                continue; // se muestra fijo, fuera del arbol dinamico
            }

            nombreModuloPorId.putIfAbsent(modulo.getIdModulo(), modulo.getNombre());
            nombreMenuPorId.putIfAbsent(menu.getIdMenu(), menu.getNombre());
            moduloDeMenu.putIfAbsent(menu.getIdMenu(), modulo.getIdModulo());

            opcionesPorMenu.computeIfAbsent(menu.getIdMenu(), k -> new ArrayList<>())
                    .add(new OpcionMenuItem(opcion.getIdOpcion(), opcion.getNombre(), pagina, icono(pagina)));
        }

        // Menus agrupados por modulo, en el orden en que aparecieron
        Map<Integer, List<MenuConOpciones>> menusPorModulo = new LinkedHashMap<>();
        for (Integer idMenu : nombreMenuPorId.keySet()) {
            Integer idModulo = moduloDeMenu.get(idMenu);
            MenuConOpciones menuConOpciones = new MenuConOpciones(
                    idMenu, nombreMenuPorId.get(idMenu), opcionesPorMenu.getOrDefault(idMenu, List.of()));
            menusPorModulo.computeIfAbsent(idModulo, k -> new ArrayList<>()).add(menuConOpciones);
        }

        List<ModuloConMenus> resultado = new ArrayList<>();
        for (Integer idModulo : nombreModuloPorId.keySet()) {
            resultado.add(new ModuloConMenus(
                    idModulo, nombreModuloPorId.get(idModulo), menusPorModulo.getOrDefault(idModulo, List.of())));
        }
        return resultado;
    }

    private Integer idRoleActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return usuarioRepository.findById(auth.getName())
                .map(u -> u.getRole() != null ? u.getRole().getIdRole() : null)
                .orElse(null);
    }

    /** Normaliza el valor de OPCION.Pagina para que la comparacion sea consistente sin importar como se haya digitado. */
    private String normalizar(String pagina) {
        if (pagina == null || pagina.isBlank()) return null;
        String limpio = pagina.trim().toLowerCase();
        if (!limpio.startsWith("/")) limpio = "/" + limpio;
        if (limpio.length() > 1 && limpio.endsWith("/")) limpio = limpio.substring(0, limpio.length() - 1);
        return limpio;
    }

    /** Icono de Bootstrap Icons para cada pantalla conocida; generico si no hay match (nunca null). */
    private String icono(String pagina) {
        if (pagina == null) return "bi-dot";
        return switch (pagina) {
            case "/empresa" -> "bi-building";
            case "/sucursal" -> "bi-signpost-split";
            case "/genero" -> "bi-people";
            case "/status-usuario" -> "bi-person-check";
            case "/role" -> "bi-person-badge";
            case "/modulo" -> "bi-grid-3x3-gap";
            case "/menu" -> "bi-list-nested";
            case "/opcion" -> "bi-ui-checks";
            case "/usuario" -> "bi-person-lines-fill";
            case "/role-opcion" -> "bi-key";
            default -> "bi-dot";
        };
    }

    /** Flags de permiso de un rol sobre una pagina puntual. Solo se usa dentro de este servicio (cache interna). */
    private record PermisoPagina(boolean alta, boolean baja, boolean cambio, boolean imprimir, boolean exportar) {
    }
}
