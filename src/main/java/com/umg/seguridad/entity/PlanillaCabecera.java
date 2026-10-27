package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Totales de la planilla procesada de un periodo (una fila por Anio+Mes). */
@Entity
@Table(name = "planilla_cabecera")
@Getter
@Setter
public class PlanillaCabecera {

    @EmbeddedId
    private PeriodoPlanillaId id;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalIngresos;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(precision = 10, scale = 2)
    private BigDecimal salarioNeto;

    private LocalDateTime fechaHoraProcesada;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;

    public String getEtiqueta() {
        return id == null ? "" : id.getMes() + "/" + id.getAnio();
    }
}
