package com.umg.seguridad.security;

import com.umg.seguridad.entity.BitacoraAcceso;
import com.umg.seguridad.entity.StatusUsuario;
import com.umg.seguridad.entity.Usuario;
import com.umg.seguridad.repository.BitacoraAccesoRepository;
import com.umg.seguridad.repository.StatusUsuarioRepository;
import com.umg.seguridad.repository.TipoAccesoRepository;
import com.umg.seguridad.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Centraliza la escritura en USUARIO (sesionActual, ultimaFechaIngreso) y en
 * BITACORA_ACCESO para TODOS los eventos del ciclo de vida de una sesion:
 * login exitoso, logout explicito, expiracion por inactividad, login fallido,
 * usuario bloqueado por intentos, usuario inactivo y usuario no encontrado.
 *
 * Es el unico punto de escritura hacia BITACORA_ACCESO (originalmente esto
 * estaba repartido entre esta clase y AuditoriaService; se unifico aqui para
 * evitar registrar el mismo evento dos veces). AuditoriaService/AuditoriaController
 * solo se encargan de LEER/mostrar lo que aqui se escribe.
 */
@Service
public class SesionRegistroService {

    public static final String TIPO_ACCESO_CONCEDIDO = "Acceso Concedido";
    public static final String TIPO_ACCESO_LOGOUT = "Logout";
    public static final String TIPO_ACCESO_EXPIRADA = "Sesion expirada por inactividad";
    public static final String TIPO_ACCESO_LOGIN_FALLIDO = "Login fallido";
    public static final String TIPO_ACCESO_BLOQUEADO = "Bloqueado por intentos de acceso";
    public static final String TIPO_ACCESO_USUARIO_INACTIVO = "Usuario inactivo";
    public static final String TIPO_ACCESO_USUARIO_NO_EXISTE = "Usuario no existe";

    // Nombre exacto tal como quedo en STATUS_USUARIO (ver script SQL)
    private static final String STATUS_BLOQUEADO = "Bloqueado por intentos de acceso";

    private final UsuarioRepository usuarioRepository;
    private final BitacoraAccesoRepository bitacoraAccesoRepository;
    private final TipoAccesoRepository tipoAccesoRepository;
    private final StatusUsuarioRepository statusUsuarioRepository;

    public SesionRegistroService(UsuarioRepository usuarioRepository,
                                  BitacoraAccesoRepository bitacoraAccesoRepository,
                                  TipoAccesoRepository tipoAccesoRepository,
                                  StatusUsuarioRepository statusUsuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.bitacoraAccesoRepository = bitacoraAccesoRepository;
        this.tipoAccesoRepository = tipoAccesoRepository;
        this.statusUsuarioRepository = statusUsuarioRepository;
    }

    @Transactional
    public void registrarLogin(String username, String sesionId, HttpServletRequest request) {
        usuarioRepository.findById(username).ifPresent(usuario -> {
            usuario.setUltimaFechaIngreso(LocalDateTime.now());
            usuario.setSesionActual(sesionId);
            // Login correcto: se reinicia el contador de intentos fallidos.
            if (usuario.getIntentosDeAcceso() != null && usuario.getIntentosDeAcceso() > 0) {
                usuario.setIntentosDeAcceso(0);
            }
            usuarioRepository.save(usuario);
            guardarBitacora(usuario, TIPO_ACCESO_CONCEDIDO, sesionId, "Exitoso", request);
        });
    }

    @Transactional
    public void registrarLogout(String username, String sesionId, HttpServletRequest request) {
        usuarioRepository.findById(username).ifPresent(usuario -> {
            usuario.setSesionActual(null);
            usuarioRepository.save(usuario);
            guardarBitacora(usuario, TIPO_ACCESO_LOGOUT, sesionId, "Exitoso", request);
        });
    }

    /**
     * Solo registra la expiracion si la sesion que murio seguia siendo la
     * "sesionActual" del usuario: si ya fue cerrada por logout explicito o
     * reemplazada por un login mas reciente, no se anota nada aqui.
     */
    @Transactional
    public void registrarExpiracionSiSigueActiva(String username, String sesionId) {
        usuarioRepository.findById(username).ifPresent(usuario -> {
            if (sesionId.equals(usuario.getSesionActual())) {
                usuario.setSesionActual(null);
                usuarioRepository.save(usuario);
                guardarBitacora(usuario, TIPO_ACCESO_EXPIRADA, sesionId, "Sesion expirada por inactividad", null);
            }
        });
    }

    /**
     * Password incorrecta para un usuario que si existe. Devuelve el usuario
     * (ya con el contador de intentos actualizado) para que el caller decida
     * si hay que bloquearlo.
     */
    @Transactional
    public Usuario registrarLoginFallido(String username, HttpServletRequest request) {
        return usuarioRepository.findById(username).map(usuario -> {
            int intentosActuales = usuario.getIntentosDeAcceso() == null ? 0 : usuario.getIntentosDeAcceso();
            usuario.setIntentosDeAcceso(intentosActuales + 1);
            usuarioRepository.save(usuario);
            guardarBitacora(usuario, TIPO_ACCESO_LOGIN_FALLIDO, null, "Password incorrecta", request);
            return usuario;
        }).orElse(null);
    }

    /**
     * Cambia el status del usuario a "Bloqueado por intentos de acceso", reinicia
     * el contador de intentos y deja constancia en bitacora. Si ese status no
     * existe en STATUS_USUARIO, el usuario NO queda bloqueado a nivel de cuenta
     * pero igual se registra el evento.
     */
    @Transactional
    public void registrarBloqueo(Usuario usuario, HttpServletRequest request) {
        statusUsuarioRepository.findAll().stream()
                .filter(s -> STATUS_BLOQUEADO.equalsIgnoreCase(s.getNombre()))
                .findFirst()
                .ifPresentOrElse(
                        (StatusUsuario status) -> usuario.setStatusUsuario(status),
                        () -> System.out.println(
                                "No se encontro el status '" + STATUS_BLOQUEADO +
                                "' en STATUS_USUARIO; el usuario no quedo bloqueado a nivel de cuenta, solo se registro en bitacora.")
                );
        usuario.setIntentosDeAcceso(0);
        usuarioRepository.save(usuario);
        guardarBitacora(usuario, TIPO_ACCESO_BLOQUEADO, null, "Bloqueado por exceder el maximo de intentos", request);
    }

    @Transactional
    public void registrarUsuarioInactivo(String username, HttpServletRequest request) {
        usuarioRepository.findById(username).ifPresent(usuario ->
                guardarBitacora(usuario, TIPO_ACCESO_USUARIO_INACTIVO, null, "Intento de ingreso con usuario inactivo/bloqueado", request));
    }

    /**
     * El usuario no existe en la tabla USUARIO, asi que no hay a que Usuario
     * relacionar la bitacora (la columna IdUsuario de BITACORA_ACCESO es FK a
     * USUARIO). Se deja el registro sin usuario y el nombre intentado queda
     * anotado en el campo "acceso" para no perder el dato.
     */
    @Transactional
    public void registrarUsuarioNoExiste(String usernameIntentado, HttpServletRequest request) {
        BitacoraAcceso bitacora = new BitacoraAcceso();
        bitacora.setUsuario(null);
        bitacora.setTipoAcceso(tipoAccesoRepository.findFirstByNombreIgnoreCase(TIPO_ACCESO_USUARIO_NO_EXISTE).orElse(null));
        bitacora.setFechaAcceso(LocalDateTime.now());
        bitacora.setAcceso("Usuario intentado: " + usernameIntentado);
        if (request != null) {
            bitacora.setDireccionIp(request.getRemoteAddr());
            bitacora.setHttpUserAgent(request.getHeader("User-Agent"));
        }
        bitacoraAccesoRepository.save(bitacora);
    }

    private void guardarBitacora(Usuario usuario, String tipoAccesoNombre, String sesionId, String detalle, HttpServletRequest request) {
        BitacoraAcceso bitacora = new BitacoraAcceso();
        bitacora.setUsuario(usuario);
        bitacora.setTipoAcceso(tipoAccesoRepository.findFirstByNombreIgnoreCase(tipoAccesoNombre).orElse(null));
        bitacora.setFechaAcceso(LocalDateTime.now());
        bitacora.setAcceso(detalle);
        bitacora.setSesion(sesionId);
        if (request != null) {
            bitacora.setDireccionIp(request.getRemoteAddr());
            bitacora.setHttpUserAgent(request.getHeader("User-Agent"));
        }
        bitacoraAccesoRepository.save(bitacora);
    }
}
