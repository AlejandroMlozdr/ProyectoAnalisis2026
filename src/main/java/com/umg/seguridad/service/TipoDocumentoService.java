package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TipoDocumentoService {

    private final TipoDocumentoRepository repository;

    public TipoDocumentoService(TipoDocumentoRepository repository) {
        this.repository = repository;
    }

    public List<TipoDocumento> listar() {
        return repository.findAll(Sort.by("nombre"));
    }

    public TipoDocumento obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de Documento no encontrado: " + id));
    }

    @Transactional
    public TipoDocumento guardar(TipoDocumento e) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdTipoDocumento() == null) {
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia.
        TipoDocumento existente = obtenerPorId(e.getIdTipoDocumento());
        existente.setNombre(e.getNombre());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer id) {
        repository.deleteById(id);
    }

    private void validar(TipoDocumento e) {
        if (e.getNombre() == null || e.getNombre().isBlank()) {
            throw new IllegalArgumentException("El campo Nombre es obligatorio.");
        }
        if (e.getNombre() != null) {
            e.setNombre(e.getNombre().trim().isEmpty() ? null : e.getNombre().trim());
            if (e.getNombre() != null && e.getNombre().length() > 50) {
                throw new IllegalArgumentException("El campo Nombre no puede exceder 50 caracteres.");
            }
        }
        boolean duplicado = repository.findByNombreIgnoreCase(e.getNombre()).stream()
                .anyMatch(x -> e.getIdTipoDocumento() == null || !x.getIdTipoDocumento().equals(e.getIdTipoDocumento()));
        if (duplicado) {
            throw new IllegalArgumentException("Ya existe un tipo de documento con el nombre \"" + e.getNombre() + "\".");
        }
    }
}