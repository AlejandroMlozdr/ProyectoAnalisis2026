package com.umg.seguridad.repository;

import com.umg.seguridad.entity.BitacoraAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BitacoraAccesoRepository extends JpaRepository<BitacoraAcceso, Integer> {

    List<BitacoraAcceso> findAllByOrderByFechaAccesoDesc();

    List<BitacoraAcceso> findByUsuario_IdUsuarioOrderByFechaAccesoDesc(String idUsuario);

    List<BitacoraAcceso> findByTipoAcceso_IdTipoAccesoOrderByFechaAccesoDesc(Integer idTipoAcceso);

    // Busqueda con filtros para la pantalla de auditoria. "usuario" se compara
    // contra el idUsuario relacionado (b.usuario puede ser null cuando el evento
    // es "Usuario no existe").
    @Query("SELECT b FROM BitacoraAcceso b " +
           "WHERE (:usuario IS NULL OR b.usuario.idUsuario LIKE %:usuario%) " +
           "AND (:tipoAcceso IS NULL OR b.tipoAcceso.idTipoAcceso = :tipoAcceso) " +
           "AND (:ip IS NULL OR b.direccionIp LIKE %:ip%) " +
           "AND (:desde IS NULL OR b.fechaAcceso >= :desde) " +
           "AND (:hasta IS NULL OR b.fechaAcceso <= :hasta) " +
           "ORDER BY b.fechaAcceso DESC")
    List<BitacoraAcceso> buscarConFiltros(
            @Param("usuario") String usuario,
            @Param("tipoAcceso") Integer tipoAcceso,
            @Param("ip") String ip,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    List<BitacoraAcceso> findByDireccionIpContainingIgnoreCaseOrderByFechaAccesoDesc(String ip);

    List<BitacoraAcceso> findByFechaAccesoBetweenOrderByFechaAccesoDesc(LocalDateTime desde, LocalDateTime hasta);

    long countByUsuario_IdUsuario(String idUsuario);

    long countByTipoAcceso_IdTipoAcceso(Integer idTipoAcceso);

    void deleteByFechaAccesoBefore(LocalDateTime fecha);
}
