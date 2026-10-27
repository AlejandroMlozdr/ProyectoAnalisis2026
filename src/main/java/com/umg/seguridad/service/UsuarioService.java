package com.umg.seguridad.service;

import com.umg.seguridad.entity.Empresa;
import com.umg.seguridad.entity.Usuario;
import com.umg.seguridad.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    // ---------- Recuperacion de password (pregunta de seguridad) ----------

    @Transactional(readOnly = true)
    public Usuario obtenerParaRecuperacion(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar el usuario.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("No fue posible validar los datos ingresados."));

        if (!"Activo".equalsIgnoreCase(usuario.getStatusUsuario().getNombre())) {
            throw new IllegalArgumentException("No fue posible validar los datos ingresados.");
        }
        if (usuario.getPregunta() == null || usuario.getPregunta().isBlank()
                || usuario.getRespuesta() == null || usuario.getRespuesta().isBlank()) {
            throw new IllegalArgumentException("El usuario no tiene configurada una pregunta de seguridad.");
        }

        return usuario;
    }

    @Transactional
    public void restablecerPassword(String idUsuario, String respuesta, String nuevaPassword) {
        Usuario usuario = obtenerParaRecuperacion(idUsuario);

        if (respuesta == null
                || !usuario.getRespuesta().trim().equalsIgnoreCase(respuesta.trim())) {
            throw new IllegalArgumentException("La respuesta de seguridad es incorrecta.");
        }
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new IllegalArgumentException("Debe ingresar una contraseña nueva.");
        }

        Empresa empresa = usuario.getSucursal().getEmpresa();
        validarPoliticaPassword(nuevaPassword, empresa);

        LocalDateTime ahora = LocalDateTime.now();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setUltimaFechaCambioPassword(ahora);
        usuario.setIntentosDeAcceso(0);
        usuario.setRequiereCambiarPassword(0);
        usuario.setFechaModificacion(ahora);
        usuario.setUsuarioModificacion("RECUPERACION_PASSWORD");
        usuarioRepository.save(usuario);
    }

    /**
     * Guarda un usuario nuevo o edita uno existente.
     * - Si es nuevo, el password (texto plano) es obligatorio.
     * - Si es edicion y el password llega vacio, se conserva el password actual
     *   (asi el formulario de edicion no obliga a escribir password de nuevo).
     * - El password se valida contra las politicas de la empresa (a traves de
     *   Sucursal -> Empresa) y se encripta con BCrypt antes de guardar.
     */
    @Transactional
    public Usuario guardar(Usuario form, String passwordPlano) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Usuario> existenteOpt = usuarioRepository.findById(form.getIdUsuario());

        Usuario usuario;
        boolean esNuevo = existenteOpt.isEmpty();

        if (esNuevo) {
            usuario = new Usuario();
            usuario.setIdUsuario(form.getIdUsuario());
            usuario.setFechaCreacion(LocalDateTime.now());
            usuario.setUsuarioCreacion(usuarioActual);
            usuario.setIntentosDeAcceso(0);
        } else {
            usuario = existenteOpt.get();
            usuario.setFechaModificacion(LocalDateTime.now());
            usuario.setUsuarioModificacion(usuarioActual);
        }

        usuario.setNombre(form.getNombre());
        usuario.setApellido(form.getApellido());
        usuario.setFechaNacimiento(form.getFechaNacimiento());
        usuario.setStatusUsuario(form.getStatusUsuario());
        usuario.setGenero(form.getGenero());
        usuario.setSucursal(form.getSucursal());
        usuario.setRole(form.getRole());
        usuario.setCorreoElectronico(form.getCorreoElectronico());
        usuario.setTelefonoMovil(form.getTelefonoMovil());
        usuario.setPregunta(form.getPregunta());
        usuario.setRespuesta(form.getRespuesta());
        usuario.setRequiereCambiarPassword(form.getRequiereCambiarPassword());

        if (passwordPlano != null && !passwordPlano.isBlank()) {
            Empresa empresa = usuario.getSucursal().getEmpresa();
            validarPoliticaPassword(passwordPlano, empresa);
            usuario.setPassword(passwordEncoder.encode(passwordPlano));
            usuario.setUltimaFechaCambioPassword(LocalDateTime.now());
        } else if (esNuevo) {
            throw new IllegalArgumentException("La contraseña es obligatoria para un usuario nuevo.");
        }
        // si es edicion y viene vacio, se conserva la password que ya tenia "usuario" (viene de la BD)

        return usuarioRepository.save(usuario);
    }

    public void eliminar(String id) {
        usuarioRepository.deleteById(id);
    }

    /**
     * Fuerza la caducidad del password de un usuario (boton "Forzar cambio de
     * contraseña" en el listado), sin tener que abrir el formulario completo.
     * Marca RequiereCambiarPassword = 1; en el proximo intento de login,
     * UsuarioDetailsService.passwordVigente(...) lo detecta y Spring Security
     * dispara CredentialsExpiredException, que CustomAuthenticationFailureHandler
     * redirige a /cambiar-password.
     */
    @Transactional
    public void marcarPasswordVencida(String idUsuario) {
        Usuario usuario = obtenerPorId(idUsuario);
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();

        usuario.setRequiereCambiarPassword(1);
        usuario.setFechaModificacion(LocalDateTime.now());
        usuario.setUsuarioModificacion(usuarioActual);
        usuarioRepository.save(usuario);
    }

    /**
     * Valida el password en texto plano contra las politicas configuradas
     * en la Empresa del usuario (EMPRESA.PasswordCantidadMayusculas, etc).
     * Si algun campo de politica es null, no se exige esa regla.
     */
    private void validarPoliticaPassword(String password, Empresa empresa) {
        if (empresa == null) return;

        if (empresa.getPasswordLargo() != null && password.length() < empresa.getPasswordLargo()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + empresa.getPasswordLargo() + " caracteres.");
        }
        if (empresa.getPasswordCantidadMayusculas() != null
                && contarCoincidencias(password, Character::isUpperCase) < empresa.getPasswordCantidadMayusculas()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + empresa.getPasswordCantidadMayusculas() + " mayúscula(s).");
        }
        if (empresa.getPasswordCantidadMinusculas() != null
                && contarCoincidencias(password, Character::isLowerCase) < empresa.getPasswordCantidadMinusculas()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + empresa.getPasswordCantidadMinusculas() + " minúscula(s).");
        }
        if (empresa.getPasswordCantidadNumeros() != null
                && contarCoincidencias(password, Character::isDigit) < empresa.getPasswordCantidadNumeros()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + empresa.getPasswordCantidadNumeros() + " número(s).");
        }
        if (empresa.getPasswordCantidadCaracteresEspeciales() != null) {
            long especiales = contarCoincidencias(password, c -> !Character.isLetterOrDigit(c));
            if (especiales < empresa.getPasswordCantidadCaracteresEspeciales()) {
                throw new IllegalArgumentException("La contraseña debe tener al menos " + empresa.getPasswordCantidadCaracteresEspeciales() + " carácter(es) especial(es).");
            }
        }
    }

    private long contarCoincidencias(String texto, java.util.function.Predicate<Character> condicion) {
        return texto.chars().mapToObj(c -> (char) c).filter(condicion).count();
    }
    @Transactional
    public void cambiarPasswordPorCaducidad(String idUsuario, String passwordActual, String passwordNueva){
        Usuario usuario = usuarioRepository.findById(idUsuario)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if( !passwordEncoder.matches (passwordActual, usuario.getPassword())){
            throw new IllegalArgumentException ("la contraseña actual es incorrecta");

        }
        Empresa empresa = usuario.getSucursal().getEmpresa();
        validarPoliticaPassword(passwordNueva, empresa);

        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuario.setUltimaFechaCambioPassword(LocalDateTime.now());
        usuario.setRequiereCambiarPassword(0);
        usuarioRepository.save(usuario);
    }
}
