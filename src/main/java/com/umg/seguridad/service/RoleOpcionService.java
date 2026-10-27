package com.umg.seguridad.service;

import com.umg.seguridad.dto.AsignarPermisosForm;
import com.umg.seguridad.dto.PermisoRow;
import com.umg.seguridad.entity.Opcion;
import com.umg.seguridad.entity.Role;
import com.umg.seguridad.entity.RoleOpcion;
import com.umg.seguridad.entity.RoleOpcionId;
import com.umg.seguridad.repository.OpcionRepository;
import com.umg.seguridad.repository.RoleOpcionRepository;
import com.umg.seguridad.repository.RoleRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RoleOpcionService {

    private final RoleOpcionRepository roleOpcionRepository;
    private final RoleRepository roleRepository;
    private final OpcionRepository opcionRepository;
    private final PermisoService permisoService;

    public RoleOpcionService(RoleOpcionRepository roleOpcionRepository, RoleRepository roleRepository,
                              OpcionRepository opcionRepository, PermisoService permisoService) {
        this.roleOpcionRepository = roleOpcionRepository;
        this.roleRepository = roleRepository;
        this.opcionRepository = opcionRepository;
        this.permisoService = permisoService;
    }

    @Transactional
    public void guardar(AsignarPermisosForm form) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();

        Role role = roleRepository.findById(form.getIdRole())
                .orElseThrow(() -> new IllegalArgumentException("Role no encontrado: " + form.getIdRole()));

        for (PermisoRow fila : form.getPermisos()) {
            RoleOpcionId id = new RoleOpcionId(form.getIdRole(), fila.getIdOpcion());

            if (!fila.isConsultar()) {
                // El rol NO debe ver esta opcion: si existia una fila, se borra.
                // Esto es lo que hace que PermisoService.puedeConsultar() y
                // construirMenu() dejen de mostrarla, sin necesitar una columna nueva.
                if (roleOpcionRepository.existsById(id)) {
                    roleOpcionRepository.deleteById(id);
                }
                continue;
            }

            Opcion opcion = opcionRepository.findById(fila.getIdOpcion())
                    .orElseThrow(() -> new IllegalArgumentException("Opcion no encontrada: " + fila.getIdOpcion()));

            RoleOpcion roleOpcion = roleOpcionRepository.findById(id).orElse(null);

            boolean esNuevo = roleOpcion == null;
            if (esNuevo) {
                roleOpcion = new RoleOpcion();
                roleOpcion.setRole(role);
                roleOpcion.setOpcion(opcion);
                roleOpcion.setFechaCreacion(LocalDateTime.now());
                roleOpcion.setUsuarioCreacion(usuarioActual);
            } else {
                roleOpcion.setFechaModificacion(LocalDateTime.now());
                roleOpcion.setUsuarioModificacion(usuarioActual);
            }

            roleOpcion.setAlta(fila.isAlta() ? 1 : 0);
            roleOpcion.setBaja(fila.isBaja() ? 1 : 0);
            roleOpcion.setCambio(fila.isCambio() ? 1 : 0);
            roleOpcion.setImprimir(fila.isImprimir() ? 1 : 0);
            roleOpcion.setExportar(fila.isExportar() ? 1 : 0);

            roleOpcionRepository.save(roleOpcion);
        }

        // El menu dinamico y los permisos de este rol quedaron en cache (PermisoService);
        // sin esto, los usuarios con ese rol seguirian viendo los permisos viejos hasta
        // que la app se reiniciara.
        permisoService.invalidarCache(form.getIdRole());
    }
}
