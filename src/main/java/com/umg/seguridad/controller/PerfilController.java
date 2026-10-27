package com.umg.seguridad.controller;

import com.umg.seguridad.entity.Usuario;
import com.umg.seguridad.service.UsuarioService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "Mi Perfil": pantalla personal de CADA usuario autenticado (no depende de
 * ROLE_OPCION, por eso "/perfil" esta en la lista de paginas excluidas de
 * PermisoInterceptor - cualquier usuario logueado puede ver y editar SU
 * PROPIO perfil, sin importar los permisos de su rol sobre el modulo Usuarios).
 */
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String verPerfil(Model model) {
        String idUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioService.obtenerPorId(idUsuario);
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(@RequestParam String passwordActual,
                                   @RequestParam String passwordNueva,
                                   @RequestParam String passwordConfirmacion,
                                   RedirectAttributes redirectAttributes) {
        String idUsuario = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!passwordNueva.equals(passwordConfirmacion)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas nuevas no coinciden.");
            return "redirect:/perfil";
        }

        try {
            // Reutiliza la misma logica que ya usa el cambio forzado por caducidad:
            // valida el password actual, valida la politica de la empresa,
            // encripta y guarda.
            usuarioService.cambiarPasswordPorCaducidad(idUsuario, passwordActual, passwordNueva);
            redirectAttributes.addFlashAttribute("exito", "Tu contraseña se actualizó correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/perfil";
    }
}
