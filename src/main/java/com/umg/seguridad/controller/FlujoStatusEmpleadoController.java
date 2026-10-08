package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.FlujoStatusEmpleadoService;
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
@RequestMapping("/flujo-status-empleado")
public class FlujoStatusEmpleadoController {

    private final FlujoStatusEmpleadoService service;
    private final StatusEmpleadoRepository statusEmpleadoRepository;

    public FlujoStatusEmpleadoController(FlujoStatusEmpleadoService service, StatusEmpleadoRepository statusEmpleadoRepository) {
        this.service = service;
        this.statusEmpleadoRepository = statusEmpleadoRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "flujo-status-empleado/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new FlujoStatusEmpleado());
        model.addAttribute("esNuevo", true);
        cargarCombos(model);
        return "flujo-status-empleado/form";
    }

    @GetMapping("/editar/{id1}/{id2}")
    public String editar(@PathVariable Integer id1, @PathVariable Integer id2, Model model) {
        model.addAttribute("item", service.obtener(id1, id2));
        model.addAttribute("esNuevo", false);
        cargarCombos(model);
        return "flujo-status-empleado/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") FlujoStatusEmpleado item,
                          @RequestParam(name = "esNuevo", defaultValue = "true") boolean esNuevo,
                          Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item, esNuevo);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/flujo-status-empleado";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("esNuevo", esNuevo);
            cargarCombos(model);
            return "flujo-status-empleado/form";
        }
    }

    @PostMapping("/eliminar/{id1}/{id2}")
    public String eliminar(@PathVariable Integer id1, @PathVariable Integer id2, RedirectAttributes redirectAttributes) {
        service.eliminar(id1, id2);
        redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        return "redirect:/flujo-status-empleado";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("statusActualOpciones", statusEmpleadoRepository.findAll());
        model.addAttribute("statusNuevoOpciones", statusEmpleadoRepository.findAll());
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Status actual", "Status nuevo", "Nombre del evento");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        ReporteUtil.safe(() -> { var x = e.getStatusActual(); return x.getNombre(); }),
                        ReporteUtil.safe(() -> { var x = e.getStatusNuevo(); return x.getNombre(); }),
                        e.getNombreEvento()))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Flujos Status Empleado", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=flujo-status-empleado.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Flujos Status Empleado", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=flujo-status-empleado.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
