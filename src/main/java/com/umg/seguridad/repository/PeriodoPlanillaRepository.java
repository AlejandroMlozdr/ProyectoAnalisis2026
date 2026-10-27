package com.umg.seguridad.repository;

import com.umg.seguridad.entity.PeriodoPlanilla;
import com.umg.seguridad.entity.PeriodoPlanillaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PeriodoPlanillaRepository extends JpaRepository<PeriodoPlanilla, PeriodoPlanillaId> {

    @Query("select p from PeriodoPlanilla p order by p.id.anio desc, p.id.mes desc")
    List<PeriodoPlanilla> findAllOrdenados();
}
