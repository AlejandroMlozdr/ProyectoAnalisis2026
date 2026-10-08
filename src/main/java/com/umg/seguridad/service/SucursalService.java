package com.umg.seguridad.service;

import com.umg.seguridad.entity.Sucursal;
import com.umg.seguridad.repository.SucursalRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SucursalService {

    private final SucursalRepository sucursalRepository;

    public SucursalService(SucursalRepository sucursalRepository) {
        this.sucursalRepository = sucursalRepository;
    }

    public List<Sucursal> listar() {
        return sucursalRepository.findAll();
    }

    public Sucursal obtenerPorId(Integer id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada: " + id));
    }

    public Sucursal guardar(Sucursal sucursal) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (sucursal.getIdSucursal() == null) {
            sucursal.setFechaCreacion(LocalDateTime.now());
            sucursal.setUsuarioCreacion(usuarioActual);
        } else {
            sucursal.setFechaModificacion(LocalDateTime.now());
            sucursal.setUsuarioModificacion(usuarioActual);
        }
        return sucursalRepository.save(sucursal);
    }

    public void eliminar(Integer id) {
        sucursalRepository.deleteById(id);
    }
}
