package com.umg.seguridad.repository;

import com.umg.seguridad.entity.PlanillaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanillaDetalleRepository extends JpaRepository<PlanillaDetalle, Integer> {

    /** Detalle completo de un periodo, con empleado/persona/puesto/status ya cargados (para reporte y boletas). */
    @Query("select d from PlanillaDetalle d "
            + "join fetch d.empleado e join fetch e.persona p "
            + "join fetch d.puesto pu join fetch pu.departamento dep "
            + "join fetch d.statusEmpleado s "
            + "where d.anio = :anio and d.mes = :mes "
            + "order by p.apellido, p.nombre")
    List<PlanillaDetalle> findDelPeriodo(@Param("anio") Integer anio, @Param("mes") Integer mes);

    /** Cantidad de empleados incluidos en la planilla de un periodo. */
    long countByAnioAndMes(Integer anio, Integer mes);

    /** Borra el detalle de un periodo (se usa al recalcular una planilla ya procesada). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PlanillaDetalle d where d.anio = :anio and d.mes = :mes")
    int borrarDelPeriodo(@Param("anio") Integer anio, @Param("mes") Integer mes);
}
