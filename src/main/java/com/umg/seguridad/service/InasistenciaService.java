package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InasistenciaService {

    private final InasistenciaRepository repository;

    public InasistenciaService(InasistenciaRepository repository) {
        this.repository = repository;
    }

    public List<Inasistencia> listar() {
        return repository.findAll();
    }

    public Inasistencia obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inasistencia no encontrado: " + id));
    }

    @Transactional
    public Inasistencia guardar(Inasistencia e) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdInasistencia() == null) {
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia.
        Inasistencia existente = obtenerPorId(e.getIdInasistencia());
        existente.setEmpleado(e.getEmpleado());
        existente.setFechaInicial(e.getFechaInicial());
        existente.setFechaFinal(e.getFechaFinal());
        existente.setMotivoInasistencia(e.getMotivoInasistencia());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer id) {
        repository.deleteById(id);
    }

    private void validar(Inasistencia e) {
        if (e.getEmpleado() == null) {
            throw new IllegalArgumentException("Debe seleccionar Empleado.");
        }
        if (e.getFechaInicial() == null) {
            throw new IllegalArgumentException("El campo Fecha inicial es obligatorio.");
        }
        if (e.getFechaFinal() == null) {
            throw new IllegalArgumentException("El campo Fecha final es obligatorio.");
        }
        if (e.getMotivoInasistencia() != null) {
            e.setMotivoInasistencia(e.getMotivoInasistencia().trim().isEmpty() ? null : e.getMotivoInasistencia().trim());
            if (e.getMotivoInasistencia() != null && e.getMotivoInasistencia().length() > 300) {
                throw new IllegalArgumentException("El campo Motivo no puede exceder 300 caracteres.");
            }
        }
        if (e.getFechaFinal().isBefore(e.getFechaInicial())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }

    }
}
