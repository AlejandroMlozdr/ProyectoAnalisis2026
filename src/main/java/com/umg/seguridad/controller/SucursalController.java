package com.umg.seguridad.controller;

import com.umg.seguridad.entity.Sucursal;
import com.umg.seguridad.repository.EmpresaRepository;
import com.umg.seguridad.service.SucursalService;
import com.umg.seguridad.util.ReporteUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/sucursal")
public class SucursalController {

    private final SucursalService sucursalService;
    private final EmpresaRepository empresaRepository; // para el ComboBox de Empresas

    public SucursalController(SucursalService sucursalService, EmpresaRepository empresaRepository) {
        this.sucursalService = sucursalService;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("sucursales", sucursalService.listar());
        return "sucursal/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("sucursal", new Sucursal());
        model.addAttribute("empresas", empresaRepository.findAll());
        return "sucursal/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("sucursal", sucursalService.obtenerPorId(id));
        model.addAttribute("empresas", empresaRepository.findAll());
        return "sucursal/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Sucursal sucursal) {
        sucursalService.guardar(sucursal);
        return "redirect:/sucursal";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        sucursalService.eliminar(id);
        return "redirect:/sucursal";
    }

    private static final List<String> COLUMNAS = List.of("Nombre", "Empresa", "Dirección");

    private List<List<String>> filas() {
        return sucursalService.listar().stream()
                .map(s -> ReporteUtil.fila(s.getNombre(), s.getEmpresa() != null ? s.getEmpresa().getNombre() : "", s.getDireccion()))
                .toList();
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = ReporteUtil.generarPdf("Listado de Sucursales", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sucursales.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] excel = ReporteUtil.generarExcel("Sucursales", COLUMNAS, filas());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sucursales.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
