package com.evaluation.prototype.auth.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.evaluation.prototype.common.enums.UserStatus;
import com.evaluation.prototype.user.entity.User;

public final class AuthenticatedUserDetails implements UserDetails {

    private final User user;
    private final Set<GrantedAuthority> authorities;

    public AuthenticatedUserDetails(User user, Set<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = Set.copyOf(authorities);
    }

    public User getUser() {
        return user;
    }

    public UUID getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
