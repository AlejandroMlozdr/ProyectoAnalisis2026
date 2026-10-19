package com.umg.seguridad.repository;

import com.umg.seguridad.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Integer> {

    // Usado para evitar nombres repetidos al guardar.
    List<TipoDocumento> findByNombreIgnoreCase(String nombre);
}