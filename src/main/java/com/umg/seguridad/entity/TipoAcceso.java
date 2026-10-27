package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "tipo_acceso")
@Getter
@Setter
public class TipoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTipoAcceso;

    @Column(nullable = false, length = 50)
    private String nombre; // ej: Login exitoso, Login fallido, Logout, Bloqueo por intentos

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;
}
