package com.couplestory.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the settings every non-dev deployment inherits from application.yml. The "test" profile
 * stands for "anything that is not dev": it must not expose API docs or log SQL parameters.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionConfigTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Environment env;

    @Test
    void apiDocsAreNotServedOutsideDev() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    void swaggerUiIsNotServedOutsideDev() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void sqlParametersAreNotLoggedOutsideDev() {
        assertThat(env.getProperty("logging.level.org.hibernate.type.descriptor.sql.BasicBinder")).isNull();
        assertThat(env.getProperty("logging.level.org.hibernate.SQL")).isNotIn("DEBUG", "TRACE");
        assertThat(env.getProperty("spring.jpa.show-sql", Boolean.class, false)).isFalse();
    }

    @Test
    void everyLogLineCarriesTheRequestId() {
        assertThat(env.getProperty("logging.pattern.level")).contains("requestId");
    }

    @Test
    void uploadLimitMatchesThePhotoLimit() {
        assertThat(env.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("10MB");
        assertThat(env.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("12MB");
    }

    @Test
    void flywayBaselineSettingsBelongToSpringFlyway() {
        assertThat(env.getProperty("spring.flyway.baseline-on-migrate", Boolean.class)).isTrue();
        assertThat(env.getProperty("spring.flyway.baseline-version")).isEqualTo("0");
    }
}
