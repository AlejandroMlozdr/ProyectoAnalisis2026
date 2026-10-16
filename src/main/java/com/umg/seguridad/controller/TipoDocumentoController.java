package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.TipoDocumentoService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tipo-documento")
public class TipoDocumentoController {

    private final TipoDocumentoService service;

    public TipoDocumentoController(TipoDocumentoService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "tipo-documento/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new TipoDocumento());
        cargarCombos(model);
        return "tipo-documento/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("item", service.obtenerPorId(id));
        cargarCombos(model);
        return "tipo-documento/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") TipoDocumento item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/tipo-documento";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            return "tipo-documento/form";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            service.eliminar(id);
            redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el tipo de documento porque está asociado a documentos de personas.");
        }
        return "redirect:/tipo-documento";
    }

    private void cargarCombos(Model model) {
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Nombre");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        e.getNombre()))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Tipos de Documentos", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tipo-documento.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Tipos de Documentos", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tipo-documento.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}