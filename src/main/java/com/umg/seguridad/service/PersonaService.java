package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PersonaService {

    private final PersonaRepository repository;

    public PersonaService(PersonaRepository repository) {
        this.repository = repository;
    }

    public List<Persona> listar() {
        return repository.findAll();
    }

    public Persona obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrado: " + id));
    }

    @Transactional
    public Persona guardar(Persona e) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdPersona() == null) {
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia.
        Persona existente = obtenerPorId(e.getIdPersona());
        existente.setNombre(e.getNombre());
        existente.setApellido(e.getApellido());
        existente.setFechaNacimiento(e.getFechaNacimiento());
        existente.setGenero(e.getGenero());
        existente.setDireccion(e.getDireccion());
        existente.setTelefono(e.getTelefono());
        existente.setCorreoElectronico(e.getCorreoElectronico());
        existente.setEstadoCivil(e.getEstadoCivil());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer id) {
        repository.deleteById(id);
    }

    private void validar(Persona e) {
        if (e.getNombre() == null || e.getNombre().isBlank()) {
            throw new IllegalArgumentException("El campo Nombre es obligatorio.");
        }
        if (e.getNombre() != null) {
            e.setNombre(e.getNombre().trim().isEmpty() ? null : e.getNombre().trim());
            if (e.getNombre() != null && e.getNombre().length() > 50) {
                throw new IllegalArgumentException("El campo Nombre no puede exceder 50 caracteres.");
            }
        }
        if (e.getApellido() == null || e.getApellido().isBlank()) {
            throw new IllegalArgumentException("El campo Apellido es obligatorio.");
        }
        if (e.getApellido() != null) {
            e.setApellido(e.getApellido().trim().isEmpty() ? null : e.getApellido().trim());
            if (e.getApellido() != null && e.getApellido().length() > 50) {
                throw new IllegalArgumentException("El campo Apellido no puede exceder 50 caracteres.");
            }
        }
        if (e.getFechaNacimiento() == null) {
            throw new IllegalArgumentException("El campo Fecha de nacimiento es obligatorio.");
        }
        if (e.getGenero() == null) {
            throw new IllegalArgumentException("Debe seleccionar Género.");
        }
        if (e.getDireccion() == null || e.getDireccion().isBlank()) {
            throw new IllegalArgumentException("El campo Dirección es obligatorio.");
        }
        if (e.getDireccion() != null) {
            e.setDireccion(e.getDireccion().trim().isEmpty() ? null : e.getDireccion().trim());
            if (e.getDireccion() != null && e.getDireccion().length() > 100) {
                throw new IllegalArgumentException("El campo Dirección no puede exceder 100 caracteres.");
            }
        }
        if (e.getTelefono() == null || e.getTelefono().isBlank()) {
            throw new IllegalArgumentException("El campo Teléfono es obligatorio.");
        }
        if (e.getTelefono() != null) {
            e.setTelefono(e.getTelefono().trim().isEmpty() ? null : e.getTelefono().trim());
            if (e.getTelefono() != null && e.getTelefono().length() > 50) {
                throw new IllegalArgumentException("El campo Teléfono no puede exceder 50 caracteres.");
            }
        }
        if (e.getCorreoElectronico() != null) {
            e.setCorreoElectronico(e.getCorreoElectronico().trim().isEmpty() ? null : e.getCorreoElectronico().trim());
            if (e.getCorreoElectronico() != null && e.getCorreoElectronico().length() > 50) {
                throw new IllegalArgumentException("El campo Correo electrónico no puede exceder 50 caracteres.");
            }
        }
        if (e.getEstadoCivil() == null) {
            throw new IllegalArgumentException("Debe seleccionar Estado civil.");
        }
        if (e.getFechaNacimiento() != null && e.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        if (e.getCorreoElectronico() != null && !e.getCorreoElectronico().contains("@")) {
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        }

    }
}
