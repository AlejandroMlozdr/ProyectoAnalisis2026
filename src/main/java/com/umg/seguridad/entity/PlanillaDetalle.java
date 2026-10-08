package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila de planilla por empleado (copia de sus valores salariales al momento de procesar). */
@Entity
@Table(name = "planilla_detalle")
@Getter
@Setter
public class PlanillaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlanillaDetalle;

    @Column(name = "Anio", nullable = false)
    private Integer anio;

    @Column(name = "Mes", nullable = false)
    private Integer mes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdEmpleado", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private LocalDate fechaContratacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdPuesto", nullable = false)
    private Puesto puesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdStatusEmpleado", nullable = false)
    private StatusEmpleado statusEmpleado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ingresoSueldoBase = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ingresoBonificacionDecreto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ingresoOtrosIngresos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoIgss = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoIsr = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoInasistencias = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal salarioNeto;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;

    public BigDecimal getTotalIngresos() {
        return ingresoSueldoBase.add(ingresoBonificacionDecreto).add(ingresoOtrosIngresos);
    }

    public BigDecimal getTotalDescuentos() {
        return descuentoIgss.add(descuentoIsr).add(descuentoInasistencias);
    }
}
