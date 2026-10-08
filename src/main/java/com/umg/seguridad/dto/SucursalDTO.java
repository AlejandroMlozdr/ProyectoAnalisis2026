package com.umg.seguridad.dto;

// DTO simple para el endpoint AJAX del ComboBox dependiente Empresa -> Sucursal.
// No se devuelve la entidad Sucursal directamente para evitar problemas de
// serializacion con las relaciones LAZY de Hibernate (Empresa, auditoria, etc).
public record SucursalDTO(Integer idSucursal, String nombre) {
}
