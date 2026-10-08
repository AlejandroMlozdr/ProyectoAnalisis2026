package com.umg.seguridad.repository;

import com.umg.seguridad.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {
    List<Sucursal> findByEmpresa_IdEmpresa(Integer idEmpresa);
}
