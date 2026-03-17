package com.revplay.musicplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Tag("unit")
class CorsConfigTest {

    @Test
    @DisplayName("corsConfigurer registers permissive global cors mapping")
    void corsConfigurerRegistersPermissiveGlobalCorsMapping() {
        CorsConfig corsConfig = new CorsConfig();
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();
        CorsRegistry registry = new CorsRegistry();

        configurer.addCorsMappings(registry);

        @SuppressWarnings("unchecked")
        List<CorsRegistration> registrations =
                (List<CorsRegistration>) ReflectionTestUtils.getField(registry, "registrations");

        assertThat(registrations).hasSize(1);

        CorsRegistration registration = registrations.getFirst();
        assertThat(ReflectionTestUtils.getField(registration, "pathPattern")).isEqualTo("/**");

        CorsConfiguration configuration =
                (CorsConfiguration) ReflectionTestUtils.getField(registration, "config");

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("*");
        assertThat(configuration.getAllowedMethods()).containsExactly("*");
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
    }
}
