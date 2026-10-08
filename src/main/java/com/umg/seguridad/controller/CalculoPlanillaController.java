package com.umg.seguridad.controller;

import com.umg.seguridad.entity.PeriodoPlanillaId;
import com.umg.seguridad.entity.PlanillaCabecera;
import com.umg.seguridad.service.PeriodoPlanillaService;
import com.umg.seguridad.service.PlanillaService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Set;

/** Proceso "Calcular Planilla": el usuario elige un periodo y se genera cabecera + detalle. */
@Controller
@RequestMapping("/calculo-planilla")
public class CalculoPlanillaController {

    private final PlanillaService planillaService;
    private final PeriodoPlanillaService periodoService;

    public CalculoPlanillaController(PlanillaService planillaService, PeriodoPlanillaService periodoService) {
        this.planillaService = planillaService;
        this.periodoService = periodoService;
    }

    @GetMapping
    public String index(Model model) {
        var procesadas = planillaService.listarCabeceras();
        Set<String> procesados = new HashSet<>();
        for (PlanillaCabecera c : procesadas) {
            procesados.add(c.getId().getAnio() + "-" + c.getId().getMes());
        }
        model.addAttribute("periodos", periodoService.listar());
        model.addAttribute("procesadas", procesadas);
        model.addAttribute("procesados", procesados);
        return "calculo-planilla/index";
    }

    @PostMapping("/procesar")
    public String procesar(@RequestParam String periodo,
                           @RequestParam(defaultValue = "false") boolean reprocesar,
                           RedirectAttributes redirectAttributes) {
        try {
            PeriodoPlanillaId id = PlanillaService.parsePeriodo(periodo);
            PlanillaCabecera c = planillaService.procesar(id.getAnio(), id.getMes(), reprocesar);
            redirectAttributes.addFlashAttribute("exito", "Planilla " + id.getMes() + "/" + id.getAnio()
                    + " procesada. Ingresos: Q " + ReporteUtil.money(c.getTotalIngresos())
                    + " | Descuentos: Q " + ReporteUtil.money(c.getTotalDescuentos())
                    + " | Neto: Q " + ReporteUtil.money(c.getSalarioNeto()));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/calculo-planilla";
    }
}
