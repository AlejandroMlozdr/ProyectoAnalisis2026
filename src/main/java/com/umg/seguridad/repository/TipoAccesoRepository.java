package com.umg.seguridad.repository;

import com.umg.seguridad.entity.TipoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoAccesoRepository extends JpaRepository<TipoAcceso, Integer> {
    Optional<TipoAcceso> findFirstByNombreIgnoreCase(String nombre);
}
