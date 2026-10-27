package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EstadoCivilService {

    private final EstadoCivilRepository repository;

    public EstadoCivilService(EstadoCivilRepository repository) {
        this.repository = repository;
    }

    public List<EstadoCivil> listar() {
        return repository.findAll();
    }

    public EstadoCivil obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estado Civil no encontrado: " + id));
    }

    @Transactional
    public EstadoCivil guardar(EstadoCivil e) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdEstadoCivil() == null) {
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia.
        EstadoCivil existente = obtenerPorId(e.getIdEstadoCivil());
        existente.setNombre(e.getNombre());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer id) {
        repository.deleteById(id);
    }

    private void validar(EstadoCivil e) {
        if (e.getNombre() == null || e.getNombre().isBlank()) {
            throw new IllegalArgumentException("El campo Nombre es obligatorio.");
        }
        if (e.getNombre() != null) {
            e.setNombre(e.getNombre().trim().isEmpty() ? null : e.getNombre().trim());
            if (e.getNombre() != null && e.getNombre().length() > 50) {
                throw new IllegalArgumentException("El campo Nombre no puede exceder 50 caracteres.");
            }
        }
    }
}
