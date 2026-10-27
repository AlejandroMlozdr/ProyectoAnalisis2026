package com.umg.seguridad.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/consola")
    public String consola() {
        // El menu dinamico (modulos/menus/opciones segun el rol autenticado) ya
        // llega al Model de forma automatica via MenuGlobalAdvice + PermisoService;
        // no hace falta inyectar nada aqui.
        return "consola";
    }
}
