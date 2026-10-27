package com.umg.seguridad.service;

import com.umg.seguridad.entity.Empresa;
import com.umg.seguridad.repository.EmpresaRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public List<Empresa> listar() {
        return empresaRepository.findAll();
    }

    public Empresa obtenerPorId(Integer id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + id));
    }

    public Empresa guardar(Empresa empresa) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();

        if (empresa.getIdEmpresa() == null) {
            // Alta
            empresa.setFechaCreacion(LocalDateTime.now());
            empresa.setUsuarioCreacion(usuarioActual);
        } else {
            // Cambio
            empresa.setFechaModificacion(LocalDateTime.now());
            empresa.setUsuarioModificacion(usuarioActual);
        }
        return empresaRepository.save(empresa);
    }

    public void eliminar(Integer id) {
        empresaRepository.deleteById(id);
    }
}
