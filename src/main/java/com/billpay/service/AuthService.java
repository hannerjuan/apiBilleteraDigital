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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.banking.initial-account-balance:0.00}")
    private BigDecimal defaultInitialBalance;

    public AuthService(UsuarioRepository usuarioRepository,
                       CuentaRepository cuentaRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByDocumentoIdentidad(request.getDocumentoIdentidad())) {
            throw new DuplicateResourceException("El documento de identidad " + request.getDocumentoIdentidad() + " ya se encuentra registrado.");
        }

        if (usuarioRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException("El correo electrónico " + request.getEmail() + " ya se encuentra registrado.");
        }

        // Crear nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setDocumentoIdentidad(request.getDocumentoIdentidad().trim());
        usuario.setNombreCompleto(request.getNombreCompleto().trim());
        usuario.setEmail(request.getEmail().toLowerCase().trim());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setFechaRegistro(LocalDateTime.now());

        Usuario savedUsuario = usuarioRepository.save(usuario);

        // Generar número de cuenta único de 10 dígitos numéricos
        String numeroCuenta = generateUniqueAccountNumber();

        BigDecimal balanceInicial = request.getSaldoInicial() != null && request.getSaldoInicial().compareTo(BigDecimal.ZERO) >= 0
                ? request.getSaldoInicial()
                : defaultInitialBalance;

        Cuenta cuenta = new Cuenta(savedUsuario, numeroCuenta, balanceInicial, EstadoCuenta.ACTIVA);
        cuentaRepository.save(cuenta);
        savedUsuario.setCuenta(cuenta);

        // Generar token JWT
        UserPrincipal principal = new UserPrincipal(savedUsuario);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", savedUsuario.getId());
        extraClaims.put("numeroCuenta", numeroCuenta);
        extraClaims.put("nombreCompleto", savedUsuario.getNombreCompleto());

        String jwtToken = jwtService.generateToken(principal, extraClaims);

        return new AuthResponse(
                jwtToken,
                savedUsuario.getEmail(),
                savedUsuario.getNombreCompleto(),
                numeroCuenta,
                jwtService.getExpirationTime()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Error al recuperar los datos del usuario autenticado."));

        String numeroCuenta = usuario.getCuenta() != null ? usuario.getCuenta().getNumeroCuenta() : null;

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("numeroCuenta", numeroCuenta);
        extraClaims.put("nombreCompleto", usuario.getNombreCompleto());

        String jwtToken = jwtService.generateToken(principal, extraClaims);

        return new AuthResponse(
                jwtToken,
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                numeroCuenta,
                jwtService.getExpirationTime()
        );
    }

    /**
     * Generates a unique 10-digit numerical account number.
     * Guaranteed to be exactly 10 digits and not collide with any existing account.
     */
    private String generateUniqueAccountNumber() {
        String numeroCuenta;
        do {
            // First digit between 1 and 9 to ensure exactly 10 digits
            long number = 1000000000L + (long) (secureRandom.nextDouble() * 9000000000L);
            numeroCuenta = String.valueOf(number);
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }
}
