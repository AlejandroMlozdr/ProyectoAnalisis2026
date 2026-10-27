package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmpleadoService {

    private final EmpleadoRepository repository;
    private final FlujoStatusEmpleadoRepository flujoRepository;

    public EmpleadoService(EmpleadoRepository repository, FlujoStatusEmpleadoRepository flujoRepository) {
        this.repository = repository;
        this.flujoRepository = flujoRepository;
    }

    public List<Empleado> listar() {
        return repository.findAll();
    }

    public Empleado obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado: " + id));
    }

    @Transactional
    public Empleado guardar(Empleado e) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdEmpleado() == null) {
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia.
        Empleado existente = obtenerPorId(e.getIdEmpleado());
        // Un cambio de status solo es valido si existe esa transicion en Flujos Status Empleado
        Integer statusActual = existente.getStatusEmpleado().getIdStatusEmpleado();
        Integer statusNuevo = e.getStatusEmpleado().getIdStatusEmpleado();
        if (!statusActual.equals(statusNuevo)
                && !flujoRepository.existsById(new FlujoStatusEmpleadoId(statusActual, statusNuevo))) {
            throw new IllegalArgumentException("El cambio de status de \"" + existente.getStatusEmpleado().getNombre()
                    + "\" a \"" + e.getStatusEmpleado().getNombre()
                    + "\" no está permitido: no existe ese flujo en Flujos Status Empleado.");
        }
        existente.setPersona(e.getPersona());
        existente.setSucursal(e.getSucursal());
        existente.setFechaContratacion(e.getFechaContratacion());
        existente.setPuesto(e.getPuesto());
        existente.setStatusEmpleado(e.getStatusEmpleado());
        existente.setIngresoSueldoBase(e.getIngresoSueldoBase());
        existente.setIngresoBonificacionDecreto(e.getIngresoBonificacionDecreto());
        existente.setIngresoOtrosIngresos(e.getIngresoOtrosIngresos());
        existente.setDescuentoIgss(e.getDescuentoIgss());
        existente.setDescuentoIsr(e.getDescuentoIsr());
        existente.setDescuentoInasistencias(e.getDescuentoInasistencias());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer id) {
        repository.deleteById(id);
    }

    private void validar(Empleado e) {
        if (e.getPersona() == null) {
            throw new IllegalArgumentException("Debe seleccionar Persona.");
        }
        if (e.getSucursal() == null) {
            throw new IllegalArgumentException("Debe seleccionar Sucursal.");
        }
        if (e.getFechaContratacion() == null) {
            throw new IllegalArgumentException("El campo Fecha de contratación es obligatorio.");
        }
        if (e.getPuesto() == null) {
            throw new IllegalArgumentException("Debe seleccionar Puesto.");
        }
        if (e.getStatusEmpleado() == null) {
            throw new IllegalArgumentException("Debe seleccionar Status.");
        }
        if (e.getIngresoSueldoBase() == null) {
            e.setIngresoSueldoBase(BigDecimal.ZERO);
        }
        if (e.getIngresoSueldoBase().signum() < 0) {
            throw new IllegalArgumentException("El campo Sueldo base no puede ser negativo.");
        }
        if (e.getIngresoBonificacionDecreto() == null) {
            e.setIngresoBonificacionDecreto(BigDecimal.ZERO);
        }
        if (e.getIngresoBonificacionDecreto().signum() < 0) {
            throw new IllegalArgumentException("El campo Bonificación Decreto no puede ser negativo.");
        }
        if (e.getIngresoOtrosIngresos() == null) {
            e.setIngresoOtrosIngresos(BigDecimal.ZERO);
        }
        if (e.getIngresoOtrosIngresos().signum() < 0) {
            throw new IllegalArgumentException("El campo Otros ingresos no puede ser negativo.");
        }
        if (e.getDescuentoIgss() == null) {
            e.setDescuentoIgss(BigDecimal.ZERO);
        }
        if (e.getDescuentoIgss().signum() < 0) {
            throw new IllegalArgumentException("El campo Descuento IGSS no puede ser negativo.");
        }
        if (e.getDescuentoIsr() == null) {
            e.setDescuentoIsr(BigDecimal.ZERO);
        }
        if (e.getDescuentoIsr().signum() < 0) {
            throw new IllegalArgumentException("El campo Descuento ISR no puede ser negativo.");
        }
        if (e.getDescuentoInasistencias() == null) {
            e.setDescuentoInasistencias(BigDecimal.ZERO);
        }
        if (e.getDescuentoInasistencias().signum() < 0) {
            throw new IllegalArgumentException("El campo Descuento por inasistencias no puede ser negativo.");
        }
    }
}
