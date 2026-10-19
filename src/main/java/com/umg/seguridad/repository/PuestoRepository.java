package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PuestoRepository extends JpaRepository<Puesto, Integer> {

    // Usado para evitar nombres repetidos al guardar.
    List<Puesto> findByNombreIgnoreCaseAndDepartamento_IdDepartamento(String nombre, Integer idDepartamento);
}