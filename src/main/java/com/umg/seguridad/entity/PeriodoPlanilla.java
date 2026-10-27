package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Periodo (mes) para el que se calcula la planilla. */
@Entity
@Table(name = "periodo_planilla")
@Getter
@Setter
public class PeriodoPlanilla {

    @EmbeddedId
    private PeriodoPlanillaId id;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;

    /** Texto corto "mes/anio" para combos y mensajes (ej. "9/2026"). */
    public String getEtiqueta() {
        return id == null ? "" : id.getMes() + "/" + id.getAnio();
    }
}
