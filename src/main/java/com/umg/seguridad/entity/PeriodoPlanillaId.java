package com.umg.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Llave compuesta (Anio, Mes) compartida por PERIODO_PLANILLA y PLANILLA_CABECERA. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PeriodoPlanillaId implements Serializable {

    @Column(name = "Anio")
    private Integer anio;

    @Column(name = "Mes")
    private Integer mes;
}
