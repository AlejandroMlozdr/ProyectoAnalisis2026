package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.LiquidacionService;
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
@RequestMapping("/liquidacion")
public class LiquidacionController {

    private final LiquidacionService service;
    private final EmpleadoRepository empleadoRepository;
    private final PuestoRepository puestoRepository;

    public LiquidacionController(LiquidacionService service, EmpleadoRepository empleadoRepository, PuestoRepository puestoRepository) {
        this.service = service;
        this.empleadoRepository = empleadoRepository;
        this.puestoRepository = puestoRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "liquidacion/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new Liquidacion());
        cargarCombos(model);
        return "liquidacion/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("item", service.obtenerPorId(id));
        cargarCombos(model);
        return "liquidacion/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") Liquidacion item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/liquidacion";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            return "liquidacion/form";
        }
    }

    // Al elegir un empleado en el formulario se precargan sus datos laborales y salariales.
    @GetMapping("/datos-empleado/{id}")
    @ResponseBody
    public Map<String, Object> datosEmpleado(@PathVariable Integer id) {
        Map<String, Object> datos = new LinkedHashMap<>();
        empleadoRepository.findById(id).ifPresent(emp -> {
            datos.put("fechaContratacion", String.valueOf(emp.getFechaContratacion()));
            datos.put("idPuesto", emp.getPuesto().getIdPuesto());
            datos.put("sueldoBase", emp.getIngresoSueldoBase());
            datos.put("bonificacion", emp.getIngresoBonificacionDecreto());
            datos.put("otrosIngresos", emp.getIngresoOtrosIngresos());
            datos.put("igss", emp.getDescuentoIgss());
            datos.put("isr", emp.getDescuentoIsr());
            datos.put("inasistencias", emp.getDescuentoInasistencias());
        });
        return datos;
    }

    private void cargarCombos(Model model) {
        model.addAttribute("empleadoOpciones", empleadoRepository.findAll());
        model.addAttribute("puestoOpciones", puestoRepository.findAll());
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Empleado", "Puesto", "Fecha de contratación", "Fecha de egreso", "Fecha de liquidación", "Motivo de egreso", "Sueldo base", "Bonificación Decreto", "Otros ingresos", "Descuento IGSS", "Descuento ISR", "Descuento por inasistencias", "Total ingresos", "Total descuentos", "Total neto");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        ReporteUtil.safe(() -> { var x = e.getEmpleado(); return x.getPersona().getNombre() + " " + x.getPersona().getApellido(); }),
                        ReporteUtil.safe(() -> { var x = e.getPuesto(); return x.getNombre(); }),
                        ReporteUtil.str(e.getFechaContratacion()),
                        ReporteUtil.str(e.getFechaEgreso()),
                        ReporteUtil.str(e.getFechaLiquidacion()),
                        e.getMotivoEgreso(),
                        ReporteUtil.money(e.getIngresoSueldoBase()),
                        ReporteUtil.money(e.getIngresoBonificacionDecreto()),
                        ReporteUtil.money(e.getIngresoOtrosIngresos()),
                        ReporteUtil.money(e.getDescuentoIgss()),
                        ReporteUtil.money(e.getDescuentoIsr()),
                        ReporteUtil.money(e.getDescuentoInasistencias()),
                        ReporteUtil.money(e.getTotalIngresos()),
                        ReporteUtil.money(e.getTotalDescuentos()),
                        ReporteUtil.money(e.getTotalNeto())))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Liquidación de Empleado", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=liquidacion.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Liquidación de Empleado", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=liquidacion.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
