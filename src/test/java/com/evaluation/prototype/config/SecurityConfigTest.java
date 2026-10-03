package com.evaluation.prototype.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void corsConfigurationKeepsFrontendOriginWhenEnvironmentOverridesOrigins() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource("https://custom.example.com");
        CorsConfiguration configuration = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/admin/dashboard"));

        assertEquals("https://des-frontend-nu.vercel.app",
                configuration.checkOrigin("https://des-frontend-nu.vercel.app"));
        assertEquals("https://custom.example.com", configuration.checkOrigin("https://custom.example.com"));
        assertTrue(configuration.checkHttpMethod(HttpMethod.OPTIONS).contains(HttpMethod.OPTIONS));
        assertTrue(configuration.checkHeaders(List.of("authorization", "content-type")) != null);
    }
}
