package com.umg.seguridad.config;

import com.umg.seguridad.security.SesionAuthenticationSuccessHandler;
import com.umg.seguridad.security.SesionLogoutFilter;
import com.umg.seguridad.service.UsuarioDetailsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;
    private final CustomAuthenticationFailureHandler authenticationFailureHandler;
    private final SesionAuthenticationSuccessHandler sesionAuthenticationSuccessHandler;
    private final SesionLogoutFilter sesionLogoutFilter;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService,
                           CustomAuthenticationFailureHandler authenticationFailureHandler,
                           SesionAuthenticationSuccessHandler sesionAuthenticationSuccessHandler,
                           SesionLogoutFilter sesionLogoutFilter) {
        this.usuarioDetailsService = usuarioDetailsService;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.sesionAuthenticationSuccessHandler = sesionAuthenticationSuccessHandler;
        this.sesionLogoutFilter = sesionLogoutFilter;
    }

    // Necesario para que Spring Security se entere cuando una sesion HTTP muere
    // (timeout, invalidate manual, etc.) y mantenga al dia el registro de
    // sesiones activas que usa la politica de "sesion unica" (maximumSessions).
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // SesionLogoutFilter es un @Component (necesita repositorios inyectados) que
    // ademas se agrega a mano a la cadena de Spring Security con addFilterBefore.
    // Sin esto, Spring Boot lo registraria TAMBIEN como filtro generico de Tomcat
    // y correria duplicado en cada logout.
    @Bean
    public FilterRegistrationBean<SesionLogoutFilter> sesionLogoutFilterRegistration() {
        FilterRegistrationBean<SesionLogoutFilter> registration = new FilterRegistrationBean<>(sesionLogoutFilter);
        registration.setEnabled(false);
        return registration;
    }

    // Las contrasenas NUNCA se guardan en texto plano: se encriptan con BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Sin esto, Spring "disfraza" el error de usuario inexistente como password
        // incorrecta (BadCredentialsException), y CustomAuthenticationFailureHandler
        // no puede distinguir el evento "Usuario no existe" del de "Login fallido".
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/cambiar-password", "/recuperar-password/**", "/css/**", "/js/**").permitAll()
                // La bitacora de auditoria no esta modelada como Opcion/RoleOpcion (ver README),
                // asi que su proteccion sigue siendo un hasRole fijo.
                .requestMatchers("/auditoria/**").hasRole("ADMINISTRADOR")
                // El resto de pantallas (Empresa, Sucursal, Role, Modulo, Usuario, etc.) ya NO
                // se restringen aqui por nombre de rol: eso obligaria a que solo el rol literal
                // "ADMINISTRADOR" pudiera usarlas, sin importar lo que el admin configure en
                // "Asignar Opciones a un Rol". Esa autorizacion fina (Consultar/Alta/Baja/Cambio/
                // Imprimir/Exportar por pagina, para cualquier rol) la aplica PermisoInterceptor
                // en cada request, usando PermisoService/ROLE_OPCION.
                .anyRequest().authenticated()
            )
            // Gestion de sesiones: regenera el id de sesion en cada login (previene
            // session fixation), redirige con un mensaje cuando la sesion ya no es
            // valida (expiro por inactividad) y permite una unica sesion activa por
            // usuario -consistente con Usuario.sesionActual, que guarda un solo id-.
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .invalidSessionUrl("/login?expired")
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired")
                .sessionRegistry(sessionRegistry())
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(sesionAuthenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // Corre antes que el LogoutFilter para dejar limpio Usuario.sesionActual
            // y registrada la bitacora de "Logout" mientras la sesion HTTP aun es valida.
            .addFilterBefore(sesionLogoutFilter, LogoutFilter.class)
            .csrf(AbstractHttpConfigurer::disable); // valorar activarlo de nuevo cuando tengan los formularios con token CSRF

        return http.build();
    }
}
