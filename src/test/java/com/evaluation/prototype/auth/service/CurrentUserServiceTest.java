package com.evaluation.prototype.auth.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.UserRepository;

class CurrentUserServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reusesAuthenticatedUserInsteadOfLoadingTheSameUserAgain() {
        User user = new User();
        user.setUsername("admin");
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        UserRepository repository = mock(UserRepository.class);

        var currentUser = new CurrentUserService(repository).getCurrentUser();

        assertSame(user, currentUser.orElseThrow());
        verifyNoInteractions(repository);
    }
}
