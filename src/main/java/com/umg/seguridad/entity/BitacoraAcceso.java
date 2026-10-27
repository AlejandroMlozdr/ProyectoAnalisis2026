package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_acceso")
@Getter
@Setter
public class BitacoraAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdBitacoraAcceso")
    private Integer idBitacoraAcceso;

    // IdUsuario en BITACORA_ACCESO es varchar(100) porque el username
    // (IdUsuario en la tabla USUARIO) es la propia llave primaria de Usuario.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdUsuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdTipoAcceso", nullable = false)
    private TipoAcceso tipoAcceso;

    @Column(nullable = false)
    private LocalDateTime fechaAcceso;

    @Column(length = 200)
    private String httpUserAgent;

    @Column(length = 50)
    private String direccionIp;

    // En la tabla real es VARCHAR(100), no boolean.
    // Se puede usar para guardar algo como "Exitoso" / "Fallido", a definir con el equipo.
    @Column(length = 100)
    private String acceso;

    @Column(length = 50)
    private String sistemaOperativo;

    @Column(length = 50)
    private String dispositivo;

    @Column(length = 50)
    private String browser;

    @Column(length = 100)
    private String sesion;
}
