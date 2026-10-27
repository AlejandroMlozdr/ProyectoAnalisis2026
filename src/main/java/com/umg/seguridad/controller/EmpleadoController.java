package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.EmpleadoService;
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
@RequestMapping("/empleado")
public class EmpleadoController {

    private final EmpleadoService service;
    private final PersonaRepository personaRepository;
    private final SucursalRepository sucursalRepository;
    private final PuestoRepository puestoRepository;
    private final StatusEmpleadoRepository statusEmpleadoRepository;

    public EmpleadoController(EmpleadoService service, PersonaRepository personaRepository, SucursalRepository sucursalRepository, PuestoRepository puestoRepository, StatusEmpleadoRepository statusEmpleadoRepository) {
        this.service = service;
        this.personaRepository = personaRepository;
        this.sucursalRepository = sucursalRepository;
        this.puestoRepository = puestoRepository;
        this.statusEmpleadoRepository = statusEmpleadoRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "empleado/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new Empleado());
        cargarCombos(model);
        return "empleado/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("item", service.obtenerPorId(id));
        cargarCombos(model);
        return "empleado/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") Empleado item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/empleado";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            return "empleado/form";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        service.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        return "redirect:/empleado";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("personaOpciones", personaRepository.findAll());
        model.addAttribute("sucursalOpciones", sucursalRepository.findAll());
        model.addAttribute("puestoOpciones", puestoRepository.findAll());
        model.addAttribute("statusEmpleadoOpciones", statusEmpleadoRepository.findAll());
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Persona", "Sucursal", "Fecha de contratación", "Puesto", "Status", "Sueldo base", "Bonificación Decreto", "Otros ingresos", "Descuento IGSS", "Descuento ISR", "Descuento por inasistencias");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        ReporteUtil.safe(() -> { var x = e.getPersona(); return x.getNombre() + " " + x.getApellido(); }),
                        ReporteUtil.safe(() -> { var x = e.getSucursal(); return x.getNombre(); }),
                        ReporteUtil.str(e.getFechaContratacion()),
                        ReporteUtil.safe(() -> { var x = e.getPuesto(); return x.getNombre(); }),
                        ReporteUtil.safe(() -> { var x = e.getStatusEmpleado(); return x.getNombre(); }),
                        ReporteUtil.money(e.getIngresoSueldoBase()),
                        ReporteUtil.money(e.getIngresoBonificacionDecreto()),
                        ReporteUtil.money(e.getIngresoOtrosIngresos()),
                        ReporteUtil.money(e.getDescuentoIgss()),
                        ReporteUtil.money(e.getDescuentoIsr()),
                        ReporteUtil.money(e.getDescuentoInasistencias())))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Empleados", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=empleado.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Empleados", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=empleado.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
