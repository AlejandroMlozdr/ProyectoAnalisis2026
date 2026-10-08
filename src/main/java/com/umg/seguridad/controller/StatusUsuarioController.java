package com.umg.seguridad.controller;

import com.umg.seguridad.entity.StatusUsuario;
import com.umg.seguridad.service.StatusUsuarioService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/status-usuario")
public class StatusUsuarioController {

    private final StatusUsuarioService statusUsuarioService;

    public StatusUsuarioController(StatusUsuarioService statusUsuarioService) {
        this.statusUsuarioService = statusUsuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("statusList", statusUsuarioService.listar());
        return "status-usuario/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("status", new StatusUsuario());
        return "status-usuario/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("status", statusUsuarioService.obtenerPorId(id));
        return "status-usuario/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute StatusUsuario status) {
        statusUsuarioService.guardar(status);
        return "redirect:/status-usuario";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        statusUsuarioService.eliminar(id);
        return "redirect:/status-usuario";
    }

    private static final List<String> COLUMNAS = List.of("Nombre");

    private List<List<String>> filas() {
        return statusUsuarioService.listar().stream()
                .map(s -> ReporteUtil.fila(s.getNombre()))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Estatus de Usuario", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estatus-usuario.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Estatus Usuario", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estatus-usuario.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
