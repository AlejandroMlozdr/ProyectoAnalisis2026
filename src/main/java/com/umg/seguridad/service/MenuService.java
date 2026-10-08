package com.umg.seguridad.service;

import com.umg.seguridad.entity.Menu;
import com.umg.seguridad.entity.Modulo;
import com.umg.seguridad.repository.MenuRepository;
import com.umg.seguridad.repository.ModuloRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final ModuloRepository moduloRepository;

    public MenuService(MenuRepository menuRepository, ModuloRepository moduloRepository) {
        this.menuRepository = menuRepository;
        this.moduloRepository = moduloRepository;
    }

    public List<Menu> listar() {
        return menuRepository.findAll();
    }

    public List<Modulo> listarModulos() {
        return moduloRepository.findAll();
    }

    public Menu obtenerPorId(Integer id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu no encontrado: " + id));
    }

    public Menu guardar(Menu menu) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (menu.getIdMenu() == null) {
            menu.setFechaCreacion(LocalDateTime.now());
            menu.setUsuarioCreacion(usuarioActual);
        } else {
            menu.setFechaModificacion(LocalDateTime.now());
            menu.setUsuarioModificacion(usuarioActual);
        }
        return menuRepository.save(menu);
    }

    public void eliminar(Integer id) {
        menuRepository.deleteById(id);
    }
}
