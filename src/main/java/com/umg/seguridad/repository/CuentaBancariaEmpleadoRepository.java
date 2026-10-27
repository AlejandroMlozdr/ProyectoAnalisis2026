package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface CuentaBancariaEmpleadoRepository extends JpaRepository<CuentaBancariaEmpleado, Integer> {

    Optional<CuentaBancariaEmpleado> findFirstByEmpleado_IdEmpleadoAndActiva(Integer idEmpleado, Character activa);
}
