package com.umg.seguridad.controller;

import com.umg.seguridad.dto.AsignarPermisosForm;
import com.umg.seguridad.dto.PermisoRow;
import com.umg.seguridad.entity.Opcion;
import com.umg.seguridad.entity.RoleOpcion;
import com.umg.seguridad.repository.ModuloRepository;
import com.umg.seguridad.repository.OpcionRepository;
import com.umg.seguridad.repository.RoleOpcionRepository;
import com.umg.seguridad.repository.RoleRepository;
import com.umg.seguridad.service.RoleOpcionService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/role-opcion")
public class RoleOpcionController {

    private final RoleRepository roleRepository;
    private final ModuloRepository moduloRepository;
    private final OpcionRepository opcionRepository;
    private final RoleOpcionRepository roleOpcionRepository;
    private final RoleOpcionService roleOpcionService;

    public RoleOpcionController(RoleRepository roleRepository, ModuloRepository moduloRepository,
                                 OpcionRepository opcionRepository, RoleOpcionRepository roleOpcionRepository,
                                 RoleOpcionService roleOpcionService) {
        this.roleRepository = roleRepository;
        this.moduloRepository = moduloRepository;
        this.opcionRepository = opcionRepository;
        this.roleOpcionRepository = roleOpcionRepository;
        this.roleOpcionService = roleOpcionService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(@RequestParam(required = false) Integer idRole,
                         @RequestParam(required = false) Integer idModulo,
                         Model model) {
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("modulos", moduloRepository.findAll());
        model.addAttribute("idRoleSeleccionado", idRole);
        model.addAttribute("idModuloSeleccionado", idModulo);

        if (idRole != null && idModulo != null) {
            List<Opcion> opciones = opcionRepository.findByMenu_Modulo_IdModulo(idModulo);
            List<RoleOpcion> existentes = roleOpcionRepository
                    .findByRole_IdRoleAndOpcion_Menu_Modulo_IdModulo(idRole, idModulo);

            Map<Integer, RoleOpcion> mapaExistentes = new HashMap<>();
            for (RoleOpcion ro : existentes) {
                mapaExistentes.put(ro.getOpcion().getIdOpcion(), ro);
            }

            AsignarPermisosForm form = new AsignarPermisosForm();
            form.setIdRole(idRole);
            form.setIdModulo(idModulo);

            List<PermisoRow> filas = new ArrayList<>();
            for (Opcion o : opciones) {
                PermisoRow fila = new PermisoRow();
                fila.setIdOpcion(o.getIdOpcion());
                fila.setNombreOpcion(o.getNombre());

                RoleOpcion existente = mapaExistentes.get(o.getIdOpcion());
                if (existente != null) {
                    fila.setConsultar(true);
                    fila.setAlta(existente.puedeAlta());
                    fila.setBaja(existente.puedeBaja());
                    fila.setCambio(existente.puedeCambio());
                    fila.setImprimir(existente.puedeImprimir());
                    fila.setExportar(existente.puedeExportar());
                }
                filas.add(fila);
            }
            form.setPermisos(filas);
            model.addAttribute("form", form);
        }

        return "role-opcion/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute AsignarPermisosForm form) {
        roleOpcionService.guardar(form);
        return "redirect:/role-opcion?idRole=" + form.getIdRole() + "&idModulo=" + form.getIdModulo();
    }
}
