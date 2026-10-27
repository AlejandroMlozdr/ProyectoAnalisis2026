package com.umg.seguridad.controller;

import com.umg.seguridad.entity.PeriodoPlanillaId;
import com.umg.seguridad.entity.PlanillaCabecera;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Reporte de planilla ya procesada (lee cabecera y detalle; no recalcula nada). */
@Controller
@RequestMapping("/reporte-planilla")
public class ReportePlanillaController {

    private static final List<String> COLUMNAS = List.of("Empleado", "Fecha de contratación", "Puesto", "Status",
            "Sueldo base", "Bonificación Decreto", "Otros ingresos", "Descuento IGSS", "Descuento ISR",
            "Descuento inasistencias", "Salario neto");

    private final PlanillaService planillaService;

    public ReportePlanillaController(PlanillaService planillaService) {
        this.planillaService = planillaService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String periodo, Model model) {
        model.addAttribute("periodos", planillaService.listarCabeceras());
        model.addAttribute("periodoSel", periodo);
        if (periodo != null && !periodo.isBlank()) {
            try {
                PeriodoPlanillaId id = PlanillaService.parsePeriodo(periodo);
                model.addAttribute("cabecera", planillaService.obtenerCabecera(id));
                model.addAttribute("detalles", planillaService.detallesDelPeriodo(id));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "reporte-planilla/index";
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf(@RequestParam String periodo) {
        try {
            PeriodoPlanillaId id = PlanillaService.parsePeriodo(periodo);
            PlanillaCabecera cab = planillaService.obtenerCabecera(id);
            byte[] pdf = ReporteUtil.generarPdf(titulo(cab), COLUMNAS, filas(planillaService.detallesDelPeriodo(id)));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planilla-" + periodo + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel(@RequestParam String periodo) {
        try {
            PeriodoPlanillaId id = PlanillaService.parsePeriodo(periodo);
            PlanillaCabecera cab = planillaService.obtenerCabecera(id);
            byte[] excel = ReporteUtil.generarExcel("Planilla " + cab.getEtiqueta().replace('/', '-'),
                    COLUMNAS, filas(planillaService.detallesDelPeriodo(id)));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=planilla-" + periodo + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String titulo(PlanillaCabecera cab) {
        return "Reporte de Planilla " + cab.getEtiqueta() + " - Ingresos Q " + ReporteUtil.money(cab.getTotalIngresos())
                + " | Descuentos Q " + ReporteUtil.money(cab.getTotalDescuentos())
                + " | Neto Q " + ReporteUtil.money(cab.getSalarioNeto());
    }

    private List<List<String>> filas(List<PlanillaDetalle> detalles) {
        List<List<String>> filas = new ArrayList<>();
        for (PlanillaDetalle d : detalles) {
            filas.add(ReporteUtil.fila(
                    d.getEmpleado().getPersona().getNombre() + " " + d.getEmpleado().getPersona().getApellido(),
                    ReporteUtil.str(d.getFechaContratacion()),
                    d.getPuesto().getNombre(),
                    d.getStatusEmpleado().getNombre(),
                    ReporteUtil.money(d.getIngresoSueldoBase()),
                    ReporteUtil.money(d.getIngresoBonificacionDecreto()),
                    ReporteUtil.money(d.getIngresoOtrosIngresos()),
                    ReporteUtil.money(d.getDescuentoIgss()),
                    ReporteUtil.money(d.getDescuentoIsr()),
                    ReporteUtil.money(d.getDescuentoInasistencias()),
                    ReporteUtil.money(d.getSalarioNeto())));
        }
        filas.add(ReporteUtil.fila("TOTAL", "", "", "",
                suma(detalles, PlanillaDetalle::getIngresoSueldoBase),
                suma(detalles, PlanillaDetalle::getIngresoBonificacionDecreto),
                suma(detalles, PlanillaDetalle::getIngresoOtrosIngresos),
                suma(detalles, PlanillaDetalle::getDescuentoIgss),
                suma(detalles, PlanillaDetalle::getDescuentoIsr),
                suma(detalles, PlanillaDetalle::getDescuentoInasistencias),
                suma(detalles, PlanillaDetalle::getSalarioNeto)));
        return filas;
    }

    private String suma(List<PlanillaDetalle> detalles, Function<PlanillaDetalle, BigDecimal> campo) {
        BigDecimal total = BigDecimal.ZERO;
        for (PlanillaDetalle d : detalles) {
            BigDecimal v = campo.apply(d);
            if (v != null) total = total.add(v);
        }
        return ReporteUtil.money(total);
    }
}
