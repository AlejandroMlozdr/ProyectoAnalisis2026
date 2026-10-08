package com.umg.seguridad.repository;

import com.umg.seguridad.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

// La llave primaria de Usuario es el propio username (String), no un int autoincremental.
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
}
