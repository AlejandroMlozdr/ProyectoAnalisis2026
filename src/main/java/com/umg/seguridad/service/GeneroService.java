package com.umg.seguridad.service;

import com.umg.seguridad.entity.Genero;
import com.umg.seguridad.repository.GeneroRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GeneroService {

    private final GeneroRepository generoRepository;

    public GeneroService(GeneroRepository generoRepository) {
        this.generoRepository = generoRepository;
    }

    public List<Genero> listar() {
        return generoRepository.findAll();
    }

    public Genero obtenerPorId(Integer id) {
        return generoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Genero no encontrado: " + id));
    }

    public Genero guardar(Genero genero) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (genero.getIdGenero() == null) {
            genero.setFechaCreacion(LocalDateTime.now());
            genero.setUsuarioCreacion(usuarioActual);
        } else {
            genero.setFechaModificacion(LocalDateTime.now());
            genero.setUsuarioModificacion(usuarioActual);
        }
        return generoRepository.save(genero);
    }

    public void eliminar(Integer id) {
        generoRepository.deleteById(id);
    }
}
