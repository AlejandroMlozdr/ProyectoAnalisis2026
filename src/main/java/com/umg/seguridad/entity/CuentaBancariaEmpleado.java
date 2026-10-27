package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cuenta_bancaria_empleado")
@Getter
@Setter
public class CuentaBancariaEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCuentaBancaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdEmpleado", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdBanco", nullable = false)
    private Banco banco;

    @Column(nullable = false, length = 50)
    private String numeroDeCuenta;

    // CHAR(1): '1' = activa, '0' = inactiva
    @Column(nullable = false, length = 1)
    private Character activa;

    /** Para el checkbox del formulario: una cuenta nueva (sin valor) se muestra marcada. */
    public boolean isActivaMarcada() {
        return activa == null || activa == '1';
    }

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;
}
