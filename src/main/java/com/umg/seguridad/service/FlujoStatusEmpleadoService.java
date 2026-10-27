package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlujoStatusEmpleadoService {

    private final FlujoStatusEmpleadoRepository repository;

    public FlujoStatusEmpleadoService(FlujoStatusEmpleadoRepository repository) {
        this.repository = repository;
    }

    public List<FlujoStatusEmpleado> listar() {
        return repository.findAll();
    }

    public FlujoStatusEmpleado obtener(Integer statusActual, Integer statusNuevo) {
        return repository.findById(new FlujoStatusEmpleadoId(statusActual, statusNuevo))
                .orElseThrow(() -> new IllegalArgumentException("Flujo de Status no encontrado."));
    }

    @Transactional
    public FlujoStatusEmpleado guardar(FlujoStatusEmpleado e, boolean esNuevo) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();
        Integer id1 = e.getStatusActual().getIdStatusEmpleado();
        Integer id2 = e.getStatusNuevo().getIdStatusEmpleado();

        if (esNuevo) {
            if (repository.existsById(new FlujoStatusEmpleadoId(id1, id2))) {
                throw new IllegalArgumentException("Ya existe un flujo entre esos dos status.");
            }
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        FlujoStatusEmpleado existente = obtener(id1, id2);
        existente.setNombreEvento(e.getNombreEvento());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer statusActual, Integer statusNuevo) {
        repository.deleteById(new FlujoStatusEmpleadoId(statusActual, statusNuevo));
    }

    private void validar(FlujoStatusEmpleado e) {
        if (e.getStatusActual() == null) {
            throw new IllegalArgumentException("Debe seleccionar Status actual.");
        }
        if (e.getStatusNuevo() == null) {
            throw new IllegalArgumentException("Debe seleccionar Status nuevo.");
        }
        if (e.getNombreEvento() == null || e.getNombreEvento().isBlank()) {
            throw new IllegalArgumentException("El campo Nombre del evento es obligatorio.");
        }
        if (e.getNombreEvento() != null) {
            e.setNombreEvento(e.getNombreEvento().trim().isEmpty() ? null : e.getNombreEvento().trim());
            if (e.getNombreEvento() != null && e.getNombreEvento().length() > 50) {
                throw new IllegalArgumentException("El campo Nombre del evento no puede exceder 50 caracteres.");
            }
        }
        if (e.getStatusActual().getIdStatusEmpleado().equals(e.getStatusNuevo().getIdStatusEmpleado())) {
            throw new IllegalArgumentException("El status actual y el status nuevo deben ser distintos.");
        }

    }
}
