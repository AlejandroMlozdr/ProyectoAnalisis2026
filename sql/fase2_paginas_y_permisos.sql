-- ============================================================================
-- FASE 2 (Planilla / RRHH): enlazar las Opciones con las pantallas de la app
-- y dar permisos completos al rol Administrador.
--
-- El script de la fase 2 inserto las OPCION con Pagina = 'estado_civil.php',
-- 'empleado.php', etc. (nombres de la version PHP). Esta aplicacion decide el
-- menu y los permisos comparando OPCION.Pagina con la ruta del controlador
-- (ej. "/estado-civil"), asi que hay que actualizar esas rutas.
--
-- Ejecutar UNA vez en la base de datos (MySQL Workbench) y luego REINICIAR la
-- aplicacion (los permisos por rol se guardan en cache en memoria).
-- ============================================================================

-- Desactiva el modo seguro solo para esta sesion de Workbench
SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- PARTE 1: rutas de las pantallas
-- ----------------------------------------------------------------------------
UPDATE OPCION SET Pagina = '/estado-civil'            WHERE Nombre = 'Estados Civiles';
UPDATE OPCION SET Pagina = '/status-empleado'         WHERE Nombre = 'Status Empleado';
UPDATE OPCION SET Pagina = '/flujo-status-empleado'   WHERE Nombre = 'Flujos Status Empleado';
UPDATE OPCION SET Pagina = '/tipo-documento'          WHERE Nombre = 'Tipos de Documentos';
UPDATE OPCION SET Pagina = '/departamento'            WHERE Nombre = 'Departamentos';
UPDATE OPCION SET Pagina = '/puesto'                  WHERE Nombre = 'Puestos';
UPDATE OPCION SET Pagina = '/persona'                 WHERE Nombre = 'Personas';
UPDATE OPCION SET Pagina = '/documento-persona'       WHERE Nombre = 'Documentos de Personas';
UPDATE OPCION SET Pagina = '/banco'                   WHERE Nombre = 'Bancos';
UPDATE OPCION SET Pagina = '/empleado'                WHERE Nombre = 'Empleados';
UPDATE OPCION SET Pagina = '/cuenta-bancaria-empleado' WHERE Nombre = 'Cuentas Bancarias Empleados';
UPDATE OPCION SET Pagina = '/inasistencia'            WHERE Nombre = 'Inasistencias de Empleados';
UPDATE OPCION SET Pagina = '/calculo-planilla'        WHERE Nombre = 'Calcular Planilla';
UPDATE OPCION SET Pagina = '/reporte-planilla'        WHERE Nombre = 'Reporte de Planilla';
UPDATE OPCION SET Pagina = '/boleta-pago'             WHERE Nombre = 'Boletas de Pago';
UPDATE OPCION SET Pagina = '/liquidacion'             WHERE Nombre = 'Liquidacion de Empleado';

-- ----------------------------------------------------------------------------
-- PARTE 2: el rol Administrador con TODOS los permisos sobre TODAS las opciones
--          (incluye las nuevas de Planilla)
-- ----------------------------------------------------------------------------
INSERT INTO role_opcion (IdRole, IdOpcion, Alta, Baja, Cambio, Imprimir, Exportar, FechaCreacion, UsuarioCreacion)
SELECT r.IdRole, o.IdOpcion, 1, 1, 1, 1, 1, NOW(), 'system'
FROM ROLE r
CROSS JOIN OPCION o
WHERE r.Nombre = 'Administrador'
ON DUPLICATE KEY UPDATE
    Alta = 1, Baja = 1, Cambio = 1, Imprimir = 1, Exportar = 1,
    FechaModificacion = NOW(), UsuarioModificacion = 'system';

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------------------------------------------------------
-- Verificacion: todas las opciones de Planilla deben mostrar su ruta "/algo"
-- ----------------------------------------------------------------------------
SELECT o.IdOpcion, m.Nombre AS Menu, o.Nombre AS Opcion, o.Pagina
FROM OPCION o
JOIN MENU m ON m.IdMenu = o.IdMenu
WHERE m.IdModulo = 2
ORDER BY m.OrdenMenu, o.OrdenMenu;

-- Nota: la tabla LIQUIDACION no tiene columnas FechaModificacion/UsuarioModificacion
-- (asi viene en el script de la fase 2), por eso al CAMBIAR una liquidacion solo se
-- conserva la auditoria de creacion.
