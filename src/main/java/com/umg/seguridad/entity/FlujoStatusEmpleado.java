package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "flujo_status_empleado")
@IdClass(FlujoStatusEmpleadoId.class)
@Getter
@Setter
public class FlujoStatusEmpleado {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdStatusActual")
    private StatusEmpleado statusActual;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdStatusNuevo")
    private StatusEmpleado statusNuevo;

    @Column(nullable = false, length = 50)
    private String nombreEvento;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;
}
