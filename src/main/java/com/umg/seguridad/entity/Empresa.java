package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresa")
@Getter
@Setter
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmpresa;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(length = 20)
    private String nit;

    // Politicas de password de la empresa
    private Integer passwordCantidadMayusculas;
    private Integer passwordCantidadMinusculas;
    private Integer passwordCantidadCaracteresEspeciales;
    private Integer passwordCantidadCaducidadDias;
    private Integer passwordLargo;
    private Integer passwordIntentosAntesDeBloquear;
    private Integer passwordCantidadNumeros;
    private Integer passwordCantidadPreguntasValidar;

    // Auditoria
    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;
}
