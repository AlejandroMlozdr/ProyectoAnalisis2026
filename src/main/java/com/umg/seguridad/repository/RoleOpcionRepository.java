package com.umg.seguridad.repository;

import com.umg.seguridad.entity.RoleOpcion;
import com.umg.seguridad.entity.RoleOpcionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RoleOpcionRepository extends JpaRepository<RoleOpcion, RoleOpcionId> {
    List<RoleOpcion> findByRole_IdRoleAndOpcion_Menu_Modulo_IdModulo(Integer idRole, Integer idModulo);
    List<RoleOpcion> findByRole_IdRole(Integer idRole);

    /**
     * Trae, en una sola consulta, todos los RoleOpcion de un rol junto con
     * Opcion -> Menu -> Modulo (JOIN FETCH). Se usa desde PermisoService para
     * armar el menu dinamico y el mapa de permisos por pagina sin caer en
     * LazyInitializationException ni en N+1 queries.
     */
    @Query("SELECT ro FROM RoleOpcion ro " +
            "JOIN FETCH ro.opcion o " +
            "JOIN FETCH o.menu m " +
            "JOIN FETCH m.modulo mo " +
            "WHERE ro.role.idRole = :idRole " +
            "ORDER BY mo.ordenMenu ASC, mo.idModulo ASC, m.ordenMenu ASC, m.idMenu ASC, o.ordenMenu ASC, o.idOpcion ASC")
    List<RoleOpcion> findConDetallePorRole(@Param("idRole") Integer idRole);
}
