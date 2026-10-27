package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "opcion")
@Getter
@Setter
public class Opcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idOpcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idMenu", nullable = false)
    private Menu menu;

    @Column(nullable = false, length = 100)
    private String nombre;

    private Integer ordenMenu;

    @Column(length = 255)
    private String pagina; // ruta/URL asociada a la opcion

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;
}
