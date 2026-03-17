package com.revplay.musicplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("unit")
class SecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder returns bcrypt implementation")
    void passwordEncoderReturnsBcrypt() {
        SecurityConfig securityConfig = new SecurityConfig(null);

        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        String encoded = passwordEncoder.encode("password-123");
        assertThat(passwordEncoder.matches("password-123", encoded)).isTrue();
    }
}
