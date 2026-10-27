package com.umg.seguridad.service;

import com.umg.seguridad.entity.Role;
import com.umg.seguridad.repository.RoleRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> listar() {
        return roleRepository.findAll();
    }

    public Role obtenerPorId(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role no encontrado: " + id));
    }

    public Role guardar(Role role) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (role.getIdRole() == null) {
            role.setFechaCreacion(LocalDateTime.now());
            role.setUsuarioCreacion(usuarioActual);
        } else {
            role.setFechaModificacion(LocalDateTime.now());
            role.setUsuarioModificacion(usuarioActual);
        }
        return roleRepository.save(role);
    }

    public void eliminar(Integer id) {
        roleRepository.deleteById(id);
    }
}
