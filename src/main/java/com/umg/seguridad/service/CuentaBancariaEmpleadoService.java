package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CuentaBancariaEmpleadoService {

    private final CuentaBancariaEmpleadoRepository repository;

    public CuentaBancariaEmpleadoService(CuentaBancariaEmpleadoRepository repository) {
        this.repository = repository;
    }

    public List<CuentaBancariaEmpleado> listar() {
        return repository.findAll();
    }

    public CuentaBancariaEmpleado obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta Bancaria no encontrado: " + id));
    }

    @Transactional
    public CuentaBancariaEmpleado guardar(CuentaBancariaEmpleado e) {
        validar(e);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdCuentaBancaria() == null) {
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia.
        CuentaBancariaEmpleado existente = obtenerPorId(e.getIdCuentaBancaria());
        existente.setEmpleado(e.getEmpleado());
        existente.setBanco(e.getBanco());
        existente.setNumeroDeCuenta(e.getNumeroDeCuenta());
        existente.setActiva(e.getActiva());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    public void eliminar(Integer id) {
        repository.deleteById(id);
    }

    private void validar(CuentaBancariaEmpleado e) {
        if (e.getEmpleado() == null) {
            throw new IllegalArgumentException("Debe seleccionar Empleado.");
        }
        if (e.getBanco() == null) {
            throw new IllegalArgumentException("Debe seleccionar Banco.");
        }
        if (e.getNumeroDeCuenta() == null || e.getNumeroDeCuenta().isBlank()) {
            throw new IllegalArgumentException("El campo Número de cuenta es obligatorio.");
        }
        if (e.getNumeroDeCuenta() != null) {
            e.setNumeroDeCuenta(e.getNumeroDeCuenta().trim().isEmpty() ? null : e.getNumeroDeCuenta().trim());
            if (e.getNumeroDeCuenta() != null && e.getNumeroDeCuenta().length() > 50) {
                throw new IllegalArgumentException("El campo Número de cuenta no puede exceder 50 caracteres.");
            }
        }
        if (e.getActiva() == null) {
            e.setActiva('0');
        }
    }
}
