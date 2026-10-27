package com.umg.seguridad.service;

import com.umg.seguridad.entity.StatusUsuario;
import com.umg.seguridad.repository.StatusUsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatusUsuarioService {

    private final StatusUsuarioRepository statusUsuarioRepository;

    public StatusUsuarioService(StatusUsuarioRepository statusUsuarioRepository) {
        this.statusUsuarioRepository = statusUsuarioRepository;
    }

    public List<StatusUsuario> listar() {
        return statusUsuarioRepository.findAll();
    }

    public StatusUsuario obtenerPorId(Integer id) {
        return statusUsuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estatus no encontrado: " + id));
    }

    public StatusUsuario guardar(StatusUsuario statusUsuario) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (statusUsuario.getIdStatusUsuario() == null) {
            statusUsuario.setFechaCreacion(LocalDateTime.now());
            statusUsuario.setUsuarioCreacion(usuarioActual);
        } else {
            statusUsuario.setFechaModificacion(LocalDateTime.now());
            statusUsuario.setUsuarioModificacion(usuarioActual);
        }
        return statusUsuarioRepository.save(statusUsuario);
    }

    public void eliminar(Integer id) {
        statusUsuarioRepository.deleteById(id);
    }
}
