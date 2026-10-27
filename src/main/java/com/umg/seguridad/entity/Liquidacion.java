package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "liquidacion")
@Getter
@Setter
public class Liquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idLiquidacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdEmpleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdPuesto", nullable = false)
    private Puesto puesto;

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaContratacion;

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEgreso;

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaLiquidacion;

    @Column(nullable = false, length = 50)
    private String motivoEgreso;

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

    @Column(precision = 10, scale = 2)
    private BigDecimal totalIngresos;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalNeto;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
}
