package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, Integer> {

    /** Liquidaciones de un empleado (para no liquidar dos veces la misma relacion laboral). */
    List<Liquidacion> findByEmpleado_IdEmpleado(Integer idEmpleado);
}
