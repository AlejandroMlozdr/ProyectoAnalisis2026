package com.umg.seguridad.service;

import com.umg.seguridad.entity.PeriodoPlanilla;
import com.umg.seguridad.entity.PeriodoPlanillaId;
import com.umg.seguridad.repository.PeriodoPlanillaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PeriodoPlanillaService {

    private final PeriodoPlanillaRepository repository;

    public PeriodoPlanillaService(PeriodoPlanillaRepository repository) {
        this.repository = repository;
    }

    /** Periodos del mas reciente al mas antiguo. */
    public List<PeriodoPlanilla> listar() {
        return repository.findAllOrdenados();
    }

    public PeriodoPlanilla obtener(Integer anio, Integer mes) {
        return repository.findById(new PeriodoPlanillaId(anio, mes))
                .orElseThrow(() -> new IllegalArgumentException("El período " + mes + "/" + anio + " no existe."));
    }
}
