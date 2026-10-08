package com.umg.seguridad.dto;

import java.util.ArrayList;
import java.util.List;

// Objeto que respalda el formulario completo de la pantalla "Asignar Opciones a un Rol".
public class AsignarPermisosForm {

    private Integer idRole;
    private Integer idModulo;
    private List<PermisoRow> permisos = new ArrayList<>();

    public Integer getIdRole() { return idRole; }
    public void setIdRole(Integer idRole) { this.idRole = idRole; }

    public Integer getIdModulo() { return idModulo; }
    public void setIdModulo(Integer idModulo) { this.idModulo = idModulo; }

    public List<PermisoRow> getPermisos() { return permisos; }
    public void setPermisos(List<PermisoRow> permisos) { this.permisos = permisos; }
}
