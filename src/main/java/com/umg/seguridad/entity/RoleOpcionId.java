package com.umg.seguridad.entity;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class RoleOpcionId implements Serializable {

    private Integer role;
    private Integer opcion;

    public RoleOpcionId() {
    }

    public RoleOpcionId(Integer role, Integer opcion) {
        this.role = role;
        this.opcion = opcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleOpcionId)) return false;
        RoleOpcionId that = (RoleOpcionId) o;
        return Objects.equals(role, that.role) && Objects.equals(opcion, that.opcion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, opcion);
    }
}
