package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.LiquidacionService;
import com.umg.seguridad.service.LiquidacionService.Prestaciones;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        Liquidacion item = new Liquidacion();
        item.setFechaLiquidacion(LocalDate.now());
        model.addAttribute("item", item);
        cargarCombos(model, item);
        return "liquidacion/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Liquidacion item = service.obtenerPorId(id);
        model.addAttribute("item", item);
        cargarCombos(model, item);
        return "liquidacion/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") Liquidacion item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Liquidación guardada correctamente. El status del empleado fue actualizado.");
            return "redirect:/liquidacion";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model, item);
            return "liquidacion/form";
        }
    }

    // Al elegir un empleado en el formulario se precargan sus datos laborales y salariales.
    // El descuento por inasistencias se sugiere con sus inasistencias pendientes de procesar.
    @GetMapping("/datos-empleado/{id}")
    @ResponseBody
    public Map<String, Object> datosEmpleado(@PathVariable Integer id,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEgreso) {
        Map<String, Object> datos = new LinkedHashMap<>();
        empleadoRepository.findById(id).ifPresent(emp -> {
            datos.put("fechaContratacion", String.valueOf(emp.getFechaContratacion()));
            datos.put("idPuesto", emp.getPuesto().getIdPuesto());
            datos.put("status", emp.getStatusEmpleado().getNombre());
            datos.put("sueldoBase", emp.getIngresoSueldoBase());
            datos.put("bonificacion", emp.getIngresoBonificacionDecreto());
            datos.put("otrosIngresos", emp.getIngresoOtrosIngresos());
            datos.put("igss", emp.getDescuentoIgss());
            datos.put("isr", emp.getDescuentoIsr());
            datos.put("diasInasistencia", service.diasInasistenciaPendientes(emp.getIdEmpleado(), fechaEgreso));
            datos.put("inasistencias", service.descuentoInasistenciasSugerido(emp, fechaEgreso));
        });
        return datos;
    }

    // Vista previa de prestaciones para el formulario (el calculo definitivo se hace al guardar).
    @GetMapping("/calcular")
    @ResponseBody
    public Map<String, Object> calcular(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaContratacion,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEgreso,
                                        @RequestParam(required = false) BigDecimal sueldo,
                                        @RequestParam(required = false) String motivo) {
        Prestaciones p = LiquidacionService.calcularPrestaciones(fechaContratacion, fechaEgreso, sueldo, motivo);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tiempoServicio", LiquidacionService.tiempoServicio(fechaContratacion, fechaEgreso));
        datos.put("indemnizacion", p.indemnizacion());
        datos.put("aguinaldo", p.aguinaldo());
        datos.put("bono14", p.bono14());
        datos.put("vacaciones", p.vacaciones());
        datos.put("totalPrestaciones", p.total());
        datos.put("statusDestino", motivo == null || motivo.isBlank() ? "" : LiquidacionService.statusDestino(motivo));
        return datos;
    }

    /**
     * Empleados que se pueden liquidar: los que no estan ya de "Baja" ni "Despedido".
     * En una edicion se incluye ademas el empleado del registro para mostrarlo.
     */
    private void cargarCombos(Model model, Liquidacion item) {
        Integer idActual = item.getEmpleado() != null ? item.getEmpleado().getIdEmpleado() : null;
        List<Empleado> empleados = empleadoRepository.findAll().stream()
                .filter(emp -> emp.getIdEmpleado().equals(idActual)
                        || (!LiquidacionService.STATUS_BAJA.equalsIgnoreCase(emp.getStatusEmpleado().getNombre())
                        && !LiquidacionService.STATUS_DESPEDIDO.equalsIgnoreCase(emp.getStatusEmpleado().getNombre())))
                .toList();
        model.addAttribute("empleadoOpciones", empleados);
        model.addAttribute("puestoOpciones", puestoRepository.findAll());
        model.addAttribute("motivoOpciones", LiquidacionService.MOTIVOS);
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Empleado", "Puesto", "Fecha de contratación", "Fecha de egreso", "Fecha de liquidación", "Motivo de egreso", "Sueldo base", "Bonificación Decreto", "Otros ingresos", "Descuento IGSS", "Descuento ISR", "Descuento por inasistencias", "Salario neto", "Total ingresos", "Total descuentos", "Total neto");

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
                        ReporteUtil.money(e.getSalarioNeto()),
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

    // ----- Finiquito individual (PDF). La ruta empieza con "exportar-pdf", asi que
    // PermisoInterceptor exige el permiso Imprimir, igual que el listado. -----

    @GetMapping("/exportar-pdf/{id}")
    public ResponseEntity<byte[]> finiquitoPdf(@PathVariable Integer id) {
        Liquidacion e = service.obtenerPorId(id);
        Prestaciones p = service.prestacionesDe(e);
        String nombre = e.getEmpleado().getPersona().getNombre() + " " + e.getEmpleado().getPersona().getApellido();
        String departamento = ReporteUtil.safe(() -> e.getPuesto().getDepartamento().getNombre());

        List<List<String>> filas = List.of(
                ReporteUtil.fila("Empleado", nombre),
                ReporteUtil.fila("Puesto", e.getPuesto().getNombre()),
                ReporteUtil.fila("Departamento", departamento),
                ReporteUtil.fila("Fecha de contratación", ReporteUtil.str(e.getFechaContratacion())),
                ReporteUtil.fila("Fecha de egreso", ReporteUtil.str(e.getFechaEgreso())),
                ReporteUtil.fila("Fecha de liquidación", ReporteUtil.str(e.getFechaLiquidacion())),
                ReporteUtil.fila("Motivo de egreso", e.getMotivoEgreso()),
                ReporteUtil.fila("Tiempo de servicio", LiquidacionService.tiempoServicio(e.getFechaContratacion(), e.getFechaEgreso())),
                ReporteUtil.fila("Sueldo base", "Q " + ReporteUtil.money(e.getIngresoSueldoBase())),
                ReporteUtil.fila("Bonificación Decreto", "Q " + ReporteUtil.money(e.getIngresoBonificacionDecreto())),
                ReporteUtil.fila("Otros ingresos", "Q " + ReporteUtil.money(e.getIngresoOtrosIngresos())),
                ReporteUtil.fila("Indemnización (Art. 82)", "Q " + ReporteUtil.money(p.indemnizacion())),
                ReporteUtil.fila("Aguinaldo proporcional", "Q " + ReporteUtil.money(p.aguinaldo())),
                ReporteUtil.fila("Bono 14 proporcional", "Q " + ReporteUtil.money(p.bono14())),
                ReporteUtil.fila("Vacaciones proporcionales", "Q " + ReporteUtil.money(p.vacaciones())),
                ReporteUtil.fila("TOTAL PRESTACIONES", "Q " + ReporteUtil.money(p.total())),
                ReporteUtil.fila("TOTAL INGRESOS", "Q " + ReporteUtil.money(e.getTotalIngresos())),
                ReporteUtil.fila("Descuento IGSS", "Q " + ReporteUtil.money(e.getDescuentoIgss())),
                ReporteUtil.fila("Descuento ISR", "Q " + ReporteUtil.money(e.getDescuentoIsr())),
                ReporteUtil.fila("Descuento por inasistencias", "Q " + ReporteUtil.money(e.getDescuentoInasistencias())),
                ReporteUtil.fila("TOTAL DESCUENTOS", "Q " + ReporteUtil.money(e.getTotalDescuentos())),
                ReporteUtil.fila("SALARIO NETO (último salario)", "Q " + ReporteUtil.money(e.getSalarioNeto())),
                ReporteUtil.fila("TOTAL NETO DE LA LIQUIDACIÓN", "Q " + ReporteUtil.money(e.getTotalNeto())));

        byte[] pdf = ReporteUtil.generarPdf("Finiquito de Liquidación - " + nombre, List.of("Concepto", "Valor"), filas);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finiquito-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
