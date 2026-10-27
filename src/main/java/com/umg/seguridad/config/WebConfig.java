package com.umg.seguridad.config;

import com.umg.seguridad.entity.*;
import com.umg.seguridad.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cuando un <select> en un formulario Thymeleaf esta ligado a una entidad completa
 * (th:field="*{empresa}"), Spring solo recibe el ID como texto desde el navegador.
 * Estos Converters le dicen como convertir ese ID de vuelta a la entidad real
 * antes de asignarla al objeto (ej. Sucursal.empresa, Menu.modulo, Opcion.menu,
 * Usuario.statusUsuario/genero/sucursal/role).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final EmpresaRepository empresaRepository;
    private final ModuloRepository moduloRepository;
    private final MenuRepository menuRepository;
    private final StatusUsuarioRepository statusUsuarioRepository;
    private final GeneroRepository generoRepository;
    private final SucursalRepository sucursalRepository;
    private final RoleRepository roleRepository;
    private final EstadoCivilRepository estadoCivilRepository;
    private final StatusEmpleadoRepository statusEmpleadoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final DepartamentoRepository departamentoRepository;
    private final PersonaRepository personaRepository;
    private final PuestoRepository puestoRepository;
    private final BancoRepository bancoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PermisoInterceptor permisoInterceptor;

    public WebConfig(EmpresaRepository empresaRepository, ModuloRepository moduloRepository,
                      MenuRepository menuRepository, StatusUsuarioRepository statusUsuarioRepository,
                      GeneroRepository generoRepository, SucursalRepository sucursalRepository,
                      RoleRepository roleRepository, EstadoCivilRepository estadoCivilRepository, StatusEmpleadoRepository statusEmpleadoRepository, TipoDocumentoRepository tipoDocumentoRepository, DepartamentoRepository departamentoRepository, PersonaRepository personaRepository, PuestoRepository puestoRepository, BancoRepository bancoRepository, EmpleadoRepository empleadoRepository, PermisoInterceptor permisoInterceptor) {
        this.empresaRepository = empresaRepository;
        this.moduloRepository = moduloRepository;
        this.menuRepository = menuRepository;
        this.statusUsuarioRepository = statusUsuarioRepository;
        this.generoRepository = generoRepository;
        this.sucursalRepository = sucursalRepository;
        this.roleRepository = roleRepository;
        this.estadoCivilRepository = estadoCivilRepository;
        this.statusEmpleadoRepository = statusEmpleadoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.departamentoRepository = departamentoRepository;
        this.personaRepository = personaRepository;
        this.puestoRepository = puestoRepository;
        this.bancoRepository = bancoRepository;
        this.empleadoRepository = empleadoRepository;
        this.permisoInterceptor = permisoInterceptor;
    }

    // Registra PermisoInterceptor para que corra en cada request a un @Controller.
    // Los recursos estaticos y las rutas publicas del login ya estan fuera de esto
    // (Spring Security las deja pasar antes de llegar aca, y ademas estan excluidas
    // explicitamente para evitar loops raros con /login o /css).
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permisoInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/logout", "/css/**", "/js/**", "/webjars/**");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Fase 2 (planilla): combos de EstadoCivil, StatusEmpleado, Persona, Puesto, etc.
        registrar(registry, EstadoCivil.class, estadoCivilRepository);
        registrar(registry, StatusEmpleado.class, statusEmpleadoRepository);
        registrar(registry, TipoDocumento.class, tipoDocumentoRepository);
        registrar(registry, Departamento.class, departamentoRepository);
        registrar(registry, Persona.class, personaRepository);
        registrar(registry, Puesto.class, puestoRepository);
        registrar(registry, Banco.class, bancoRepository);
        registrar(registry, Empleado.class, empleadoRepository);

        registry.addConverter(new Converter<String, Empresa>() {
            @Override
            public Empresa convert(String id) {
                if (id == null || id.isBlank()) return null;
                return empresaRepository.findById(Integer.valueOf(id)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, Modulo>() {
            @Override
            public Modulo convert(String id) {
                if (id == null || id.isBlank()) return null;
                return moduloRepository.findById(Integer.valueOf(id)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, Menu>() {
            @Override
            public Menu convert(String id) {
                if (id == null || id.isBlank()) return null;
                return menuRepository.findById(Integer.valueOf(id)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, StatusUsuario>() {
            @Override
            public StatusUsuario convert(String id) {
                if (id == null || id.isBlank()) return null;
                return statusUsuarioRepository.findById(Integer.valueOf(id)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, Genero>() {
            @Override
            public Genero convert(String id) {
                if (id == null || id.isBlank()) return null;
                return generoRepository.findById(Integer.valueOf(id)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, Sucursal>() {
            @Override
            public Sucursal convert(String id) {
                if (id == null || id.isBlank()) return null;
                return sucursalRepository.findById(Integer.valueOf(id)).orElse(null);
            }
        });

        registry.addConverter(new Converter<String, Role>() {
            @Override
            public Role convert(String id) {
                if (id == null || id.isBlank()) return null;
                return roleRepository.findById(Integer.valueOf(id)).orElse(null);
            }
        });
    }

    /** Converter generico String(id) -> entidad, para los combos de la fase 2. Vacio => null. */
    private <T> void registrar(FormatterRegistry registry, Class<T> tipo, JpaRepository<T, Integer> repository) {
        registry.addConverter(String.class, tipo, id -> {
            if (id == null || id.isBlank()) {
                return null;
            }
            return repository.findById(Integer.valueOf(id.trim())).orElse(null);
        });
    }
}
