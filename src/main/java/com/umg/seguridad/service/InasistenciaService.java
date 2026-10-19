package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Inasistencias de Empleados (Transaccion / CRUD).
 *
 * Reglas de negocio:
 *  - Solo se registran inasistencias nuevas para empleados con status "Activo".
 *  - La fecha inicial no puede ser anterior a la fecha de contratacion del empleado.
 *  - Un empleado no puede tener dos inasistencias que se traslapen en dias
 *    (Calcular Planilla descontaria dos veces los mismos dias).
 *  - Una inasistencia con FechaProcesado ya fue tomada en cuenta por una
 *    planilla o una liquidacion: queda como historial y no se puede
 *    modificar ni eliminar.
 */
@Service
public class InasistenciaService {

    public static final String STATUS_ACTIVO = "Activo";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InasistenciaRepository repository;

    public InasistenciaService(InasistenciaRepository repository) {
        this.repository = repository;
    }

    public List<Inasistencia> listar() {
        return repository.findAll();
    }

    public Inasistencia obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inasistencia no encontrada: " + id));
    }

    /** Lanza IllegalArgumentException si la inasistencia ya fue procesada (no editable). */
    public void validarEditable(Inasistencia e) {
        if (e.getFechaProcesado() != null) {
            throw new IllegalArgumentException("Esta inasistencia ya fue procesada el "
                    + e.getFechaProcesado().format(FORMATO_FECHA)
                    + " (planilla o liquidación) y forma parte del historial: no se puede modificar ni eliminar.");
        }
    }

    @Transactional
    public Inasistencia guardar(Inasistencia e) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();

        if (e.getIdInasistencia() == null) {
            validar(e, true);
            e.setFechaProcesado(null); // nunca se recibe desde el formulario
            e.setFechaCreacion(ahora);
            e.setUsuarioCreacion(usuarioActual);
            return repository.save(e);
        }

        // Edicion: se parte del registro guardado para no perder la auditoria de creacion
        // ni los campos que el formulario no envia (FechaProcesado).
        Inasistencia existente = obtenerPorId(e.getIdInasistencia());
        validarEditable(existente);
        boolean cambioEmpleado = e.getEmpleado() != null && existente.getEmpleado() != null
                && !e.getEmpleado().getIdEmpleado().equals(existente.getEmpleado().getIdEmpleado());
        validar(e, cambioEmpleado);

        existente.setEmpleado(e.getEmpleado());
        existente.setFechaInicial(e.getFechaInicial());
        existente.setFechaFinal(e.getFechaFinal());
        existente.setMotivoInasistencia(e.getMotivoInasistencia());
        existente.setFechaModificacion(ahora);
        existente.setUsuarioModificacion(usuarioActual);
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Inasistencia existente = obtenerPorId(id);
        validarEditable(existente);
        repository.delete(existente);
    }

    /**
     * @param exigirActivo true cuando se registra la inasistencia a un empleado
     *                     (alta, o cambio de empleado en una edicion).
     */
    private void validar(Inasistencia e, boolean exigirActivo) {
        if (e.getEmpleado() == null) {
            throw new IllegalArgumentException("Debe seleccionar Empleado.");
        }
        if (e.getFechaInicial() == null) {
            throw new IllegalArgumentException("El campo Fecha inicial es obligatorio.");
        }
        if (e.getFechaFinal() == null) {
            throw new IllegalArgumentException("El campo Fecha final es obligatorio.");
        }
        if (e.getMotivoInasistencia() != null) {
            e.setMotivoInasistencia(e.getMotivoInasistencia().trim().isEmpty() ? null : e.getMotivoInasistencia().trim());
            if (e.getMotivoInasistencia() != null && e.getMotivoInasistencia().length() > 300) {
                throw new IllegalArgumentException("El campo Motivo no puede exceder 300 caracteres.");
            }
        }
        if (e.getFechaFinal().isBefore(e.getFechaInicial())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }

        Empleado empleado = e.getEmpleado();
        String status = empleado.getStatusEmpleado() != null ? empleado.getStatusEmpleado().getNombre() : null;
        if (exigirActivo && !STATUS_ACTIVO.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Solo se pueden registrar inasistencias a empleados con status \""
                    + STATUS_ACTIVO + "\". El empleado seleccionado está en \"" + status + "\".");
        }

        LocalDate contratacion = empleado.getFechaContratacion();
        if (contratacion != null && e.getFechaInicial().toLocalDate().isBefore(contratacion)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser anterior a la fecha de contratación del empleado ("
                    + contratacion.format(FORMATO_FECHA) + ").");
        }

        // Traslapes por dia: la planilla descuenta dias completos, asi que dos
        // inasistencias que comparten un mismo dia descontarian ese dia dos veces.
        LocalDate desde = e.getFechaInicial().toLocalDate();
        LocalDate hasta = e.getFechaFinal().toLocalDate();
        for (Inasistencia otra : repository.findByEmpleado_IdEmpleado(empleado.getIdEmpleado())) {
            if (otra.getIdInasistencia().equals(e.getIdInasistencia())) {
                continue; // es el mismo registro que se esta editando
            }
            LocalDate otraDesde = otra.getFechaInicial().toLocalDate();
            LocalDate otraHasta = otra.getFechaFinal().toLocalDate();
            if (!desde.isAfter(otraHasta) && !hasta.isBefore(otraDesde)) {
                throw new IllegalArgumentException("El empleado ya tiene una inasistencia registrada del "
                        + otraDesde.format(FORMATO_FECHA) + " al " + otraHasta.format(FORMATO_FECHA)
                        + " que se traslapa con las fechas ingresadas.");
            }
        }
    }
}
