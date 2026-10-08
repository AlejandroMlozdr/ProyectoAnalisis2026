package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InasistenciaRepository extends JpaRepository<Inasistencia, Integer> {

    /** Inasistencias de un empleado que se traslapan con [inicio, finExclusivo). */
    @Query("select i from Inasistencia i where i.empleado.idEmpleado = :idEmpleado "
            + "and i.fechaInicial < :finExclusivo and i.fechaFinal >= :inicio")
    List<Inasistencia> findSolapadas(@Param("idEmpleado") Integer idEmpleado,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("finExclusivo") LocalDateTime finExclusivo);
}
