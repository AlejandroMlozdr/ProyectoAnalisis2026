package com.umg.seguridad.controller;

import com.umg.seguridad.entity.Menu;
import com.umg.seguridad.service.MenuService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("menus", menuService.listar());
        return "menu/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("menu", new Menu());
        model.addAttribute("modulos", menuService.listarModulos());
        return "menu/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("menu", menuService.obtenerPorId(id));
        model.addAttribute("modulos", menuService.listarModulos());
        return "menu/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Menu menu) {
        menuService.guardar(menu);
        return "redirect:/menu";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        menuService.eliminar(id);
        return "redirect:/menu";
    }

    private static final List<String> COLUMNAS = List.of("Nombre", "Módulo", "Orden");

    private List<List<String>> filas() {
        return menuService.listar().stream()
                .map(m -> ReporteUtil.fila(
                        m.getNombre(),
                        m.getModulo() != null ? m.getModulo().getNombre() : "",
                        m.getOrdenMenu() != null ? m.getOrdenMenu().toString() : ""))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Menús", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=menus.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Menús", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=menus.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
