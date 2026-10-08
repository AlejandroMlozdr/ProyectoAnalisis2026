package com.umg.seguridad.dto;

// Una fila de la cuadricula "Asignar Opciones a un Rol": una Opcion con sus permisos.
// "consultar" NO es una columna nueva en la base de datos: es solo la marca en
// pantalla de si el rol debe poder VER esa opcion. Al guardar, si viene en false,
// se borra la fila de ROLE_OPCION (asi PermisoService.puedeConsultar/construirMenu
// dejan de mostrarla); si viene en true, se crea/actualiza la fila normalmente.
public class PermisoRow {

    private Integer idOpcion;
    private String nombreOpcion;
    private boolean consultar;
    private boolean alta;
    private boolean baja;
    private boolean cambio;
    private boolean imprimir;
    private boolean exportar;

    public Integer getIdOpcion() { return idOpcion; }
    public void setIdOpcion(Integer idOpcion) { this.idOpcion = idOpcion; }

    public String getNombreOpcion() { return nombreOpcion; }
    public void setNombreOpcion(String nombreOpcion) { this.nombreOpcion = nombreOpcion; }

    public boolean isConsultar() { return consultar; }
    public void setConsultar(boolean consultar) { this.consultar = consultar; }

    public boolean isAlta() { return alta; }
    public void setAlta(boolean alta) { this.alta = alta; }

    public boolean isBaja() { return baja; }
    public void setBaja(boolean baja) { this.baja = baja; }

    public boolean isCambio() { return cambio; }
    public void setCambio(boolean cambio) { this.cambio = cambio; }

    public boolean isImprimir() { return imprimir; }
    public void setImprimir(boolean imprimir) { this.imprimir = imprimir; }

    public boolean isExportar() { return exportar; }
    public void setExportar(boolean exportar) { this.exportar = exportar; }
}
