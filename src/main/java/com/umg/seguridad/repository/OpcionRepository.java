package com.umg.seguridad.repository;

import com.umg.seguridad.entity.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OpcionRepository extends JpaRepository<Opcion, Integer> {
    List<Opcion> findByMenu_IdMenu(Integer idMenu);
    List<Opcion> findByMenu_Modulo_IdModulo(Integer idModulo);
}
