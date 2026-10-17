package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.InasistenciaService;
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
@RequestMapping("/inasistencia")
public class InasistenciaController {

    private final InasistenciaService service;
    private final EmpleadoRepository empleadoRepository;

    public InasistenciaController(InasistenciaService service, EmpleadoRepository empleadoRepository) {
        this.service = service;
        this.empleadoRepository = empleadoRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "inasistencia/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Inasistencia item = new Inasistencia();
        model.addAttribute("item", item);
        cargarCombos(model, item);
        return "inasistencia/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Inasistencia item = service.obtenerPorId(id);
            service.validarEditable(item);
            model.addAttribute("item", item);
            cargarCombos(model, item);
            return "inasistencia/form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/inasistencia";
        }
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") Inasistencia item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/inasistencia";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model, item);
            return "inasistencia/form";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            service.eliminar(id);
            redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inasistencia";
    }

    /**
     * Solo empleados "Activo" en el combo. En una edicion se incluye ademas el
     * empleado actual del registro aunque ya no este activo, para que el combo
     * lo muestre seleccionado.
     */
    private void cargarCombos(Model model, Inasistencia item) {
        Integer idActual = item.getEmpleado() != null ? item.getEmpleado().getIdEmpleado() : null;
        List<Empleado> empleados = empleadoRepository.findAll().stream()
                .filter(emp -> InasistenciaService.STATUS_ACTIVO.equalsIgnoreCase(emp.getStatusEmpleado().getNombre())
                        || emp.getIdEmpleado().equals(idActual))
                .toList();
        model.addAttribute("empleadoOpciones", empleados);
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Empleado", "Fecha inicial", "Fecha final", "Motivo", "Fecha procesado");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        ReporteUtil.safe(() -> { var x = e.getEmpleado(); return x.getPersona().getNombre() + " " + x.getPersona().getApellido(); }),
                        ReporteUtil.str(e.getFechaInicial()),
                        ReporteUtil.str(e.getFechaFinal()),
                        e.getMotivoInasistencia(),
                        e.getFechaProcesado() == null ? "Pendiente" : ReporteUtil.str(e.getFechaProcesado())))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Inasistencias de Empleados", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inasistencia.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Inasistencias de Empleados", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inasistencia.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
