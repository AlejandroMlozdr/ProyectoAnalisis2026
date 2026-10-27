package com.umg.seguridad.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/** Llave compuesta de FlujoStatusEmpleado (mismo patron que RoleOpcionId: cada campo es el id de la entidad relacionada). */
@Getter
@Setter
public class FlujoStatusEmpleadoId implements Serializable {

    private Integer statusActual;
    private Integer statusNuevo;

    public FlujoStatusEmpleadoId() {
    }

    public FlujoStatusEmpleadoId(Integer statusActual, Integer statusNuevo) {
        this.statusActual = statusActual;
        this.statusNuevo = statusNuevo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlujoStatusEmpleadoId)) return false;
        FlujoStatusEmpleadoId that = (FlujoStatusEmpleadoId) o;
        return Objects.equals(statusActual, that.statusActual) && Objects.equals(statusNuevo, that.statusNuevo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusActual, statusNuevo);
    }
}
