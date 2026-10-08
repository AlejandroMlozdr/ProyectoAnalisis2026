package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.PersonaService;
import com.umg.seguridad.util.ReporteUtil;
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
@RequestMapping("/persona")
public class PersonaController {

    private final PersonaService service;
    private final GeneroRepository generoRepository;
    private final EstadoCivilRepository estadoCivilRepository;

    public PersonaController(PersonaService service, GeneroRepository generoRepository, EstadoCivilRepository estadoCivilRepository) {
        this.service = service;
        this.generoRepository = generoRepository;
        this.estadoCivilRepository = estadoCivilRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "persona/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new Persona());
        cargarCombos(model);
        return "persona/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("item", service.obtenerPorId(id));
        cargarCombos(model);
        return "persona/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") Persona item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/persona";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            return "persona/form";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        service.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        return "redirect:/persona";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("generoOpciones", generoRepository.findAll());
        model.addAttribute("estadoCivilOpciones", estadoCivilRepository.findAll());
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Nombre", "Apellido", "Fecha de nacimiento", "Género", "Dirección", "Teléfono", "Correo electrónico", "Estado civil");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        e.getNombre(),
                        e.getApellido(),
                        ReporteUtil.str(e.getFechaNacimiento()),
                        ReporteUtil.safe(() -> { var x = e.getGenero(); return x.getNombre(); }),
                        e.getDireccion(),
                        e.getTelefono(),
                        e.getCorreoElectronico(),
                        ReporteUtil.safe(() -> { var x = e.getEstadoCivil(); return x.getNombre(); })))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Personas", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=persona.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Personas", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=persona.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
