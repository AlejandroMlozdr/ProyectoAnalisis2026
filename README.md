Sistema de Seguridad RBAC + Planilla / RRHH

Aplicación web desarrollada con Spring Boot + Thymeleaf que implementa un módulo de seguridad completo basado en roles (RBAC) y, sobre él, un módulo de Planilla y Recursos Humanos.

Proyecto académico — Universidad Mariano Gálvez (UMG) Integrantes: (completar) · Curso: (completar) · Catedrático: (completar)

1. ¿Qué hace el sistema?
Fase	Módulo	Qué resuelve
1	Seguridad	Autenticación, roles y permisos, auditoría de accesos, sesiones, recuperación y caducidad de contraseñas, administración de empresas, sucursales y usuarios.
2	Planilla / RRHH	Catálogos de personal, empleados, inasistencias, cálculo mensual de planilla, reportes, boletas de pago y liquidaciones.

Ambos módulos comparten el mismo esquema de seguridad: cada pantalla habilita o deshabilita sus acciones según el rol del usuario autenticado.

2. Tecnologías
Capa	Tecnología
Lenguaje / framework	Java 17 · Spring Boot 3.3 (MVC)
Seguridad	Spring Security 6 · BCrypt
Persistencia	Spring Data JPA · Hibernate · MySQL
Vistas	Thymeleaf + Layout Dialect · Bootstrap 5.3 · Bootstrap Icons
Reportes	OpenPDF (PDF) · Apache POI (Excel)
Utilidades	Lombok · Maven
3. Seguridad implementada
Autenticación con usuario y contraseña; contraseñas cifradas con BCrypt.
RBAC dinámico: cada usuario pertenece a un rol y cada rol tiene permisos sobre cada opción del sistema: Consultar, Alta, Baja, Cambio, Imprimir y Exportar.
Menú dinámico: el sidebar y la consola se construyen en tiempo real con las opciones que el rol tiene asignadas.
Control de acceso en el servidor: un interceptor valida cada petición (no basta con ocultar botones). Quien intenta entrar por URL a algo sin permiso es enviado a Acceso denegado.
Bloqueo de cuenta tras N intentos fallidos (configurable por empresa, 5 por defecto).
Políticas de contraseña por empresa: mayúsculas, minúsculas, números, caracteres especiales, longitud mínima y días de vigencia.
Caducidad de contraseña: automática por días de vigencia y manual (botón Forzar cambio de contraseña en Usuarios); el usuario debe cambiarla al siguiente inicio de sesión.
Recuperación de contraseña mediante pregunta de seguridad.
Sesiones: una sola sesión activa por usuario y cierre automático por inactividad (3 minutos por defecto).
Bitácora de accesos: registra ingresos, salidas, intentos fallidos, bloqueos y sesiones expiradas (con IP). Solo visible para el administrador.
Prevención de ataques: consultas parametrizadas con JPA (SQL injection) y escape automático de Thymeleaf (XSS).
Eliminación segura: si un registro tiene dependencias, el sistema muestra un mensaje claro en lugar de un error técnico.
4. Pantallas y rutas
Módulo Seguridad
Pantalla	Ruta
Consola del sistema	/consola
Empresas (incluye políticas de contraseña)	/empresa
Sucursales	/sucursal
Géneros	/genero
Estatus de usuario	/status-usuario
Roles	/role
Módulos / Menús / Opciones	/modulo · /menu · /opcion
Usuarios	/usuario
Asignar opciones a un rol	/role-opcion
Perfil · Cambio de contraseña	/perfil · /cambiar-password
Bitácora de accesos (admin)	/auditoria
Módulo Planilla / RRHH
Pantalla	Ruta	Tipo
Estados civiles	/estado-civil	CRUD
Status de empleado	/status-empleado	CRUD
Flujos de status	/flujo-status-empleado	CRUD
Tipos de documento	/tipo-documento	CRUD
Departamentos	/departamento	CRUD
Puestos	/puesto	CRUD
Personas	/persona	CRUD
Documentos de personas	/documento-persona	CRUD
Bancos	/banco	CRUD
Empleados	/empleado	CRUD
Cuentas bancarias de empleados	/cuenta-bancaria-empleado	CRUD
Inasistencias	/inasistencia	Transacción / CRUD
Calcular planilla	/calculo-planilla	Proceso
Reporte de planilla	/reporte-planilla	Reporte
Boletas de pago	/boleta-pago	Reporte / Documento
Liquidación de empleado	/liquidacion	Proceso / Transacción

Todos los listados incluyen Imprimir (PDF) y Exportar (Excel), sujetos a los permisos del rol.

5. Lógica de Calcular Planilla

POST /calculo-planilla/procesar → PlanillaService.procesar

Se elige un período (año y mes). Si ya fue procesado, hay que marcar Recalcular.
Entran los empleados con status Activo contratados hasta el fin del período.
Por cada empleado:
Ingresos = sueldo base + bonificación decreto + otros ingresos.
Descuento por inasistencias = (sueldo base ÷ 30) × días ausentes dentro del período (máximo 30).
Descuentos = IGSS + ISR + inasistencias.
Salario neto = ingresos − descuentos.
Se guarda el detalle por empleado y la cabecera con los totales y la fecha y hora de proceso. Todo en una sola transacción: si algo falla, no queda nada a medias.
Las inasistencias consideradas se marcan como procesadas.

El reporte y las boletas leen la planilla ya guardada; no recalculan.

Liquidación: total ingresos = sueldo base + bonificación + otros; total descuentos = IGSS + ISR + inasistencias; total neto = ingresos − descuentos. Al elegir un empleado se precargan sus datos, y cada liquidación se puede imprimir en PDF.

6. Otras reglas de negocio
Cambiar el status de un empleado solo se permite si esa transición existe en Flujos de status.
La combinación persona + tipo de documento es única.
En una inasistencia, la fecha final no puede ser anterior a la inicial.
Toda tabla guarda auditoría de creación y modificación (usuario y fecha).
7. Instalación y ejecución

Requisitos: JDK 17 · Maven 3.9+ · MySQL 8

Crear la base de datos y ejecutar, en este orden:
Script de la fase 1 (Seguridad).
sql/seed_tipo_acceso.sql
Script de la fase 2 (Planilla).
sql/fase2_paginas_y_permisos.sql (rutas de las opciones y permisos del administrador).
Configurar src/main/resources/application.properties con la URL, usuario y contraseña de MySQL.
Asegurarse de que el usuario administrador tenga su contraseña guardada en BCrypt (el sistema no acepta hashes MD5).
Ejecutar:
bash
   mvn spring-boot:run
Abrir http://localhost:8086/login

Con ddl-auto=validate, al arrancar Hibernate verifica que las entidades coincidan con las tablas; si algo no cuadra, la aplicación indica exactamente qué columna falla. Los permisos por rol se guardan en memoria: después de cambiar permisos directamente en la base de datos, hay que reiniciar la aplicación (desde la pantalla Asignar opciones a un rol se actualizan solos).

Convención importante

OPCION.Pagina debe coincidir con la ruta de la pantalla (por ejemplo /empleado). El sistema tolera nombres antiguos como estado_civil.php y los convierte a /estado-civil.

8. Estructura del proyecto
src/main/java/com/umg/seguridad/
├── config/       Seguridad, interceptor de permisos, manejo global de errores, menú global
├── controller/   Rutas MVC (una por pantalla)
├── service/      Reglas de negocio, auditoría, cálculo de planilla, permisos (RBAC)
├── repository/   Acceso a datos (Spring Data JPA)
├── entity/       Entidades JPA (módulo Seguridad y Planilla)
├── dto/          Objetos del menú dinámico
└── util/         Generación de PDF y Excel

src/main/resources/
├── templates/    Vistas Thymeleaf, una carpeta por pantalla
└── application.properties

sql/              Scripts de apoyo (tipos de acceso, rutas y permisos de la fase 2)