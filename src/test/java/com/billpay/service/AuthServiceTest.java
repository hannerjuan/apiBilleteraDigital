package com.billpay.service;

import com.billpay.dto.request.LoginRequest;
import com.billpay.dto.request.RegisterRequest;
import com.billpay.dto.response.AuthResponse;
import com.billpay.exception.DuplicateResourceException;
import com.billpay.model.Cuenta;
import com.billpay.model.EstadoCuenta;
import com.billpay.model.Usuario;
import com.billpay.repository.CuentaRepository;
import com.billpay.repository.UsuarioRepository;
import com.billpay.security.JwtService;
import com.billpay.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "defaultInitialBalance", BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario y crear su cuenta de 10 dígitos exitosamente")
    void register_Success() {
        RegisterRequest request = new RegisterRequest(
                "1234567890",
                "Juan Perez",
                "juan.perez@example.com",
                "Password123!",
                BigDecimal.valueOf(100.00)
        );

        when(usuarioRepository.existsByDocumentoIdentidad("1234567890")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan.perez@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), any())).thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("juan.perez@example.com", response.getEmail());
        assertEquals("Juan Perez", response.getNombreCompleto());
        assertEquals("mocked.jwt.token", response.getToken());
        assertNotNull(response.getNumeroCuenta());
        assertEquals(10, response.getNumeroCuenta().length());

        verify(usuarioRepository).save(any(Usuario.class));
        verify(cuentaRepository).save(any(Cuenta.class));
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException cuando el documento ya existe")
    void register_DuplicateDocument() {
        RegisterRequest request = new RegisterRequest(
                "1234567890",
                "Juan Perez",
                "juan.perez@example.com",
                "Password123!",
                BigDecimal.ZERO
        );

        when(usuarioRepository.existsByDocumentoIdentidad("1234567890")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(usuarioRepository, never()).save(any());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar DuplicateResourceException cuando el correo ya existe")
    void register_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "1234567890",
                "Juan Perez",
                "juan.perez@example.com",
                "Password123!",
                BigDecimal.ZERO
        );

        when(usuarioRepository.existsByDocumentoIdentidad("1234567890")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan.perez@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe autenticar correctamente con credenciales válidas y retornar JWT")
    void login_Success() {
        LoginRequest request = new LoginRequest("juan.perez@example.com", "Password123!");

        Usuario usuario = new Usuario("1234567890", "Juan Perez", "juan.perez@example.com", "hashed_pwd");
        usuario.setId(1L);
        Cuenta cuenta = new Cuenta(usuario, "1234567890", BigDecimal.valueOf(500.00), EstadoCuenta.ACTIVA);
        usuario.setCuenta(cuenta);

        UserPrincipal principal = new UserPrincipal(usuario);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(usuarioRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any(), any())).thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("juan.perez@example.com", response.getEmail());
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("1234567890", response.getNumeroCuenta());
    }

    @Test
    @DisplayName("Debe propagar BadCredentialsException cuando las credenciales no son válidas")
    void login_InvalidCredentials() {
        LoginRequest request = new LoginRequest("juan.perez@example.com", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
