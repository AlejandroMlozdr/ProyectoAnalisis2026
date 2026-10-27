package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.DocumentoPersonaService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/documento-persona")
public class DocumentoPersonaController {

    private final DocumentoPersonaService service;
    private final PersonaRepository personaRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    public DocumentoPersonaController(DocumentoPersonaService service, PersonaRepository personaRepository, TipoDocumentoRepository tipoDocumentoRepository) {
        this.service = service;
        this.personaRepository = personaRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "documento-persona/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new DocumentoPersona());
        model.addAttribute("esNuevo", true);
        cargarCombos(model);
        return "documento-persona/form";
    }

    @GetMapping("/editar/{id1}/{id2}")
    public String editar(@PathVariable Integer id1, @PathVariable Integer id2, Model model) {
        model.addAttribute("item", service.obtener(id1, id2));
        model.addAttribute("esNuevo", false);
        cargarCombos(model);
        return "documento-persona/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") DocumentoPersona item,
                          @RequestParam(name = "esNuevo", defaultValue = "true") boolean esNuevo,
                          Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item, esNuevo);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/documento-persona";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("esNuevo", esNuevo);
            cargarCombos(model);
            return "documento-persona/form";
        }
    }

    @PostMapping("/eliminar/{id1}/{id2}")
    public String eliminar(@PathVariable Integer id1, @PathVariable Integer id2, RedirectAttributes redirectAttributes) {
        service.eliminar(id1, id2);
        redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        return "redirect:/documento-persona";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("personaOpciones", personaRepository.findAll());
        model.addAttribute("tipoDocumentoOpciones", tipoDocumentoRepository.findAll());
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Persona", "Tipo de documento", "Número de documento");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        ReporteUtil.safe(() -> { var x = e.getPersona(); return x.getNombre() + " " + x.getApellido(); }),
                        ReporteUtil.safe(() -> { var x = e.getTipoDocumento(); return x.getNombre(); }),
                        e.getNoDocumento()))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Documentos de Personas", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documento-persona.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Documentos de Personas", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documento-persona.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
