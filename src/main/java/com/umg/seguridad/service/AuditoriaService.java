package com.umg.seguridad.service;

import com.umg.seguridad.entity.BitacoraAcceso;
import com.umg.seguridad.repository.BitacoraAccesoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consulta de BITACORA_ACCESO para la pantalla /auditoria y su API. Los eventos
 * se escriben desde SesionRegistroService (login, logout, expiracion, fallos,
 * bloqueo, etc.) - este servicio solo lee/expone esos datos.
 */
@Service
public class AuditoriaService {

    private final BitacoraAccesoRepository bitacoraAccesoRepository;

    public AuditoriaService(BitacoraAccesoRepository bitacoraAccesoRepository) {
        this.bitacoraAccesoRepository = bitacoraAccesoRepository;
    }

    @Transactional(readOnly = true)
    public List<BitacoraAcceso> listar() {
        return bitacoraAccesoRepository.findAllByOrderByFechaAccesoDesc();
    }

    @Transactional(readOnly = true)
    public BitacoraAcceso obtenerPorId(Integer id) {
        return bitacoraAccesoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<BitacoraAcceso> buscarPorUsuario(String usuario) {
        return bitacoraAccesoRepository.findByUsuario_IdUsuarioOrderByFechaAccesoDesc(usuario);
    }

    @Transactional(readOnly = true)
    public List<BitacoraAcceso> buscarPorTipo(Integer idTipo) {
        return bitacoraAccesoRepository.findByTipoAcceso_IdTipoAccesoOrderByFechaAccesoDesc(idTipo);
    }

    @Transactional(readOnly = true)
    public List<BitacoraAcceso> buscarConFiltros(String usuario, Integer tipoAcceso, String ip,
                                                  LocalDateTime desde, LocalDateTime hasta) {
        return bitacoraAccesoRepository.buscarConFiltros(usuario, tipoAcceso, ip, desde, hasta);
    }

    public void eliminar(Integer id) {
        bitacoraAccesoRepository.deleteById(id);
    }
}
