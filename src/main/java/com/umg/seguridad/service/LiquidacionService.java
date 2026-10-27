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
public class LiquidacionService {

    private final LiquidacionRepository repository;

    public LiquidacionService(LiquidacionRepository repository) {
        this.repository = repository;
    }

    public List<Liquidacion> listar() {
        return repository.findAll();
    }

    public Liquidacion obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrado: " + id));
    }

    @Transactional
    public Liquidacion guardar(Liquidacion e) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdLiquidacion() == null) {
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia.
        Liquidacion existente = obtenerPorId(e.getIdLiquidacion());
        existente.setEmpleado(e.getEmpleado());
        existente.setPuesto(e.getPuesto());
        existente.setFechaContratacion(e.getFechaContratacion());
        existente.setFechaEgreso(e.getFechaEgreso());
        existente.setFechaLiquidacion(e.getFechaLiquidacion());
        existente.setMotivoEgreso(e.getMotivoEgreso());
        existente.setIngresoSueldoBase(e.getIngresoSueldoBase());
        existente.setIngresoBonificacionDecreto(e.getIngresoBonificacionDecreto());
        existente.setIngresoOtrosIngresos(e.getIngresoOtrosIngresos());
        existente.setDescuentoIgss(e.getDescuentoIgss());
        existente.setDescuentoIsr(e.getDescuentoIsr());
        existente.setDescuentoInasistencias(e.getDescuentoInasistencias());
        existente.setSalarioNeto(e.getSalarioNeto());
        existente.setTotalIngresos(e.getTotalIngresos());
        existente.setTotalDescuentos(e.getTotalDescuentos());
        existente.setTotalNeto(e.getTotalNeto());
        return repository.save(existente);
    }

    private void validar(Liquidacion e) {
        if (e.getEmpleado() == null) {
            throw new IllegalArgumentException("Debe seleccionar Empleado.");
        }
        if (e.getPuesto() == null) {
            throw new IllegalArgumentException("Debe seleccionar Puesto.");
        }
        if (e.getFechaContratacion() == null) {
            throw new IllegalArgumentException("El campo Fecha de contratación es obligatorio.");
        }
        if (e.getFechaEgreso() == null) {
            throw new IllegalArgumentException("El campo Fecha de egreso es obligatorio.");
        }
        if (e.getFechaLiquidacion() == null) {
            throw new IllegalArgumentException("El campo Fecha de liquidación es obligatorio.");
        }
        if (e.getMotivoEgreso() == null || e.getMotivoEgreso().isBlank()) {
            throw new IllegalArgumentException("El campo Motivo de egreso es obligatorio.");
        }
        if (e.getMotivoEgreso() != null) {
            e.setMotivoEgreso(e.getMotivoEgreso().trim().isEmpty() ? null : e.getMotivoEgreso().trim());
            if (e.getMotivoEgreso() != null && e.getMotivoEgreso().length() > 50) {
                throw new IllegalArgumentException("El campo Motivo de egreso no puede exceder 50 caracteres.");
            }
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
        if (e.getFechaEgreso().isBefore(e.getFechaContratacion())) {
            throw new IllegalArgumentException("La fecha de egreso no puede ser anterior a la fecha de contratación.");
        }
        // Calculo de la liquidacion (se recalcula siempre al guardar)
        BigDecimal totalIngresos = e.getIngresoSueldoBase()
                .add(e.getIngresoBonificacionDecreto()).add(e.getIngresoOtrosIngresos());
        BigDecimal totalDescuentos = e.getDescuentoIgss()
                .add(e.getDescuentoIsr()).add(e.getDescuentoInasistencias());
        BigDecimal neto = totalIngresos.subtract(totalDescuentos);
        e.setTotalIngresos(totalIngresos);
        e.setTotalDescuentos(totalDescuentos);
        e.setSalarioNeto(neto);
        e.setTotalNeto(neto);

    }
}
