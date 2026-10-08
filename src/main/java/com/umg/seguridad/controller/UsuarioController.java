package com.umg.seguridad.controller;

import com.umg.seguridad.dto.SucursalDTO;
import com.umg.seguridad.entity.Usuario;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.UsuarioService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final GeneroRepository generoRepository;
    private final StatusUsuarioRepository statusUsuarioRepository;
    private final RoleRepository roleRepository;

    public UsuarioController(UsuarioService usuarioService, EmpresaRepository empresaRepository,
                              SucursalRepository sucursalRepository, GeneroRepository generoRepository,
                              StatusUsuarioRepository statusUsuarioRepository, RoleRepository roleRepository) {
        this.usuarioService = usuarioService;
        this.empresaRepository = empresaRepository;
        this.sucursalRepository = sucursalRepository;
        this.generoRepository = generoRepository;
        this.statusUsuarioRepository = statusUsuarioRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuario/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("esNuevo", true);
        cargarCombos(model);
        model.addAttribute("sucursales", List.of()); // se llenan por AJAX al elegir empresa
        model.addAttribute("empresaSeleccionada", (Integer) null);
        return "usuario/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        model.addAttribute("usuario", usuario);
        model.addAttribute("esNuevo", false);
        cargarCombos(model);

        Integer idEmpresaActual = usuario.getSucursal().getEmpresa().getIdEmpresa();
        model.addAttribute("sucursales", sucursalRepository.findByEmpresa_IdEmpresa(idEmpresaActual));
        model.addAttribute("empresaSeleccionada", idEmpresaActual);
        return "usuario/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario,
                           @RequestParam(required = false) String passwordPlano,
                           Model model) {
        try {
            usuarioService.guardar(usuario, passwordPlano);
            return "redirect:/usuario";
        } catch (IllegalArgumentException e) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("esNuevo", !usuarioExiste(usuario.getIdUsuario()));
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            Integer idEmpresaActual = usuario.getSucursal() != null && usuario.getSucursal().getEmpresa() != null
                    ? usuario.getSucursal().getEmpresa().getIdEmpresa() : null;
            model.addAttribute("sucursales", idEmpresaActual != null
                    ? sucursalRepository.findByEmpresa_IdEmpresa(idEmpresaActual) : List.of());
            model.addAttribute("empresaSeleccionada", idEmpresaActual);
            return "usuario/form";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable String id) {
        usuarioService.eliminar(id);
        return "redirect:/usuario";
    }

    // Boton "Forzar cambio de contraseña" en el listado: marca RequiereCambiarPassword=1
    // sin tener que abrir el formulario completo. Al proximo login, el usuario
    // queda obligado a cambiar su password antes de entrar (ver UsuarioDetailsService).
    @PostMapping("/caducar-password/{id}")
    public String caducarPassword(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.marcarPasswordVencida(id);
            redirectAttributes.addFlashAttribute("exito",
                    "Se forzó el cambio de contraseña para el usuario \"" + id + "\". " +
                            "Deberá cambiarla en su próximo inicio de sesión.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuario";
    }

    // Endpoint AJAX que usa el formulario para llenar el ComboBox de Sucursal
    // cuando el usuario elige una Empresa distinta.
    @GetMapping("/sucursales-por-empresa/{idEmpresa}")
    @ResponseBody
    public List<SucursalDTO> sucursalesPorEmpresa(@PathVariable Integer idEmpresa) {
        return sucursalRepository.findByEmpresa_IdEmpresa(idEmpresa).stream()
                .map(s -> new SucursalDTO(s.getIdSucursal(), s.getNombre()))
                .toList();
    }

    private void cargarCombos(Model model) {
        model.addAttribute("empresas", empresaRepository.findAll());
        model.addAttribute("generos", generoRepository.findAll());
        model.addAttribute("statusList", statusUsuarioRepository.findAll());
        model.addAttribute("roles", roleRepository.findAll());
    }

    private boolean usuarioExiste(String id) {
        try {
            usuarioService.obtenerPorId(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static final List<String> COLUMNAS = List.of("Usuario", "Nombre", "Correo", "Sucursal", "Rol", "Estatus");

    private List<List<String>> filas() {
        return usuarioService.listar().stream()
                .map(u -> ReporteUtil.fila(
                        u.getIdUsuario(),
                        u.getNombre() + " " + u.getApellido(),
                        u.getCorreoElectronico(),
                        u.getSucursal() != null ? u.getSucursal().getNombre() : "",
                        u.getRole() != null ? u.getRole().getNombre() : "",
                        u.getStatusUsuario() != null ? u.getStatusUsuario().getNombre() : ""))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Usuarios", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=usuarios.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Usuarios", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=usuarios.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
