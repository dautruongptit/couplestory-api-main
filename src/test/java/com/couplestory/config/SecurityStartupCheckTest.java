package com.couplestory.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityStartupCheckTest {

    private static String secretOfBytes(int length) {
        return Base64.getEncoder().encodeToString("k".repeat(length).getBytes(StandardCharsets.UTF_8));
    }

    private static final Set<String> PROD = Set.of();

    @Test
    void acceptsAStrongSecretWithSecureCookiesInProduction() {
        assertThatCode(() -> SecurityStartupCheck.validate(secretOfBytes(48), true, PROD)).doesNotThrowAnyException();
    }

    @Test
    void rejectsASecretShorterThan32Bytes() {
        assertThatThrownBy(() -> SecurityStartupCheck.validate(secretOfBytes(16), true, PROD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void rejectsASecretThatIsNotBase64() {
        assertThatThrownBy(() -> SecurityStartupCheck.validate("not base64 !!!", true, PROD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void rejectsAMissingSecret() {
        assertThatThrownBy(() -> SecurityStartupCheck.validate("", true, PROD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void rejectsInsecureCookiesOutsideDevAndTest() {
        assertThatThrownBy(() -> SecurityStartupCheck.validate(secretOfBytes(48), false, Set.of("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cookie");
    }

    @Test
    void allowsInsecureCookiesInDevAndTest() {
        assertThatCode(() -> SecurityStartupCheck.validate(secretOfBytes(48), false, Set.of("dev"))).doesNotThrowAnyException();
        assertThatCode(() -> SecurityStartupCheck.validate(secretOfBytes(48), false, Set.of("test"))).doesNotThrowAnyException();
    }

    @Test
    void neverPutsTheSecretInTheErrorMessage() {
        String weak = secretOfBytes(16);

        assertThatThrownBy(() -> SecurityStartupCheck.validate(weak, true, PROD))
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(weak).doesNotContain("kkkk"));
    }
}
