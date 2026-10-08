package com.umg.seguridad.controller;

import com.umg.seguridad.entity.Empresa;
import com.umg.seguridad.service.EmpresaService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/empresa")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    // Consultar (listado)
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empresas", empresaService.listar());
        return "empresa/lista"; // templates/empresa/lista.html
    }

    // Formulario para Alta
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("empresa", new Empresa());
        return "empresa/form"; // templates/empresa/form.html
    }

    // Formulario para Cambio
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("empresa", empresaService.obtenerPorId(id));
        return "empresa/form";
    }

    // Alta o Cambio (mismo metodo: si trae id es cambio, si no, es alta)
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Empresa empresa) {
        empresaService.guardar(empresa);
        return "redirect:/empresa";
    }

    // Baja
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        empresaService.eliminar(id);
        return "redirect:/empresa";
    }

    private static final List<String> COLUMNAS = List.of("Nombre", "NIT", "Dirección");

    private List<List<String>> filas() {
        return empresaService.listar().stream()
                .map(e -> ReporteUtil.fila(e.getNombre(), e.getNit(), e.getDireccion()))
                .toList();
    }

    // Imprimir
    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Empresas", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=empresas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // Exportar
    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Empresas", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=empresas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
