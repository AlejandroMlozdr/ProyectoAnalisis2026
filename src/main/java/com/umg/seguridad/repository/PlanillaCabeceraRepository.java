package com.umg.seguridad.repository;

import com.umg.seguridad.entity.PeriodoPlanillaId;
import com.umg.seguridad.entity.PlanillaCabecera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlanillaCabeceraRepository extends JpaRepository<PlanillaCabecera, PeriodoPlanillaId> {

    @Query("select c from PlanillaCabecera c order by c.id.anio desc, c.id.mes desc")
    List<PlanillaCabecera> findAllOrdenadas();
}
