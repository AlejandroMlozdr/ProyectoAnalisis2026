package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.DepartamentoService;
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
@RequestMapping("/departamento")
public class DepartamentoController {

    private final DepartamentoService service;
    private final EmpresaRepository empresaRepository;

    public DepartamentoController(DepartamentoService service, EmpresaRepository empresaRepository) {
        this.service = service;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "departamento/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new Departamento());
        cargarCombos(model);
        return "departamento/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("item", service.obtenerPorId(id));
        cargarCombos(model);
        return "departamento/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") Departamento item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/departamento";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            return "departamento/form";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            service.eliminar(id);
            redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el departamento porque tiene puestos asociados.");
        }
        return "redirect:/departamento";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("empresaOpciones", empresaRepository.findAll(Sort.by("nombre")));
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Nombre", "Empresa");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        e.getNombre(),
                        ReporteUtil.safe(() -> { var x = e.getEmpresa(); return x.getNombre(); })))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Departamentos", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=departamento.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Departamentos", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=departamento.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}