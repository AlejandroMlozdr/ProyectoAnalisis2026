package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Tabla puente que define, para cada combinacion Rol-Opcion,
 * que acciones puede ejecutar el usuario.
 * OJO: la tabla real NO tiene columna "Consultar" (todo rol puede consultar
 * las opciones que ve en su menu); solo Alta, Baja, Cambio, Imprimir, Exportar.
 * Ademas son INT (0/1) en la base, no BIT, por eso se mapean como Integer.
 */
@Entity
@Table(name = "role_opcion")
@IdClass(RoleOpcionId.class)
@Getter
@Setter
public class RoleOpcion {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdRole")
    private Role role;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdOpcion")
    private Opcion opcion;

    @Column(nullable = false)
    private Integer alta = 0;

    @Column(nullable = false)
    private Integer baja = 0;

    @Column(nullable = false)
    private Integer cambio = 0;

    @Column(nullable = false)
    private Integer imprimir = 0;

    @Column(nullable = false)
    private Integer exportar = 0;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;

    // Helpers para leer los permisos como boolean en el resto del codigo
    @Transient
    public boolean puedeAlta() { return alta != null && alta != 0; }
    @Transient
    public boolean puedeBaja() { return baja != null && baja != 0; }
    @Transient
    public boolean puedeCambio() { return cambio != null && cambio != 0; }
    @Transient
    public boolean puedeImprimir() { return imprimir != null && imprimir != 0; }
    @Transient
    public boolean puedeExportar() { return exportar != null && exportar != 0; }
}
