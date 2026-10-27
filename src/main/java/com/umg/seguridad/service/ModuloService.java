package com.umg.seguridad.service;

import com.umg.seguridad.entity.Modulo;
import com.umg.seguridad.repository.ModuloRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModuloService {

    private final ModuloRepository moduloRepository;

    public ModuloService(ModuloRepository moduloRepository) {
        this.moduloRepository = moduloRepository;
    }

    public List<Modulo> listar() {
        return moduloRepository.findAll();
    }

    public Modulo obtenerPorId(Integer id) {
        return moduloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modulo no encontrado: " + id));
    }

    public Modulo guardar(Modulo modulo) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (modulo.getIdModulo() == null) {
            modulo.setFechaCreacion(LocalDateTime.now());
            modulo.setUsuarioCreacion(usuarioActual);
        } else {
            modulo.setFechaModificacion(LocalDateTime.now());
            modulo.setUsuarioModificacion(usuarioActual);
        }
        return moduloRepository.save(modulo);
    }

    public void eliminar(Integer id) {
        moduloRepository.deleteById(id);
    }
}
