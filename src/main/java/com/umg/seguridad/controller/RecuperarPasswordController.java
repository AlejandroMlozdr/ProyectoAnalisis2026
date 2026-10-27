package com.umg.seguridad.controller;

import com.umg.seguridad.entity.Usuario;
import com.umg.seguridad.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/recuperar-password")
public class RecuperarPasswordController {

    private static final String USUARIO_RECUPERACION = "usuarioRecuperacion";
    private final UsuarioService usuarioService;

    public RecuperarPasswordController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String mostrarSolicitud(HttpSession session) {
        session.removeAttribute(USUARIO_RECUPERACION);
        return "recuperar-password/solicitud";
    }

    @PostMapping("/buscar")
    public String buscarUsuario(@RequestParam String idUsuario,
                                HttpSession session,
                                Model model) {
        try {
            Usuario usuario = usuarioService.obtenerParaRecuperacion(idUsuario.trim());
            session.setAttribute(USUARIO_RECUPERACION, usuario.getIdUsuario());
            cargarPregunta(model, usuario);
            return "recuperar-password/pregunta";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("idUsuario", idUsuario);
            return "recuperar-password/solicitud";
        }
    }

    @PostMapping("/restablecer")
    public String restablecerPassword(@RequestParam String respuesta,
                                      @RequestParam String nuevaPassword,
                                      @RequestParam String confirmarPassword,
                                      HttpSession session,
                                      Model model) {
        String idUsuario = (String) session.getAttribute(USUARIO_RECUPERACION);
        if (idUsuario == null) {
            return "redirect:/recuperar-password";
        }

        try {
            if (!nuevaPassword.equals(confirmarPassword)) {
                throw new IllegalArgumentException("Las contraseñas no coinciden.");
            }

            usuarioService.restablecerPassword(idUsuario, respuesta, nuevaPassword);
            session.removeAttribute(USUARIO_RECUPERACION);
            return "redirect:/login?passwordRestablecido";
        } catch (IllegalArgumentException e) {
            Usuario usuario = usuarioService.obtenerParaRecuperacion(idUsuario);
            cargarPregunta(model, usuario);
            model.addAttribute("error", e.getMessage());
            return "recuperar-password/pregunta";
        }
    }

    private void cargarPregunta(Model model, Usuario usuario) {
        model.addAttribute("idUsuario", usuario.getIdUsuario());
        model.addAttribute("pregunta", usuario.getPregunta());
    }
}
