package com.umg.seguridad.controller;

import com.umg.seguridad.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CambiarPasswordController {

    private final UsuarioService usuarioService;

    public CambiarPasswordController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/cambiar-password")
    public String mostrarFormulario(HttpSession session, Model model) {
        String username = (String) session.getAttribute("usuarioExpirado");
        if (username == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", username);
        return "cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String procesarCambio(HttpSession session,
                                  @RequestParam String passwordActual,
                                  @RequestParam String passwordNueva,
                                  @RequestParam String passwordConfirmacion,
                                  Model model) {
        String username = (String) session.getAttribute("usuarioExpirado");
        if (username == null) {
            return "redirect:/login";
        }
        if (!passwordNueva.equals(passwordConfirmacion)) {
            model.addAttribute("usuario", username);
            model.addAttribute("error", "Las contraseñas nuevas no coinciden.");
            return "cambiar-password";
        }
        try {
            usuarioService.cambiarPasswordPorCaducidad(username, passwordActual, passwordNueva);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("usuario", username);
            model.addAttribute("error", ex.getMessage());
            return "cambiar-password";
        }
        session.removeAttribute("usuarioExpirado");
        return "redirect:/login?passwordCambiada";
    }
}