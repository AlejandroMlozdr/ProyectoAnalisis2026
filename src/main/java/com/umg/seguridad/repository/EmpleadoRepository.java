package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    /** Empleados con un status dado (sin importar mayusculas) contratados hasta una fecha. */
    List<Empleado> findByStatusEmpleado_NombreIgnoreCaseAndFechaContratacionLessThanEqual(String nombre, LocalDate fecha);
}
