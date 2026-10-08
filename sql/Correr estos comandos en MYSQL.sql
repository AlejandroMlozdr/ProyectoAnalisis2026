select * from sucursal;

Select * from usuario;

UPDATE USUARIO 
SET Password = '$2b$10$QmMh6.xJ9TbQyYKuQBwT0es2KlD25jGFcnm5yIWp6q232YXnO7QXy' 
WHERE IdUsuario = 'Administrador';

INSERT INTO TIPO_ACCESO (Nombre, FechaCreacion, UsuarioCreacion) VALUES
('Logout', NOW(), 'system'),
('Sesion expirada por inactividad', NOW(), 'system');


-- Desactiva el modo seguro solo para esta sesion de Workbench
SET SQL_SAFE_UPDATES = 0;



-- ============================================================
-- PARTE 1: Corregir las rutas de OPCION
-- ============================================================
UPDATE OPCION SET Pagina = '/empresa' WHERE Nombre = 'Empresas';
UPDATE OPCION SET Pagina = '/sucursal' WHERE Nombre = 'Sucursales';
UPDATE OPCION SET Pagina = '/genero' WHERE Nombre = 'Generos';
UPDATE OPCION SET Pagina = '/status-usuario' WHERE Nombre = 'Estatus Usuario';
UPDATE OPCION SET Pagina = '/role' WHERE Nombre = 'Roles';
UPDATE OPCION SET Pagina = '/modulo' WHERE Nombre = 'Modulos';
UPDATE OPCION SET Pagina = '/menu' WHERE Nombre = 'Menus';
UPDATE OPCION SET Pagina = '/opcion' WHERE Nombre = 'Opciones';
UPDATE OPCION SET Pagina = '/usuario' WHERE Nombre = 'Usuarios';
UPDATE OPCION SET Pagina = '/role-opcion' WHERE Nombre = 'Asignar Opciones a un Role';

-- ============================================================
-- PARTE 2: Asegurar que el rol Administrador tenga TODOS los permisos
-- ============================================================
INSERT INTO role_opcion (IdRole, IdOpcion, Alta, Baja, Cambio, Imprimir, Exportar, FechaCreacion, UsuarioCreacion)
SELECT r.IdRole, o.IdOpcion, 1, 1, 1, 1, 1, NOW(), 'system'
FROM ROLE r
CROSS JOIN OPCION o
WHERE r.Nombre = 'Administrador'
ON DUPLICATE KEY UPDATE
    Alta = 1, Baja = 1, Cambio = 1, Imprimir = 1, Exportar = 1,
    FechaModificacion = NOW(), UsuarioModificacion = 'system';

-- Lo vuelve a activar (buena practica dejarlo prendido para el resto del trabajo)
SET SQL_SAFE_UPDATES = 1;





SET SQL_SAFE_UPDATES = 0;

-- Agrega los tipos de acceso que el codigo actual necesita EXACTAMENTE con
-- estos nombres (findFirstByNombreIgnoreCase ignora mayusculas/minusculas,
-- pero el texto debe ser igual). Se usa INSERT ... WHERE NOT EXISTS para que
-- se pueda correr mas de una vez sin duplicar filas.

INSERT INTO TIPO_ACCESO (Nombre, FechaCreacion, UsuarioCreacion)
SELECT 'Login fallido', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM TIPO_ACCESO WHERE Nombre = 'Login fallido');

INSERT INTO TIPO_ACCESO (Nombre, FechaCreacion, UsuarioCreacion)
SELECT 'Bloqueado por intentos de acceso', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM TIPO_ACCESO WHERE Nombre = 'Bloqueado por intentos de acceso');

INSERT INTO TIPO_ACCESO (Nombre, FechaCreacion, UsuarioCreacion)
SELECT 'Usuario no existe', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM TIPO_ACCESO WHERE Nombre = 'Usuario no existe');

INSERT INTO TIPO_ACCESO (Nombre, FechaCreacion, UsuarioCreacion)
SELECT 'Logout', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM TIPO_ACCESO WHERE Nombre = 'Logout');

-- 'Acceso Concedido' y 'Usuario Inactivo' ya existen en tu script original
-- y coinciden (la comparacion ignora mayusculas/minusculas), no hace falta tocarlos.

SET SQL_SAFE_UPDATES = 1;

-- Verificacion: deberias ver 9 filas en total (las 5 originales + estas 4 nuevas)
SELECT * FROM TIPO_ACCESO ORDER BY IdTipoAcceso;