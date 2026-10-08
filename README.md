# Sistema de Seguridad - RBAC (UMG)

Modelo inicial del proyecto en Spring Boot + Thymeleaf (MVC clásico) según el
documento de requerimientos y el diagrama entidad-relación del módulo de Seguridad.

## Proyecto fusionado (4 versiones -> 1)

Esta copia integra en una sola base el trabajo de los 4 proyectos individuales
(maje, byron, fermin, franco2), que partían de la misma plantilla:

- **Base**: la versión de maje (incluye "cambiar contraseña por caducidad").
- **Recuperar contraseña** (byron): `RecuperarPasswordController`, pantallas
  `recuperar-password/*` y los métodos `obtenerParaRecuperacion` /
  `restablecerPassword` en `UsuarioService`.
- **Timeout / sesión única** (fermin): `SesionRegistroService`,
  `SesionAuthenticationSuccessHandler`, `SesionLogoutFilter`,
  `SesionExpiradaListener`, más el bloque de `application.properties`:
  `seguridad.sesion.timeout-minutos` (por defecto 3 minutos) y
  `maximumSessions(1)` en `SecurityConfig` (una sola sesión activa por usuario).
- **Auditoría / bitácora de accesos** (franco2): pantalla `/auditoria` con
  filtros, `AuditoriaController`/`AuditoriaService` (solo lectura) y el
  bloqueo automático de cuenta tras N intentos fallidos, integrado en
  `CustomAuthenticationFailureHandler`.

### Decisiones de fusión importantes

- **Un solo punto de escritura hacia BITACORA_ACCESO**: `SesionRegistroService`
  (antes esto estaba repartido entre `SesionRegistroService` y `AuditoriaService`,
  lo que hubiera duplicado eventos). `AuditoriaService`/`AuditoriaController`
  ahora solo leen/muestran esos datos.
- Se descartaron `CustomAuthenticationSuccessHandler` y `CustomLogoutSuccessHandler`
  de franco2 por redundantes: `SesionAuthenticationSuccessHandler` y
  `SesionLogoutFilter` de fermin ya cubren esa función (y además son las que
  sostienen el timeout / sesión única).
- `BitacoraAcceso.usuario` sigue siendo una relación `@ManyToOne` a `Usuario`
  (como en maje), no un campo `String idUsuario` suelto (como en franco2).
  Por eso, cuando alguien intenta ingresar con un usuario que **no existe**,
  la bitácora no puede tener una fila con relación real a `Usuario`: se guarda
  igual el registro con `usuario = null` y el nombre intentado queda anotado
  en el campo `acceso`.
- El acceso a `/auditoria/**` quedó restringido a `ROLE_ADMINISTRADOR` (no
  estaba explícito en ninguno de los 4 proyectos; es un supuesto razonable
  dado que expone IPs y detalle de accesos de todos los usuarios — ajústenlo
  si no era la intención).

### Pendiente antes de correrlo

**Ejecutar `sql/seed_tipo_acceso.sql`** contra la base de datos. Ninguno de los
4 proyectos originales tenía sembrada la tabla `TIPO_ACCESO`, y ahora
`SesionRegistroService` necesita 7 nombres específicos ahí (Acceso Concedido,
Logout, Sesión expirada por inactividad, Login fallido, Bloqueado por intentos
de acceso, Usuario inactivo, Usuario no existe). El script usa
`INSERT ... WHERE NOT EXISTS`, así que se puede correr más de una vez sin
duplicar filas.

También verifiquen que `STATUS_USUARIO` tenga una fila con el nombre exacto
`Bloqueado por intentos de acceso` (así se llama en el script de franco2);
si en su base tiene otro nombre, ajústenlo en
`SesionRegistroService.STATUS_BLOQUEADO`.

## Cómo levantarlo (tablas ya existentes)

Este proyecto está configurado para trabajar contra una base de datos que
**ya tiene las tablas creadas** (no las crea ni las modifica):

1. Editar `src/main/resources/application.properties` con el nombre de su
   base de datos, usuario y password de MySQL.
2. Ejecutar:
   ```
   mvn spring-boot:run
   ```
3. Con `spring.jpa.hibernate.ddl-auto=validate`, al arrancar Hibernate solo
   **verifica** que las entidades coincidan con las tablas reales — si algo
   no cuadra (una columna que falta, un tipo distinto), la app no arranca y
   dice exactamente cuál es el problema. Cuando ya confíen en el modelo,
   pueden cambiarlo a `none` para que ni siquiera valide.
4. Abrir `http://localhost:8080/login`

### Si la app no arranca por un error de columna

Casi siempre es porque el nombre de una columna en su base de datos no
coincide exactamente con el campo de la entidad correspondiente. Revisen:
- Que el nombre de la tabla en `@Table(name = "...")` sea igual al real.
- Que cada columna especial tenga su `@Column(name = "...")` si el nombre
  del campo Java no coincide con el de la columna.
- La línea `spring.jpa.hibernate.naming.physical-strategy=...` en
  `application.properties` evita que Hibernate convierta los nombres a
  snake_case, porque sus tablas usan PascalCase (`IdEmpresa`, no `id_empresa`).

## Qué ya está armado

- **Entidades JPA** de las 13 tablas del diagrama (`entity/`), con sus relaciones
  (`Sucursal -> Empresa`, `Usuario -> Role/Genero/Sucursal/StatusUsuario`,
  `RoleOpcion` como tabla puente del RBAC, etc.)
- **Repositorios** (`repository/`) con los métodos de consulta que van a necesitar
  para los ComboBox dependientes (ej. sucursales por empresa) y para armar
  el menú dinámico por rol.
- **Seguridad** (`config/SecurityConfig.java` + `service/UsuarioDetailsService.java`):
  login contra la tabla `usuario`, contraseñas encriptadas con BCrypt,
  logout, y un ejemplo de restricción de rutas por rol.
- **Módulo Empresa completo** (`service/EmpresaService.java`,
  `controller/EmpresaController.java`, `templates/empresa/*.html`) como
  plantilla para que cada integrante replique el mismo patrón en las
  demás 9 pantallas.

### ⚠️ Aviso importante: contraseñas de los usuarios semilla

El script SQL crea los usuarios `system` y `Administrador` con contraseñas
guardadas como **MD5** (`MD5('erpwijoeli')`, `MD5('ITAdmin')`). Pero
`SecurityConfig.java` está configurado para verificar contraseñas con
**BCrypt**, que es el estándar recomendado hoy en día (MD5 ya no se
considera seguro para contraseñas).

Esto significa que, tal como está, **no vas a poder iniciar sesión** con
esos dos usuarios del script — Spring Security va a rechazar el login
porque el hash no coincide con el formato que espera.

Opciones para resolverlo (elijan una en equipo):
1. **Recomendado**: generar un hash BCrypt para esas contraseñas y
   actualizar la columna `Password` de esos usuarios en la base de datos
   directamente (`UPDATE USUARIO SET Password = '$2a$10$...' WHERE IdUsuario = 'Administrador';`).
2. Crear un usuario nuevo desde la pantalla de "Usuarios" una vez que esa
   pantalla esté implementada, usando `PasswordEncoder.encode(...)` al guardar.
3. (No recomendado, solo para probar rápido) cambiar temporalmente
   `SecurityConfig` para usar `NoOpPasswordEncoder`, pero esto NO cifra
   nada — no lo dejen así para la entrega final.

### Otros ajustes hechos contra el script real

- `USUARIO.IdUsuario` es el propio username (varchar), no un id numérico —
  la entidad `Usuario` usa `String` como llave primaria.
- `ROLE_OPCION` no tiene columna `Consultar` (se asume que todo rol puede
  consultar lo que ve en su menú); los permisos son `Integer` (0/1) porque
  la tabla los define como `INT`, no `BIT`.
- `BITACORA_ACCESO.Acceso` es `VARCHAR`, no booleano.
- `EMPRESA.PasswordCantidadCaducidadDias` (no `PasswordCantidadDias`).

## Qué falta (para repartir en el equipo)

Cada pantalla restante sigue el mismo patrón que `Empresa`:
`Controller -> Service -> Repository`, más su par de vistas `lista.html` / `form.html`.

- [x] Sucursal, Género, Estatus de Usuario, Módulos, Menús, Opciones (CRUD simple)
- [x] Roles (CRUD simple)
- [x] Usuarios (CRUD con los ComboBox dependientes Empresa→Sucursal, y encriptar
      password con `PasswordEncoder` antes de guardar)
- [x] Pantalla "Asignar Opciones a un Rol" (la cuadrícula de checkboxes por permiso,
      usa `RoleOpcionRepository`)
- [x] `PermisoService`: arma el menú dinámico en `consola.html`/`layout.html` a partir de
      `RoleOpcion` del usuario autenticado, y expone `puedeAlta/puedeBaja/puedeCambio/
      puedeImprimir/puedeExportar/puedeConsultar(pagina)` para las vistas.
- [ ] `AuthService`: lógica de bloqueo tras 5 intentos fallidos, registro en
      `BitacoraAcceso`, y recuperación de contraseña (pregunta/respuesta o token)
      — **ya implementado**, ver `CustomAuthenticationFailureHandler`,
      `SesionRegistroService` y `RecuperarPasswordController`.
- [x] Botones Imprimir / Exportar (PDF con OpenPDF, Excel con Apache POI)
- [ ] Validaciones de política de password contra los campos de `Empresa`
      (usar `spring-boot-starter-validation` con un validador custom)
      — **ya implementado** de forma manual en `UsuarioService.validarPoliticaPassword`;
      falta migrarlo a un `@Constraint` custom si el equipo lo quiere más "a la Spring".
- [x] Mensaje amigable al intentar eliminar un registro con dependencias (antes
      tiraba un error 500 crudo) — ver `GlobalExceptionHandler` y el banner
      global en `layout.html`.
- [x] Botón "Forzar cambio de contraseña" en el listado de Usuarios (caducidad
      manual, sin esperar los días de vigencia ni abrir el formulario completo)
      — ver `UsuarioService.marcarPasswordVencida` y `POST /usuario/caducar-password/{id}`.

## Mensajes flash (éxito / error) — convención del proyecto

`layout.html` pinta automáticamente dos banners (verde para éxito, rojo para
error) arriba del contenido de **cualquier** pantalla, leyendo los atributos
de modelo `exito` y `error`. No hace falta agregarlos a mano en cada
`lista.html`/`form.html` nuevo — solo hay que setearlos desde el controlador:

```java
// Via redirect (lo normal: PRG pattern, ej. despues de guardar/eliminar)
redirectAttributes.addFlashAttribute("exito", "Guardado correctamente.");
redirectAttributes.addFlashAttribute("error", "Algo salió mal.");

// Via forward (cuando se re-muestra el mismo formulario con errores de validación)
model.addAttribute("error", "Mensaje de validación.");
```

**Importante:** si una pantalla ya muestra su propio `${error}`/`${exito}`
inline (como hacía antes `usuario/form.html` y `perfil.html`), hay que
quitarlo para no duplicar el mensaje — el banner global ya lo cubre.

## Eliminar con dependencias (`GlobalExceptionHandler`)

Antes, borrar un registro que todavía tenía dependencias (ej. una Empresa con
Sucursales, un Módulo con Menús, una Opción referenciada en `ROLE_OPCION`)
lanzaba `DataIntegrityViolationException` desde la base de datos y Spring lo
mostraba como un error 500 crudo (Whitelabel Error Page), sin decirle nada
útil al usuario.

`GlobalExceptionHandler` (`config/`) captura esa excepción en **un solo
lugar** para las 10 pantallas (no hubo que tocar ningún `Service.eliminar()`),
y redirige de vuelta al listado de origen con un mensaje claro vía el banner
de error descrito arriba.

## RBAC dinámico: cómo funciona (PermisoService)

Todo el control de acceso fino (quién puede Consultar/Alta/Baja/Cambio/Imprimir/
Exportar cada pantalla) se resuelve en tiempo real a partir de `ROLE_OPCION`,
sin nombres de rol quemados en el código. Piezas involucradas:

- **`PermisoService`** (`service/`): dado el usuario autenticado, resuelve su
  `Role` y expone `puedeAlta("/empresa")`, `puedeBaja("/empresa")`, etc. También
  arma el árbol `Modulo -> Menu -> Opcion` (`construirMenu()`) para pintar el
  sidebar y la consola. Cachea los permisos por rol en memoria; el caché se
  invalida automáticamente cuando un administrador guarda cambios en
  "Asignar Opciones a un Rol" (`RoleOpcionService.guardar` llama a
  `permisoService.invalidarCache(idRole)`).
- **`MenuGlobalAdvice`** (`config/`, `@ControllerAdvice`): agrega el atributo
  `menuLateral` al `Model` de **todas** las vistas automáticamente, así
  ningún controlador tiene que acordarse de inyectarlo.
- **`PermisoInterceptor`** (`config/`): el enforcement real, del lado del
  servidor. Ocultar un botón en el HTML no es seguridad — cualquiera puede
  escribir la URL a mano o mandar el POST con curl/Postman. Este interceptor
  corre en cada request e infiere qué acción se necesita según la URL
  (`/empresa/nuevo` -> Alta, `/empresa/editar/5` -> Cambio,
  `/empresa/eliminar/5` -> Baja, `/empresa/exportar-pdf` -> Imprimir,
  `/empresa/exportar-excel` -> Exportar, cualquier otro GET -> Consultar), y
  redirige a `/acceso-denegado` si el rol no tiene el permiso. Se registra en
  `WebConfig.addInterceptors`.
- **`SecurityConfig`** ya NO restringe rutas por nombre de rol fijo
  (`hasRole("ADMINISTRADOR")`) salvo `/auditoria/**` (ver más abajo). Antes,
  solo Empresa/Role/Módulo estaban protegidas y únicamente para el rol
  literal "ADMINISTRADOR" — eso rompía el RBAC, porque un rol distinto con
  permisos reales asignados en `ROLE_OPCION` igual quedaba bloqueado. Ahora
  cualquier rol con los permisos correctos puede usar cualquier pantalla.

### ⚠️ Convención obligatoria: `OPCION.Pagina`

Todo esto depende de que la columna `OPCION.Pagina` de cada opción contenga
**exactamente** la ruta base del controlador correspondiente (case-insensitive,
con o sin `/` inicial/final — se normaliza), por ejemplo:

| Opción | OPCION.Pagina esperado |
|---|---|
| Empresas | `/empresa` |
| Sucursales | `/sucursal` |
| Géneros | `/genero` |
| Estatus Usuario | `/status-usuario` |
| Roles | `/role` |
| Módulos | `/modulo` |
| Menús | `/menu` |
| Opciones | `/opcion` |
| Usuarios | `/usuario` |
| Asignar Opciones a un Role | `/role-opcion` |
| Consola del Sistema | `/consola` (se excluye del árbol dinámico; se muestra siempre fija arriba del sidebar) |

Si `Seguridad-SQL.sql` no sembró estos valores así, hay que corregirlos en la
tabla `OPCION` (o editarlos desde la pantalla "Opciones" una vez que el
usuario que las edita ya tenga permiso de Cambio ahí). Mientras una Opción no
tenga `Pagina` o no siga esta convención, esa pantalla queda inalcanzable para
todos los roles (el interceptor la bloquea por defecto) — es una medida
"fail-closed" intencional: mejor bloquear de más que dejar pasar algo sin
validar.

**Nota sobre "Consultar":** la tabla `ROLE_OPCION` no tiene columna
`Consultar` (fue decisión de diseño previa del equipo: si el admin asignó la
Opción al rol, ya puede verla). `PermisoService.puedeConsultar(pagina)`
refleja eso: es `true` si existe una fila en `ROLE_OPCION` para esa
combinación Rol/Opción, sin mirar los flags de Alta/Baja/etc.

**Nota sobre `/auditoria`:** la bitácora de accesos no forma parte de las 10
pantallas del documento de requerimientos (la agregó franco2 aparte), así que
no está modelada como `Opcion`/`RoleOpcion`. Sigue protegida con
`hasRole("ADMINISTRADOR")` directo en `SecurityConfig`, y su link en el
sidebar usa `sec:authorize="hasRole('ADMINISTRADOR')"` en vez del menú
dinámico. Si el equipo quiere que sea configurable por rol como el resto,
habría que darle una fila en `OPCION` (`Pagina = "/auditoria"`) y quitar esa
excepción de `PermisoInterceptor`/`SecurityConfig`/`layout.html`.

## Imprimir / Exportar

Cada pantalla de listado tiene botones "Imprimir" (PDF) y "Exportar" (Excel),
visibles solo si `PermisoService.puedeImprimir`/`puedeExportar` lo permiten
para esa página. La generación de archivos vive en un solo lugar,
`util/ReporteUtil.java` (genérico: recibe título + columnas + filas de texto),
para no repetir lógica de reportería en cada uno de los 9 mantenimientos:

- **PDF**: [OpenPDF](https://github.com/LibrePDF/OpenPDF) (fork de iText 2.1.7,
  licencia LGPL/MPL — no AGPL, sin problemas de licenciamiento para un
  proyecto académico que después el equipo puede querer distribuir).
- **Excel**: Apache POI (`poi-ooxml`, genera `.xlsx` con `XSSFWorkbook`).

Ambas dependencias ya están en `pom.xml`. Como en este entorno no hay acceso
a Maven Central para compilar, **el equipo debe correr `mvn clean install` (o
simplemente `mvn spring-boot:run`) en su máquina** para que Maven descargue
`openpdf` y `poi-ooxml` la primera vez.

Los endpoints son `GET /<pantalla>/exportar-pdf` y `GET /<pantalla>/exportar-excel`
en cada controlador (ej. `EmpresaController.exportarPdf()`), y quedan
protegidos automáticamente por `PermisoInterceptor` igual que el resto de
acciones.

## Fase 2 — Planilla / RRHH (mapeo de la nueva base de datos)

Se mapearon las 16 tablas de la fase 2 (entity + repository + service + controller + vistas),
con el mismo esquema de seguridad: permisos por rol (Consultar/Alta/Baja/Cambio/Imprimir/Exportar),
auditoría de creación/modificación e Imprimir (PDF) / Exportar (Excel) en cada listado.

| Pantalla | Ruta (= `OPCION.Pagina`) | Tablas |
|---|---|---|
| Estados Civiles | `/estado-civil` | ESTADO_CIVIL |
| Status Empleado | `/status-empleado` | STATUS_EMPLEADO |
| Flujos Status Empleado | `/flujo-status-empleado` | FLUJO_STATUS_EMPLEADO (llave compuesta) |
| Tipos de Documentos | `/tipo-documento` | TIPO_DOCUMENTO |
| Departamentos | `/departamento` | DEPARTAMENTO |
| Puestos | `/puesto` | PUESTO |
| Personas | `/persona` | PERSONA |
| Documentos de Personas | `/documento-persona` | DOCUMENTO_PERSONA (llave compuesta) |
| Bancos | `/banco` | BANCO |
| Empleados | `/empleado` | EMPLEADO |
| Cuentas Bancarias Empleados | `/cuenta-bancaria-empleado` | CUENTA_BANCARIA_EMPLEADO |
| Inasistencias de Empleados | `/inasistencia` | INASISTENCIA |
| Calcular Planilla | `/calculo-planilla` | PERIODO_PLANILLA, PLANILLA_CABECERA, PLANILLA_DETALLE |
| Reporte de Planilla | `/reporte-planilla` | PLANILLA_CABECERA, PLANILLA_DETALLE |
| Boletas de Pago | `/boleta-pago` | PLANILLA_DETALLE, CUENTA_BANCARIA_EMPLEADO |
| Liquidación de Empleado | `/liquidacion` | LIQUIDACION |

`PERIODO_PLANILLA` tiene entity/repository/service pero no pantalla CRUD (el documento de
requerimientos no la pide); alimenta el combo de períodos.

### Para activarlo (importante)

1. Ejecutar `sql/fase2_paginas_y_permisos.sql` en MySQL: cambia `OPCION.Pagina` de
   `estado_civil.php` (versión PHP) a las rutas de esta tabla y da todos los permisos al rol
   Administrador sobre las opciones nuevas.
2. **Reiniciar la aplicación** (los permisos por rol se guardan en cache). Para otros roles,
   asignar permisos desde "Asignar Opciones a un Rol".
3. Spring valida el esquema al arrancar (`ddl-auto=validate`): las tablas de la fase 2 deben
   existir en la misma base que la fase 1.

### Reglas de negocio implementadas

- **Flujos de status:** al editar un Empleado, cambiar su status solo se permite si existe esa
  transición (actual → nuevo) en Flujos Status Empleado. En un flujo, status actual y nuevo deben ser distintos.
- **Documentos de personas:** la combinación persona + tipo de documento es única.
- **Inasistencias:** la fecha final no puede ser anterior a la inicial. `Fecha procesado` la
  llena el sistema al calcular una planilla.
- **Cuentas bancarias:** el indicador "activa" se guarda como `'1'`/`'0'` (columna `CHAR`).
- **Calcular Planilla** (`PlanillaService.procesar`):
  - entran los empleados con status **Activo** contratados hasta el fin del período;
  - ingresos, IGSS e ISR se toman tal como están en el empleado;
  - descuento por inasistencias = días ausentes dentro del período (máx. 30) × (sueldo base / 30);
  - salario neto = sueldo base + bonificación + otros ingresos − IGSS − ISR − inasistencias;
  - la cabecera guarda los totales; si el período ya estaba procesado hay que marcar "Recalcular"
    (borra y regenera el detalle); marca las inasistencias consideradas con su `FechaProcesado`.
- **Reporte y Boletas** leen la planilla ya procesada (no recalculan).
- **Liquidación:** total ingresos = sueldo base + bonificación + otros; total descuentos =
  IGSS + ISR + inasistencias; salario neto y total neto = ingresos − descuentos (se recalculan
  al guardar). Al elegir un empleado se precargan sus datos. No tiene "Baja" (historial laboral).
  La tabla `LIQUIDACION` no trae columnas de modificación, por eso solo guarda auditoría de creación.

## Estructura

```
entity/       -> las 13 tablas del diagrama, mapeadas con JPA
repository/   -> interfaces JpaRepository (CRUD automático + queries custom)
service/      -> reglas de negocio, auditoría (fechaCreacion/usuarioCreacion, etc.)
controller/   -> rutas MVC, reciben el request y devuelven la vista
config/       -> SecurityConfig (login, roles, encriptación)
templates/    -> vistas Thymeleaf, organizadas por pantalla
```
