package com.billpay.security;

import com.billpay.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String nombreCompleto;
    private final String numeroCuenta;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.password = usuario.getPasswordHash();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.numeroCuenta = usuario.getCuenta() != null ? usuario.getCuenta().getNumeroCuenta() : null;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
