package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InasistenciaRepository extends JpaRepository<Inasistencia, Integer> {

    /** Inasistencias de un empleado que se traslapan con [inicio, finExclusivo). Lo usa Calcular Planilla. */
    @Query("select i from Inasistencia i where i.empleado.idEmpleado = :idEmpleado "
            + "and i.fechaInicial < :finExclusivo and i.fechaFinal >= :inicio")
    List<Inasistencia> findSolapadas(@Param("idEmpleado") Integer idEmpleado,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("finExclusivo") LocalDateTime finExclusivo);

    /** Todas las inasistencias de un empleado (para validar que no se traslapen entre si). */
    List<Inasistencia> findByEmpleado_IdEmpleado(Integer idEmpleado);

    /** Inasistencias de un empleado que todavia no han sido consideradas por planilla ni liquidacion. */
    List<Inasistencia> findByEmpleado_IdEmpleadoAndFechaProcesadoIsNull(Integer idEmpleado);
}
