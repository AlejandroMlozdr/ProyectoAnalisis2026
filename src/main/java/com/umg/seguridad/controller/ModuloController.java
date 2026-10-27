package com.umg.seguridad.controller;

import com.umg.seguridad.entity.Modulo;
import com.umg.seguridad.service.ModuloService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/modulo")
public class ModuloController {

    private final ModuloService moduloService;

    public ModuloController(ModuloService moduloService) {
        this.moduloService = moduloService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("modulos", moduloService.listar());
        return "modulo/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("modulo", new Modulo());
        return "modulo/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("modulo", moduloService.obtenerPorId(id));
        return "modulo/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Modulo modulo) {
        moduloService.guardar(modulo);
        return "redirect:/modulo";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        moduloService.eliminar(id);
        return "redirect:/modulo";
    }

    private static final List<String> COLUMNAS = List.of("Nombre", "Orden");

    private List<List<String>> filas() {
        return moduloService.listar().stream()
                .map(m -> ReporteUtil.fila(m.getNombre(), m.getOrdenMenu() != null ? m.getOrdenMenu().toString() : ""))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Módulos", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=modulos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Módulos", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=modulos.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
