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
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Liquidacion de Empleado (Proceso / Transaccion).
 *
 * Calculo (se recalcula siempre en el servidor al guardar):
 *  - Ingresos del ultimo salario = sueldo base + bonificacion decreto + otros ingresos.
 *  - Descuentos = IGSS + ISR + inasistencias.
 *  - Salario neto = ingresos del ultimo salario - descuentos.
 *  - Prestaciones laborales (Codigo de Trabajo de Guatemala), calculadas sobre el
 *    sueldo base (la bonificacion incentivo, Decreto 78-89, no forma parte del
 *    salario para prestaciones):
 *      * Indemnizacion (Art. 82): solo en despido injustificado.
 *        sueldo x 7/6 x anios de servicio (proporcional por dias). El factor 7/6
 *        integra la doceava parte de aguinaldo y de Bono 14.
 *      * Aguinaldo proporcional (Decreto 76-78): periodo 1-dic a 30-nov.
 *        sueldo x dias laborados en el periodo / 365.
 *      * Bono 14 proporcional (Decreto 42-92): periodo 1-jul a 30-jun.
 *        sueldo x dias laborados en el periodo / 365.
 *      * Vacaciones proporcionales (Art. 130): 15 dias por anio de servicio.
 *        Se asume que las vacaciones de anios anteriores ya fueron gozadas y se
 *        paga lo proporcional desde el ultimo aniversario de contratacion:
 *        (sueldo / 30) x 15 x dias desde el aniversario / 365.
 *  - Total ingresos = ingresos del ultimo salario + prestaciones.
 *  - Total neto de la liquidacion = total ingresos - descuentos.
 *
 * Efectos sobre el empleado:
 *  - Al liquidar, el status del empleado cambia segun el motivo de egreso
 *    (Despido -> "Despedido", Renuncia / Fin de contrato -> "Baja"), validando
 *    que exista esa transicion en Flujos Status Empleado. Asi deja de entrar
 *    en Calcular Planilla.
 *  - Las inasistencias pendientes (sin FechaProcesado) hasta la fecha de egreso
 *    se marcan como procesadas por esta liquidacion.
 *  - Una misma relacion laboral (empleado + fecha de contratacion) solo se
 *    puede liquidar una vez.
 */
@Service
public class LiquidacionService {

    public static final String MOTIVO_DESPIDO_INJUSTIFICADO = "Despido injustificado";
    public static final String MOTIVO_DESPIDO_JUSTIFICADO = "Despido justificado";
    public static final String MOTIVO_RENUNCIA = "Renuncia";
    public static final String MOTIVO_FIN_CONTRATO = "Fin de contrato";
    public static final List<String> MOTIVOS = List.of(
            MOTIVO_DESPIDO_INJUSTIFICADO, MOTIVO_DESPIDO_JUSTIFICADO, MOTIVO_RENUNCIA, MOTIVO_FIN_CONTRATO);

    public static final String STATUS_DESPEDIDO = "Despedido";
    public static final String STATUS_BAJA = "Baja";

    private static final BigDecimal DIAS_ANIO = BigDecimal.valueOf(365);
    private static final BigDecimal DIAS_MES = BigDecimal.valueOf(30);
    private static final BigDecimal DIAS_VACACIONES_ANIO = BigDecimal.valueOf(15);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Desglose de prestaciones laborales de una liquidacion. */
    public record Prestaciones(long diasServicio, BigDecimal indemnizacion, BigDecimal aguinaldo,
                               BigDecimal bono14, BigDecimal vacaciones) {
        public BigDecimal total() {
            return indemnizacion.add(aguinaldo).add(bono14).add(vacaciones);
        }

        static Prestaciones vacias() {
            return new Prestaciones(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private final LiquidacionRepository repository;
    private final EmpleadoRepository empleadoRepository;
    private final InasistenciaRepository inasistenciaRepository;
    private final StatusEmpleadoRepository statusEmpleadoRepository;
    private final FlujoStatusEmpleadoRepository flujoRepository;

    public LiquidacionService(LiquidacionRepository repository,
                              EmpleadoRepository empleadoRepository,
                              InasistenciaRepository inasistenciaRepository,
                              StatusEmpleadoRepository statusEmpleadoRepository,
                              FlujoStatusEmpleadoRepository flujoRepository) {
        this.repository = repository;
        this.empleadoRepository = empleadoRepository;
        this.inasistenciaRepository = inasistenciaRepository;
        this.statusEmpleadoRepository = statusEmpleadoRepository;
        this.flujoRepository = flujoRepository;
    }

    public List<Liquidacion> listar() {
        return repository.findAll();
    }

    public Liquidacion obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada: " + id));
    }

    // ------------------------------------------------------------------ guardar

    @Transactional
    public Liquidacion guardar(Liquidacion e) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime ahora = LocalDateTime.now();
        boolean esNueva = e.getIdLiquidacion() == null;

        Liquidacion destino;
        String motivoAnterior = null;
        if (esNueva) {
            if (e.getEmpleado() == null) {
                throw new IllegalArgumentException("Debe seleccionar Empleado.");
            }
            destino = e;
        } else {
            // Edicion: el empleado de una liquidacion no se cambia (su status ya fue afectado).
            destino = obtenerPorId(e.getIdLiquidacion());
            motivoAnterior = destino.getMotivoEgreso();
            destino.setPuesto(e.getPuesto());
            destino.setFechaContratacion(e.getFechaContratacion());
            destino.setFechaEgreso(e.getFechaEgreso());
            destino.setFechaLiquidacion(e.getFechaLiquidacion());
            destino.setMotivoEgreso(e.getMotivoEgreso());
            destino.setIngresoSueldoBase(e.getIngresoSueldoBase());
            destino.setIngresoBonificacionDecreto(e.getIngresoBonificacionDecreto());
            destino.setIngresoOtrosIngresos(e.getIngresoOtrosIngresos());
            destino.setDescuentoIgss(e.getDescuentoIgss());
            destino.setDescuentoIsr(e.getDescuentoIsr());
            destino.setDescuentoInasistencias(e.getDescuentoInasistencias());
        }

        Empleado empleado = empleadoRepository.findById(destino.getEmpleado().getIdEmpleado())
                .orElseThrow(() -> new IllegalArgumentException("El empleado seleccionado no existe."));

        // El puesto y la fecha de contratacion se toman del empleado si no se enviaron.
        if (destino.getPuesto() == null) {
            destino.setPuesto(empleado.getPuesto());
        }
        if (destino.getFechaContratacion() == null) {
            destino.setFechaContratacion(empleado.getFechaContratacion());
        }

        validar(destino);
        validarNoDuplicada(destino);
        calcular(destino);

        // Cambia el status del empleado (Despedido / Baja) validando Flujos Status Empleado.
        // En una edicion solo se toca si el cambio de motivo implica otro status: asi,
        // editar una liquidacion antigua de un empleado recontratado no lo vuelve a dar de baja.
        if (esNueva || !statusDestino(motivoAnterior).equals(statusDestino(destino.getMotivoEgreso()))) {
            aplicarStatusEgreso(empleado, destino.getMotivoEgreso(), usuarioActual, ahora);
        }

        if (esNueva) {
            destino.setFechaCreacion(ahora);
            destino.setUsuarioCreacion(usuarioActual);
            marcarInasistenciasProcesadas(empleado.getIdEmpleado(), destino.getFechaEgreso(), usuarioActual, ahora);
        }
        return repository.save(destino);
    }

    private void validar(Liquidacion e) {
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
        String motivo = e.getMotivoEgreso().trim();
        e.setMotivoEgreso(MOTIVOS.stream()
                .filter(m -> m.equalsIgnoreCase(motivo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Motivo de egreso no válido. Seleccione uno de: "
                        + String.join(", ", MOTIVOS) + ".")));
        if (e.getFechaEgreso().isBefore(e.getFechaContratacion())) {
            throw new IllegalArgumentException("La fecha de egreso no puede ser anterior a la fecha de contratación.");
        }
        if (e.getFechaLiquidacion().isBefore(e.getFechaEgreso())) {
            throw new IllegalArgumentException("La fecha de liquidación no puede ser anterior a la fecha de egreso.");
        }

        e.setIngresoSueldoBase(noNegativo(e.getIngresoSueldoBase(), "Sueldo base"));
        e.setIngresoBonificacionDecreto(noNegativo(e.getIngresoBonificacionDecreto(), "Bonificación Decreto"));
        e.setIngresoOtrosIngresos(noNegativo(e.getIngresoOtrosIngresos(), "Otros ingresos"));
        e.setDescuentoIgss(noNegativo(e.getDescuentoIgss(), "Descuento IGSS"));
        e.setDescuentoIsr(noNegativo(e.getDescuentoIsr(), "Descuento ISR"));
        e.setDescuentoInasistencias(noNegativo(e.getDescuentoInasistencias(), "Descuento por inasistencias"));
        if (e.getIngresoSueldoBase().signum() == 0) {
            throw new IllegalArgumentException("El campo Sueldo base debe ser mayor a cero.");
        }
    }

    private static BigDecimal noNegativo(BigDecimal valor, String campo) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("El campo " + campo + " no puede ser negativo.");
        }
        return valor;
    }

    /** Una relacion laboral (empleado + fecha de contratacion) se liquida una sola vez. */
    private void validarNoDuplicada(Liquidacion e) {
        boolean duplicada = repository.findByEmpleado_IdEmpleado(e.getEmpleado().getIdEmpleado()).stream()
                .filter(otra -> !otra.getIdLiquidacion().equals(e.getIdLiquidacion()))
                .anyMatch(otra -> otra.getFechaContratacion().equals(e.getFechaContratacion()));
        if (duplicada) {
            throw new IllegalArgumentException("Este empleado ya tiene una liquidación registrada para la relación laboral "
                    + "iniciada el " + e.getFechaContratacion().format(FORMATO_FECHA) + ".");
        }
    }

    // ------------------------------------------------------------------ calculo

    private void calcular(Liquidacion e) {
        Prestaciones p = calcularPrestaciones(e.getFechaContratacion(), e.getFechaEgreso(),
                e.getIngresoSueldoBase(), e.getMotivoEgreso());

        BigDecimal ingresosSalario = e.getIngresoSueldoBase()
                .add(e.getIngresoBonificacionDecreto()).add(e.getIngresoOtrosIngresos());
        BigDecimal totalDescuentos = e.getDescuentoIgss()
                .add(e.getDescuentoIsr()).add(e.getDescuentoInasistencias());
        BigDecimal totalIngresos = ingresosSalario.add(p.total());

        e.setSalarioNeto(ingresosSalario.subtract(totalDescuentos).setScale(2, RoundingMode.HALF_UP));
        e.setTotalIngresos(totalIngresos.setScale(2, RoundingMode.HALF_UP));
        e.setTotalDescuentos(totalDescuentos.setScale(2, RoundingMode.HALF_UP));
        e.setTotalNeto(totalIngresos.subtract(totalDescuentos).setScale(2, RoundingMode.HALF_UP));
    }

    /** Prestaciones de una liquidacion ya guardada (para el finiquito). */
    public Prestaciones prestacionesDe(Liquidacion e) {
        return calcularPrestaciones(e.getFechaContratacion(), e.getFechaEgreso(),
                e.getIngresoSueldoBase(), e.getMotivoEgreso());
    }

    public static Prestaciones calcularPrestaciones(LocalDate contratacion, LocalDate egreso,
                                                    BigDecimal sueldo, String motivo) {
        if (contratacion == null || egreso == null || sueldo == null
                || sueldo.signum() <= 0 || egreso.isBefore(contratacion)) {
            return Prestaciones.vacias();
        }
        long diasServicio = ChronoUnit.DAYS.between(contratacion, egreso) + 1;

        // Indemnizacion: solo despido injustificado (Art. 82).
        BigDecimal indemnizacion = BigDecimal.ZERO;
        if (MOTIVO_DESPIDO_INJUSTIFICADO.equalsIgnoreCase(motivo)) {
            indemnizacion = sueldo.multiply(BigDecimal.valueOf(7))
                    .multiply(BigDecimal.valueOf(diasServicio))
                    .divide(BigDecimal.valueOf(6).multiply(DIAS_ANIO), 2, RoundingMode.HALF_UP);
        }

        // Aguinaldo: periodo del 1 de diciembre al 30 de noviembre.
        int anioAguinaldo = egreso.getMonthValue() == 12 ? egreso.getYear() : egreso.getYear() - 1;
        BigDecimal aguinaldo = proporcional(sueldo,
                mayor(LocalDate.of(anioAguinaldo, 12, 1), contratacion), egreso);

        // Bono 14: periodo del 1 de julio al 30 de junio.
        int anioBono14 = egreso.getMonthValue() >= 7 ? egreso.getYear() : egreso.getYear() - 1;
        BigDecimal bono14 = proporcional(sueldo,
                mayor(LocalDate.of(anioBono14, 7, 1), contratacion), egreso);

        // Vacaciones: 15 dias por anio, proporcional desde el ultimo aniversario.
        long aniosCompletos = ChronoUnit.YEARS.between(contratacion, egreso);
        LocalDate ultimoAniversario = contratacion.plusYears(aniosCompletos);
        long diasPeriodoVacaciones = Math.min(ChronoUnit.DAYS.between(ultimoAniversario, egreso) + 1, 365);
        BigDecimal vacaciones = sueldo.multiply(DIAS_VACACIONES_ANIO)
                .multiply(BigDecimal.valueOf(diasPeriodoVacaciones))
                .divide(DIAS_MES.multiply(DIAS_ANIO), 2, RoundingMode.HALF_UP);

        return new Prestaciones(diasServicio, indemnizacion, aguinaldo, bono14, vacaciones);
    }

    private static BigDecimal proporcional(BigDecimal sueldo, LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias <= 0) {
            return BigDecimal.ZERO;
        }
        dias = Math.min(dias, 365);
        return sueldo.multiply(BigDecimal.valueOf(dias)).divide(DIAS_ANIO, 2, RoundingMode.HALF_UP);
    }

    private static LocalDate mayor(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    /** Texto del tiempo de servicio, ej. "2 años, 3 meses y 10 días". */
    public static String tiempoServicio(LocalDate contratacion, LocalDate egreso) {
        if (contratacion == null || egreso == null || egreso.isBefore(contratacion)) {
            return "";
        }
        Period p = Period.between(contratacion, egreso.plusDays(1));
        return p.getYears() + " año(s), " + p.getMonths() + " mes(es) y " + p.getDays() + " día(s)";
    }

    // ------------------------------------------------------------------ inasistencias

    /** Dias de inasistencia pendientes (sin procesar) del empleado hasta una fecha (o todos si es null). */
    public long diasInasistenciaPendientes(Integer idEmpleado, LocalDate hasta) {
        long dias = 0;
        for (Inasistencia ina : inasistenciaRepository.findByEmpleado_IdEmpleadoAndFechaProcesadoIsNull(idEmpleado)) {
            LocalDate desde = ina.getFechaInicial().toLocalDate();
            LocalDate fin = ina.getFechaFinal().toLocalDate();
            if (hasta != null && fin.isAfter(hasta)) {
                fin = hasta;
            }
            if (!fin.isBefore(desde)) {
                dias += ChronoUnit.DAYS.between(desde, fin) + 1;
            }
        }
        return dias;
    }

    /** Descuento sugerido por inasistencias pendientes: dias x (sueldo base / 30). */
    public BigDecimal descuentoInasistenciasSugerido(Empleado empleado, LocalDate hasta) {
        long dias = diasInasistenciaPendientes(empleado.getIdEmpleado(), hasta);
        BigDecimal sueldo = empleado.getIngresoSueldoBase() == null ? BigDecimal.ZERO : empleado.getIngresoSueldoBase();
        return sueldo.divide(DIAS_MES, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
    }

    private void marcarInasistenciasProcesadas(Integer idEmpleado, LocalDate hasta, String usuario, LocalDateTime ahora) {
        for (Inasistencia ina : inasistenciaRepository.findByEmpleado_IdEmpleadoAndFechaProcesadoIsNull(idEmpleado)) {
            if (!ina.getFechaInicial().toLocalDate().isAfter(hasta)) {
                ina.setFechaProcesado(ahora);
                ina.setFechaModificacion(ahora);
                ina.setUsuarioModificacion(usuario);
                inasistenciaRepository.save(ina);
            }
        }
    }

    // ------------------------------------------------------------------ status

    /** Status en el que queda el empleado segun el motivo de egreso. */
    public static String statusDestino(String motivo) {
        if (MOTIVO_DESPIDO_INJUSTIFICADO.equalsIgnoreCase(motivo) || MOTIVO_DESPIDO_JUSTIFICADO.equalsIgnoreCase(motivo)) {
            return STATUS_DESPEDIDO;
        }
        return STATUS_BAJA;
    }

    private void aplicarStatusEgreso(Empleado empleado, String motivo, String usuario, LocalDateTime ahora) {
        String nombreDestino = statusDestino(motivo);
        StatusEmpleado destino = statusEmpleadoRepository.findAll().stream()
                .filter(s -> nombreDestino.equalsIgnoreCase(s.getNombre()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe el status \"" + nombreDestino
                        + "\" en Status Empleado; regístrelo antes de liquidar."));

        StatusEmpleado actual = empleado.getStatusEmpleado();
        if (actual.getIdStatusEmpleado().equals(destino.getIdStatusEmpleado())) {
            return; // ya esta en el status correspondiente
        }
        if (!flujoRepository.existsById(new FlujoStatusEmpleadoId(actual.getIdStatusEmpleado(), destino.getIdStatusEmpleado()))) {
            throw new IllegalArgumentException("No se puede liquidar con el motivo \"" + motivo + "\": el empleado está en status \""
                    + actual.getNombre() + "\" y no existe un flujo configurado hacia \"" + destino.getNombre()
                    + "\" en Flujos Status Empleado.");
        }
        empleado.setStatusEmpleado(destino);
        empleado.setFechaModificacion(ahora);
        empleado.setUsuarioModificacion(usuario);
        empleadoRepository.save(empleado);
    }
}
