package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentoPersonaService {

    private final DocumentoPersonaRepository repository;

    public DocumentoPersonaService(DocumentoPersonaRepository repository) {
        this.repository = repository;
    }

    public List<DocumentoPersona> listar() {
        return repository.findAll();
    }

    public DocumentoPersona obtener(Integer persona, Integer tipoDocumento) {
        return repository.findById(new DocumentoPersonaId(persona, tipoDocumento))
                .orElseThrow(() -> new IllegalArgumentException("Documento de Persona no encontrado."));
    }

    @Transactional
    public DocumentoPersona guardar(DocumentoPersona e, boolean esNuevo) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();
        Integer id1 = e.getPersona().getIdPersona();
        Integer id2 = e.getTipoDocumento().getIdTipoDocumento();

        if (esNuevo) {
            if (repository.existsById(new DocumentoPersonaId(id1, id2))) {
                throw new IllegalArgumentException("Esa persona ya tiene registrado un documento de ese tipo.");
            }
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        DocumentoPersona existente = obtener(id1, id2);
        existente.setNoDocumento(e.getNoDocumento());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer persona, Integer tipoDocumento) {
        repository.deleteById(new DocumentoPersonaId(persona, tipoDocumento));
    }

    private void validar(DocumentoPersona e) {
        if (e.getPersona() == null) {
            throw new IllegalArgumentException("Debe seleccionar Persona.");
        }
        if (e.getTipoDocumento() == null) {
            throw new IllegalArgumentException("Debe seleccionar Tipo de documento.");
        }
        if (e.getNoDocumento() == null || e.getNoDocumento().isBlank()) {
            throw new IllegalArgumentException("El campo Número de documento es obligatorio.");
        }
        if (e.getNoDocumento() != null) {
            e.setNoDocumento(e.getNoDocumento().trim().isEmpty() ? null : e.getNoDocumento().trim());
            if (e.getNoDocumento() != null && e.getNoDocumento().length() > 50) {
                throw new IllegalArgumentException("El campo Número de documento no puede exceder 50 caracteres.");
            }
        }
    }
}
