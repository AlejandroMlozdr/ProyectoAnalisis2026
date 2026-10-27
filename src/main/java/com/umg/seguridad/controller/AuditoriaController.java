package com.umg.seguridad.controller;

import com.umg.seguridad.entity.BitacoraAcceso;
import com.umg.seguridad.service.AuditoriaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String paginaAuditoria(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) Integer tipo,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDateTime hasta,
            Model model) {

        if (desde != null) {
            desde = desde.withHour(0).withMinute(0).withSecond(0);
        }
        if (hasta != null) {
            hasta = hasta.withHour(23).withMinute(59).withSecond(59);
        }

        List<BitacoraAcceso> registros = auditoriaService.buscarConFiltros(usuario, tipo, ip, desde, hasta);
        model.addAttribute("registros", registros);

        return "auditoria";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<BitacoraAcceso>> listar() {
        return ResponseEntity.ok(auditoriaService.listar());
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<BitacoraAcceso> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(auditoriaService.obtenerPorId(id));
    }

    @GetMapping("/api/usuario/{usuario}")
    @ResponseBody
    public ResponseEntity<List<BitacoraAcceso>> buscarPorUsuario(@PathVariable String usuario) {
        return ResponseEntity.ok(auditoriaService.buscarPorUsuario(usuario));
    }

    @GetMapping("/api/tipo/{idTipo}")
    @ResponseBody
    public ResponseEntity<List<BitacoraAcceso>> buscarPorTipo(@PathVariable Integer idTipo) {
        return ResponseEntity.ok(auditoriaService.buscarPorTipo(idTipo));
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        auditoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
