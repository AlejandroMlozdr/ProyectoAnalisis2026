package com.umg.seguridad.service;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Proceso de Calculo de Planilla y consultas de planilla ya procesada
 * (cabecera, detalle, boletas).
 *
 * Reglas del calculo (documentadas tambien en el README):
 *  - Entran los empleados con status "Activo" contratados hasta la fecha fin del periodo.
 *  - Ingresos y descuentos de IGSS/ISR se toman tal como estan registrados en el empleado.
 *  - El descuento por inasistencias se calcula con las inasistencias que se traslapan con el
 *    periodo: dias ausentes (recortados al periodo, maximo 30) x (sueldo base / 30).
 *  - Salario neto = sueldo base + bonificacion + otros ingresos - IGSS - ISR - inasistencias.
 *  - La cabecera guarda la suma de ingresos, de descuentos y el neto de todos los empleados.
 */
@Service
public class PlanillaService {

    private static final BigDecimal DIAS_MES = BigDecimal.valueOf(30);
    private static final String STATUS_PAGABLE = "Activo";

    private final PeriodoPlanillaRepository periodoRepository;
    private final PlanillaCabeceraRepository cabeceraRepository;
    private final PlanillaDetalleRepository detalleRepository;
    private final EmpleadoRepository empleadoRepository;
    private final InasistenciaRepository inasistenciaRepository;
    private final CuentaBancariaEmpleadoRepository cuentaRepository;

    public PlanillaService(PeriodoPlanillaRepository periodoRepository,
                           PlanillaCabeceraRepository cabeceraRepository,
                           PlanillaDetalleRepository detalleRepository,
                           EmpleadoRepository empleadoRepository,
                           InasistenciaRepository inasistenciaRepository,
                           CuentaBancariaEmpleadoRepository cuentaRepository) {
        this.periodoRepository = periodoRepository;
        this.cabeceraRepository = cabeceraRepository;
        this.detalleRepository = detalleRepository;
        this.empleadoRepository = empleadoRepository;
        this.inasistenciaRepository = inasistenciaRepository;
        this.cuentaRepository = cuentaRepository;
    }

    // ---------------------------------------------------------------- consultas

    /** Convierte el valor de un combo ("2026-9") en la llave del periodo. */
    public static PeriodoPlanillaId parsePeriodo(String valor) {
        if (valor == null || !valor.matches("\\d{4}-\\d{1,2}")) {
            throw new IllegalArgumentException("Período no válido.");
        }
        String[] partes = valor.split("-");
        return new PeriodoPlanillaId(Integer.valueOf(partes[0]), Integer.valueOf(partes[1]));
    }

    public List<PlanillaCabecera> listarCabeceras() {
        return cabeceraRepository.findAllOrdenadas();
    }

    public PlanillaCabecera obtenerCabecera(PeriodoPlanillaId id) {
        return cabeceraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El período " + id.getMes() + "/" + id.getAnio() + " todavía no ha sido procesado."));
    }

    public List<PlanillaDetalle> detallesDelPeriodo(PeriodoPlanillaId id) {
        return detalleRepository.findDelPeriodo(id.getAnio(), id.getMes());
    }

    public Optional<CuentaBancariaEmpleado> cuentaActiva(Integer idEmpleado) {
        return cuentaRepository.findFirstByEmpleado_IdEmpleadoAndActiva(idEmpleado, '1');
    }

    // ---------------------------------------------------------------- proceso

    @Transactional
    public PlanillaCabecera procesar(Integer anio, Integer mes, boolean reprocesar) {
        PeriodoPlanillaId id = new PeriodoPlanillaId(anio, mes);
        PeriodoPlanilla periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El período " + mes + "/" + anio + " no existe."));
        if (periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
            throw new IllegalArgumentException("El período " + mes + "/" + anio + " no tiene fechas de inicio y fin.");
        }
        LocalDate inicio = periodo.getFechaInicio();
        LocalDate fin = periodo.getFechaFin();

        boolean yaProcesada = cabeceraRepository.existsById(id);
        if (yaProcesada && !reprocesar) {
            throw new IllegalArgumentException("El período " + mes + "/" + anio
                    + " ya fue procesado. Marca la opción \"Recalcular\" si deseas procesarlo de nuevo.");
        }

        // Se borra ANTES de cargar empleados: el borrado masivo limpia el contexto de persistencia.
        detalleRepository.borrarDelPeriodo(anio, mes);

        List<Empleado> empleados = empleadoRepository
                .findByStatusEmpleado_NombreIgnoreCaseAndFechaContratacionLessThanEqual(STATUS_PAGABLE, fin);
        if (empleados.isEmpty()) {
            throw new IllegalArgumentException("No hay empleados con status \"" + STATUS_PAGABLE
                    + "\" para procesar en el período " + mes + "/" + anio + ".");
        }

        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime finExclusivo = fin.plusDays(1).atStartOfDay();

        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        List<PlanillaDetalle> detalles = new ArrayList<>();
        List<Inasistencia> consideradas = new ArrayList<>();

        for (Empleado emp : empleados) {
            long dias = 0;
            for (Inasistencia ina : inasistenciaRepository.findSolapadas(emp.getIdEmpleado(), inicioDt, finExclusivo)) {
                LocalDate desde = ina.getFechaInicial().toLocalDate().isBefore(inicio)
                        ? inicio : ina.getFechaInicial().toLocalDate();
                LocalDate hasta = ina.getFechaFinal().toLocalDate().isAfter(fin)
                        ? fin : ina.getFechaFinal().toLocalDate();
                if (!hasta.isBefore(desde)) {
                    dias += ChronoUnit.DAYS.between(desde, hasta) + 1;
                }
                consideradas.add(ina);
            }
            dias = Math.min(dias, 30);

            BigDecimal base = nz(emp.getIngresoSueldoBase());
            BigDecimal bonificacion = nz(emp.getIngresoBonificacionDecreto());
            BigDecimal otros = nz(emp.getIngresoOtrosIngresos());
            BigDecimal igss = nz(emp.getDescuentoIgss());
            BigDecimal isr = nz(emp.getDescuentoIsr());
            BigDecimal inasistencias = base.divide(DIAS_MES, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);

            BigDecimal ingresos = base.add(bonificacion).add(otros);
            BigDecimal descuentos = igss.add(isr).add(inasistencias);

            PlanillaDetalle d = new PlanillaDetalle();
            d.setAnio(anio);
            d.setMes(mes);
            d.setEmpleado(emp);
            d.setFechaContratacion(emp.getFechaContratacion());
            d.setPuesto(emp.getPuesto());
            d.setStatusEmpleado(emp.getStatusEmpleado());
            d.setIngresoSueldoBase(base);
            d.setIngresoBonificacionDecreto(bonificacion);
            d.setIngresoOtrosIngresos(otros);
            d.setDescuentoIgss(igss);
            d.setDescuentoIsr(isr);
            d.setDescuentoInasistencias(inasistencias);
            d.setSalarioNeto(ingresos.subtract(descuentos));
            d.setFechaCreacion(ahora);
            d.setUsuarioCreacion(usuario);
            detalles.add(d);

            totalIngresos = totalIngresos.add(ingresos);
            totalDescuentos = totalDescuentos.add(descuentos);
        }

        PlanillaCabecera cabecera = cabeceraRepository.findById(id).orElse(null);
        if (cabecera == null) {
            cabecera = new PlanillaCabecera();
            cabecera.setId(id);
            cabecera.setFechaCreacion(ahora);
            cabecera.setUsuarioCreacion(usuario);
        } else {
            cabecera.setFechaModificacion(ahora);
            cabecera.setUsuarioModificacion(usuario);
        }
        cabecera.setTotalIngresos(totalIngresos);
        cabecera.setTotalDescuentos(totalDescuentos);
        cabecera.setSalarioNeto(totalIngresos.subtract(totalDescuentos));
        cabecera.setFechaHoraProcesada(ahora);
        // La cabecera debe existir en la BD antes que sus detalles (FK Anio+Mes)
        cabecera = cabeceraRepository.saveAndFlush(cabecera);

        detalleRepository.saveAll(detalles);

        // Marca las inasistencias que se tomaron en cuenta
        for (Inasistencia ina : consideradas) {
            if (ina.getFechaProcesado() == null) {
                ina.setFechaProcesado(ahora);
                ina.setFechaModificacion(ahora);
                ina.setUsuarioModificacion(usuario);
                inasistenciaRepository.save(ina);
            }
        }
        return cabecera;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
