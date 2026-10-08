package com.umg.seguridad.controller;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import com.umg.seguridad.service.CuentaBancariaEmpleadoService;
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
@RequestMapping("/cuenta-bancaria-empleado")
public class CuentaBancariaEmpleadoController {

    private final CuentaBancariaEmpleadoService service;
    private final EmpleadoRepository empleadoRepository;
    private final BancoRepository bancoRepository;

    public CuentaBancariaEmpleadoController(CuentaBancariaEmpleadoService service, EmpleadoRepository empleadoRepository, BancoRepository bancoRepository) {
        this.service = service;
        this.empleadoRepository = empleadoRepository;
        this.bancoRepository = bancoRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("items", service.listar());
        return "cuenta-bancaria-empleado/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("item", new CuentaBancariaEmpleado());
        cargarCombos(model);
        return "cuenta-bancaria-empleado/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("item", service.obtenerPorId(id));
        cargarCombos(model);
        return "cuenta-bancaria-empleado/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("item") CuentaBancariaEmpleado item, Model model, RedirectAttributes redirectAttributes) {
        try {
            service.guardar(item);
            redirectAttributes.addFlashAttribute("exito", "Registro guardado correctamente.");
            return "redirect:/cuenta-bancaria-empleado";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarCombos(model);
            return "cuenta-bancaria-empleado/form";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        service.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Registro eliminado correctamente.");
        return "redirect:/cuenta-bancaria-empleado";
    }

    private void cargarCombos(Model model) {
        model.addAttribute("empleadoOpciones", empleadoRepository.findAll());
        model.addAttribute("bancoOpciones", bancoRepository.findAll());
    }

    // ----- Imprimir (PDF) y Exportar (Excel) -----

    private static final List<String> COLUMNAS = List.of("Empleado", "Banco", "Número de cuenta", "Cuenta activa");

    private List<List<String>> filas() {
        return service.listar().stream()
                .map(e -> ReporteUtil.fila(
                        ReporteUtil.safe(() -> { var x = e.getEmpleado(); return x.getPersona().getNombre() + " " + x.getPersona().getApellido(); }),
                        ReporteUtil.safe(() -> { var x = e.getBanco(); return x.getNombre(); }),
                        e.getNumeroDeCuenta(),
                        e.isActivaMarcada() ? "Activa" : "Inactiva"))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Cuentas Bancarias de Empleados", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cuenta-bancaria-empleado.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Cuentas Bancarias de Empleados", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cuenta-bancaria-empleado.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
