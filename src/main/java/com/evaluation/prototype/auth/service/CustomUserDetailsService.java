package com.evaluation.prototype.auth.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.evaluation.prototype.common.enums.UserStatus;
import com.evaluation.prototype.user.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> {
                    Set<SimpleGrantedAuthority> authorities = new HashSet<>();
                    user.getRoles().stream()
                            .filter(role -> role.isEnabled() || "ROLE_ADMIN".equals(role.getName()))
                            .forEach(role -> {
                                authorities.add(new SimpleGrantedAuthority(role.getName()));
                                role.getPermissions().stream()
                                        .filter(permission -> permission.isEnabled() || "ROLE_ADMIN".equals(role.getName()))
                                        .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                                        .forEach(authorities::add);
                            });

                    return org.springframework.security.core.userdetails.User.builder()
                            .username(user.getUsername())
                            .password(user.getPasswordHash())
                            .disabled(!user.isEnabled())
                            .accountLocked(user.getStatus() == UserStatus.LOCKED)
                            .authorities(authorities)
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
