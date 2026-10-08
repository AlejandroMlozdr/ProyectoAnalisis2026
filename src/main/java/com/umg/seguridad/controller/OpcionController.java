package com.umg.seguridad.controller;

import com.umg.seguridad.entity.Opcion;
import com.umg.seguridad.service.OpcionService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/opcion")
public class OpcionController {

    private final OpcionService opcionService;

    public OpcionController(OpcionService opcionService) {
        this.opcionService = opcionService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("opciones", opcionService.listar());
        return "opcion/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("opcion", new Opcion());
        model.addAttribute("menus", opcionService.listarMenus());
        return "opcion/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("opcion", opcionService.obtenerPorId(id));
        model.addAttribute("menus", opcionService.listarMenus());
        return "opcion/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Opcion opcion) {
        opcionService.guardar(opcion);
        return "redirect:/opcion";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        opcionService.eliminar(id);
        return "redirect:/opcion";
    }

    private static final List<String> COLUMNAS = List.of("Nombre", "Menú", "Página", "Orden");

    private List<List<String>> filas() {
        return opcionService.listar().stream()
                .map(o -> ReporteUtil.fila(
                        o.getNombre(),
                        o.getMenu() != null ? o.getMenu().getNombre() : "",
                        o.getPagina(),
                        o.getOrdenMenu() != null ? o.getOrdenMenu().toString() : ""))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Opciones", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=opciones.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Opciones", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=opciones.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
