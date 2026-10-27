package com.umg.seguridad.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/** Llave compuesta de DocumentoPersona (mismo patron que RoleOpcionId: cada campo es el id de la entidad relacionada). */
@Getter
@Setter
public class DocumentoPersonaId implements Serializable {

    private Integer persona;
    private Integer tipoDocumento;

    public DocumentoPersonaId() {
    }

    public DocumentoPersonaId(Integer persona, Integer tipoDocumento) {
        this.persona = persona;
        this.tipoDocumento = tipoDocumento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentoPersonaId)) return false;
        DocumentoPersonaId that = (DocumentoPersonaId) o;
        return Objects.equals(persona, that.persona) && Objects.equals(tipoDocumento, that.tipoDocumento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(persona, tipoDocumento);
    }
}
