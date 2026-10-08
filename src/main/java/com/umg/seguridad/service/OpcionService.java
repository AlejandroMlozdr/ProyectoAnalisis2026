package com.umg.seguridad.service;

import com.umg.seguridad.entity.Menu;
import com.umg.seguridad.entity.Opcion;
import com.umg.seguridad.repository.MenuRepository;
import com.umg.seguridad.repository.OpcionRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OpcionService {

    private final OpcionRepository opcionRepository;
    private final MenuRepository menuRepository;

    public OpcionService(OpcionRepository opcionRepository, MenuRepository menuRepository) {
        this.opcionRepository = opcionRepository;
        this.menuRepository = menuRepository;
    }

    public List<Opcion> listar() {
        return opcionRepository.findAll();
    }

    public List<Menu> listarMenus() {
        return menuRepository.findAll();
    }

    public Opcion obtenerPorId(Integer id) {
        return opcionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Opcion no encontrada: " + id));
    }

    public Opcion guardar(Opcion opcion) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (opcion.getIdOpcion() == null) {
            opcion.setFechaCreacion(LocalDateTime.now());
            opcion.setUsuarioCreacion(usuarioActual);
        } else {
            opcion.setFechaModificacion(LocalDateTime.now());
            opcion.setUsuarioModificacion(usuarioActual);
        }
        return opcionRepository.save(opcion);
    }

    public void eliminar(Integer id) {
        opcionRepository.deleteById(id);
    }
}
