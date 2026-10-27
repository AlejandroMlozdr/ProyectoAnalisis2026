package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;

import java.time.LocalDate;
import java.util.List;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    /** Empleados con un status dado (sin importar mayusculas) contratados hasta una fecha. */
    List<Empleado> findByStatusEmpleado_NombreIgnoreCaseAndFechaContratacionLessThanEqual(String nombre, LocalDate fecha);
}
