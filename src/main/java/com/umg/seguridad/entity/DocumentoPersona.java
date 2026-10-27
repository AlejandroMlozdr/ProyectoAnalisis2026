package com.umg.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_persona")
@IdClass(DocumentoPersonaId.class)
@Getter
@Setter
public class DocumentoPersona {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdPersona")
    private Persona persona;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IdTipoDocumento")
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 50)
    private String noDocumento;

    private LocalDateTime fechaCreacion;
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioModificacion;
}
