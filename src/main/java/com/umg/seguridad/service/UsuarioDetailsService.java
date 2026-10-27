package com.umg.seguridad.service;

import com.umg.seguridad.entity.Usuario;
import com.umg.seguridad.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        boolean habilitado = "Activo".equalsIgnoreCase(usuario.getStatusUsuario().getNombre());

        return User.builder()
                .username(usuario.getIdUsuario())
                .password(usuario.getPassword())
                .disabled(!habilitado)
                .credentialsExpired(!passwordVigente(usuario))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().getNombre().toUpperCase())))
                .build();
    }

    private boolean passwordVigente(Usuario usuario) {
        if (usuario.getRequiereCambiarPassword() != null && usuario.getRequiereCambiarPassword() == 1) {
            return false;
        }
        Integer diasCaducidad = usuario.getSucursal().getEmpresa().getPasswordCantidadCaducidadDias();
        if (diasCaducidad == null) {
            return true;
        }
        if (usuario.getUltimaFechaCambioPassword() == null) {
            return false;
        }
        LocalDateTime fechaExpiracion = usuario.getUltimaFechaCambioPassword().plusDays(diasCaducidad);
        return LocalDateTime.now().isBefore(fechaExpiracion);
    }
}