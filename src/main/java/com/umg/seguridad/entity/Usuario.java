package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {

    // IMPORTANTE: en la tabla real, IdUsuario ES el nombre de usuario (varchar),
    // no un id autoincremental. No lleva @GeneratedValue: se asigna a mano al crear el usuario.
    @Id
    @Column(name = "IdUsuario", length = 100)
    private String idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdStatusUsuario", nullable = false)
    private StatusUsuario statusUsuario;

    @Column(nullable = false, length = 100)
    private String password; // se guarda encriptado (BCrypt)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdGenero", nullable = false)
    private Genero genero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdSucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdRole", nullable = false)
    private Role role;

    @Column(length = 100)
    private String correoElectronico;

    @Column(length = 30)
    private String telefonoMovil;

    @Lob
    @Column(columnDefinition = "mediumblob")
    private byte[] fotografia;

    @Column(nullable = false, length = 200)
    private String pregunta;

    @Column(nullable = false, length = 200)
    private String respuesta;

    // Campos que administra el propio sistema, no el usuario
    private LocalDateTime ultimaFechaIngreso;
    private Integer intentosDeAcceso = 0;

    @Column(length = 100)
    private String sesionActual;

    private LocalDateTime ultimaFechaCambioPassword;

    // En la tabla es INT (0/1), no boolean
    private Integer requiereCambiarPassword = 0;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;
}
