package com.umg.seguridad.controller;

import com.umg.seguridad.entity.CuentaBancariaEmpleado;
import com.umg.seguridad.entity.PeriodoPlanillaId;
import com.umg.seguridad.entity.PlanillaDetalle;
import com.umg.seguridad.service.PlanillaService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Boleta de pago de un empleado para un periodo ya procesado (consulta, impresion y exportacion). */
@Controller
@RequestMapping("/boleta-pago")
public class BoletaPagoController {

    private static final List<String> COLUMNAS = List.of("Concepto", "Valor");

    private final PlanillaService planillaService;

    public BoletaPagoController(PlanillaService planillaService) {
        this.planillaService = planillaService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String periodo,
                        @RequestParam(required = false) Integer idEmpleado,
                        Model model) {
        model.addAttribute("periodos", planillaService.listarCabeceras());
        model.addAttribute("periodoSel", periodo);
        model.addAttribute("empleadoSel", idEmpleado);
        if (periodo != null && !periodo.isBlank()) {
            try {
                PeriodoPlanillaId id = PlanillaService.parsePeriodo(periodo);
                planillaService.obtenerCabecera(id); // valida que el periodo este procesado
                List<PlanillaDetalle> detalles = planillaService.detallesDelPeriodo(id);
                model.addAttribute("detallesPeriodo", detalles);
                if (idEmpleado != null) {
                    PlanillaDetalle detalle = buscar(detalles, idEmpleado);
                    model.addAttribute("detalle", detalle);
                    model.addAttribute("cuenta", planillaService.cuentaActiva(idEmpleado).orElse(null));
                }
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "boleta-pago/index";
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf(@RequestParam String periodo, @RequestParam Integer idEmpleado) {
        try {
            PlanillaDetalle d = cargar(periodo, idEmpleado);
            byte[] pdf = ReporteUtil.generarPdf("Boleta de Pago " + d.getMes() + "/" + d.getAnio(),
                    COLUMNAS, filas(d, planillaService.cuentaActiva(idEmpleado).orElse(null)));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=boleta-" + periodo + "-" + idEmpleado + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel(@RequestParam String periodo, @RequestParam Integer idEmpleado) {
        try {
            PlanillaDetalle d = cargar(periodo, idEmpleado);
            byte[] excel = ReporteUtil.generarExcel("Boleta " + d.getMes() + "-" + d.getAnio(),
                    COLUMNAS, filas(d, planillaService.cuentaActiva(idEmpleado).orElse(null)));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=boleta-" + periodo + "-" + idEmpleado + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private PlanillaDetalle cargar(String periodo, Integer idEmpleado) {
        PeriodoPlanillaId id = PlanillaService.parsePeriodo(periodo);
        planillaService.obtenerCabecera(id);
        return buscar(planillaService.detallesDelPeriodo(id), idEmpleado);
    }

    private PlanillaDetalle buscar(List<PlanillaDetalle> detalles, Integer idEmpleado) {
        return detalles.stream()
                .filter(d -> d.getEmpleado().getIdEmpleado().equals(idEmpleado))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El empleado no tiene boleta en este período."));
    }

    private List<List<String>> filas(PlanillaDetalle d, CuentaBancariaEmpleado cuenta) {
        String cuentaTxt = cuenta == null ? "No registrada"
                : cuenta.getBanco().getNombre() + " - " + cuenta.getNumeroDeCuenta();
        return List.of(
                ReporteUtil.fila("Empleado", d.getEmpleado().getPersona().getNombre() + " " + d.getEmpleado().getPersona().getApellido()),
                ReporteUtil.fila("Puesto", d.getPuesto().getNombre()),
                ReporteUtil.fila("Departamento", d.getPuesto().getDepartamento().getNombre()),
                ReporteUtil.fila("Fecha de contratación", ReporteUtil.str(d.getFechaContratacion())),
                ReporteUtil.fila("Período de pago", d.getMes() + "/" + d.getAnio()),
                ReporteUtil.fila("Cuenta de depósito", cuentaTxt),
                ReporteUtil.fila("Sueldo base", "Q " + ReporteUtil.money(d.getIngresoSueldoBase())),
                ReporteUtil.fila("Bonificación Decreto", "Q " + ReporteUtil.money(d.getIngresoBonificacionDecreto())),
                ReporteUtil.fila("Otros ingresos", "Q " + ReporteUtil.money(d.getIngresoOtrosIngresos())),
                ReporteUtil.fila("TOTAL INGRESOS", "Q " + ReporteUtil.money(d.getTotalIngresos())),
                ReporteUtil.fila("Descuento IGSS", "Q " + ReporteUtil.money(d.getDescuentoIgss())),
                ReporteUtil.fila("Descuento ISR", "Q " + ReporteUtil.money(d.getDescuentoIsr())),
                ReporteUtil.fila("Descuento por inasistencias", "Q " + ReporteUtil.money(d.getDescuentoInasistencias())),
                ReporteUtil.fila("TOTAL DESCUENTOS", "Q " + ReporteUtil.money(d.getTotalDescuentos())),
                ReporteUtil.fila("SALARIO NETO", "Q " + ReporteUtil.money(d.getSalarioNeto())));
    }
}
