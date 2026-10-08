package com.umg.seguridad.repository;

import com.umg.seguridad.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Integer> {
    List<Menu> findByModulo_IdModulo(Integer idModulo);
}
